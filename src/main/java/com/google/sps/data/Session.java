package com.google.sps.data;

import com.google.appengine.api.datastore.Key;
import com.google.sps.data.Note;

public final class Session {
    private final Key datastoreKey; 
    private final Note note;
    private final long timestamp;
    
    public Session(Key datastoreKey, Note note, long timestamp) {
        this.datastoreKey = datastoreKey;
        this.note = note;
        this.timestamp = timestamp;
    }

    public Key getKey(){
        return datastoreKey;
    }

    public Note getNote(){
        return note;
    }

    public long getTimestamp(){
        return timestamp;
    }
}