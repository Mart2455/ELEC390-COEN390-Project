package com.example.coen390.fridgefriends.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;

import com.example.coen390.fridgefriends.SensorMonitoringService;
import com.example.coen390.fridgefriends.MonitoringState;
import com.example.coen390.fridgefriends.R;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity {

    private MonitoringState.Snapshot monitoringState;
    private DeviceAdapter deviceAdapter;
    private final MonitoringState.Listener monitoringListener = state -> {

        TextView textTemperatureView = findViewById(R.id.tvTempMain);   // this holds the value of the Text view

        // Update the textview to have the temperature value
        // Need to first verify that the readings are actually happening



        monitoringState = state;

        // Np readings
        if(!state.hasReadings)
        {
            textTemperatureView.setText("--°C");
        }
        else
        {
            // Important to convert to a string!!!
            textTemperatureView.setText(String.format(java.util.Locale.getDefault(), "%.1f°C",state.temperature));
        }
        if (deviceAdapter != null)
            deviceAdapter.notifyDataSetChanged();
    };

    @Override
    protected void onStart()
    {
        super.onStart();
        MonitoringState.observe(monitoringListener);
    }

    @Override
    protected void onStop()
    {

        MonitoringState.removeObserver(monitoringListener);
        super.onStop();
    }

    private ImageButton btnPlus;
    private RelativeLayout overlayAddMenu;
    private Button btnAddDevice;
    private ImageButton btnOverlayClose;

    //popup asking the user to allow notifs (Android 13+)
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                //nothing to do: if refused, AlertNotifier just won't show alerts
            });

    //ask only on Android 13+ and only if not already allowed
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        askNotificationPermission();

        // Make the monitoring reset everytime the app is turned on

        SharedPreferences prefs = getSharedPreferences("DevicePrefs" , MODE_PRIVATE);

        String[] devices = {"Device 1", "Device 2" , "Device 3"}; // each device is created

        // loop through the devices, activate the service
        for (int i = 0; i < devices.length; i++)
        {
            // return false if this device not connected, meaning sensoring device not setup
            boolean added = prefs.getBoolean(devices[i] + "_added" , false);

            // next is to verify if the sensor is actually tracking something

            boolean trackingEnabled = prefs.getBoolean(devices[i] + "_track_moisture", false)
                                                        || prefs.getBoolean(devices[i] + "_track_freshness", false)
                                                        || prefs.getBoolean(devices[i] + "_track_fermentation", false);

            // If both are true the sensor is atleast tracking a single thing

            if (added && trackingEnabled)
            {
                // Basically abstract class of an operation to be performed
                // !!! CAN BE USED BY SERVICE CLASS !!!
                Intent serviceIntent = new Intent(this, SensorMonitoringService.class);
                ContextCompat.startForegroundService(this, serviceIntent);
                break;
            }
        }

        // Hide the default Action Bar to match your clean mockup
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().hide();
        }

        // Link UI Elements
        btnPlus = findViewById(R.id.btnPlus);
        overlayAddMenu = findViewById(R.id.overlayAddMenu);
        btnAddDevice = findViewById(R.id.btnAddDevice);
        btnOverlayClose = findViewById(R.id.btnOverlayClose);

        // Click on '+' to show the dark overlay menu
        btnPlus.setOnClickListener(v -> {
            overlayAddMenu.setVisibility(View.VISIBLE);

            // Load the animation and apply it to the white button
            Animation scaleUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_up);
            btnAddDevice.startAnimation(scaleUp);
        });

        // 'Add device'
        btnAddDevice.setOnClickListener(v -> {
            overlayAddMenu.setVisibility(View.GONE);

            // Navigate to the AddDeviceActivity
            android.content.Intent intent = new android.content.Intent(MainActivity.this, AddDeviceActivity.class);
            startActivity(intent);
        });

        // Close the overlay if the user clicks the 'X'
        btnOverlayClose.setOnClickListener(v -> overlayAddMenu.setVisibility(View.GONE));

        // Close the overlay if the user clicks anywhere in the empty dark space
        overlayAddMenu.setOnClickListener(v -> overlayAddMenu.setVisibility(View.GONE));

        ImageButton navCart = findViewById(R.id.navCart);
        ImageButton navFridge = findViewById(R.id.navFridge);
        ImageButton navSettings = findViewById(R.id.navSettings);

        // Make the Fridge icon the active one by default when this page opens
        navFridge.setSelected(true);

        // Change colors when they click other icons
        navCart.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GroceryListActivity.class));
            overridePendingTransition(0, 0);
        });

        navFridge.setOnClickListener(v -> {
            navFridge.setSelected(true);
            navCart.setSelected(false);
            navSettings.setSelected(false);
        });

        navSettings.setOnClickListener(v -> {
            navSettings.setSelected(true);
            navCart.setSelected(false);
            navFridge.setSelected(false);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDevices();
    }

    private void loadDevices() {
        LinearLayout layoutEmptyState = findViewById(R.id.layoutEmptyState);
        LinearLayout layoutAddedDevices = findViewById(R.id.layoutAddedDevices);
        TextView tvTempMain = findViewById(R.id.tvTempMain);
        RecyclerView rvDevices = findViewById(R.id.rvDevices);

        SharedPreferences prefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        List<DeviceItem> items = new ArrayList<>();
        String[] possibleDevices = {"Device 1", "Device 2", "Device 3"};

        for (String dName : possibleDevices) {
            if (prefs.getBoolean(dName + "_added", false)) {
                DeviceItem item = new DeviceItem();
                item.name = dName;
                String iconName = prefs.getString(dName + "_icon_name", "");
                item.iconResId = FridgeSetupActivity.resolveIconResId(this, iconName, dName);
                
                boolean trackFresh = prefs.getBoolean(dName + "_track_freshness", false);
                boolean trackMoist = prefs.getBoolean(dName + "_track_moisture", false);
                boolean trackFerm = prefs.getBoolean(dName + "_track_fermentation", false);
                
                item.trackFreshness = trackFresh;
                item.trackMoisture = trackMoist;
                item.trackFermentation = trackFerm;

                StringBuilder sb = new StringBuilder();
                if (trackFresh) sb.append("Freshness");
                if (trackMoist) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append("Moisture");
                }
                if (trackFerm) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append("Fermentation");
                }

                if (sb.length() > 0) {
                    item.trackingText = sb.toString();
                    if (trackFresh) item.trackingColor = "#85B751";
                    else if (trackMoist) item.trackingColor = "#7EC8E3";
                    else item.trackingColor = "#9D84B7";
                } else {
                    item.trackingText = "None";
                    item.trackingColor = "#777777";
                }
                
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            layoutAddedDevices.setVisibility(View.GONE);
            tvTempMain.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            layoutAddedDevices.setVisibility(View.VISIBLE);
            tvTempMain.setVisibility(View.VISIBLE);

            rvDevices.setLayoutManager(new GridLayoutManager(this, 2));
            deviceAdapter = new DeviceAdapter(items);
            rvDevices.setAdapter(deviceAdapter);
        }
    }

    class DeviceItem {
        String name;
        int iconResId;
        String trackingText;
        String trackingColor;
        boolean trackFreshness, trackMoisture, trackFermentation;
    }

    class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
        List<DeviceItem> items;
        DeviceAdapter(List<DeviceItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main_device, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DeviceItem item = items.get(position);
            holder.tvDeviceNameMain.setText(item.name);
            holder.ivDeviceIconMain.setImageResource(item.iconResId);

            SpannableStringBuilder ssb = new SpannableStringBuilder("Tracking: ");
            boolean hasAny = false;

            if (item.trackFreshness) {
                int start = ssb.length();
                ssb.append("Freshness");
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#85B751")), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                hasAny = true;
            }

            if (item.trackMoisture) {
                if (hasAny) {
                    int commaStart = ssb.length();
                    ssb.append(", ");
                    ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#888888")), commaStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                int start = ssb.length();
                ssb.append("Moisture");
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#7EC8E3")), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                hasAny = true;
            }

            if (item.trackFermentation) {
                if (hasAny) {
                    int commaStart = ssb.length();
                    ssb.append(", ");
                    ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#888888")), commaStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                int start = ssb.length();
                ssb.append("Fermentation");
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#9D84B7")), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                hasAny = true;
            }

            if (!hasAny) {
                int start = ssb.length();
                ssb.append("None");
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#777777")), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            holder.tvTrackingMain.setText(ssb);

            String status;
            boolean warning = false;
            if (monitoringState == null || !monitoringState.attempted)
            {
                status = "Connecting...";
            }
            else if (!monitoringState.connected)
            {
                warning = true;
                status = monitoringState.hasReadings
                        ? "Connection lost — readings outdated"
                        : "Unable to connect — no readings available";
            }
            else if ((item.trackMoisture && Double.isNaN(monitoringState.humidity))
                    || (item.trackFreshness && monitoringState.tvoc < 0)
                    || (item.trackFermentation
                        && (monitoringState.eco2 < 0 || monitoringState.mq3Raw < 0)))
            {
                warning = true;
                status = "Sensor reading unavailable";
            }
            else
            {
                status = "Connected";
            }
            holder.tvConnectionStatusMain.setText(status);
            holder.tvConnectionStatusMain.setTextColor(
                    warning ? Color.RED : Color.parseColor("#777777"));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, DeviceTrackerActivity.class);
                intent.putExtra("device_name", item.name);
                
                SharedPreferences prefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
                intent.putExtra("selected_icon", prefs.getInt(item.name + "_icon", R.drawable.baseline_add_24));
                intent.putExtra("track_freshness", prefs.getBoolean(item.name + "_track_freshness", false));
                intent.putExtra("track_moisture", prefs.getBoolean(item.name + "_track_moisture", false));
                intent.putExtra("track_fermentation", prefs.getBoolean(item.name + "_track_fermentation", false));
                
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDeviceNameMain, tvTrackingMain, tvConnectionStatusMain;
            ImageView ivDeviceIconMain;
            ViewHolder(View itemView) {
                super(itemView);
                tvDeviceNameMain = itemView.findViewById(R.id.tvDeviceNameMain);
                tvTrackingMain = itemView.findViewById(R.id.tvTrackingMain);
                tvConnectionStatusMain = itemView.findViewById(R.id.tvConnectionStatusMain);
                ivDeviceIconMain = itemView.findViewById(R.id.ivDeviceIconMain);
            }
        }
    }
}
