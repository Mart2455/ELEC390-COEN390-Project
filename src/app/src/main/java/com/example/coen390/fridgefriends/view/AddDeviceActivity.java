package com.example.coen390.fridgefriends.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.coen390.fridgefriends.R;

public class AddDeviceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Button btnDevice1 = findViewById(R.id.btnDevice1);
        Button btnDevice2 = findViewById(R.id.btnDevice2);
        Button btnDevice3 = findViewById(R.id.btnDevice3);

        SharedPreferences prefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        boolean d1Added = prefs.getBoolean("Device 1_added", false);
        boolean d2Added = prefs.getBoolean("Device 2_added", false);
        boolean d3Added = prefs.getBoolean("Device 3_added", false);

        if (d1Added) btnDevice1.setVisibility(View.GONE);
        if (d2Added) btnDevice2.setVisibility(View.GONE);
        if (d3Added) btnDevice3.setVisibility(View.GONE);

        android.widget.TextView tvTempAdd = findViewById(R.id.tvTempAdd);
        if (tvTempAdd != null) {
            tvTempAdd.setVisibility((d1Added || d2Added || d3Added) ? View.VISIBLE : View.GONE);
        }

        if (d1Added && d2Added && d3Added) {
            Toast.makeText(this, "All devices have been added!", Toast.LENGTH_LONG).show();
        }

        android.widget.ImageButton navFridgeAdd = findViewById(R.id.navFridgeAdd);
        if (navFridgeAdd != null) {
            navFridgeAdd.setOnClickListener(v -> {
                Intent intent = new Intent(AddDeviceActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        // Toggle selection state for each pill button individually
        View.OnClickListener deviceClickListener = v -> {
            Intent intent = new Intent(AddDeviceActivity.this, FridgeSetupActivity.class);
            if (v.getId() == R.id.btnDevice1) intent.putExtra("device_name", "Device 1");
            else if (v.getId() == R.id.btnDevice2) intent.putExtra("device_name", "Device 2");
            else if (v.getId() == R.id.btnDevice3) intent.putExtra("device_name", "Device 3");
            startActivity(intent);
            finish();
        };

        btnDevice1.setOnClickListener(deviceClickListener);
        btnDevice2.setOnClickListener(deviceClickListener);
        btnDevice3.setOnClickListener(deviceClickListener);
    }
}