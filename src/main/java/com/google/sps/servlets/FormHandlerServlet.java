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
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
<<<<<<< HEAD
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.Word;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.protobuf.ByteString;
=======
import org.docx4j.openpackaging.exceptions.Docx4JException;
>>>>>>> Write test file
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

/**
 * When the user submits the form, Blobstore processes the file upload and then forwards the request
 * to this servlet. This servlet can then process the request using the file URL we get from
 * Blobstore.
 */
@WebServlet("/form-handler")
public class FormHandlerServlet extends HttpServlet {

  /** Load notes from Datastore */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    Query query = new Query("Note").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    List<Note> notes = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String imageUrl = (String) entity.getProperty("imageUrl");
      long timestamp = (long) entity.getProperty("timestamp");
      String message = (String) entity.getProperty("message");
      Note note = new Note(id, imageUrl, timestamp, message);

      try {
        note.writeConvertedDoc();
      } catch (Docx4JException e){
        System.out.println(e);
      }
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
    
    String imageUrl = getUploadedFileUrl(request, "image");
    long timestamp = System.currentTimeMillis();
    String message = detectDocumentText(imageUrl);
    Entity noteEntity = new Entity("Note");
    noteEntity.setProperty("imageUrl", imageUrl);
    noteEntity.setProperty("timestamp", timestamp);
    noteEntity.setProperty("message", message);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(noteEntity);

    response.sendRedirect("/index.html");
  }

  /** Returns a URL that points to the uploaded file, or null if the user didn't upload a file. */
  private String getUploadedFileUrl(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a URL. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so we can't get a URL. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

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
   * Detects words from a picture and returns them
   * TODO: Clean up the result, sometimes the api misreads, misses entirely, or adds additional words
   *        
   */
  private static String detectDocumentText(String path) throws IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();
    //This only works when you publish, api can not read local urls
    ImageSource imgSource = ImageSource.newBuilder().setImageUri(path).build(); 
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the "close" method on the client to safely clean up any remaining background resources.
    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();
      client.close();
      
      // Check to see if any of the responses are errors
      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          System.out.format("Error: %s%n", res.getError().getMessage());
          return "Error: " + res.getError().getMessage();
        }
    
        // For full list of available annotations, see http://g.co/cloud/vision/docs
        TextAnnotation annotation = res.getFullTextAnnotation();
        for (Page page : annotation.getPagesList()) {
          String pageText = "";
            for (Block block : page.getBlocksList()) {
            String blockText = "";
            for (Paragraph para : block.getParagraphsList()) {
              String paraText = "";
              for (Word word : para.getWordsList()) {
              String wordText = "";
              for (Symbol symbol : word.getSymbolsList()) {
                wordText = wordText + symbol.getText();
                System.out.format(
                  "Symbol text: %s (confidence: %f)%n",
                  symbol.getText(), symbol.getConfidence());
                }
                System.out.format(
                  "Word text: %s (confidence: %f)%n%n", wordText, word.getConfidence());
                  paraText = String.format("%s %s", paraText, wordText);
                }
                // Output Example using Paragraph:
                System.out.println("%nParagraph: %n" + paraText);
                System.out.format("Paragraph Confidence: %f%n", para.getConfidence());
                blockText = blockText + paraText;
            }
            pageText = pageText + blockText;
            }
        }
        return annotation.getText();
      }
    }
    catch(Exception e) {
      return "ERROR: ImageAnnotatorClient Failed, " + e;
    }
    // Case where the ImageAnnotatorClient works, but there are no responses from it.
    return "Error: No responses";
  }
}