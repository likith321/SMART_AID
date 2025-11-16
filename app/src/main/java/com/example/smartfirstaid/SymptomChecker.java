package com.example.smartfirstaid; // change to your package

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import com.example.smartfirstaid.util.NumSymptomsDialogFragment;
import java.util.ArrayList;
import android.content.Intent;

public class SymptomChecker extends AppCompatActivity
        implements NumSymptomsDialogFragment.NumSymptomsListener {

    private LinearLayout symptomContainer;
    private Button btnAddSymptom, btnRemoveSymptom;
    private Button btnDiagnose;
    private TextView tvTitle;
    private ScrollView scrollView;

    private static final String KEY_SYMPTOM_TEXTS = "key_symptom_texts";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_checker);

        symptomContainer = findViewById(R.id.symptom_container);
        btnAddSymptom = findViewById(R.id.button_add_symptom);
        btnDiagnose = findViewById(R.id.button_diagnose);
        tvTitle = findViewById(R.id.tv_title);
        scrollView = findViewById(R.id.scroll_view);
        Button btnRemoveSymptom = findViewById(R.id.button_remove_symptom);

        // Open dialog to ask number of symptoms when activity starts (you can change this behavior)
        findViewById(R.id.tv_title).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNumSymptomsDialog();
            }
        });

        // Default: open the dialog immediately
        openNumSymptomsDialog();

        btnAddSymptom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSymptomField(null);
                // Scroll to bottom so user sees added field
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        btnRemoveSymptom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = symptomContainer.getChildCount();

                // Skip the instruction TextView at index 0
                if (count > 1) {
                    symptomContainer.removeViewAt(count - 1);
                } else {
                    Toast.makeText(SymptomChecker.this, "No symptoms to remove", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDiagnose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                ArrayList<String> symptoms = collectSymptoms();
                if (symptoms.isEmpty()) {
                    Toast.makeText(SymptomChecker.this, "Please enter at least one symptom", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(SymptomChecker.this, DiagnoseActivity.class);
                i.putStringArrayListExtra("symptoms", symptoms);
                startActivity(i);
            }
        });

        // Restore fields on configuration change (if any)
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SYMPTOM_TEXTS)) {
            ArrayList<String> saved = savedInstanceState.getStringArrayList(KEY_SYMPTOM_TEXTS);
            if (saved != null) {
                symptomContainer.removeAllViews();
                for (String s : saved) addSymptomField(s);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(KEY_SYMPTOM_TEXTS, collectSymptomsIncludingEmpty());
    }

    // Implementation of the dialog callback - creates 'count' EditTexts
    @Override
    public void onNumberOfSymptomsChosen(int count) {
        // Clear existing and add `count` fields
        symptomContainer.removeAllViews();
        for (int i = 1; i <= count; i++) {
            addSymptomField(null);
        }

        // If none requested, add one empty field so user can start
        if (count == 0) addSymptomField(null);
    }

    private void openNumSymptomsDialog() {
        FragmentManager fm = getSupportFragmentManager();
        NumSymptomsDialogFragment dialog = new NumSymptomsDialogFragment();
        dialog.show(fm, "num_symptoms_dialog");
    }

    // Helper to add a symptom EditText; if 'text' non-null it populates the field
    private void addSymptomField(@Nullable String text) {
        EditText et = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dpToPx(8);
        et.setLayoutParams(lp);
        et.setHint("Symptom");
        et.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        et.setBackgroundResource(android.R.drawable.edit_text);
        if (text != null) et.setText(text);

        symptomContainer.addView(et);
        et.requestFocus();
    }

    // Collect non-empty symptoms only
    private ArrayList<String> collectSymptoms() {
        ArrayList<String> list = new ArrayList<>();
        int childCount = symptomContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = symptomContainer.getChildAt(i);
            if (v instanceof EditText) {
                String s = ((EditText) v).getText().toString().trim();
                if (!s.isEmpty()) list.add(s);
            }
        }
        return list;
    }

    // Collect all fields including empty ones (for restoring state)
    private ArrayList<String> collectSymptomsIncludingEmpty() {
        ArrayList<String> list = new ArrayList<>();
        int childCount = symptomContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = symptomContainer.getChildAt(i);
            if (v instanceof EditText) {
                String s = ((EditText) v).getText().toString();
                list.add(s);
            }
        }
        return list;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
