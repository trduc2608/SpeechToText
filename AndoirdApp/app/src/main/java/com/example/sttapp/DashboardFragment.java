 package com.example.sttapp;

import android.Manifest;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

    private SpeechRecognizer speechRecognizer;
    private MaterialButton idBtnTranslation;
    private Spinner idFromSpinner, idToSpinner;
    private TextInputEditText inputTrans;
    private ImageView ivMic;
    private TextView translatedTextView;
    private boolean isListening = false;
    private static final int REQUEST_CODE_PERMISSION = 1;
    private long lastClickTime = 0;
    private Translator translator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        idToSpinner = view.findViewById(R.id.idToSpinner);
        idFromSpinner = view.findViewById(R.id.idFromSpinner);
        inputTrans = view.findViewById(R.id.inputTrans);
        translatedTextView = view.findViewById(R.id.idTranslateTV);
        idBtnTranslation = view.findViewById(R.id.idBtnTranslation);
        ivMic = view.findViewById(R.id.idIVMic);

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
        checkAndRequestPermissions();

        ivMic.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return;
            }
            lastClickTime = SystemClock.elapsedRealtime();
            toggleSpeechRecognition();
        });

        // Set up Spinners
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(), R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        idFromSpinner.setAdapter(adapter);
        idToSpinner.setAdapter(adapter);

        idBtnTranslation.setOnClickListener(v -> prepareTranslation());

        return view;
    }

    private void prepareTranslation() {
        String fromLang = getLanguageCode(idFromSpinner.getSelectedItem().toString());
        String toLang = getLanguageCode(idToSpinner.getSelectedItem().toString());

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(fromLang)
                .setTargetLanguage(toLang)
                .build();
        translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> translateText())
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Model download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void translateText() {
        String textToTranslate = inputTrans.getText().toString();

        if (textToTranslate.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter text to translate.", Toast.LENGTH_SHORT).show();
            return;
        }

        translator.translate(textToTranslate)
                .addOnSuccessListener(translatedText -> {
                    translatedTextView.setText(translatedText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Translation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
            default:
                return TranslateLanguage.ENGLISH; // Default
        }
    }

    private void toggleSpeechRecognition() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Permission denied. The app needs audio access to work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startListening() {
        String selectedLanguage = getLanguageCode(idFromSpinner.getSelectedItem().toString());
        Locale locale;
        switch (selectedLanguage) {
            case TranslateLanguage.VIETNAMESE:
                locale = new Locale("vi");
                break;
            case TranslateLanguage.ENGLISH:
                locale = Locale.ENGLISH;
                break;
            case TranslateLanguage.GERMAN:
                locale = Locale.GERMAN;
                break;
            case TranslateLanguage.JAPANESE:
                locale = Locale.JAPANESE;
                break;
            case TranslateLanguage.KOREAN:
                locale = Locale.KOREAN;
                break;
            case TranslateLanguage.CHINESE:
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            default:
                locale = Locale.ENGLISH; // Default
                break;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                inputTrans.setText("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {
                // Optional
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Optional
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Optional
            }

            @Override
            public void onEndOfSpeech() {
                inputTrans.setText("Processing...");
            }

            @Override
            public void onError(int error) {
                inputTrans.setText("Error occurred: " + getErrorMessage(error));
                isListening = false;
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    inputTrans.setText(matches.get(0));
                } else {
                    inputTrans.setText("No speech recognized, try again.");
                }
                isListening = false;
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Optional
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Optional
            }
        });

        speechRecognizer.startListening(intent);
        isListening = true;
    }

    private void stopListening() {
        if (isListening && speechRecognizer != null) {
            speechRecognizer.stopListening();
            inputTrans.setText("Stopped listening. Click to start again.");
            isListening = false;
        }
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client-side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No speech recognized";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input detected";
            default: return "Unknown error occurred";
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (translator != null) {
            translator.close();
        }
    }
}
