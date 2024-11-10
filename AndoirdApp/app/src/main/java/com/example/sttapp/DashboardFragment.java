 package com.example.sttapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sttapp.databinding.FragmentDashboardBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;
 
public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private TranslationHistoryAdapter historyAdapter;


    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        dashboardViewModel.getInputText().observe(getViewLifecycleOwner(), text -> binding.inputTrans.setText(text));
        dashboardViewModel.getTranslatedText().observe(getViewLifecycleOwner(), text -> binding.idTranslateTV.setText(text));

        initializeSpeechRecognizer();

        setupPermissionRequest();

        setupSpinners();
        setupMicClickListener();
        setupTranslationButton();
        setupSwapButton();

        // Initialize RecyclerView
        setupHistoryRecyclerView();

        return binding.getRoot();
    }

    private void setupHistoryRecyclerView() {
        historyAdapter = new TranslationHistoryAdapter();
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.historyRecyclerView.setAdapter(historyAdapter);

        // Observe history data
        dashboardViewModel.getHistory().observe(getViewLifecycleOwner(), historyItems -> {
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

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(), R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.idFromSpinner.setAdapter(adapter);
        binding.idToSpinner.setAdapter(adapter);
    }

    private void setupMicClickListener() {
        binding.idIVMic.setOnClickListener(v -> toggleSpeechRecognition());
    }

    private void setupTranslationButton() {
        binding.idBtnTranslation.setOnClickListener(v -> prepareTranslation());
    }

    private void setupSwapButton() {
        binding.ivSwap.setOnClickListener(v -> swapSpinnerSelections());
    }

    private void toggleSpeechRecognition() {
        if(isListening){
            stopListening();
            binding.idIVMic.setImageResource(R.drawable.microphone);
        } else {
            startListening();
            binding.idIVMic.setImageResource(R.drawable.ic_mic_on);
        }
    }

    private void startListening() {
        if (!checkAudioPermission()) {
            requestAudioPermission();
            return;
        }

        String selectedLanguage = getLanguageCode(binding.idFromSpinner.getSelectedItem().toString());
        Locale locale = getLocale(selectedLanguage);

        Intent intent = createSpeechRecognizerIntent(locale);

        try {
            speechRecognizer.startListening(intent);
            isListening = true;
            binding.inputTrans.setText(getString(R.string.listening));
        } catch (Exception e){
            binding.inputTrans.setText(getString(R.string.error_starting_listening));
            isListening = false;
        }
    }

    private void stopListening() {
        if(isListening && speechRecognizer!= null) {
            speechRecognizer.stopListening();
            binding.inputTrans.setText(getString(R.string.stopped_listening));
            isListening = false;
            binding.idIVMic.setImageResource(R.drawable.microphone);
        }
    }

    private Intent createSpeechRecognizerIntent(Locale locale) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        return intent;
    }

    private boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

    private void swapSpinnerSelections() {
        int fromLangPosition = binding.idFromSpinner.getSelectedItemPosition();
        int toLangPosition = binding.idToSpinner.getSelectedItemPosition();

        binding.idFromSpinner.setSelection(toLangPosition);
        binding.idToSpinner.setSelection(fromLangPosition);
    }

    private void prepareTranslation() {
        String fromLang = getLanguageCode(binding.idFromSpinner.getSelectedItem().toString());
        String toLang = getLanguageCode(binding.idToSpinner.getSelectedItem().toString());

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(fromLang)
                .setTargetLanguage(toLang)
                .build();

        dashboardViewModel.setTranslator(Translation.getClient(options));

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        dashboardViewModel.getTranslator().downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> translateText())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), getString(R.string.model_download_failed) + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void translateText() {
        String textToTranslate = binding.inputTrans.getText().toString();

        if (textToTranslate.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_text), Toast.LENGTH_SHORT).show();
            return;
        }

        dashboardViewModel.getTranslator().translate(textToTranslate)
                .addOnSuccessListener(translatedText -> dashboardViewModel.setTranslatedText(translatedText, binding.idFromSpinner.getSelectedItem().toString(), binding.idToSpinner.getSelectedItem().toString()))
                .addOnFailureListener(e -> Toast.makeText(requireContext(), getString(R.string.translation_failed) + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String getLanguageCode(String language) {
        switch (language) {
            case "Vietnamese":
                return TranslateLanguage.VIETNAMESE;
            case "English":
                return TranslateLanguage.ENGLISH;
            case "German":
                return TranslateLanguage.GERMAN;
            case "Japanese":
                return TranslateLanguage.JAPANESE;
            case "Korean":
                return TranslateLanguage.KOREAN;
            case "Chinese":
                return TranslateLanguage.CHINESE;
            default:
                Toast.makeText(requireContext(), getString(R.string.language_not_supported), Toast.LENGTH_SHORT).show();
                return TranslateLanguage.ENGLISH; // Default to English
        }
    }

    private Locale getLocale(String languageCode) {
        switch (languageCode) {
            case TranslateLanguage.VIETNAMESE:
                return new Locale("vi");
            case TranslateLanguage.ENGLISH:
                return Locale.ENGLISH;
            case TranslateLanguage.GERMAN:
                return Locale.GERMAN;
            case TranslateLanguage.JAPANESE:
                return Locale.JAPANESE;
            case TranslateLanguage.KOREAN:
                return Locale.KOREAN;
            case TranslateLanguage.CHINESE:
                return Locale.SIMPLIFIED_CHINESE;
            default:
                return Locale.ENGLISH;
        }
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            binding.inputTrans.setText(getString(R.string.listening));
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
            binding.inputTrans.setText(getString(R.string.processing));
        }

        @Override
        public void onError(int error) {
            String message = getErrorMessage(error);
            binding.inputTrans.setText(message);
            isListening = false;
            binding.idIVMic.setImageResource(R.drawable.microphone); // Reset mic icon
        }

        @Override
        public void onResults(Bundle results) {
            requireActivity().runOnUiThread(() -> {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    dashboardViewModel.setInputText(matches.get(0));
                } else {
                    binding.inputTrans.setText(getString(R.string.no_speech_recognized));
                }
                isListening = false;
                binding.idIVMic.setImageResource(R.drawable.microphone); // Reset mic icon
            });
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (partial != null && !partial.isEmpty()) {
                binding.inputTrans.setText(partial.get(0));
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

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
        if (isListening) {
            stopListening();
            binding.idIVMic.setImageResource(R.drawable.microphone); // Reset mic icon
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
        // Close the translator
        if (dashboardViewModel.getTranslator() != null) {
            dashboardViewModel.getTranslator().close();
        }
        // Nullify binding
        binding = null;
    }
}
