package com.google.sps.data;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public final class Note {
    private long id;
    private final String imageUrl;
    private final String message;

    // how I'd imagine the NLP text can be sorted
    // key is the keyword/heading
    // value is a list of related sentences
    private HashMap<String, List<String>> categorizedText;

    // Not sure if we still need this, but leaving for now
    public Note(long id, String imageUrl, String message) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.message = message;
    }

    public Note(String imageUrl) throws IOException{
        this.imageUrl = imageUrl;
        this.message = detectDocumentText(this.imageUrl);
    }

    public String getOriginalImageUrl(){
        return imageUrl;
    }
    public String getMessage(){
        return message;
    }

    public HashMap<String, List<String>> getCategorizedText(){
        // temporary data
        categorizedText = new HashMap<>();
        ArrayList<String> someSentences = new ArrayList<>(Arrays.asList(
            "Adult cats have 30 teeth, while kittens have 26.", 
            "A house cat is genetically 95.6% tiger.",
            "Cats can jump 5 times their height."));
        ArrayList<String> someMoreSentences = new ArrayList<>(Arrays.asList(
            "A dog’s nose print is unique, much like a person’s fingerprint.", 
            "The shape of a dog’s face suggests its longevity: A long face means a longer life.",
            "All puppies are born deaf."));
        categorizedText.put("Cats", someSentences);
        categorizedText.put("Dog", someMoreSentences);

        return categorizedText;
    }

    /**
   * Detects words from a picture and returns them
   * TODO: Clean up the result, sometimes the api misreads, misses entirely, or adds additional words
   *        
   */
  private String detectDocumentText(String path) throws IOException {
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