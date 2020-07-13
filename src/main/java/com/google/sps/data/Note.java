package com.google.sps.data;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.File; 

public final class Note {

  private final long id;
  private final String imageUrl;
  private final long timestamp;
  private final String message;

  public Note(long id, String imageUrl, long timestamp, String message) {
    this.id = id;
    this.imageUrl = imageUrl;
    this.timestamp = timestamp;
    this.message = message;
  }

  public static void writeConvertedDoc() throws Docx4JException, IOException {
    WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();		
    wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Title", "Test Notes");

    wordMLPackage.getMainDocumentPart().addParagraphOfText("from docx4j!");

    // saved in Team-33-Note-App/target/portfolio-1
    wordMLPackage.save(new File(System.getProperty("user.dir") + "/getcha_notes.docx"));

    // TODO: set as unique session id for object name
    uploadObject("test-output-file.docx", System.getProperty("user.dir") + "/getcha_notes.docx");
  }

  private static void uploadObject(String objectName, String filePath) throws IOException {
    String PROEJCT_ID = "summer20-sps-33";
    String BUCKET_NAME = "summer20-sps-33-output-files";

    Storage storage = StorageOptions.newBuilder().setProjectId(PROEJCT_ID).build().getService();
    BlobId blobId = BlobId.of(BUCKET_NAME, objectName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));

    System.out.println(
        "File " + filePath + " uploaded to bucket " + BUCKET_NAME + " as " + objectName);
  }
}