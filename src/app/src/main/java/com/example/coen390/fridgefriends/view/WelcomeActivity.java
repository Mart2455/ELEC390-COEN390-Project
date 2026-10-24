package com.example.coen390.fridgefriends.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.coen390.fridgefriends.R;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Hide the default top Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Apples jump :D
        ImageView ivApples = findViewById(R.id.ivApples);
        Animation appleJump = AnimationUtils.loadAnimation(this, R.anim.apple_jump);
        ivApples.startAnimation(appleJump);

        // YES button
        Button btnYes = findViewById(R.id.btnYes);
        btnYes.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}