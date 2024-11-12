package com.example.sttapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TranslationHistoryDao {
    @Insert
    void insert(TranslationHistoryItem item);

    @Delete
    void delete(TranslationHistoryItem item);

    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    LiveData<List<TranslationHistoryItem>> getAllHistory();
}
