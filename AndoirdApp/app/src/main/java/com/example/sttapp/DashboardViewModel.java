package com.example.sttapp;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.mlkit.nl.translate.Translator;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DashboardViewModel extends AndroidViewModel {
    private final MutableLiveData<String> inputText = new MutableLiveData<>();
    private final MutableLiveData<String> translatedText = new MutableLiveData<>();
    private Translator translator;
    private final LiveData<List<TranslationHistoryItem>> history;
    private final AppDatabase database;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public DashboardViewModel(Application application) {
        super(application);
        database = AppDatabase.getInstance(application);
        history = database.translationHistoryDao().getAllHistory();
    }

    public LiveData<String> getInputText() {
        return inputText;
    }

    public void setInputText(String text) {
        inputText.setValue(text);
    }

    public LiveData<String> getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String text, String fromLanguage, String toLanguage) {
        translatedText.setValue(text);
        saveToHistory(inputText.getValue(), text, fromLanguage, toLanguage);
    }

    public Translator getTranslator() {
        return translator;
    }

    public void setTranslator(Translator translator) {
        this.translator = translator;
    }

    public LiveData<List<TranslationHistoryItem>> getHistory() {
        return history;
    }

    private void saveToHistory(String originalText, String translatedText, String fromLanguage, String toLanguage) {
        TranslationHistoryItem item = new TranslationHistoryItem(
                originalText,
                translatedText,
                fromLanguage,
                toLanguage,
                System.currentTimeMillis()
        );
        executor.execute(() -> database.translationHistoryDao().insert(item));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (translator != null) {
            translator.close();
            translator = null;
        }
    }
}
