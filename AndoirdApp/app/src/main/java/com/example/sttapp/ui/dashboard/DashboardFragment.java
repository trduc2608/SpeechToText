package com.example.sttapp.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sttapp.R;
import com.example.sttapp.databinding.FragmentDashboardBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import java.util.ArrayList;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private Spinner fromSpinner, toSpinner;
    private TextInputEditText sourceText;
    private ImageView micIV;
    private TextView translateTV;

    String[] fromLanguage = {"From", "English", "Vietnamese"};
    String[] toLanguage = {"To", "English", "Vietnamese"};

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    int languageCode, fromLanguageCode, toLanguageCode = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        // Inflate layout using View Binding
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Views
        fromSpinner = binding.idFromSpinner;
        toSpinner = binding.idToSpinner;
        sourceText = binding.iEditSource;
        micIV = binding.idIVMic;
        translateTV = binding.idTranslateTV;

        // Set up spinners with adapters
        setupSpinners();

        // Set microphone click listener to trigger speech recognition
        micIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something to translate");
                try {
                    startActivityForResult(intent, REQUEST_PERMISSIONS_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();  // Fix context here
                }
            }
        });

        return root;
    }

    // Setup Spinners
    private void setupSpinners() {
        ArrayAdapter<String> fromAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, fromLanguage);  // Fix context here
        fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(fromAdapter);

        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
                fromLanguageCode = getLanguageCode(fromLanguage[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<String> toAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, toLanguage);  // Fix context here
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toSpinner.setAdapter(toAdapter);

        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
                toLanguageCode = getLanguageCode(toLanguage[i]);  // Corrected: Using `toLanguage` array
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Handle activity result from speech recognition
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PERMISSIONS_CODE && resultCode == getActivity().RESULT_OK && data != null) {  // Added null check for `data`
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                sourceText.setText(result.get(0));
            }
        }
    }

    // Get language code based on selected language
    private int getLanguageCode(String language) {
        switch (language) {
            case "English":
                return FirebaseTranslateLanguage.EN;
            case "Vietnamese":
                return FirebaseTranslateLanguage.VI;
            default:
                return 0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
