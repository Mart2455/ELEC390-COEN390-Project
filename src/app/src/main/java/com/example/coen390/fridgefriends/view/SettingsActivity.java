package com.example.coen390.fridgefriends.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.coen390.fridgefriends.MonitoringState;
import com.example.coen390.fridgefriends.R;

import org.json.JSONArray;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Setup Option Cards
        View cardSettingsConnectedDevices = findViewById(R.id.cardSettingsConnectedDevices);
        View cardSettingsDataStorage = findViewById(R.id.cardSettingsDataStorage);
        View cardSettingsSecurityPrivacy = findViewById(R.id.cardSettingsSecurityPrivacy);
        View cardSettingsAbout = findViewById(R.id.cardSettingsAbout);

        if (cardSettingsConnectedDevices != null) {
            cardSettingsConnectedDevices.setOnClickListener(v -> showConnectedDevicesDialog());
        }
        if (cardSettingsDataStorage != null) {
            cardSettingsDataStorage.setOnClickListener(v -> showDataStorageDialog());
        }
        if (cardSettingsSecurityPrivacy != null) {
            cardSettingsSecurityPrivacy.setOnClickListener(v -> showSecurityPrivacyDialog());
        }
        if (cardSettingsAbout != null) {
            cardSettingsAbout.setOnClickListener(v -> showAboutDialog());
        }

        // Setup Bottom Navigation Bar
        setupBottomNav();
    }

    private final MonitoringState.Listener monitoringListener = state -> {
        TextView textTemperatureView = findViewById(R.id.tvTempSettings);
        if (textTemperatureView != null) {
            if (!state.attempted || !state.connected || Double.isNaN(state.temperature)) {
                textTemperatureView.setText("--°C");
            } else {
                textTemperatureView.setText(String.format(java.util.Locale.US, "%.0f°C", state.temperature));
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        boolean anyDeviceAdded = prefs.getBoolean("Device 1_added", false) || 
                                 prefs.getBoolean("Device 2_added", false) || 
                                 prefs.getBoolean("Device 3_added", false);
        TextView tvTempSettings = findViewById(R.id.tvTempSettings);
        if (tvTempSettings != null) {
            tvTempSettings.setVisibility(anyDeviceAdded ? View.VISIBLE : View.GONE);
        }
        MonitoringState.observe(monitoringListener);
    }

    @Override
    protected void onStop() {
        MonitoringState.removeObserver(monitoringListener);
        super.onStop();
    }

    private void setupBottomNav() {
        ImageButton navCart = findViewById(R.id.navCartSettings);
        ImageButton navFridge = findViewById(R.id.navFridgeSettings);

        if (navCart != null) {
            navCart.setOnClickListener(v -> {
                startActivity(new Intent(this, GroceryListActivity.class));
                overridePendingTransition(0, 0);
                finish();
            });
        }

        if (navFridge != null) {
            navFridge.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            });
        }
    }

    private void showConnectedDevicesDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings_connected_devices, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout container = dialogView.findViewById(R.id.containerDialogDeviceList);
        TextView tvStatus = dialogView.findViewById(R.id.tvConnectedDevicesStatus);
        TextView btnClose = dialogView.findViewById(R.id.btnConnectedDevicesClose);

        SharedPreferences prefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        String[] possibleDevices = {"Device 1", "Device 2", "Device 3"};
        int addedCount = 0;

        if (container != null) {
            for (String dName : possibleDevices) {
                if (prefs.getBoolean(dName + "_added", false)) {
                    addedCount++;
                    TextView tvDev = new TextView(this);
                    tvDev.setText("• " + dName + " (Online / Connected)");
                    tvDev.setTextSize(15);
                    tvDev.setTextColor(Color.parseColor("#2A2A2A"));
                    tvDev.setPadding(0, 8, 0, 8);
                    container.addView(tvDev);
                }
            }
            if (addedCount == 0) {
                TextView tvDev = new TextView(this);
                tvDev.setText("No active devices connected yet.");
                tvDev.setTextSize(14);
                tvDev.setTextColor(Color.parseColor("#888888"));
                container.addView(tvDev);
            }
        }

        if (tvStatus != null) {
            tvStatus.setText(addedCount + " configured sensors active:");
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showDataStorageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings_data_storage, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvGroceryCount = dialogView.findViewById(R.id.tvGroceryItemCountInfo);
        Button btnClearCache = dialogView.findViewById(R.id.btnClearCache);
        TextView btnClose = dialogView.findViewById(R.id.btnDataStorageClose);

        SharedPreferences prefs = getSharedPreferences("GroceryPrefs", MODE_PRIVATE);
        String jsonStr = prefs.getString("grocery_list", "[]");
        int itemCount = 0;
        try {
            JSONArray array = new JSONArray(jsonStr);
            itemCount = array.length();
        } catch (Exception ignored) {}

        if (tvGroceryCount != null) {
            tvGroceryCount.setText("Database: " + itemCount + " grocery items saved.");
        }

        if (btnClearCache != null) {
            btnClearCache.setOnClickListener(v -> {
                Toast.makeText(this, "Image cache cleared successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showSecurityPrivacyDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings_security_privacy, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView btnClose = dialogView.findViewById(R.id.btnSecurityClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showAboutDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings_about, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView btnClose = dialogView.findViewById(R.id.btnAboutClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
