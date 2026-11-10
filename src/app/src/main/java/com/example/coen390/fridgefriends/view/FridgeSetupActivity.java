package com.example.coen390.fridgefriends.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.coen390.fridgefriends.R;
import com.example.coen390.fridgefriends.SensorMonitoringService;

public class FridgeSetupActivity extends AppCompatActivity {

    private int selectedIconResId = R.drawable.baseline_add_24;
    private String deviceName = "Unknown Device";
    private ActivityResultLauncher<Intent> iconSelectionLauncher;

    private int initialIconResId;
    private String selectedIconName = "";
    private boolean initialFreshness;
    private boolean initialMoisture;
    private boolean initialFermentation;
    private boolean isAdded;

    public static int resolveIconResId(android.content.Context context, String iconName, String deviceName) {
        if (iconName != null && !iconName.trim().isEmpty()) {
            int id = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
            if (id != 0) return id;
        }
        if ("Device 1".equalsIgnoreCase(deviceName)) return R.drawable.happy_apples;
        if ("Device 2".equalsIgnoreCase(deviceName)) return R.drawable.cheese_icon;
        if ("Device 3".equalsIgnoreCase(deviceName)) return R.drawable.fermentation_icon;
        return R.drawable.fridge_icon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fridge_setup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (getIntent() != null && getIntent().hasExtra("device_name")) {
            deviceName = getIntent().getStringExtra("device_name");
            TextView tvDeviceNameLabel = findViewById(R.id.tvDeviceNameLabel);
            if(tvDeviceNameLabel != null) {
                tvDeviceNameLabel.setText(deviceName);
            }
        }

        Button btnFreshness = findViewById(R.id.btnFreshness);
        Button btnMoisture = findViewById(R.id.btnMoisture);
        Button btnFermentation = findViewById(R.id.btnFermentation);

        ImageView ivSetupIcon = findViewById(R.id.ivSetupIcon);

        SharedPreferences prefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        isAdded = prefs.getBoolean(deviceName + "_added", false);
        String storedIconName = prefs.getString(deviceName + "_icon_name", "");
        selectedIconResId = resolveIconResId(this, storedIconName, deviceName);

        if (isAdded) {
            initialFreshness = prefs.getBoolean(deviceName + "_track_freshness", false);
            initialMoisture = prefs.getBoolean(deviceName + "_track_moisture", false);
            initialFermentation = prefs.getBoolean(deviceName + "_track_fermentation", false);
            
            btnFreshness.setSelected(initialFreshness);
            btnMoisture.setSelected(initialMoisture);
            btnFermentation.setSelected(initialFermentation);
        } else {
            // Default state: Freshness selected by default
            initialFreshness = true;
            initialMoisture = false;
            initialFermentation = false;
            
            btnFreshness.setSelected(true);
            btnMoisture.setSelected(false);
            btnFermentation.setSelected(false);
        }
        
        if (ivSetupIcon != null) {
            ivSetupIcon.setImageResource(selectedIconResId);
        }

        // Toggle selected state on click
        btnFreshness.setOnClickListener(v -> btnFreshness.setSelected(!btnFreshness.isSelected()));
        btnMoisture.setOnClickListener(v -> btnMoisture.setSelected(!btnMoisture.isSelected()));
        btnFermentation.setOnClickListener(v -> btnFermentation.setSelected(!btnFermentation.isSelected()));

        iconSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedIconResId = result.getData().getIntExtra("selected_icon", R.drawable.baseline_add_24);
                    selectedIconName = result.getData().getStringExtra("selected_icon_name");
                    if (ivSetupIcon != null) {
                        ivSetupIcon.setImageResource(selectedIconResId);
                    }
                }
            }
        );

        // Make the 'add icon' box selectable to open the Icon Selection page
        View viewIconBox = findViewById(R.id.viewIconBox);
        TextView tvEditIcon = findViewById(R.id.tvEditIcon);
        View.OnClickListener editIconListener = v -> {
            Intent intent = new Intent(FridgeSetupActivity.this, IconSelectionActivity.class);
            iconSelectionLauncher.launch(intent);
        };
        viewIconBox.setOnClickListener(editIconListener);
        if (tvEditIcon != null) tvEditIcon.setOnClickListener(editIconListener);

        ImageButton btnBackSetup = findViewById(R.id.btnBackSetup);
        if (btnBackSetup != null) {
            btnBackSetup.setOnClickListener(v -> handleBackPress());
            btnBackSetup.setVisibility(View.VISIBLE);
        }

        Button btnRemoveDevice = findViewById(R.id.btnRemoveDevice);
        btnRemoveDevice.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(deviceName + "_added", false);
            editor.remove(deviceName + "_icon");
            editor.remove(deviceName + "_track_freshness");
            editor.remove(deviceName + "_track_moisture");
            editor.remove(deviceName + "_track_fermentation");
            editor.apply();

            // Keep monitoring if another sensor is still configured.
            boolean anyDevice = prefs.getBoolean("Device 1_added", false)
                                || prefs.getBoolean("Device 2_added", false)
                                || prefs.getBoolean("Device 3_added", false);

            // !!! Stop monitoring when the last sensor is removed. !!!
            if (!anyDevice)
            {
                stopService(new Intent(
                        FridgeSetupActivity.this,
                        SensorMonitoringService.class
                ));
            }

            Intent intent = new Intent(FridgeSetupActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {
            boolean trackFreshness = btnFreshness.isSelected();
            boolean trackMoisture = btnMoisture.isSelected();
            boolean trackFermentation = btnFermentation.isSelected();

            if (!trackFreshness && !trackMoisture && !trackFermentation) {
                Toast.makeText(this, "Please select at least 1 option to track.", Toast.LENGTH_LONG).show();
                return;
            }

            // Save device to SharedPreferences so it doesn't appear in AddDeviceActivity again
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(deviceName + "_added", true);
            editor.putInt(deviceName + "_icon", selectedIconResId);
            if (selectedIconName != null && !selectedIconName.isEmpty()) {
                editor.putString(deviceName + "_icon_name", selectedIconName);
            }
            editor.putBoolean(deviceName + "_track_freshness", trackFreshness);
            editor.putBoolean(deviceName + "_track_moisture", trackMoisture);
            editor.putBoolean(deviceName + "_track_fermentation", trackFermentation);
            editor.apply();

            // Start the monitoring service so it can receive sensor readings
            // even when the monitoring screen is no longer visible.
            Intent serviceIntent =
                    new Intent(FridgeSetupActivity.this,
                            SensorMonitoringService.class);

            ContextCompat.startForegroundService(
                    FridgeSetupActivity.this,
                    serviceIntent
            );

            Intent intent = new Intent(FridgeSetupActivity.this, DeviceTrackerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("device_name", deviceName);
            intent.putExtra("selected_icon", selectedIconResId);
            intent.putExtra("track_freshness", trackFreshness);
            intent.putExtra("track_moisture", trackMoisture);
            intent.putExtra("track_fermentation", trackFermentation);
            startActivity(intent);
            finish();
        });
    }

    private void handleBackPress()
    {
        Button btnFreshness = findViewById(R.id.btnFreshness);
        Button btnMoisture = findViewById(R.id.btnMoisture);
        Button btnFermentation = findViewById(R.id.btnFermentation);

        boolean hasChanges = (selectedIconResId != initialIconResId) ||
                (btnFreshness.isSelected() != initialFreshness) ||
                (btnMoisture.isSelected() != initialMoisture) ||
                (btnFermentation.isSelected() != initialFermentation);

        if (hasChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Are you sure you want to exit without saving?")
                    .setPositiveButton("Yes", (dialog, which) -> exitActivity())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            exitActivity();
        }
    }

    private void exitActivity() {
        if (isAdded) {
            finish();
        } else {
            Intent intent = new Intent(FridgeSetupActivity.this, AddDeviceActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }
}