package com.example.coen390.fridgefriends.view;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.coen390.fridgefriends.R;
import com.example.coen390.fridgefriends.MonitoringState;

import java.util.ArrayList;
import java.util.List;


public class DeviceTrackerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private ImageView[] dots;
    private List<TrackerData> trackerList;
    private int currentIconResId = R.drawable.baseline_add_24;

    private boolean connectionLost = false;
    private boolean hasReadings = false;
    private TrackerPagerAdapter adapter;
    private final MonitoringState.Listener monitoringListener = this::updateReadings;

    @Override
    protected void onStart() {
        super.onStart();
        MonitoringState.observe(monitoringListener);
    }

    @Override
    protected void onStop() {
        MonitoringState.removeObserver(monitoringListener);
        super.onStop();
    }

    // The service owns HTTP requests; this screen only renders its latest result.
    private void updateReadings(MonitoringState.Snapshot state) {
        android.widget.TextView textTemperatureView = findViewById(R.id.tvTempIcon);
        if (!state.hasReadings)
        {
            textTemperatureView.setText("--°C");
        }
        else
        {
            textTemperatureView.setText(String.format(java.util.Locale.getDefault(), "%.1f°C", state.temperature));
        }
        connectionLost = state.attempted && !state.connected;
        hasReadings = state.hasReadings;
        if (hasReadings) {
            for (TrackerData item : trackerList) {
                switch (item.name) {
                    case "Moisture":
                        item.value = Double.isNaN(state.humidity)
                                ? "Reading unavailable" : state.humidity + " %";
                        break;
                    case "Freshness":
                        item.value = state.tvoc < 0
                                ? "Reading unavailable" : state.tvoc + " ppb";
                        break;
                    case "Fermentation":
                        item.value = "eCO2: " + (state.eco2 < 0 ? "unavailable" : state.eco2 + " ppm")
                                + "\nMQ-3: " + (state.mq3Raw < 0 ? "unavailable" : state.mq3Raw + " raw");
                        break;
                }
                item.unavailable = (item.name.equals("Moisture") && Double.isNaN(state.humidity))
                        || (item.name.equals("Freshness") && state.tvoc < 0)
                        || (item.name.equals("Fermentation") && (state.eco2 < 0 || state.mq3Raw < 0));
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_tracker);

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().hide();
        }

        ImageButton btnBack = findViewById(R.id.btnBackTracker);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(DeviceTrackerActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        viewPager = findViewById(R.id.viewPagerTrackers);
        layoutDots = findViewById(R.id.layoutDots);

        ImageButton btnOptionsTracker = findViewById(R.id.btnOptionsTracker);
        RelativeLayout overlayOptionsMenuTracker = findViewById(R.id.overlayOptionsMenuTracker);
        Button btnEditDeviceTracker = findViewById(R.id.btnEditDeviceTracker);
        ImageButton btnOverlayCloseTracker = findViewById(R.id.btnOverlayCloseTracker);

        btnOptionsTracker.setOnClickListener(v -> {
            overlayOptionsMenuTracker.setVisibility(View.VISIBLE);
            Animation scaleUp = AnimationUtils.loadAnimation(DeviceTrackerActivity.this, R.anim.scale_up);
            btnEditDeviceTracker.startAnimation(scaleUp);
        });

        String intentDeviceName = getIntent() != null ? getIntent().getStringExtra("device_name") : null;
        final String finalDeviceName = (intentDeviceName != null) ? intentDeviceName : "Unknown Device";

        btnEditDeviceTracker.setOnClickListener(v -> {
            overlayOptionsMenuTracker.setVisibility(View.GONE);
            Intent intent = new Intent(DeviceTrackerActivity.this, FridgeSetupActivity.class);
            intent.putExtra("device_name", finalDeviceName);
            startActivity(intent);
        });

        btnOverlayCloseTracker.setOnClickListener(v -> overlayOptionsMenuTracker.setVisibility(View.GONE));
        overlayOptionsMenuTracker.setOnClickListener(v -> overlayOptionsMenuTracker.setVisibility(View.GONE));

        ImageButton navFridgeTracker = findViewById(R.id.navFridgeTracker);
        if (navFridgeTracker != null) {
            navFridgeTracker.setSelected(true);
            navFridgeTracker.setOnClickListener(v -> {
                Intent intent = new Intent(DeviceTrackerActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }
        
        ImageButton navCartTracker = findViewById(R.id.navCartTracker);
        if (navCartTracker != null) {
            navCartTracker.setOnClickListener(v -> {
                Intent intent = new Intent(DeviceTrackerActivity.this, GroceryListActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Read intent extras
        String deviceName = finalDeviceName;
        boolean trackFreshness = false;
        boolean trackMoisture = false;
        boolean trackFermentation = false;

        SharedPreferences devicePrefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        String storedIconName = devicePrefs.getString(deviceName + "_icon_name", "");
        currentIconResId = FridgeSetupActivity.resolveIconResId(this, storedIconName, deviceName);

        if (getIntent() != null) {
            trackFreshness = getIntent().getBooleanExtra("track_freshness", false);
            trackMoisture = getIntent().getBooleanExtra("track_moisture", false);
            trackFermentation = getIntent().getBooleanExtra("track_fermentation", false);
        }

        // Apply device name and icon
        TextView tvDeviceNameTracker = findViewById(R.id.tvDeviceNameTracker);
        if (tvDeviceNameTracker != null) tvDeviceNameTracker.setText(deviceName);

        ImageView ivSelectedIcon = findViewById(R.id.ivSelectedIcon);
        if (ivSelectedIcon != null) ivSelectedIcon.setImageResource(currentIconResId);

        // Create pages before requesting data so first-request failures are visible.
        trackerList = new ArrayList<>();

        if (trackMoisture)
        {
            trackerList.add(new TrackerData(
                    "Moisture", "Humidity levels", "#7EC8E3",
                    "No readings available", R.drawable.moisture_icon));
        }
        if (trackFreshness)
        {
            trackerList.add(new TrackerData(
                    "Freshness", "VOC levels", "#85B751",
                    "No readings available", R.drawable.veggie_icon));
        }
        if (trackFermentation)
        {
            trackerList.add(new TrackerData(
                    "Fermentation", "CO2 and ethanol levels", "#9D84B7",
                    "No readings available", R.drawable.voc_icon));
        }

        adapter = new TrackerPagerAdapter(trackerList);
        viewPager.setAdapter(adapter);
        setupDots(trackerList.size());

        // Listen for swipes to change the active dot color
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DeviceTrackerActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    // --- DOT INDICATOR LOGIC ---
    private void setupDots(int count) {
        dots = new ImageView[count];
        layoutDots.removeAllViews();

        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageResource(R.drawable.bg_circle_button);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(24, 24);
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);

            layoutDots.addView(dots[i]);
        }
        if (dots.length > 0) updateDots(0);
    }

    private void updateDots(int activePosition) {
        for (int i = 0; i < dots.length; i++) {
            // Dark green for active dot, soft earthy white/beige for inactive dot
            if (i == activePosition) {
                dots[i].setColorFilter(ContextCompat.getColor(this, R.color.dark_green));
            } else {
                dots[i].setColorFilter(Color.parseColor("#D8D4CB"));
            }
        }
    }

    // --- INNER DATA MODEL ---
    class TrackerData {
        String name, subtitle, colorHex, value;
        int iconResId;
        boolean unavailable;

        TrackerData(String name, String subtitle, String colorHex, String value, int iconResId) {
            this.name = name;
            this.subtitle = subtitle;
            this.colorHex = colorHex;
            this.value = value;
            this.iconResId = iconResId;
        }
    }

    // --- VIEWPAGER ADAPTER (INNER CLASS) ---
    class TrackerPagerAdapter extends RecyclerView.Adapter<TrackerPagerAdapter.ViewHolder> {
        List<TrackerData> data;
        TrackerPagerAdapter(List<TrackerData> data) { this.data = data; }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tracker_page, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TrackerData item = data.get(position);
            // Here it says the tracking name maybe i should put the error message here

            holder.tvTitle.setText("Tracking: " + item.name);

            if (connectionLost)
            {
                holder.tvStatus.setText(hasReadings
                        ? "Connection lost — readings outdated"
                        : "Unable to connect — no readings available");
                holder.tvStatus.setTextColor(Color.RED);
            }
            else if (item.unavailable)
            {
                holder.tvStatus.setText("Sensor reading unavailable");
                holder.tvStatus.setTextColor(Color.RED);
            }
            else
            {
                holder.tvStatus.setText(hasReadings ? "Connected" : "Connecting...");
                holder.tvStatus.setTextColor(Color.parseColor(item.colorHex));
            }

            holder.tvSubtitle.setText(item.subtitle);
            holder.tvValue.setText(item.value);

            // Set the specific icon for this page
            holder.ivIcon.setImageResource(item.iconResId);

            // Set dynamic colors based on the tracker type
            int color = Color.parseColor(item.colorHex);
            holder.tvValue.setTextColor(color);
            holder.ivIcon.setColorFilter(color);
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvStatus, tvValue;
            ImageView ivIcon;
            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTrackingTitle);
                tvSubtitle = itemView.findViewById(R.id.tvTrackingSubtitle);
                tvStatus = itemView.findViewById(R.id.tvStatusLabel);
                tvValue = itemView.findViewById(R.id.tvSensorValue);
                ivIcon = itemView.findViewById(R.id.ivTrackerIcon);
            }
        }
    }
}