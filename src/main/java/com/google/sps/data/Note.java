package com.google.sps.data;

public final class Note {

  private final long id;
  private final String imageUrl;
  private final long timestamp;

  public Note(long id, String imageUrl, long timestamp) {
    this.id = id;
    this.imageUrl = imageUrl;
    this.timestamp = timestamp;
  }
}