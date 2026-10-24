package com.example.coen390.fridgefriends.view;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.coen390.fridgefriends.R;

public class IconSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_selection);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        android.content.SharedPreferences prefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        boolean anyDeviceAdded = prefs.getBoolean("Device 1_added", false) || 
                                 prefs.getBoolean("Device 2_added", false) || 
                                 prefs.getBoolean("Device 3_added", false);
        android.widget.TextView tvTempIcon = findViewById(R.id.tvTempIcon);
        if (tvTempIcon != null) {
            tvTempIcon.setVisibility(anyDeviceAdded ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        // Back arrow returns to the previous tracker menu
        ImageButton btnBack = findViewById(R.id.btnBackTracker);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout optionFruits = findViewById(R.id.optionFruits);
        LinearLayout optionCheeses = findViewById(R.id.optionCheeses);
        LinearLayout optionDeli = findViewById(R.id.optionDeli);
        LinearLayout optionGreens = findViewById(R.id.optionGreens);
        LinearLayout optionFermented = findViewById(R.id.optionFermented);

        optionFruits.setOnClickListener(v -> returnIconResult(R.drawable.happy_apples));
        optionCheeses.setOnClickListener(v -> returnIconResult(R.drawable.cheese_icon));
        optionDeli.setOnClickListener(v -> returnIconResult(R.drawable.meat_icon));
        optionGreens.setOnClickListener(v -> returnIconResult(R.drawable.veggie_icon));
        optionFermented.setOnClickListener(v -> returnIconResult(R.drawable.fermentation_icon));

        ImageButton navFridgeIcon = findViewById(R.id.navFridgeIcon);
        if (navFridgeIcon != null) {
            navFridgeIcon.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(IconSelectionActivity.this, MainActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }
    }

    private void returnIconResult(int iconResId) {
        android.content.Intent resultIntent = new android.content.Intent();
        resultIntent.putExtra("selected_icon", iconResId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}