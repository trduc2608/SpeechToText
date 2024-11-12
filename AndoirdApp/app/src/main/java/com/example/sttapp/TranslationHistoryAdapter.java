package com.example.sttapp;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
 
import java.util.List;

public class TranslationHistoryAdapter extends RecyclerView.Adapter<TranslationHistoryAdapter.HistoryViewHolder> {
    private List<TranslationHistoryItem> historyList;
    private Context context;

    // Add an interface for item deletion callback
    public interface OnDeleteClickListener {
        void onDeleteClick(TranslationHistoryItem item);
    }

    private OnDeleteClickListener deleteClickListener;

    public TranslationHistoryAdapter(Context context, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.deleteClickListener = deleteClickListener;
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

        // Set up click listener for the Delete button
        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Entry")
                    .setMessage("Are you sure you want to delete this entry?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        if (deleteClickListener != null) {
                            deleteClickListener.onDeleteClick(item);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
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

    public void removeItem(TranslationHistoryItem item) {
        int position = historyList.indexOf(item);
        if (position != -1) {
            historyList.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView originalText, translatedText, languages, timestamp;
        Button deleteButton;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            originalText = itemView.findViewById(R.id.originalText);
            translatedText = itemView.findViewById(R.id.translatedText);
            languages = itemView.findViewById(R.id.languages);
            timestamp = itemView.findViewById(R.id.timestamp);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
