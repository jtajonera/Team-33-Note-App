package com.google.sps.data;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Session {
  private final String PROEJCT_ID = "summer20-sps-33";
  private final String BUCKET_NAME = "summer20-sps-33-output-files";
  private String outputDoc;

  public Session() {

  }

  public String getOutputDoc(){
    return outputDoc;
  }

  public void uploadObject(String objectName, byte[] outputByteArray) throws IOException {
    Storage storage = StorageOptions.newBuilder().setProjectId(PROEJCT_ID).build().getService();
    BlobId blobId = BlobId.of(BUCKET_NAME, objectName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    storage.create(blobInfo, outputByteArray);

    System.out.println("Uploaded to bucket " + BUCKET_NAME + " as " + objectName);
  }

  /**
  * Signing a URL requires Credentials which implement ServiceAccountSigner. These can be set
  * explicitly using the Storage.SignUrlOption.signWith(ServiceAccountSigner) option. If you don't,
  * you could also pass a service account signer to StorageOptions, i.e.
  * StorageOptions().newBuilder().setCredentials(ServiceAccountSignerCredentials).
  */
  public void generateV4GetObjectSignedUrl(String objectName) throws StorageException {
    Storage storage = StorageOptions.newBuilder().setProjectId(PROEJCT_ID).build().getService();

    // Define resource
    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(BUCKET_NAME, objectName)).build();
    outputDoc = storage.signUrl(blobInfo, 7, TimeUnit.DAYS, Storage.SignUrlOption.withV4Signature()).toString();

    System.out.println("Generated GET signed URL:");
    System.out.println(outputDoc);
    System.out.println("You can use this URL with any user agent, for example:");
    System.out.println("curl '" + outputDoc + "'");
  }
}