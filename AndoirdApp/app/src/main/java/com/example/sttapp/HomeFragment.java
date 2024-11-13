package com.example.sttapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sttapp.databinding.FragmentHomeBinding;
import java.util.ArrayList;
import java.util.Locale;
import android.Manifest;

 
public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private SpeechHistoryAdapter historyAdapter;

    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    private String selectedLanguage = "en-US";
    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        homeViewModel.getRecognizedText().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                binding.recognizedTextView.setText(text);
                Log.d(TAG, "Text updated via LiveData observer");
            }
        });

        initializeComponents();

        return binding.getRoot();
    }

    private void initializeComponents() {
        initializeSpeechRecognizer();
        setupPermissionRequest();
        setupLanguageSpinner();
        setupMicClickListener();
        setupHistoryRecyclerView();

        binding.recognizedTextView.setOnLongClickListener(v -> {
            String text = binding.recognizedTextView.getText().toString();
            if (!text.isEmpty()) {
                copyTextToClipboard(text);
            } else {
                Toast.makeText(getContext(), getString(R.string.no_text_to_copy), Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Recognized Text", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), getString(R.string.text_copied), Toast.LENGTH_SHORT).show();
    }

    private void setupHistoryRecyclerView() {
        historyAdapter = new SpeechHistoryAdapter(getContext(), item -> {

            homeViewModel.deleteHistoryItem(item);
        });
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.historyRecyclerView.setAdapter(historyAdapter);


        homeViewModel.getHistory().observe(getViewLifecycleOwner(), historyItems -> {
            historyAdapter.setHistoryList(historyItems);
        });
    }

    private void initializeSpeechRecognizer() {
        Context context = getContext();
        if (context != null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_initializing_speech_recognizer), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupPermissionRequest() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(getActivity(), getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(), R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.languageSpinner.setAdapter(adapter);

        binding.languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View viewSpinner, int position, long id) {
                String selectedLanguageText = parent.getItemAtPosition(position).toString();
                selectedLanguage = getLanguageCode(selectedLanguageText);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

                selectedLanguage = "en-US";
            }
        });
    }

    private void setupMicClickListener() {
        binding.micImageView.setOnClickListener(v -> toggleSpeechRecognition());
    }

    private void updateUIForRecordingState(boolean isRecording) {
        if (isRecording) {
            binding.micImageView.setImageResource(R.drawable.ic_mic_on);

        } else {
            binding.micImageView.setImageResource(R.drawable.microphone);
        }
    }

    private void toggleSpeechRecognition() {
        if (isRecording) {
            stopListening();
        } else {
            startListening();
        }
        updateUIForRecordingState(isRecording);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void startListening() {
        if (!checkAudioPermission()) {
            requestAudioPermission();
            return;
        }

        if (!isInternetAvailable()) {
            Toast.makeText(getContext(), getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = createSpeechRecognizerIntent();

        try {
            speechRecognizer.startListening(intent);
            isRecording = true;
            binding.recognizedTextView.setText(getString(R.string.listening));
            updateUIForRecordingState(true);
        } catch (Exception e) {
            binding.recognizedTextView.setText(getString(R.string.error_starting_listening));
            isRecording = false;
            updateUIForRecordingState(false);
            Log.e(TAG, "Error starting speech recognition", e);
        }
    }

    private void stopListening() {
        if (isRecording && speechRecognizer != null) {
            speechRecognizer.stopListening();
            isRecording = false;
            updateUIForRecordingState(false);
        }
    }

    private Intent createSpeechRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage);
        return intent;
    }

    private boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

    private String getLanguageCode(String language) {
        switch (language) {
            case "Vietnamese":
                return "vi-VN";
            case "English":
                return "en-US";
            case "German":
                return "de-DE";
            case "Japanese":
                return "ja-JP";
            case "Korean":
                return "ko-KR";
            case "Chinese":
                return "zh-CN"; 
            default:
                Toast.makeText(requireContext(),
                        getString(R.string.language_not_supported),
                        Toast.LENGTH_SHORT).show();
                return "en-US"; 
        }
    }

    private class SpeechRecognitionListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            binding.recognizedTextView.setText(getString(R.string.listening));
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            binding.recognizedTextView.setText(getString(R.string.processing));
        }

        @Override
        public void onError(int error) {
            String message = getErrorMessage(error);
            binding.recognizedTextView.setText(message);
            isRecording = false;
            updateUIForRecordingState(false);
            Log.e(TAG, "Error occurred: " + message);
        }

        @Override
        public void onResults(Bundle results) {
            requireActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String recognizedText = matches.get(0);
                        homeViewModel.setRecognizedText(recognizedText);
                        Log.d(TAG, "Recognized Text: " + recognizedText);
                    } else {
                        binding.recognizedTextView.setText(getString(R.string.no_speech_recognized));
                        Log.d(TAG, "No speech recognized");
                    }
                    isRecording = false;
                    updateUIForRecordingState(false);
                }
            });
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }

    private String getErrorMessage(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = getString(R.string.error_audio);
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = getString(R.string.error_client);
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = getString(R.string.error_permissions);
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = getString(R.string.error_network);
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = getString(R.string.error_network_timeout);
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = getString(R.string.error_no_match);
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = getString(R.string.error_recognizer_busy);
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = getString(R.string.error_server);
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = getString(R.string.error_speech_timeout);
                break;
            default:
                message = getString(R.string.error_unknown) + " (Code: " + errorCode + ")";
                break;
        }
        Log.e(TAG, "Error Code: " + errorCode + ", Message: " + message);
        return message;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRecording) {
            stopListening();
            updateUIForRecordingState(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        // Nullify binding
        binding = null;
    }
}
