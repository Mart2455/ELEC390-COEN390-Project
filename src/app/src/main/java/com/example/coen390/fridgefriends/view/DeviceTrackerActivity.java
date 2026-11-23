package com.example.coen390.fridgefriends.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coen390.fridgefriends.MonitoringState;
import com.example.coen390.fridgefriends.R;
import com.example.coen390.fridgefriends.controller.DatabaseHelper;
import com.example.coen390.fridgefriends.model.DateParser;
import com.example.coen390.fridgefriends.model.GroceryItem;
import com.example.coen390.fridgefriends.model.ProductImageResolver;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DeviceTrackerActivity extends AppCompatActivity {

    private String mDeviceName = "Unknown Device";
    private int mCurrentIconResId = R.drawable.baseline_add_24;

    private LinearLayout mLayoutStatGrid;
    private RecyclerView mRvTrackedItems;
    private TextView mTvNoTrackedItems;
    private DatabaseHelper mDatabaseHelper;
    private TrackedItemsAdapter mTrackedItemsAdapter;

    private final List<GroceryItem> mAssignedItems = new ArrayList<>();
    private final List<StatCardHolder> mStatCardHolders = new ArrayList<>();

    private boolean mConnectionLost = false;
    private boolean mHasReadings = false;
    private final MonitoringState.Listener mMonitoringListener = this::updateReadings;

    @Override
    protected void onStart() {
        super.onStart();
        MonitoringState.observe(mMonitoringListener);
    }

    @Override
    protected void onStop() {
        MonitoringState.removeObserver(mMonitoringListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAssignedItems();
    }

    // Real-time sensor readings update
    private void updateReadings(MonitoringState.Snapshot state) {
        TextView textTemperatureView = findViewById(R.id.tvTempIcon);
        if (!state.hasReadings) {
            textTemperatureView.setText("--°C");
        } else {
            textTemperatureView.setText(String.format(java.util.Locale.getDefault(), "%.1f°C", state.temperature));
        }

        mConnectionLost = state.attempted && !state.connected;
        mHasReadings = state.hasReadings;

        for (StatCardHolder holder : mStatCardHolders) {
            int mainColor = Color.parseColor(holder.colorHex);

            if (!state.attempted) {
                holder.tvStatus.setText("Connecting...");
                holder.tvStatus.setTextColor(mainColor);
                holder.tvValue.setText("No readings available");
                continue;
            }

            if (mConnectionLost) {
                if (!mHasReadings) {
                    holder.tvStatus.setText("Unable to connect — no readings available");
                    holder.tvStatus.setTextColor(Color.RED);
                    holder.tvValue.setText("No readings available");
                } else {
                    holder.tvStatus.setText("Connection lost — readings outdated");
                    holder.tvStatus.setTextColor(Color.RED);
                    // Retain last reading value in tvValue
                }
                continue;
            }

            switch (holder.type) {
                case "Moisture":
                    if (Double.isNaN(state.humidity)) {
                        holder.tvStatus.setText("Sensor reading unavailable");
                        holder.tvStatus.setTextColor(Color.RED);
                        holder.tvValue.setText("Reading unavailable");
                    } else {
                        holder.tvStatus.setText("Connected");
                        holder.tvStatus.setTextColor(mainColor);
                        holder.tvValue.setText(String.format(java.util.Locale.getDefault(), "%.1f %%", state.humidity));
                    }
                    break;
                case "Freshness":
                    if (state.tvoc < 0) {
                        holder.tvStatus.setText("Sensor reading unavailable");
                        holder.tvStatus.setTextColor(Color.RED);
                        holder.tvValue.setText("Reading unavailable");
                    } else {
                        holder.tvStatus.setText("Connected");
                        holder.tvStatus.setTextColor(mainColor);
                        holder.tvValue.setText(state.tvoc + " ppb");
                    }
                    break;
                case "Fermentation":
                    if (state.eco2 < 0 || state.mq3Raw < 0) {
                        holder.tvStatus.setText("Sensor reading unavailable");
                        holder.tvStatus.setTextColor(Color.RED);
                        holder.tvValue.setText("Reading unavailable");
                    } else {
                        holder.tvStatus.setText("Connected");
                        holder.tvStatus.setTextColor(mainColor);
                        holder.tvValue.setText("eCO2: " + state.eco2 + " ppm\nMQ-3: " + state.mq3Raw + " raw");
                    }
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_tracker);

        mDatabaseHelper = DatabaseHelper.getInstance(this);

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
        mDeviceName = finalDeviceName;

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

        ImageButton navSettingsTracker = findViewById(R.id.navSettingsTracker);
        if (navSettingsTracker != null) {
            navSettingsTracker.setOnClickListener(v -> {
                Intent intent = new Intent(DeviceTrackerActivity.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Read intent extras & device state
        String deviceName = finalDeviceName;
        boolean trackFreshness = false;
        boolean trackMoisture = false;
        boolean trackFermentation = false;

        SharedPreferences devicePrefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        String storedIconName = devicePrefs.getString(deviceName + "_icon_name", "");
        mCurrentIconResId = FridgeSetupActivity.resolveIconResId(this, storedIconName, deviceName);

        if (getIntent() != null) {
            trackFreshness = getIntent().getBooleanExtra("track_freshness", false);
            trackMoisture = getIntent().getBooleanExtra("track_moisture", false);
            trackFermentation = getIntent().getBooleanExtra("track_fermentation", false);
        }

        // Apply device name and icon
        TextView tvDeviceNameTracker = findViewById(R.id.tvDeviceNameTracker);
        if (tvDeviceNameTracker != null) tvDeviceNameTracker.setText(deviceName);

        ImageView ivSelectedIcon = findViewById(R.id.ivSelectedIcon);
        if (ivSelectedIcon != null) ivSelectedIcon.setImageResource(mCurrentIconResId);

        // Setup Stat Grid Container
        mLayoutStatGrid = findViewById(R.id.layoutStatGrid);
        setupStatGrid(trackMoisture, trackFreshness, trackFermentation);

        // Setup Tracked Produce Items RecyclerView
        mRvTrackedItems = findViewById(R.id.rvTrackedItems);
        mTvNoTrackedItems = findViewById(R.id.tvNoTrackedItems);

        if (mRvTrackedItems != null) {
            mRvTrackedItems.setLayoutManager(new LinearLayoutManager(this));
            mTrackedItemsAdapter = new TrackedItemsAdapter(mAssignedItems);
            mRvTrackedItems.setAdapter(mTrackedItemsAdapter);
        }

        // Setup Scroll Listener to hide floating 3-dot options button near the bottom
        NestedScrollView nestedScrollViewTracker = findViewById(R.id.nestedScrollViewTracker);
        if (nestedScrollViewTracker != null && btnOptionsTracker != null) {
            nestedScrollViewTracker.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                int scrollRange = v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight();

                // If scrolling down or approaching bottom, smoothly hide options button
                if (scrollY > oldScrollY + 8 || (scrollRange > 0 && scrollY >= scrollRange - 60)) {
                    if (btnOptionsTracker.getVisibility() == View.VISIBLE) {
                        btnOptionsTracker.animate()
                                .alpha(0f)
                                .scaleX(0.7f)
                                .scaleY(0.7f)
                                .setDuration(150)
                                .withEndAction(() -> btnOptionsTracker.setVisibility(View.GONE));
                    }
                } else if (scrollY < oldScrollY - 8) {
                    // Scrolling up towards top, reveal options button
                    if (btnOptionsTracker.getVisibility() != View.VISIBLE) {
                        btnOptionsTracker.setVisibility(View.VISIBLE);
                        btnOptionsTracker.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150);
                    }
                }
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DeviceTrackerActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        loadAssignedItems();
    }

    /**
     * Builds a balanced, symmetric 2-column or adaptive stat grid displaying only
     * the tracking parameters enabled for this device.
     */
    private void setupStatGrid(boolean trackMoisture, boolean trackFreshness, boolean trackFermentation) {
        if (mLayoutStatGrid == null) return;
        mLayoutStatGrid.removeAllViews();
        mStatCardHolders.clear();

        List<StatCardHolder> activeHolders = new ArrayList<>();

        if (trackMoisture) {
            activeHolders.add(createStatCardHolder("Moisture", "Humidity levels", "#7EC8E3", R.drawable.moisture_icon));
        }
        if (trackFreshness) {
            activeHolders.add(createStatCardHolder("Freshness", "VOC levels", "#85B751", R.drawable.veggie_icon));
        }
        if (trackFermentation) {
            activeHolders.add(createStatCardHolder("Fermentation", "CO2 & ethanol", "#9D84B7", R.drawable.voc_icon));
        }

        mStatCardHolders.addAll(activeHolders);

        int total = activeHolders.size();
        if (total == 0) return;

        if (total == 1) {
            LinearLayout row = createRowLayout();
            View card = activeHolders.get(0).cardView;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            card.setLayoutParams(lp);
            row.addView(card);
            mLayoutStatGrid.addView(row);
        } else if (total == 2) {
            LinearLayout row = createRowLayout();
            for (int i = 0; i < 2; i++) {
                View card = activeHolders.get(i).cardView;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                if (i == 0) lp.setMarginEnd(dpToPx(6));
                else lp.setMarginStart(dpToPx(6));
                card.setLayoutParams(lp);
                row.addView(card);
            }
            mLayoutStatGrid.addView(row);
        } else if (total == 3) {
            // Row 1: 2 Cards side-by-side
            LinearLayout row1 = createRowLayout();
            for (int i = 0; i < 2; i++) {
                View card = activeHolders.get(i).cardView;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                if (i == 0) lp.setMarginEnd(dpToPx(6));
                else lp.setMarginStart(dpToPx(6));
                card.setLayoutParams(lp);
                row1.addView(card);
            }
            mLayoutStatGrid.addView(row1);

            // Row 2: 1 Card centered / full-width
            LinearLayout row2 = createRowLayout();
            row2.setPadding(0, dpToPx(10), 0, 0);
            View card = activeHolders.get(2).cardView;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            card.setLayoutParams(lp);
            row2.addView(card);
            mLayoutStatGrid.addView(row2);
        }
    }

    private LinearLayout createRowLayout() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private StatCardHolder createStatCardHolder(String title, String subtitle, String colorHex, int iconResId) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_stat_card, mLayoutStatGrid, false);

        StatCardHolder holder = new StatCardHolder();
        holder.type = title;
        holder.cardView = cardView;
        holder.ivIcon = cardView.findViewById(R.id.ivTrackerIcon);
        holder.tvTitle = cardView.findViewById(R.id.tvTrackingTitle);
        holder.tvSubtitle = cardView.findViewById(R.id.tvTrackingSubtitle);
        holder.tvStatus = cardView.findViewById(R.id.tvStatusLabel);
        holder.tvValue = cardView.findViewById(R.id.tvSensorValue);
        holder.colorHex = colorHex;

        int color = Color.parseColor(colorHex);
        holder.tvTitle.setText("Tracking: " + title);
        holder.tvSubtitle.setText(subtitle);
        holder.ivIcon.setImageResource(iconResId);
        holder.ivIcon.setColorFilter(color);
        holder.tvStatus.setTextColor(color);
        holder.tvStatus.setText("Connecting...");
        holder.tvValue.setText("No readings available");

        return holder;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void loadAssignedItems() {
        if (mDatabaseHelper == null || mDeviceName == null) return;
        mDatabaseHelper.syncFromSharedPreferences();

        List<GroceryItem> items = mDatabaseHelper.getItemsByDevice(mDeviceName);
        mAssignedItems.clear();
        if (items != null) {
            mAssignedItems.addAll(items);
        }

        if (mAssignedItems.isEmpty()) {
            if (mRvTrackedItems != null) mRvTrackedItems.setVisibility(View.GONE);
            if (mTvNoTrackedItems != null) mTvNoTrackedItems.setVisibility(View.VISIBLE);
        } else {
            if (mRvTrackedItems != null) mRvTrackedItems.setVisibility(View.VISIBLE);
            if (mTvNoTrackedItems != null) mTvNoTrackedItems.setVisibility(View.GONE);
            if (mTrackedItemsAdapter != null) mTrackedItemsAdapter.notifyDataSetChanged();
        }
    }

    private void fetchImageToView(String urlStr, ImageView imageView) {
        if (urlStr == null || urlStr.trim().isEmpty() || imageView == null) {
            return;
        }
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                InputStream is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    // --- STAT CARD HOLDER CLASS ---
    static class StatCardHolder {
        String type;
        View cardView;
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvStatus;
        TextView tvValue;
        String colorHex;
    }

    // --- TRACKED PRODUCE ITEMS RECYCLERVIEW ADAPTER ---
    class TrackedItemsAdapter extends RecyclerView.Adapter<TrackedItemsAdapter.ViewHolder> {
        private final List<GroceryItem> items;

        TrackedItemsAdapter(List<GroceryItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tracked_produce, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GroceryItem item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvQuantity.setText(item.getQuantity() + " " + (item.getUnit() == null ? "pcs" : item.getUnit()));

            if (item.getExpiryDate() != null && !item.getExpiryDate().trim().isEmpty() && !item.getExpiryDate().equalsIgnoreCase("no expiration")) {
                holder.tvExpiry.setText(DateParser.formatDisplayDate(item.getExpiryDate()));
                holder.tvExpiry.setVisibility(View.VISIBLE);
            } else {
                holder.tvExpiry.setText("No expiry set");
                holder.tvExpiry.setVisibility(View.VISIBLE);
            }

            String imageUrl = item.getImageUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = ProductImageResolver.resolveImageUrl(item.getName(), item.getCategory());
            }
            fetchImageToView(imageUrl, holder.imgPhoto);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvQuantity, tvExpiry;
            ImageView imgPhoto;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvTrackedItemName);
                tvQuantity = itemView.findViewById(R.id.tvTrackedItemQuantity);
                tvExpiry = itemView.findViewById(R.id.tvTrackedItemExpiry);
                imgPhoto = itemView.findViewById(R.id.imgTrackedItemPhoto);
            }
        }
    }
}