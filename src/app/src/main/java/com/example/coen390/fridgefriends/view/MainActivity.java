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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;
import com.example.coen390.fridgefriends.R;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnPlus;
    private RelativeLayout overlayAddMenu;
    private Button btnAddDevice;
    private ImageButton btnOverlayClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide the default Action Bar to match your clean mockup
        if (getSupportActionBar() != null) {
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
            navCart.setSelected(true);
            navFridge.setSelected(false);
            navSettings.setSelected(false);
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
                item.iconResId = prefs.getInt(dName + "_icon", R.drawable.baseline_add_24);
                
                boolean trackFresh = prefs.getBoolean(dName + "_track_freshness", false);
                boolean trackMoist = prefs.getBoolean(dName + "_track_moisture", false);
                boolean trackFerm = prefs.getBoolean(dName + "_track_fermentation", false);
                
                if (trackFresh) { item.trackingText = "Freshness"; item.trackingColor = "#85B751"; }
                else if (trackMoist) { item.trackingText = "Moisture"; item.trackingColor = "#7EC8E3"; }
                else if (trackFerm) { item.trackingText = "Fermentation"; item.trackingColor = "#9D84B7"; }
                else { item.trackingText = "None"; item.trackingColor = "#777777"; }
                
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
            rvDevices.setAdapter(new DeviceAdapter(items));
        }
    }

    class DeviceItem {
        String name;
        int iconResId;
        String trackingText;
        String trackingColor;
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

            String fullText = "Tracking: " + item.trackingText;
            SpannableString spannable = new SpannableString(fullText);
            int startIndex = "Tracking: ".length();
            int color = Color.parseColor(item.trackingColor);
            spannable.setSpan(new ForegroundColorSpan(color), startIndex, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.tvTrackingMain.setText(spannable);

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
            TextView tvDeviceNameMain, tvTrackingMain;
            ImageView ivDeviceIconMain;
            ViewHolder(View itemView) {
                super(itemView);
                tvDeviceNameMain = itemView.findViewById(R.id.tvDeviceNameMain);
                tvTrackingMain = itemView.findViewById(R.id.tvTrackingMain);
                ivDeviceIconMain = itemView.findViewById(R.id.ivDeviceIconMain);
            }
        }
    }
}