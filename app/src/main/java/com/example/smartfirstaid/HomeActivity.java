package com.example.smartfirstaid;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;

public class HomeActivity extends AppCompatActivity {

    TextView welcome_txt;
    LinearLayout symptom_check, voiceControl, training;
    Button emergency;
    private static final String usr_SHARED_PREFS = "SmartFirstAidPrefs";
    private static final String KEY_NAME = "user_name";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity); // make sure you create this layout XML

        //creating animation drawable
        ConstraintLayout constraintlayout = findViewById(R.id.homeConstraint);
        AnimationDrawable homebackground = (AnimationDrawable) constraintlayout.getBackground();
        homebackground.setEnterFadeDuration(1000);
        homebackground.setExitFadeDuration(1000);
        homebackground.start();

        welcome_txt = (TextView) findViewById(R.id.tvWelcome);
        symptom_check = (LinearLayout) findViewById(R.id.btnSymptomChecker);
        voiceControl = (LinearLayout) findViewById(R.id.btnVoiceControl);
        training = (LinearLayout) findViewById(R.id.btnTraining);
        emergency = (Button) findViewById(R.id.btnEmergency);


        SharedPreferences prefs = getSharedPreferences(usr_SHARED_PREFS, MODE_PRIVATE);
        String username = prefs.getString(KEY_NAME, "Null");
        welcome_txt.setText("Welcome! " + username);

        emergency.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, EmergencyActivity.class);
            Toast toast = Toast.makeText(HomeActivity.this,
                    "!!! Emergency Activated !!!",
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 100); // show at bottom
            toast.show();
            startActivity(intent);
        });
        symptom_check.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SymptomChecker.class);
            Toast toast = Toast.makeText(HomeActivity.this,
                    "Symptom Checker ???",
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 100); // show at bottom
            toast.show();
            startActivity(intent);
        });

        training.setOnClickListener(v -> {
            String url = "https://youtu.be/kFbvJkbUukQ?si=uqLAQg-tEFfY59NB";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }
}
