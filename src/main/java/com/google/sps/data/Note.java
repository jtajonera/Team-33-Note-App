package com.google.sps.data;

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
}