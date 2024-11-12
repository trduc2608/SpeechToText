package com.example.sttapp;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
 
import java.util.List;

public class SpeechHistoryAdapter extends RecyclerView.Adapter<SpeechHistoryAdapter.HistoryViewHolder> {

    private List<SpeechHistoryItem> historyList;

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
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView recognizedText, timestamp;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            recognizedText = itemView.findViewById(R.id.recognizedText);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}
