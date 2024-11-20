package com.example.sttapp;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;
  
public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";

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

        // Observe LiveData from ViewModel
        dashboardViewModel.getInputText().observe(getViewLifecycleOwner(), text -> binding.inputTextView.setText(text));
        dashboardViewModel.getTranslatedText().observe(getViewLifecycleOwner(), text -> binding.translatedTextView.setText(text));

        initializeSpeechRecognizer();
        setupPermissionRequest();
        setupSpinners();
        setupMicClickListener();
        setupTranslationButton();
        setupSwapButton();
        setupSaveButton();
        setupHistoryRecyclerView();

        binding.translatedTextView.setOnLongClickListener(v -> {
            String translatedText = binding.translatedTextView.getText().toString();
            if (!translatedText.isEmpty()) {
                copyTextToClipboard(translatedText);
            } else {
                Toast.makeText(getContext(), "No text to copy", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        return binding.getRoot();
    }

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Translated Text", text);
        clipboard.setPrimaryClip(clip);
        Snackbar.make(binding.getRoot(), "Translated text copied to clipboard", Snackbar.LENGTH_SHORT).show();
    }

    private void setupHistoryRecyclerView() {
        historyAdapter = new TranslationHistoryAdapter(getContext(), item -> {
            dashboardViewModel.deleteHistoryItem(item);
        });
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.historyRecyclerView.setAdapter(historyAdapter);

        dashboardViewModel.getHistory().observe(getViewLifecycleOwner(), historyItems -> {
            historyAdapter.setHistoryList(historyItems);
        });
    }

    private void setupSaveButton() {
        binding.saveBtn.setOnClickListener(v -> saveTranslationToHistory());
    }

    private void saveTranslationToHistory() {
        String originalText = binding.inputTextView.getText().toString();
        String translatedText = binding.translatedTextView.getText().toString();
        String fromLanguage = binding.fromLanguageSpinner.getSelectedItem().toString();
        String toLanguage = binding.toLanguageSpinner.getSelectedItem().toString();

        if (originalText.isEmpty() || translatedText.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.nothing_to_save), Toast.LENGTH_SHORT).show();
            return;
        }

        dashboardViewModel.saveTranslationToHistory(originalText, translatedText, fromLanguage, toLanguage);
        Toast.makeText(requireContext(), getString(R.string.translation_saved), Toast.LENGTH_SHORT).show();
    }

    // Removed misplaced methods that belong to DashboardViewModel

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
        binding.fromLanguageSpinner.setAdapter(adapter);
        binding.toLanguageSpinner.setAdapter(adapter);
    }

    private void setupMicClickListener() {
        binding.micImageView.setOnClickListener(v -> toggleSpeechRecognition());
    }

    private void setupTranslationButton() {
        binding.idBtnTranslation.setOnClickListener(v -> prepareTranslation());
    }

    private void setupSwapButton() {
        binding.ivSwap.setOnClickListener(v -> swapSpinnerSelections());
    }

    private void toggleSpeechRecognition() {
        if (isListening) {
            stopListening();
            binding.micImageView.setImageResource(R.drawable.microphone);
        } else {
            startListening();
            binding.micImageView.setImageResource(R.drawable.ic_mic_on);
        }
    }

    private void startListening() {
        if (!checkAudioPermission()) {
            requestAudioPermission();
            return;
        }

        String selectedLanguage = getSpeechRecognizerLanguageCode(binding.fromLanguageSpinner.getSelectedItem().toString());

        if (selectedLanguage == null) {
            showUnsupportedLanguageToast();
            return;
        }

        Intent intent = createSpeechRecognizerIntent(selectedLanguage);

        try {
            speechRecognizer.startListening(intent);
            isListening = true;
            updateUIForListeningState();
        } catch (Exception e) {
            handleStartListeningError(e);
        }
    }

    private void stopListening() {
        if (isListening && speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
            binding.micImageView.setImageResource(R.drawable.microphone);
        }
    }

    private void showUnsupportedLanguageToast() {
        Toast.makeText(getContext(), getString(R.string.language_not_supported_for_speech), Toast.LENGTH_SHORT).show();
    }

    private void updateUIForListeningState() {
        binding.translatedTextView.setText(getString(R.string.listening));
        binding.micImageView.setImageResource(R.drawable.ic_mic_on);
    }

    private void handleStartListeningError(Exception e) {
        binding.translatedTextView.setText(getString(R.string.error_starting_listening));
        isListening = false;
        Log.e("DashboardFragment", "Error starting speech recognition", e);
    }

    private Intent createSpeechRecognizerIntent(String languageCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode);
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
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
        int fromLangPosition = binding.fromLanguageSpinner.getSelectedItemPosition();
        int toLangPosition = binding.toLanguageSpinner.getSelectedItemPosition();

        binding.fromLanguageSpinner.setSelection(toLangPosition);
        binding.toLanguageSpinner.setSelection(fromLangPosition);
    }

    private void setUIEnabled(boolean enabled) {
        binding.idBtnTranslation.setEnabled(enabled);
        binding.micImageView.setEnabled(enabled);
        binding.ivSwap.setEnabled(enabled);
        binding.fromLanguageSpinner.setEnabled(enabled);
        binding.toLanguageSpinner.setEnabled(enabled);
    }

    private void prepareTranslation() {
        String fromLangCode = getTranslatorLanguageCode(binding.fromLanguageSpinner.getSelectedItem().toString());
        String toLangCode = getTranslatorLanguageCode(binding.toLanguageSpinner.getSelectedItem().toString());

        if (dashboardViewModel.getTranslator() != null &&
                fromLangCode.equals(dashboardViewModel.getCurrentSourceLanguage()) &&
                toLangCode.equals(dashboardViewModel.getCurrentTargetLanguage())) {
            // Translator is already initialized and ready
            translateText();
            return;
        }

        // Close the previous translator
        if (dashboardViewModel.getTranslator() != null) {
            dashboardViewModel.getTranslator().close();
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(fromLangCode)
                .setTargetLanguage(toLangCode)
                .build();

        Translator translator = Translation.getClient(options);
        dashboardViewModel.setTranslator(translator);
        dashboardViewModel.setCurrentSourceLanguage(fromLangCode);
        dashboardViewModel.setCurrentTargetLanguage(toLangCode);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        // Show progress indicator
        binding.progressBar.setVisibility(View.VISIBLE);

        setUIEnabled(false);

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    binding.progressBar.setVisibility(View.GONE);
                    setUIEnabled(true);
                    translateText();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    setUIEnabled(true);
                    Toast.makeText(requireContext(),
                            getString(R.string.model_download_failed) + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Model download failed", e);
                });
    }

    private void translateText() {
        String textToTranslate = binding.inputTextView.getText().toString();

        if (textToTranslate.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_text), Toast.LENGTH_SHORT).show();
            return;
        }

        dashboardViewModel.setInputText(textToTranslate);

        dashboardViewModel.getTranslator().translate(textToTranslate)
                .addOnSuccessListener(translatedText -> {
                    dashboardViewModel.setTranslatedText(translatedText);
                    binding.translatedTextView.setText(translatedText);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        getString(R.string.translation_failed) + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private String getSpeechRecognizerLanguageCode(String language) {
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
                return null;
        }
    }

    private String getTranslatorLanguageCode(String language) {
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
                Toast.makeText(requireContext(),
                        getString(R.string.language_not_supported),
                        Toast.LENGTH_SHORT).show();
                return TranslateLanguage.ENGLISH; // Default to English
        }
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d("SpeechRecognition", "onReadyForSpeech");
            binding.translatedTextView.setText(getString(R.string.listening));
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("SpeechRecognition", "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // Implement if needed
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            // Implement if needed
        }

        @Override
        public void onEndOfSpeech() {
            Log.d("SpeechRecognition", "onEndOfSpeech");
            binding.translatedTextView.setText(getString(R.string.processing));
        }

        @Override
        public void onError(int error) {
            String message = getErrorMessage(error);
            Log.e("SpeechRecognition", "onError code: " + error + ", message: " + message);
            binding.translatedTextView.setText(message);
            isListening = false;
            binding.micImageView.setImageResource(R.drawable.microphone);
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults");
            requireActivity().runOnUiThread(() -> {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d(TAG, "Recognized Text: " + recognizedText);
                    dashboardViewModel.setInputText(recognizedText);
                    binding.inputTextView.setText(recognizedText);
                    // Do not automatically initiate translation
                    // The user can click the translate button
                } else {
                    Log.d(TAG, "No speech recognized");
                    binding.inputTextView.setText(getString(R.string.no_speech_recognized));
                }
                isListening = false;
                binding.micImageView.setImageResource(R.drawable.microphone);
            });
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (partial != null && !partial.isEmpty()) {
                binding.translatedTextView.setText(partial.get(0));
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            // Implement if needed
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
        return message;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isListening) {
            stopListening();
            binding.micImageView.setImageResource(R.drawable.microphone);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (dashboardViewModel.getTranslator() != null) {
            dashboardViewModel.getTranslator().close();
            dashboardViewModel.setTranslator(null);
        }
    }
}
