package com.example.sttapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "speech_history")
public class SpeechHistoryItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String recognizedText;
    private long timestamp;

    // Constructor, getters, and setters

    public SpeechHistoryItem(String recognizedText, long timestamp) {
        this.recognizedText = recognizedText;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    // Room uses this method to set the id
    public void setId(int id) {
        this.id = id;
    }

    public String getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
