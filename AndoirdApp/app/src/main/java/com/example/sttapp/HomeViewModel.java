package com.example.sttapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<String> recognizedText = new MutableLiveData<>();
    private final LiveData<List<SpeechHistoryItem>> history;
    private final AppDatabase database;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        database = AppDatabase.getInstance(application);
        history = database.speechHistoryDao().getAllHistory();
    }

    public LiveData<String> getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String text) {
        recognizedText.setValue(text);
        saveToHistory(text);
    }

    public LiveData<List<SpeechHistoryItem>> getHistory() {
        return history;
    }

    public void deleteHistoryItem(SpeechHistoryItem item) {
        executor.execute(() -> database.speechHistoryDao().delete(item));
    }

    private void saveToHistory(String text) {
        SpeechHistoryItem item = new SpeechHistoryItem(text, System.currentTimeMillis());
        executor.execute(() -> database.speechHistoryDao().insert(item));
    }
}
