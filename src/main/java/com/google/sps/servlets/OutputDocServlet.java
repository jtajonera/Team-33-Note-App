// package com.google.sps.servlets;

// import com.google.appengine.api.blobstore.BlobInfo;
// import com.google.appengine.api.blobstore.BlobInfoFactory;
// import com.google.appengine.api.blobstore.BlobKey;
// import com.google.appengine.api.blobstore.BlobstoreService;
// import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
// import com.google.appengine.api.images.ImagesService;
// import com.google.appengine.api.images.ImagesServiceFactory;
// import com.google.appengine.api.images.ServingUrlOptions;
// import com.google.appengine.api.datastore.DatastoreService;
// import com.google.appengine.api.datastore.DatastoreServiceFactory;
// import com.google.appengine.api.datastore.Entity;
// import com.google.appengine.api.datastore.PreparedQuery;
// import com.google.appengine.api.datastore.Query;
// import com.google.appengine.api.datastore.Query.SortDirection;
// import org.docx4j.openpackaging.exceptions.Docx4JException;
// import java.io.IOException;
// import java.io.PrintWriter;
// import java.util.List;
// import java.util.ArrayList;
// import java.util.Map;
// import com.google.gson.Gson;
// import javax.servlet.annotation.WebServlet;
// import javax.servlet.http.HttpServlet;
// import javax.servlet.http.HttpServletRequest;
// import javax.servlet.http.HttpServletResponse;
// import com.google.sps.data.Note;

// /**
//  * When the user submits the form, Blobstore processes the file upload and then forwards the request
//  * to this servlet. This servlet can then process the request using the file URL we get from
//  * Blobstore.
//  */
// @WebServlet("/output-doc")
// public class FormHandlerServlet extends HttpServlet {

//   /** Load notes from Datastore */
//   @Override
//   public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
//     Query query = new Query("Note").addSort("timestamp", SortDirection.DESCENDING);

//     DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
//     PreparedQuery results = datastore.prepare(query);

//     List<Note> notes = new ArrayList<>();
//     for (Entity entity : results.asIterable()) {
//       long id = entity.getKey().getId();
//       String imageUrl = (String) entity.getProperty("imageUrl");
//       long timestamp = (long) entity.getProperty("timestamp");

//       Note note = new Note(id, imageUrl, timestamp);
//       try {
//         note.writeConvertedDoc();
//       } catch (Docx4JException e){
//         System.out.println(e);
//       }
//       notes.add(note);
//     }

//     // Convert the list of notes to JSON.
//     Gson gson = new Gson();
//     String json = gson.toJson(notes);

//     response.setContentType("application/json;");
//     response.getWriter().println(json);
//   }
// }