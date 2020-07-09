
// package com.google.sps.servlets;


// import com.google.appengine.api.images.ImagesService;
// import com.google.appengine.api.images.ImagesServiceFactory;
// import com.google.appengine.api.images.ServingUrlOptions;
// import com.google.appengine.api.datastore.DatastoreService;
// import com.google.appengine.api.datastore.DatastoreServiceFactory;
// import com.google.appengine.api.datastore.Entity;
// import com.google.appengine.api.datastore.PreparedQuery;
// import com.google.appengine.api.datastore.Query;
// import com.google.appengine.api.datastore.Query.SortDirection;
// import java.io.IOException;
// import java.io.PrintWriter;
// import java.util.List;
// import java.util.ArrayList;
// import com.google.gson.Gson;
// import javax.servlet.annotation.WebServlet;
// import javax.servlet.http.HttpServlet;
// import javax.servlet.http.HttpServletRequest;
// import javax.servlet.http.HttpServletResponse;
// import com.google.sps.data.Note;

// import FormHandlerServlet; //uses Lily's last method to extract image url

// /**
//  * Use this endpoint to get
//  */
// @WebServlet("/ocr-handler")
// public class FormHandlerServlet extends HttpServlet {

//   /** Grab note from Datastore */
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
//       notes.add(note);
//     }

//     // Convert the list of notes to JSON.
//     Gson gson = new Gson();
//     String json = gson.toJson(notes);

//     response.setContentType("application/json;");
//     response.getWriter().println(json);
//   }

//   /** Add new notes to Datastore. */
//   @Override
//   public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
//     String imageUrl = getUploadedFileUrl(request, "image");
//     long timestamp = System.currentTimeMillis();

//     Entity noteEntity = new Entity("Note");
//     noteEntity.setProperty("imageUrl", imageUrl);
//     noteEntity.setProperty("timestamp", timestamp);

//     DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
//     datastore.put(noteEntity);

//     response.sendRedirect("/index.html");
//   }

//     //Detects text from document
//     public static void detectDocumentText(String filePath) throws IOException {
//         List<AnnotateImageRequest> requests = new ArrayList<>();

//         ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

//         Image img = Image.newBuilder().setContent(imgBytes).build();
//         Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
//         AnnotateImageRequest request =
//             AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
//         requests.add(request);

//         // Initialize client that will be used to send requests. This client only needs to be created
//         // once, and can be reused for multiple requests. After completing all of your requests, call
//         // the "close" method on the client to safely clean up any remaining background resources.
//         try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
//             BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
//             List<AnnotateImageResponse> responses = response.getResponsesList();
//             client.close();

//             for (AnnotateImageResponse res : responses) {
//             if (res.hasError()) {
//                 System.out.format("Error: %s%n", res.getError().getMessage());
//                 return;
//             }

//             // For full list of available annotations, see http://g.co/cloud/vision/docs
//             TextAnnotation annotation = res.getFullTextAnnotation();
//             for (Page page : annotation.getPagesList()) {
//                 String pageText = "";
//                 for (Block block : page.getBlocksList()) {
//                 String blockText = "";
//                 for (Paragraph para : block.getParagraphsList()) {
//                     String paraText = "";
//                     for (Word word : para.getWordsList()) {
//                     String wordText = "";
//                     for (Symbol symbol : word.getSymbolsList()) {
//                         wordText = wordText + symbol.getText();
//                         System.out.format(
//                             "Symbol text: %s (confidence: %f)%n",
//                             symbol.getText(), symbol.getConfidence());
//                     }
//                     System.out.format(
//                         "Word text: %s (confidence: %f)%n%n", wordText, word.getConfidence());
//                     paraText = String.format("%s %s", paraText, wordText);
//                     }
//                     // Output Example using Paragraph:
//                     System.out.println("%nParagraph: %n" + paraText);
//                     System.out.format("Paragraph Confidence: %f%n", para.getConfidence());
//                     blockText = blockText + paraText;
//                 }
//                 pageText = pageText + blockText;
//                 }
//             }
//             System.out.println("%nComplete annotation:");
//             System.out.println(annotation.getText());
//             }
//         }
//     }
// }