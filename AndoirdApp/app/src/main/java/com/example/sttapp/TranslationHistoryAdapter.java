package com.example.sttapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
 
import java.util.List;

public class TranslationHistoryAdapter extends RecyclerView.Adapter<TranslationHistoryAdapter.HistoryViewHolder> {
    private List<TranslationHistoryItem> historyList;
    private Context context;

    public TranslationHistoryAdapter(Context context) {
        this.context = context;
    }

    public void setHistoryList(List<TranslationHistoryItem> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder  onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_translation_history, parent, false);
        return new HistoryViewHolder (v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        TranslationHistoryItem item = historyList.get(position);
        holder.originalText.setText(item.getOriginalText());
        holder.translatedText.setText(item.getTranslatedText());
        holder.languages.setText(item.getFromLanguage() + " -> " + item.getToLanguage());
        String date = DateFormat.format("dd/MM/yyyy hh:mm:ss", item.getTimestamp()).toString();
        holder.timestamp.setText(date);

        // Set up click listener to copy text to clipboard
        holder.itemView.setOnLongClickListener(v -> {
            copyTextToClipboard(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    private void copyTextToClipboard(TranslationHistoryItem item) {
        String textToCopy = "Original: " + item.getOriginalText() + "\n" +
                "Translated: " + item.getTranslatedText() + "\n" +
                "Languages: " + item.getFromLanguage() + " → " + item.getToLanguage();

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Translation History", textToCopy);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Translation copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView originalText, translatedText, languages, timestamp;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            originalText = itemView.findViewById(R.id.originalText);
            translatedText = itemView.findViewById(R.id.translatedText);
            languages = itemView.findViewById(R.id.languages);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}
