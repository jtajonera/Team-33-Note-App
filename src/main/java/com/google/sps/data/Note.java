package com.google.sps.data;

import com.google.appengine.api.datastore.Key;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.Image;
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

import com.google.cloud.language.v1.ClassificationCategory;
import com.google.cloud.language.v1.ClassifyTextRequest;
import com.google.cloud.language.v1.ClassifyTextResponse;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.EncodingType;
import com.google.cloud.language.v1.LanguageServiceClient;

import com.google.protobuf.ByteString;
import java.io.File; 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.exceptions.Docx4JException;

public final class Note {
  private long id;
  private Key sessionKey;
  private String filePath;
  private String fileName;
  private final String imageUrl;
  private final String message;
  private final ArrayList<String> categories;
  private String downloadUrl;
    
  /** Constructor called when loading from Datastore. */
  public Note(long id, String imageUrl, String message, ArrayList<String> categories, String downloadUrl) {
    this.id = id;
    this.imageUrl = imageUrl;
    this.message = message;
    this.categories = categories;
    this.downloadUrl = downloadUrl;
  }

  /** Constructor called when creating from a POST request. */
  public Note(Key sessionKey, String imageUrl, byte[] imageBytes) throws IOException{
    this.sessionKey = sessionKey;
    this.imageUrl = imageUrl;
    this.message = detectDocumentText(imageBytes);
    this.categories = classifyText();
  }

  public String getOriginalImageUrl(){
    return imageUrl;
  }

  public String getMessage(){
    return message;
  }
    
  public String getFilePath(){
    return filePath;
  }

  public String getFileName(){
    return fileName;
  }

  public ArrayList<String> classifyText() {
    // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
    try (LanguageServiceClient language = LanguageServiceClient.create()) {
      // set content to the text string
      Document doc = Document.newBuilder().setContent(getMessage()).setType(Type.PLAIN_TEXT).build();
      ClassifyTextRequest request = ClassifyTextRequest.newBuilder().setDocument(doc).build();
      // detect categories in the given text
      ClassifyTextResponse response = language.classifyText(request);

      ArrayList<String> categories = new ArrayList<>();

      for (ClassificationCategory category : response.getCategoriesList()) {
        String output = category.getName().substring(1);
        output = output.replaceAll("&","and");
        categories.add(output);  
      }

      return categories;
    } 
    catch(Exception e) {
      return new ArrayList<>();
    }
  }

  public void writeConvertedDoc() throws Docx4JException, IOException {
    //if no categories classified then the title is "Getcha Notes" by default
    String title = !categories.isEmpty() ? categories.get(0) : "Getcha Notes";

    WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
    wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Title", title);
    wordMLPackage.getMainDocumentPart().addParagraphOfText(message);

    // saved in Team-33-Note-App/target/portfolio-1
    fileName = String.format("Getcha_Notes_%d", sessionKey.getId());
    filePath = String.format("%s/%s", System.getProperty("user.dir"), fileName);
    wordMLPackage.save(new File(filePath));
  }

  /**
   * Detects words from a picture and returns them
   * TODO: Clean up the result, sometimes the api misreads, misses entirely, or adds additional words
   *        
   */
  private String detectDocumentText(byte[] imageBytes) throws IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();
    Image img = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();
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