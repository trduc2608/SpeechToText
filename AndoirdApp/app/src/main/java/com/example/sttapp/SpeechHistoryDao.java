package com.example.sttapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SpeechHistoryDao {
    @Insert
    void insert(SpeechHistoryItem item);

    @Query("SELECT * FROM speech_history ORDER BY timestamp DESC")
    LiveData<List<SpeechHistoryItem>> getAllHistory();
}
