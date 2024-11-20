package com.example.sttapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;


import java.sql.BatchUpdateException;
import java.util.List;

public class SpeechHistoryAdapter extends RecyclerView.Adapter<SpeechHistoryAdapter.HistoryViewHolder> {

    private List<SpeechHistoryItem> historyList;
    private Context context;

    public interface OnDeleteClickListener {
        void onDeleteClick(SpeechHistoryItem item);
    }

    private OnDeleteClickListener deleteClickListener;

    public SpeechHistoryAdapter(Context context, OnDeleteClickListener deleteClickListener){
        this.context = context;
        this.deleteClickListener = deleteClickListener;
    }

    public void setHistoryList(List<SpeechHistoryItem> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_speech_history, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SpeechHistoryItem item = historyList.get(position);
        holder.recognizedText.setText(item.getRecognizedText());
        String date = DateFormat.format("dd/MM/yyyy hh:mm:ss", item.getTimestamp()).toString();
        holder.timestamp.setText(date);

        holder.itemView.setOnLongClickListener(v -> {
            copyTextToClipboard(item.getRecognizedText());
            return true;
        });

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

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Recognized Text", text);
        clipboard.setPrimaryClip(clip);
        Snackbar.make(((Activity) context).findViewById(android.R.id.content),
                "Text copied to clipboard", Snackbar.LENGTH_SHORT).show();
    }

    public void removeItem(SpeechHistoryItem item) {
        int position = historyList.indexOf(item);
        if (position != -1) {
            historyList.remove(position);
            notifyItemRemoved(position);
        }
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView recognizedText, timestamp;
        Button deleteButton;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            recognizedText = itemView.findViewById(R.id.recognizedText);
            timestamp = itemView.findViewById(R.id.timestamp);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
