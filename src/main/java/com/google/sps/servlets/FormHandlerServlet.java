package com.google.sps.servlets;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.protobuf.ByteString;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import com.google.gson.Gson;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.sps.data.Note;
import com.google.sps.data.Session;

/**
 * When the user submits the form, Blobstore processes the file upload and then forwards the request
 * to this servlet. This servlet can then process the request using the file URL we get from
 * Blobstore.
 */
@WebServlet("/form-handler")
public class FormHandlerServlet extends HttpServlet {
  private DatastoreService datastore;
    
  @Override
  public void init() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  /** Load notes from Datastore */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    Query query = new Query("Note").addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);

    List<Note> notes = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String imageUrl = (String) entity.getProperty("imageUrl");
      String message = (String) entity.getProperty("message");
      ArrayList<String> categories = (ArrayList) entity.getProperty("categories");

      Note note = new Note(id, imageUrl, message, categories);
    
      notes.add(note);
    }

    // Convert the list of notes to JSON.
    Gson gson = new Gson();
    String json = gson.toJson(notes);

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /** Add new notes to Datastore. */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long timestamp = System.currentTimeMillis();

    Entity sessionEntity = new Entity("Session");
    sessionEntity.setProperty("outputFile", null);
    sessionEntity.setProperty("timestamp", timestamp);
    datastore.put(sessionEntity);

    BlobKey blobKey = getBlobKey(request, "image");
    String imageUrl = getUploadedFileUrl(blobKey);
    byte[] imageBytes = getBlobBytes(blobKey);

    Key sessionEntityKey = sessionEntity.getKey();
    Note note = new Note(sessionEntityKey, imageUrl, imageBytes);

    Entity noteEntity = new Entity("Note");
    noteEntity.setProperty("sessionKey", sessionEntityKey);
    noteEntity.setProperty("imageUrl", imageUrl);
    noteEntity.setProperty("message", note.getMessage());
    noteEntity.setProperty("categories", note.classifyText());
    noteEntity.setProperty("timestamp", timestamp);
    

    datastore.put(noteEntity);

    try {
        note.writeConvertedDoc();
    } catch (Docx4JException e) {
        System.out.println(e);
    }

    Session session = new Session();
    String objectName = note.getFileName() + ".docx";

    session.uploadObject(objectName, note.getFilePath());
    session.generateV4GetObjectSignedUrl(objectName);
    sessionEntity.setProperty("outputFile", session.getOutputDoc());
    datastore.put(sessionEntity);

    response.sendRedirect("/output.html");
  }

  /** Returns a URL that points to the uploaded file, or null if the user didn't upload a file. */
  private String getUploadedFileUrl(BlobKey blobKey) {
    // We could check the validity of the file here, e.g. to make sure it's an image file
    // https://stackoverflow.com/q/10779564/873165

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);
    String url = imagesService.getServingUrl(options);

    // GCS's localhost preview is not actually on localhost,
    // so make the URL relative to the current domain.
    if(url.startsWith("http://localhost:8080/")){
      url = url.replace("http://localhost:8080/", "/");
    } 
    return url;
  }

  /**
   * Returns the BlobKey that points to the file uploaded by the user, or null if the user didn't
   * upload a file.
   */
  private BlobKey getBlobKey(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get(formInputElementName);

    // User submitted form without selecting a file, so we can't get a BlobKey. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so the BlobKey is empty. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    return blobKey;
  }

  /**
   * Blobstore stores files as binary data. This function retrieves the binary data stored at the
   * BlobKey parameter.
   */
  private byte[] getBlobBytes(BlobKey blobKey) throws IOException {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();

    int fetchSize = BlobstoreService.MAX_BLOB_FETCH_SIZE;
    long currentByteIndex = 0;
    boolean continueReading = true;
    while (continueReading) {
      // end index is inclusive, so we have to subtract 1 to get fetchSize bytes
      byte[] b =
          blobstoreService.fetchData(blobKey, currentByteIndex, currentByteIndex + fetchSize - 1);
      outputBytes.write(b);

      // if we read fewer bytes than we requested, then we reached the end
      if (b.length < fetchSize) {
        continueReading = false;
      }

      currentByteIndex += fetchSize;
    }

    return outputBytes.toByteArray();
  }
}