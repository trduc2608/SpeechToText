package com.example.sttapp;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SpeechRepository {
    private final SpeechHistoryDao speechHistoryDao;
    private final Executor executor;

    public SpeechRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        speechHistoryDao = database.speechHistoryDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<SpeechHistoryItem>> getHistory() {
        return speechHistoryDao.getAllHistory();
    }

    public void saveToHistory(SpeechHistoryItem item) {
        executor.execute(() -> speechHistoryDao.insert(item));
    }

    public void deleteHistoryItem(SpeechHistoryItem item) {
        executor.execute(() -> speechHistoryDao.delete(item));
    }
}
