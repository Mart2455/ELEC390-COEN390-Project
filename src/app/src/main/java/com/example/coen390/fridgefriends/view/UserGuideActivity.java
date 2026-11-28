package com.example.coen390.fridgefriends.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.coen390.fridgefriends.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserGuideActivity extends AppCompatActivity {

    private static class GuideTopic {
        String title;
        String category;
        int iconResId;
        String description;
        List<String> bullets;
        String tip;
        boolean isExpanded;

        GuideTopic(String title, String category, int iconResId, String description, List<String> bullets, String tip) {
            this.title = title;
            this.category = category;
            this.iconResId = iconResId;
            this.description = description;
            this.bullets = bullets;
            this.tip = tip;
            this.isExpanded = true;
        }
    }

    private final List<GuideTopic> allTopics = new ArrayList<>();
    private String selectedCategory = "ALL";
    private String searchQuery = "";

    private LinearLayout containerGuideTopics;
    private TextView tvGuideNoResults;
    private EditText etGuideSearch;
    private ImageButton btnClearGuideSearch;

    private TextView chipFilterAll, chipFilterNav, chipFilterSensors, chipFilterInventory, chipFilterAlerts, chipFilterSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        loadGuideTopics();
        setupSearchAndFilters();
        setupShortcutsAndNav();
        renderTopics();
    }

    private void initViews() {
        containerGuideTopics = findViewById(R.id.containerGuideTopics);
        tvGuideNoResults = findViewById(R.id.tvGuideNoResults);
        etGuideSearch = findViewById(R.id.etGuideSearch);
        btnClearGuideSearch = findViewById(R.id.btnClearGuideSearch);

        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterNav = findViewById(R.id.chipFilterNav);
        chipFilterSensors = findViewById(R.id.chipFilterSensors);
        chipFilterInventory = findViewById(R.id.chipFilterInventory);
        chipFilterAlerts = findViewById(R.id.chipFilterAlerts);
        chipFilterSetup = findViewById(R.id.chipFilterSetup);

        ImageButton btnBack = findViewById(R.id.btnBackUserGuide);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadGuideTopics() {
        allTopics.clear();

        // 1. Quick Start & Overview
        allTopics.add(new GuideTopic(
                "Quick Start & Overview",
                "Navigation",
                R.drawable.ic_user_guide,
                "Fridge Friends is your intelligent smart fridge assistant designed to eliminate food waste, monitor environmental conditions, and automate food inventory management.",
                Arrays.asList(
                        "Real-time sensor tracking for Temperature, Moisture, Freshness (VOC), and Fermentation Gas.",
                        "Automatic expiry date calculation and smart inventory management.",
                        "Proactive push alerts to warn you before produce or dairy spoils."
                ),
                "Check the top banner on the Main Dashboard for an instant overview of your fridge's temperature."
        ));

        // 2. Navigating the App
        allTopics.add(new GuideTopic(
                "Navigating the Application",
                "Navigation",
                R.drawable.fridge_icon,
                "Use the bottom navigation bar and touch gestures to move between primary screens:",
                Arrays.asList(
                        "Cart Tab (Left): Open your Food Inventory & Grocery List screen.",
                        "Fridge Tab (Center): Return to the Main Dashboard showing your overall fridge temperature and active devices.",
                        "Settings Tab (Right): Access device settings, data & storage, security preferences, and this User Guide.",
                        "Swipe Gestures: On the Main Dashboard, tap any device card or swipe left/right across device pages to view individual Device Tracker detail screens.",
                        "Floating Add (+) Button: Tap on the Main Dashboard to quickly pair new sensor modules or configure fridge zones."
                ),
                "You can tap the top green banner anywhere in the app to return to your main dashboard view."
        ));

        // 3. Real-Time Sensor Monitoring
        allTopics.add(new GuideTopic(
                "Real-Time Sensor Monitoring",
                "Sensors",
                R.drawable.voc_icon,
                "Fridge Friends connects to ESP32 hardware sensors placed inside your fridge or pantry compartments to monitor critical environmental metrics:",
                Arrays.asList(
                        "Temperature (1°C – 4°C): Ensures ambient temperatures stay in safe storage ranges.",
                        "Moisture & Humidity: Detects excess moisture buildup that accelerates mold growth on fruits and vegetables.",
                        "Freshness (VOC / Volatile Organic Compounds): Measures air quality indicators emitted by produce to track freshness.",
                        "Fermentation (Gas & Alcohol): Detects early gas build-up caused by fermenting dairy, cooked meals, or spoiled items.",
                        "Device Tracker View: View live readouts for Device 1, Device 2, or Device 3 and see produce assigned to each zone."
                ),
                "Assign specific devices to different fridge zones (e.g. Crisper Drawer vs. Dairy Compartment) for target monitoring."
        ));

        // 4. Food Inventory & Barcode Scanning
        allTopics.add(new GuideTopic(
                "Food Inventory & Barcode Scanner",
                "Food Inventory",
                R.drawable.cart_icon,
                "Manage your food inventory effortlessly and keep track of expiration dates:",
                Arrays.asList(
                        "Barcode Scanner: Tap the barcode icon to scan product barcodes with your camera for instant product auto-lookup.",
                        "Manual Item Entry: Input item name, category (Produce, Dairy, Meat, Bakery, Cheese, etc.), location, and quantity.",
                        "Automatic Expiry Prediction: The app automatically calculates recommended expiration dates based on food type and storage placement.",
                        "Owned vs. Expiring Soon: Filter your inventory to highlight items nearing expiration so you can use them first.",
                        "Quantity Controls: Increment (+) or decrement (-) items directly from the list as you cook or consume them."
                ),
                "Items marked as 'Expiring Soon' are highlighted so you never lose track of food buried in the back of your fridge."
        ));

        // 5. Automated Alerts & Notifications
        allTopics.add(new GuideTopic(
                "Automated Alerts & Notifications",
                "Alerts",
                R.drawable.baseline_settings_24,
                "Stay protected against food spoilage with smart background monitoring:",
                Arrays.asList(
                        "Background Service: SensorMonitoringService continuously samples hardware readings in the background.",
                        "Spoilage Risk Warnings: High VOC or gas readings trigger immediate notifications to alert you to spoiling items.",
                        "Temperature Warnings: Receive alerts if fridge temperature drifts out of safe ranges (e.g., door left cracked open).",
                        "Expiration Notifications: Receive reminders for items reaching expiration within 24–48 hours."
                ),
                "Ensure notification permissions are enabled under Settings -> Security & Privacy or System Settings to receive alerts."
        ));

        // 6. Device Setup & Pairing
        allTopics.add(new GuideTopic(
                "Device Setup & Hardware Pairing",
                "Setup & FAQ",
                R.drawable.fresh_icon,
                "Connecting new hardware sensor modules to your Fridge Friends application:",
                Arrays.asList(
                        "1. Power on your ESP32 sensor module and connect it to your local network.",
                        "2. Go to Settings -> Connected Devices or tap the '+' button on the Main Dashboard.",
                        "3. Select a slot (Device 1, Device 2, or Device 3).",
                        "4. Choose a custom icon (Produce, Meat, Cheese, Fresh, etc.).",
                        "5. Enable active tracking sensors (Moisture, Freshness, Fermentation).",
                        "6. Save settings to start receiving live sensor streams!"
                ),
                "Make sure your mobile phone and ESP32 module are connected to the same WiFi network."
        ));

        // 7. Frequently Asked Questions
        allTopics.add(new GuideTopic(
                "Frequently Asked Questions & Troubleshooting",
                "Setup & FAQ",
                R.drawable.cheese_icon,
                "Quick answers to common questions:",
                Arrays.asList(
                        "Q: Why does temperature display '--°C'?\nA: Make sure at least one device is added in Connected Devices and your sensor is powered on.",
                        "Q: How do I clear cached app data?\nA: Go to Settings -> Data and Storage -> Clear Cache.",
                        "Q: Can I change an item's expiration date manually?\nA: Yes, tap on any item in your Grocery Inventory to edit details, quantity, or dates.",
                        "Q: How many devices can I connect?\nA: Fridge Friends currently supports up to 3 active hardware sensor modules simultaneously."
                ),
                "If sensor readings stop updating, restart your ESP32 module or re-check WiFi network settings."
        ));
    }

    private void setupSearchAndFilters() {
        if (etGuideSearch != null) {
            etGuideSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    if (btnClearGuideSearch != null) {
                        btnClearGuideSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    renderTopics();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearGuideSearch != null) {
            btnClearGuideSearch.setOnClickListener(v -> {
                if (etGuideSearch != null) {
                    etGuideSearch.setText("");
                }
            });
        }

        View.OnClickListener chipListener = v -> {
            int id = v.getId();
            if (id == R.id.chipFilterAll) {
                selectedCategory = "ALL";
            } else if (id == R.id.chipFilterNav) {
                selectedCategory = "Navigation";
            } else if (id == R.id.chipFilterSensors) {
                selectedCategory = "Sensors";
            } else if (id == R.id.chipFilterInventory) {
                selectedCategory = "Food Inventory";
            } else if (id == R.id.chipFilterAlerts) {
                selectedCategory = "Alerts";
            } else if (id == R.id.chipFilterSetup) {
                selectedCategory = "Setup & FAQ";
            }
            updateChipStyles();
            renderTopics();
        };

        if (chipFilterAll != null) chipFilterAll.setOnClickListener(chipListener);
        if (chipFilterNav != null) chipFilterNav.setOnClickListener(chipListener);
        if (chipFilterSensors != null) chipFilterSensors.setOnClickListener(chipListener);
        if (chipFilterInventory != null) chipFilterInventory.setOnClickListener(chipListener);
        if (chipFilterAlerts != null) chipFilterAlerts.setOnClickListener(chipListener);
        if (chipFilterSetup != null) chipFilterSetup.setOnClickListener(chipListener);
    }

    private void updateChipStyles() {
        TextView[] chips = {chipFilterAll, chipFilterNav, chipFilterSensors, chipFilterInventory, chipFilterAlerts, chipFilterSetup};
        String[] categories = {"ALL", "Navigation", "Sensors", "Food Inventory", "Alerts", "Setup & FAQ"};

        for (int i = 0; i < chips.length; i++) {
            TextView chip = chips[i];
            if (chip != null) {
                boolean isSelected = categories[i].equals(selectedCategory);
                chip.setBackgroundResource(isSelected ? R.drawable.bg_pill_dark : R.drawable.bg_pill_light);
                chip.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#4A4A4A"));
            }
        }
    }

    private void setupShortcutsAndNav() {
        Button btnInventory = findViewById(R.id.btnShortcutInventory);
        Button btnDashboard = findViewById(R.id.btnShortcutDashboard);
        Button btnAddDevice = findViewById(R.id.btnShortcutAddDevice);

        if (btnInventory != null) {
            btnInventory.setOnClickListener(v -> {
                startActivity(new Intent(this, GroceryListActivity.class));
                finish();
            });
        }

        if (btnDashboard != null) {
            btnDashboard.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        if (btnAddDevice != null) {
            btnAddDevice.setOnClickListener(v -> {
                startActivity(new Intent(this, AddDeviceActivity.class));
                finish();
            });
        }

        ImageButton navCart = findViewById(R.id.navCartGuide);
        ImageButton navFridge = findViewById(R.id.navFridgeGuide);
        ImageButton navSettings = findViewById(R.id.navSettingsGuide);

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

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
            });
        }
    }

    private void renderTopics() {
        if (containerGuideTopics == null) return;
        containerGuideTopics.removeAllViews();

        int visibleCount = 0;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (GuideTopic topic : allTopics) {
            // Check Category filter
            if (!"ALL".equals(selectedCategory) && !selectedCategory.equalsIgnoreCase(topic.category)) {
                continue;
            }

            // Check Search query
            if (!searchQuery.isEmpty()) {
                boolean matchesTitle = topic.title.toLowerCase().contains(searchQuery);
                boolean matchesCat = topic.category.toLowerCase().contains(searchQuery);
                boolean matchesDesc = topic.description.toLowerCase().contains(searchQuery);
                boolean matchesTip = topic.tip != null && topic.tip.toLowerCase().contains(searchQuery);
                boolean matchesBullets = false;
                for (String b : topic.bullets) {
                    if (b.toLowerCase().contains(searchQuery)) {
                        matchesBullets = true;
                        break;
                    }
                }
                if (!matchesTitle && !matchesCat && !matchesDesc && !matchesTip && !matchesBullets) {
                    continue;
                }
            }

            visibleCount++;
            View topicView = inflater.inflate(R.layout.item_guide_topic, containerGuideTopics, false);

            ImageView ivIcon = topicView.findViewById(R.id.ivTopicIcon);
            TextView tvTitle = topicView.findViewById(R.id.tvTopicTitle);
            TextView tvCategory = topicView.findViewById(R.id.tvTopicCategory);
            ImageView ivChevron = topicView.findViewById(R.id.ivExpandChevron);
            View layoutHeader = topicView.findViewById(R.id.layoutTopicHeader);
            View layoutBody = topicView.findViewById(R.id.layoutTopicBody);
            TextView tvDescription = topicView.findViewById(R.id.tvTopicDescription);
            LinearLayout containerBullets = topicView.findViewById(R.id.containerTopicBullets);
            View layoutTip = topicView.findViewById(R.id.layoutTopicTip);
            TextView tvTipText = topicView.findViewById(R.id.tvTopicTipText);

            if (ivIcon != null) ivIcon.setImageResource(topic.iconResId);
            if (tvTitle != null) tvTitle.setText(topic.title);
            if (tvCategory != null) tvCategory.setText("• " + topic.category);

            if (tvDescription != null) tvDescription.setText(topic.description);

            // Populate bullets
            if (containerBullets != null) {
                containerBullets.removeAllViews();
                for (String bullet : topic.bullets) {
                    TextView tvBullet = new TextView(this);
                    tvBullet.setText("•  " + bullet);
                    tvBullet.setTextSize(13);
                    tvBullet.setTextColor(Color.parseColor("#444444"));
                    tvBullet.setPadding(0, 4, 0, 4);
                    tvBullet.setTypeface(null, android.graphics.Typeface.NORMAL);
                    containerBullets.addView(tvBullet);
                }
            }

            // Populate Pro Tip
            if (layoutTip != null && tvTipText != null) {
                if (topic.tip != null && !topic.tip.isEmpty()) {
                    layoutTip.setVisibility(View.VISIBLE);
                    tvTipText.setText(topic.tip);
                } else {
                    layoutTip.setVisibility(View.GONE);
                }
            }

            // Handle Expand / Collapse
            layoutBody.setVisibility(topic.isExpanded ? View.VISIBLE : View.GONE);
            ivChevron.setRotation(topic.isExpanded ? 180f : 0f);

            layoutHeader.setOnClickListener(v -> {
                topic.isExpanded = !topic.isExpanded;
                layoutBody.setVisibility(topic.isExpanded ? View.VISIBLE : View.GONE);
                ivChevron.animate().rotation(topic.isExpanded ? 180f : 0f).setDuration(200).start();
            });

            containerGuideTopics.addView(topicView);
        }

        if (tvGuideNoResults != null) {
            tvGuideNoResults.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
        }
    }
}
