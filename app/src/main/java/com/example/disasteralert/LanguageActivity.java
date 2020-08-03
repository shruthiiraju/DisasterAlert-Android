package com.example.disasteralert;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.Locale;

public class LanguageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //This method is used so that your splash activity
        //can cover the entire screen.


        setContentView(R.layout.activity_language);
        //this will bind your MainActivity.class file with activity_main.
        LinearLayout root_layout = findViewById(R.id.root_layout);
        AnimationDrawable animDrawable = (AnimationDrawable) root_layout.getBackground();
        animDrawable.setEnterFadeDuration(1000);
        animDrawable.setExitFadeDuration(5000);
        animDrawable.start();

        CardView hindiButton = findViewById(R.id.hindiButton);
        CardView engButton = findViewById(R.id.englishButton);
        CardView kanButton = findViewById(R.id.kanButton);

        hindiButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                setLocale("hn");
                Intent goToLoginActivity = new Intent(LanguageActivity.this, LoginActivity.class);
                LanguageActivity.this.startActivity(goToLoginActivity);
            }
        });

        engButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                setLocale("en");
                Intent goToLoginActivity = new Intent(LanguageActivity.this, LoginActivity.class);
                LanguageActivity.this.startActivity(goToLoginActivity);
            }
        });
        kanButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                setLocale("ka");
                Intent goToLoginActivity = new Intent(LanguageActivity.this, LoginActivity.class);
                LanguageActivity.this.startActivity(goToLoginActivity);
            }
        });
    }
    public void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }
}