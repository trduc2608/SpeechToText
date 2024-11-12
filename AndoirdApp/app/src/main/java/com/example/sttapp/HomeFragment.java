package com.example.sttapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize View Binding
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Observe LiveData
        homeViewModel.getRecognizedText().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                binding.textstore.setText(text);
                Log.d("SpeechRecognition", "Text updated via LiveData observer");
            }
        });

        // Initialize Speech Recognizer
        initializeSpeechRecognizer();

        // Setup Permission Request
        setupPermissionRequest();

        // Setup UI Components
        setupLanguageSpinner();
        setupMicClickListener();

        setupHistoryRecyclerView();

        return binding.getRoot();
    }

    private void setupHistoryRecyclerView() {
        historyAdapter = new SpeechHistoryAdapter(getContext());
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.historyRecyclerView.setAdapter(historyAdapter);

        // Observe history data
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
        // Assuming you have set up an adapter for the spinner in your layout
        binding.languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View viewSpinner, int position, long id) {
                String selectedLanguageText = parent.getItemAtPosition(position).toString();
                selectedLanguage = getLanguageCode(selectedLanguageText);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default to English if nothing is selected
                selectedLanguage = "en-US";
            }
        });
    }

    private void setupMicClickListener() {
        binding.IVMic.setOnClickListener(v -> toggleSpeechRecognition());
    }

    private void toggleSpeechRecognition() {
        if (isRecording) {
            stopListening();
            binding.IVMic.setImageResource(R.drawable.microphone); // Replace with your mic off icon
        } else {
            startListening();
            binding.IVMic.setImageResource(R.drawable.ic_mic_on); // Replace with your mic on icon
        }
    }

    private void startListening() {
        if (!checkAudioPermission()) {
            requestAudioPermission();
            return;
        }

        Intent intent = createSpeechRecognizerIntent();

        try {
            speechRecognizer.startListening(intent);
            isRecording = true;
            binding.textstore.setText(getString(R.string.listening));
        } catch (Exception e) {
            binding.textstore.setText(getString(R.string.error_starting_listening));
            isRecording = false;
        }
    }

    private void stopListening() {
        if (isRecording && speechRecognizer != null) {
            speechRecognizer.stopListening();
            binding.textstore.setText(getString(R.string.stopped_listening));
            isRecording = false;
            binding.IVMic.setImageResource(R.drawable.microphone); // Reset mic icon
        }
    }

    private Intent createSpeechRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage);
        // Do not request partial results
        // intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

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
                return "zh-CN"; // Simplified Chinese
            default:
                Toast.makeText(requireContext(),
                        getString(R.string.language_not_supported),
                        Toast.LENGTH_SHORT).show();
                return "en-US"; // Default to English
        }
    }

    private class SpeechRecognitionListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            binding.textstore.setText(getString(R.string.listening));
        }

        @Override
        public void onBeginningOfSpeech() {
            // You can update UI here if needed
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // Implement if you want to show sound level changes
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            // Implement if needed
        }

        @Override
        public void onEndOfSpeech() {
            binding.textstore.setText(getString(R.string.processing));
        }

        @Override
        public void onError(int error) {
            String message = getErrorMessage(error);
            binding.textstore.setText(message);
            isRecording = false;
            binding.IVMic.setImageResource(R.drawable.microphone); // Reset mic icon
            Log.d("SpeechRecognition", "Error occurred: " + message);
        }

        @Override
        public void onResults(Bundle results) {
            if (isAdded()) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    homeViewModel.setRecognizedText(recognizedText); // Update ViewModel and save to history
                    Log.d("SpeechRecognition", "Recognized Text: " + recognizedText);
                } else {
                    binding.textstore.setText(getString(R.string.no_speech_recognized));
                    Log.d("SpeechRecognition", "No speech recognized");
                }
                isRecording = false;
                binding.IVMic.setImageResource(R.drawable.microphone); // Reset mic icon
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            // Do not process partial results
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            // Implement if needed
        }
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return getString(R.string.error_audio);
            case SpeechRecognizer.ERROR_CLIENT:
                return getString(R.string.error_client);
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return getString(R.string.error_permissions);
            case SpeechRecognizer.ERROR_NETWORK:
                return getString(R.string.error_network);
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return getString(R.string.error_network_timeout);
            case SpeechRecognizer.ERROR_NO_MATCH:
                return getString(R.string.error_no_match);
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return getString(R.string.error_recognizer_busy);
            case SpeechRecognizer.ERROR_SERVER:
                return getString(R.string.error_server);
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return getString(R.string.error_speech_timeout);
            default:
                return getString(R.string.error_unknown);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRecording) {
            stopListening();
            binding.IVMic.setImageResource(R.drawable.microphone); // Reset mic icon
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release SpeechRecognizer resources
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        // Nullify binding
        binding = null;
    }
}
