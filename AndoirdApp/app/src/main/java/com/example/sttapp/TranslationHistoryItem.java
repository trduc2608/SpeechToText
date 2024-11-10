package com.example.sttapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "translation_history")
public class TranslationHistoryItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String originalText;
    private String translatedText;
    private String fromLanguage;
    private String toLanguage;
    private long timestamp;

    // Constructor, getters, and setters

    public TranslationHistoryItem(String originalText, String translatedText, String fromLanguage, String toLanguage, long timestamp) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.fromLanguage = fromLanguage;
        this.toLanguage = toLanguage;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    // Room uses this method to set the id
    public void setId(int id) {
        this.id = id;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getFromLanguage() {
        return fromLanguage;
    }

    public String getToLanguage() {
        return toLanguage;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
