package com.example.smartfirstaid.util; // change to your package

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.smartfirstaid.R;

public class NumSymptomsDialogFragment extends DialogFragment {

    public interface NumSymptomsListener {
        void onNumberOfSymptomsChosen(int count);
    }

    private NumSymptomsListener listener;
    private NumberPicker numberPicker;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // host activity must implement the listener
        try {
            listener = (NumSymptomsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement NumSymptomsListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // inflate the provided fragment_num_symptoms.xml
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_num_symptoms, null);

        numberPicker = view.findViewById(R.id.number_picker);
        // configure number picker limits - adjust max as you like
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(20);
        numberPicker.setValue(1); // sensible default

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view);

        final AlertDialog dialog = builder.create();

        Button btnOk = view.findViewById(R.id.btn_ok);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int chosen = numberPicker.getValue();
                if (listener != null) listener.onNumberOfSymptomsChosen(chosen);
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });

        return dialog;
    }
}
