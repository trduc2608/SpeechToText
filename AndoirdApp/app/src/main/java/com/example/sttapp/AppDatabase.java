package com.example.sttapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SpeechHistoryItem.class, TranslationHistoryItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract SpeechHistoryDao speechHistoryDao();

    public abstract TranslationHistoryDao translationHistoryDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "stt_app_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
