package com.example.coen390.fridgefriends.view;

import android.graphics.Color;
import android.os.Bundle;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.coen390.fridgefriends.R;
import java.util.ArrayList;
import java.util.List;

public class DeviceTrackerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private ImageView[] dots;
    private List<TrackerData> trackerList;
    private int currentIconResId = R.drawable.baseline_add_24;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_tracker);

        if (getSupportActionBar() != null) {
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
            });
        }

        // Read intent extras
        String deviceName = finalDeviceName;
        boolean trackFreshness = false;
        boolean trackMoisture = false;
        boolean trackFermentation = false;

        if (getIntent() != null) {
            currentIconResId = getIntent().getIntExtra("selected_icon", R.drawable.baseline_add_24);
            trackFreshness = getIntent().getBooleanExtra("track_freshness", false);
            trackMoisture = getIntent().getBooleanExtra("track_moisture", false);
            trackFermentation = getIntent().getBooleanExtra("track_fermentation", false);
        }

        // Apply device name and icon
        TextView tvDeviceNameTracker = findViewById(R.id.tvDeviceNameTracker);
        if (tvDeviceNameTracker != null) tvDeviceNameTracker.setText(deviceName);

        ImageView ivSelectedIcon = findViewById(R.id.ivSelectedIcon);
        if (ivSelectedIcon != null) ivSelectedIcon.setImageResource(currentIconResId);

        // Initialize the data list for the swipeable pages based on selections
        trackerList = new ArrayList<>();

        if (trackMoisture) {
            trackerList.add(new TrackerData("Moisture", "Humidity levels", "#7EC8E3", "30% (low)", R.drawable.moisture_icon));
        }
        if (trackFreshness) {
            trackerList.add(new TrackerData("Freshness", "VOC levels", "#85B751", "< 10 ppm (fresh)", R.drawable.veggie_icon));
        }
        if (trackFermentation) {
            trackerList.add(new TrackerData("Fermentation", "CO2 and ethanol levels", "#9D84B7", "30% (low)", R.drawable.voc_icon));
        }

        // Set up the adapter
        TrackerPagerAdapter adapter = new TrackerPagerAdapter(trackerList);
        viewPager.setAdapter(adapter);

        setupDots(trackerList.size());

        // Listen for swipes to change the active dot color
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(DeviceTrackerActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
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
            // Dark green for active dot, light green for inactive
            if (i == activePosition) {
                dots[i].setColorFilter(Color.parseColor("#4A5D4E"));
            } else {
                dots[i].setColorFilter(Color.parseColor("#C8D5B9"));
            }
        }
    }

    // --- INNER DATA MODEL ---
    class TrackerData {
        String name, subtitle, colorHex, value;
        int iconResId;

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
            holder.tvTitle.setText("Tracking: " + item.name);
            holder.tvSubtitle.setText(item.subtitle);
            holder.tvValue.setText(item.value);

            // Set the specific icon for this page
            holder.ivIcon.setImageResource(item.iconResId);

            // Set dynamic colors based on the tracker type
            int color = Color.parseColor(item.colorHex);
            holder.tvStatus.setTextColor(color);
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