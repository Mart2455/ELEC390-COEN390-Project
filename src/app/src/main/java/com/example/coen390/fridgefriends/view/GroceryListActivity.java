package com.example.coen390.fridgefriends.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.example.coen390.fridgefriends.model.DateParser;
import com.example.coen390.fridgefriends.model.GroceryItem;

import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import com.example.coen390.fridgefriends.R;
import com.example.coen390.fridgefriends.MonitoringState;
import com.example.coen390.fridgefriends.adapter.GroceryAdapter;
import com.example.coen390.fridgefriends.model.DateParser;
import com.example.coen390.fridgefriends.model.GroceryItem;
import com.example.coen390.fridgefriends.model.ProductImageResolver;
import com.example.coen390.fridgefriends.view.BarcodeScannerActivity;
import com.example.coen390.fridgefriends.view.MainActivity;
import com.example.coen390.fridgefriends.view.NewItemActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GroceryListActivity extends AppCompatActivity implements GroceryAdapter.OnItemInteractionListener {

    private final MonitoringState.Listener monitoringListener = state -> {
        android.widget.TextView textTemperatureView = findViewById(R.id.tvTempGrocery);
        if (!state.hasReadings)
        {
            textTemperatureView.setText("--°C");
        }
        else
        {
            textTemperatureView.setText(String.format(java.util.Locale.getDefault(), "%.1f°C", state.temperature));
        }
    };

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


    private RecyclerView rvGroceryList;
    private TextView tabToBuy, tabOwned, tvGroceryTitle;
    private GroceryAdapter adapter;
    private final List<GroceryItem> allItems = new ArrayList<>();
    private final List<GroceryItem> displayedItems = new ArrayList<>();
    private boolean isOwnedTab = false;
    private boolean isVisualOwnedMode = true;
    private boolean isSelectionModeActive = false;
    private String selectedCategoryFilter = "All Types";
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Intent> barcodeScannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String scannedName = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_SCANNED_NAME);
                    String scannedExpiry = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_SCANNED_EXPIRY);
                    String scannedImageUrl = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_SCANNED_IMAGE_URL);

                    Intent intent = new Intent(this, NewItemActivity.class);
                    if (scannedName != null) intent.putExtra(NewItemActivity.EXTRA_ITEM_NAME, scannedName);
                    if (scannedExpiry != null) intent.putExtra(NewItemActivity.EXTRA_ITEM_EXPIRY, scannedExpiry);
                    if (scannedImageUrl != null) intent.putExtra(NewItemActivity.EXTRA_ITEM_IMAGE_URL, scannedImageUrl);
                    startActivity(intent);
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startQRScanner();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan barcodes", Toast.LENGTH_SHORT).show();
                }
            });

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startQRScanner();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_list);

        prefs = getSharedPreferences("GroceryPrefs", MODE_PRIVATE);

        rvGroceryList = findViewById(R.id.rvGroceryList);
        tabToBuy = findViewById(R.id.tabToBuy);
        tabOwned = findViewById(R.id.tabOwned);
        tvGroceryTitle = findViewById(R.id.tvGroceryTitle);
        TextView tvTempGrocery = findViewById(R.id.tvTempGrocery);

        rvGroceryList.setLayoutManager(new LinearLayoutManager(this));

        // Conditionally show temperature
        SharedPreferences devicePrefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE);
        boolean anyDeviceAdded = devicePrefs.getBoolean("Device 1_added", false) ||
                                 devicePrefs.getBoolean("Device 2_added", false) ||
                                 devicePrefs.getBoolean("Device 3_added", false);
        if (anyDeviceAdded) {
            tvTempGrocery.setVisibility(View.VISIBLE);
        } else {
            tvTempGrocery.setVisibility(View.GONE);
        }

        loadItems();
        setupBottomNav();

        View mainRoot = findViewById(R.id.mainGroceryRoot);
        if (mainRoot != null) {
            mainRoot.setOnClickListener(v -> clearFocusAndHideKeyboard());
            mainRoot.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                android.graphics.Rect r = new android.graphics.Rect();
                mainRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = mainRoot.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                if (keypadHeight < screenHeight * 0.15) {
                    View current = getCurrentFocus();
                    if (current instanceof android.widget.EditText) {
                        ((android.widget.EditText) current).setCursorVisible(false);
                        current.clearFocus();
                        mainRoot.requestFocus();
                    }
                }
            });
        }

        View topBanner = findViewById(R.id.topBannerGrocery);
        if (topBanner != null) topBanner.setOnClickListener(v -> clearFocusAndHideKeyboard());
        tvGroceryTitle.setOnClickListener(v -> clearFocusAndHideKeyboard());

        rvGroceryList.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View child = rvGroceryList.findChildViewUnder(event.getX(), event.getY());
                if (child == null) {
                    clearFocusAndHideKeyboard();
                    v.performClick();
                }
            }
            return false;
        });

        tabOwned.setOnClickListener(v -> {
            clearFocusAndHideKeyboard();
            switchTab(true);
        });
        tabToBuy.setOnClickListener(v -> {
            clearFocusAndHideKeyboard();
            switchTab(false);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                View llSelectionControls = findViewById(R.id.llSelectionControls);
                if (llSelectionControls != null && llSelectionControls.getVisibility() == View.VISIBLE) {
                    exitSelectionMode();
                    return;
                }

                if (!isOwnedTab) {
                    switchTab(true);
                    return;
                }

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        ImageButton btnToggleViewMode = findViewById(R.id.btnToggleViewMode);
        if (btnToggleViewMode != null) {
            btnToggleViewMode.setOnClickListener(v -> {
                if (isOwnedTab) {
                    isVisualOwnedMode = !isVisualOwnedMode;
                    switchTab(true);
                }
            });
        }

        CheckBox cbSelectAll = findViewById(R.id.cbSelectAll);
        TextView tvSelectAllLabel = findViewById(R.id.tvSelectAllLabel);

        if (cbSelectAll != null) {
            cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                for (GroceryItem item : allItems) {
                    if (item.isOwned() == isOwnedTab && !item.isEmptyRow()) {
                        item.setSelected(isChecked);
                    }
                }
                if (isOwnedTab && isVisualOwnedMode) {
                    renderOwnedVisualView();
                } else if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        if (tvSelectAllLabel != null && cbSelectAll != null) {
            tvSelectAllLabel.setOnClickListener(v -> cbSelectAll.toggle());
        }

        ImageButton btnMoveSelected = findViewById(R.id.btnMoveSelected);
        if (btnMoveSelected != null) {
            btnMoveSelected.setOnClickListener(v -> {
                boolean anyMoved = false;
                for (GroceryItem item : allItems) {
                    if (item.isOwned() == isOwnedTab && item.isSelected() && !item.isEmptyRow()) {
                        item.setOwned(!isOwnedTab);
                        item.setStriked(false);
                        item.setSelected(false);
                        anyMoved = true;
                    }
                }
                if (anyMoved) {
                    saveItems();
                    switchTab(isOwnedTab);
                    exitSelectionMode();
                    Toast.makeText(this, "Moved items", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
                }
            });
        }

        ImageButton btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        if (btnDeleteSelected != null) {
            btnDeleteSelected.setOnClickListener(v -> {
                boolean hasSelected = false;
                for (GroceryItem item : allItems) {
                    if (item.isOwned() == isOwnedTab && item.isSelected() && !item.isEmptyRow()) {
                        hasSelected = true;
                        break;
                    }
                }
                if (!hasSelected) {
                    Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                showDeleteConfirmationDialog();
            });
        }

        ImageButton btnCancelSelection = findViewById(R.id.btnCancelSelection);
        if (btnCancelSelection != null) {
            btnCancelSelection.setOnClickListener(v -> exitSelectionMode());
        }

        ImageButton btnAddOwned = findViewById(R.id.btnAddOwned);
        if (btnAddOwned != null) {
            btnAddOwned.setOnClickListener(v -> showAddFridgeOptionsDialog());
        }

        View layoutOwnedVisualView = findViewById(R.id.layoutOwnedVisualView);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (layoutOwnedVisualView instanceof android.widget.ScrollView) {
                layoutOwnedVisualView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (btnAddOwned != null) {
                        android.widget.ScrollView sv = (android.widget.ScrollView) v;
                        View child = sv.getChildAt(0);
                        if (child != null) {
                            boolean isAtBottom = (child.getBottom() - (sv.getHeight() + sv.getScrollY())) < 50;
                            if (isAtBottom && btnAddOwned.getAlpha() == 1f) {
                                btnAddOwned.animate().alpha(0f).setDuration(200).start();
                            } else if (!isAtBottom && btnAddOwned.getAlpha() == 0f) {
                                btnAddOwned.animate().alpha(1f).setDuration(200).start();
                            }
                        }
                    }
                });
            }
            if (rvGroceryList != null) {
                rvGroceryList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (btnAddOwned != null) {
                            boolean isAtBottom = !recyclerView.canScrollVertically(1);
                            if (isAtBottom && btnAddOwned.getAlpha() == 1f) {
                                btnAddOwned.animate().alpha(0f).setDuration(200).start();
                            } else if (!isAtBottom && btnAddOwned.getAlpha() == 0f) {
                                btnAddOwned.animate().alpha(1f).setDuration(200).start();
                            }
                        }
                    }
                });
            }
        }

        EditText etOwnedSearch = findViewById(R.id.etOwnedSearch);
        if (etOwnedSearch != null) {
            etOwnedSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isOwnedTab) {
                        if (isVisualOwnedMode) {
                            renderOwnedVisualView();
                        } else {
                            filterClassicList(s.toString());
                        }
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        ImageButton btnFilterType = findViewById(R.id.btnFilterType);
        if (btnFilterType != null) {
            btnFilterType.setOnClickListener(v -> showCategoryFilterDialog(btnFilterType));
        }

        switchTab(true); // Default to Owned
    }

    public void clearFocusAndHideKeyboard() {
        View mainRoot = findViewById(R.id.mainGroceryRoot);
        View current = getCurrentFocus();
        if (current instanceof android.widget.EditText) {
            android.widget.EditText et = (android.widget.EditText) current;
            et.setCursorVisible(false);
            et.clearFocus();
        } else if (current != null) {
            current.clearFocus();
        }
        if (mainRoot != null) {
            mainRoot.requestFocus();
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            View tokenView = current != null ? current : mainRoot;
            if (tokenView != null) {
                imm.hideSoftInputFromWindow(tokenView.getWindowToken(), 0);
            }
        }
    }

    private void setupBottomNav() {
        ImageButton navFridge = findViewById(R.id.navFridge);
        ImageButton navSettings = findViewById(R.id.navSettings);

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

    private void switchTab(boolean toOwned) {
        if (isSelectionModeActive) {
            exitSelectionMode();
        }
        isOwnedTab = toOwned;
        displayedItems.clear();

        ImageButton btnAddOwned = findViewById(R.id.btnAddOwned);
        if (btnAddOwned != null) {
            btnAddOwned.setVisibility(toOwned ? View.VISIBLE : View.GONE);
        }

        ImageButton btnToggleViewMode = findViewById(R.id.btnToggleViewMode);
        if (btnToggleViewMode != null) {
            btnToggleViewMode.setVisibility(toOwned ? View.VISIBLE : View.GONE);
        }

        View layoutOwnedVisualView = findViewById(R.id.layoutOwnedVisualView);

        if (toOwned) {
            tabToBuy.setBackgroundResource(R.drawable.bg_tab_inactive);
            tabToBuy.setTextColor(Color.parseColor("#999999"));
            tabOwned.setBackgroundResource(R.drawable.bg_tab_active);
            tabOwned.setTextColor(Color.parseColor("#2A2A2A"));
            tvGroceryTitle.setText("In fridge");

            if (isVisualOwnedMode) {
                if (layoutOwnedVisualView != null) {
                    layoutOwnedVisualView.setVisibility(View.VISIBLE);
                }
                rvGroceryList.setVisibility(View.GONE);
                renderOwnedVisualView();
            } else {
                if (layoutOwnedVisualView != null) {
                    layoutOwnedVisualView.setVisibility(View.GONE);
                }
                rvGroceryList.setVisibility(View.VISIBLE);

                for (GroceryItem item : allItems) {
                    if (item.isOwned()) {
                        displayedItems.add(item);
                    }
                }
                sortItemsByExpiry(displayedItems);

                int currentSize = displayedItems.size();
                for (int i = 0; i < 15 - currentSize; i++) {
                    displayedItems.add(new GroceryItem()); // Empty row
                }

                adapter = new GroceryAdapter(displayedItems, isOwnedTab, this);
                rvGroceryList.setAdapter(adapter);
            }
        } else {
            tabToBuy.setBackgroundResource(R.drawable.bg_tab_active);
            tabToBuy.setTextColor(Color.parseColor("#2A2A2A"));
            tabOwned.setBackgroundResource(R.drawable.bg_tab_inactive);
            tabOwned.setTextColor(Color.parseColor("#999999"));
            tvGroceryTitle.setText("Grocery list");

            if (layoutOwnedVisualView != null) {
                layoutOwnedVisualView.setVisibility(View.GONE);
            }
            rvGroceryList.setVisibility(View.VISIBLE);

            for (GroceryItem item : allItems) {
                if (!item.isOwned()) {
                    displayedItems.add(item);
                }
            }

            // Pad to 15 items
            int currentSize = displayedItems.size();
            for (int i = 0; i < 15 - currentSize; i++) {
                displayedItems.add(new GroceryItem()); // Empty row
            }

            adapter = new GroceryAdapter(displayedItems, isOwnedTab, this);
            rvGroceryList.setAdapter(adapter);
        }
    }

    private void showCategoryFilterDialog(ImageButton btnFilterType) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_type, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RadioGroup rgCategories = dialogView.findViewById(R.id.rgFilterCategories);
        TextView btnCancel = dialogView.findViewById(R.id.btnFilterCancel);

        String[] options = {"All Types", "Produce", "Dairy & Refrigerated", "Meat & Seafood", "Bakery & Bread", "Beverages", "Condiments & Sauces", "Snacks & Candy", "Pantry", "Other"};

        for (int i = 0; i < options.length; i++) {
            String opt = options[i];
            RadioButton rb = new RadioButton(this);
            rb.setText(opt);
            rb.setTextSize(16);
            rb.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
            rb.setPadding(16, 12, 16, 12);
            rb.setId(View.generateViewId());

            if (opt.equalsIgnoreCase(selectedCategoryFilter)) {
                rb.setChecked(true);
            }

            rb.setOnClickListener(v -> {
                selectedCategoryFilter = opt;
                if (btnFilterType != null) {
                    if (selectedCategoryFilter.equalsIgnoreCase("All Types")) {
                        btnFilterType.setColorFilter(Color.parseColor("#888888"));
                    } else {
                        btnFilterType.setColorFilter(ContextCompat.getColor(this, R.color.dark_green));
                    }
                }
                dialog.dismiss();
                if (isOwnedTab) {
                    if (isVisualOwnedMode) {
                        renderOwnedVisualView();
                    } else {
                        EditText etOwnedSearch = findViewById(R.id.etOwnedSearch);
                        filterClassicList(etOwnedSearch != null ? etOwnedSearch.getText().toString() : "");
                    }
                }
            });

            if (rgCategories != null) {
                rgCategories.addView(rb);
            }
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void filterClassicList(String query) {
        displayedItems.clear();
        String q = query != null ? query.toLowerCase().trim() : "";
        for (GroceryItem item : allItems) {
            if (item.isOwned() && !item.isEmptyRow()) {
                boolean matchesSearch = q.isEmpty() || item.getName().toLowerCase().contains(q);
                boolean matchesCategory = selectedCategoryFilter.equalsIgnoreCase("All Types")
                        || (item.getCategory() != null && item.getCategory().equalsIgnoreCase(selectedCategoryFilter));

                if (matchesSearch && matchesCategory) {
                    displayedItems.add(item);
                }
            }
        }
        sortItemsByExpiry(displayedItems);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void sortItemsByExpiry(List<GroceryItem> items) {
        java.util.Collections.sort(items, (item1, item2) -> {
            if (item1.isEmptyRow()) return 1;
            if (item2.isEmptyRow()) return -1;
            int days1 = DateParser.getDaysUntilExpiry(item1.getExpiryDate());
            int days2 = DateParser.getDaysUntilExpiry(item2.getExpiryDate());
            return Integer.compare(days1, days2);
        });
    }

    private void renderOwnedVisualView() {
        View layoutOwnedVisualView = findViewById(R.id.layoutOwnedVisualView);
        if (layoutOwnedVisualView == null) return;

        RecyclerView rvExpiringSoon = findViewById(R.id.rvExpiringSoon);
        LinearLayout containerExpired = findViewById(R.id.containerExpired);
        LinearLayout containerExpiresToday = findViewById(R.id.containerExpiresToday);
        LinearLayout containerExpiresTomorrow = findViewById(R.id.containerExpiresTomorrow);
        LinearLayout containerExpiresLater = findViewById(R.id.containerExpiresLater);
        LinearLayout containerExpiresMuchLater = findViewById(R.id.containerExpiresMuchLater);
        LinearLayout containerNoExpiration = findViewById(R.id.containerNoExpiration);

        TextView tvHeaderExpired = findViewById(R.id.tvHeaderExpired);
        TextView tvHeaderExpiresToday = findViewById(R.id.tvHeaderExpiresToday);
        TextView tvHeaderExpiresTomorrow = findViewById(R.id.tvHeaderExpiresTomorrow);
        TextView tvHeaderExpiresLater = findViewById(R.id.tvHeaderExpiresLater);
        TextView tvHeaderExpiresMuchLater = findViewById(R.id.tvHeaderExpiresMuchLater);
        TextView tvHeaderNoExpiration = findViewById(R.id.tvHeaderNoExpiration);
        EditText etOwnedSearch = findViewById(R.id.etOwnedSearch);

        if (containerExpired != null) containerExpired.removeAllViews();
        if (containerExpiresToday != null) containerExpiresToday.removeAllViews();
        if (containerExpiresTomorrow != null) containerExpiresTomorrow.removeAllViews();
        if (containerExpiresLater != null) containerExpiresLater.removeAllViews();
        if (containerExpiresMuchLater != null) containerExpiresMuchLater.removeAllViews();
        if (containerNoExpiration != null) containerNoExpiration.removeAllViews();

        List<GroceryItem> ownedItems = new ArrayList<>();
        List<GroceryItem> expiringSoonItems = new ArrayList<>();
        String searchQuery = etOwnedSearch != null ? etOwnedSearch.getText().toString().toLowerCase().trim() : "";

        for (GroceryItem item : allItems) {
            if (item.isOwned() && !item.isEmptyRow()) {
                boolean matchesSearch = searchQuery.isEmpty() || item.getName().toLowerCase().contains(searchQuery);
                boolean matchesCategory = selectedCategoryFilter.equalsIgnoreCase("All Types")
                        || (item.getCategory() != null && item.getCategory().equalsIgnoreCase(selectedCategoryFilter));

                if (matchesSearch && matchesCategory) {
                    ownedItems.add(item);
                    int days = DateParser.getDaysUntilExpiry(item.getExpiryDate());
                    if (days >= 0 && days <= 7) {
                        expiringSoonItems.add(item);
                    }
                }
            }
        }

        java.util.Collections.sort(expiringSoonItems, (item1, item2) -> {
            int days1 = DateParser.getDaysUntilExpiry(item1.getExpiryDate());
            int days2 = DateParser.getDaysUntilExpiry(item2.getExpiryDate());
            return Integer.compare(days1, days2);
        });

        // Setup horizontal carousel for Expiring Soon
        if (rvExpiringSoon != null) {
            rvExpiringSoon.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvExpiringSoon.setAdapter(new ExpiringSoonAdapter(expiringSoonItems));
        }

        // Group into Today, Tomorrow, Later, and No Expiration
        for (GroceryItem item : ownedItems) {
            View cardView = getLayoutInflater().inflate(R.layout.item_owned_expiry_group, null);

            // Set explicit LayoutParams with 10dp bottom margin to prevent vertical overlap
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int marginPx = (int) (10 * getResources().getDisplayMetrics().density);
            lp.setMargins(0, 0, 0, marginPx);
            cardView.setLayoutParams(lp);

            ImageView imgPhoto = cardView.findViewById(R.id.imgOwnedRowPhoto);
            TextView tvName = cardView.findViewById(R.id.tvOwnedRowName);
            TextView tvExpiry = cardView.findViewById(R.id.tvOwnedRowExpiry);
            TextView tvQty = cardView.findViewById(R.id.tvOwnedRowQty);

            tvName.setText(item.getName());
            int days = DateParser.getDaysUntilExpiry(item.getExpiryDate());
            boolean hasExpiry = (days != 9999);

            tvExpiry.setText(hasExpiry ? DateParser.formatDisplayDate(item.getExpiryDate()) : "No expiration");
            tvQty.setText(item.getQuantity() + " " + item.getUnit());

            String imgUrl = item.getImageUrl();
            if (imgUrl == null || imgUrl.trim().isEmpty()) {
                imgUrl = ProductImageResolver.resolveImageUrl(item.getName(), item.getCategory());
                item.setImageUrl(imgUrl);
            }
            fetchImageToView(imgUrl, imgPhoto);

            CheckBox cbSelectOwned = cardView.findViewById(R.id.cbSelectOwned);
            if (cbSelectOwned != null) {
                cbSelectOwned.setVisibility(isSelectionModeActive ? View.VISIBLE : View.GONE);
                cbSelectOwned.setChecked(item.isSelected());
            }

            cardView.setOnClickListener(v -> {
                if (isSelectionModeActive) {
                    item.setSelected(!item.isSelected());
                    if (cbSelectOwned != null) cbSelectOwned.setChecked(item.isSelected());
                } else {
                    openEditItemScreen(item);
                }
            });

            cardView.setOnLongClickListener(v -> {
                if (!isSelectionModeActive) {
                    item.setSelected(true);
                    enterSelectionMode();
                }
                return true;
            });

            if (!hasExpiry) {
                tvExpiry.setText("No expiration");
                tvExpiry.setTextColor(Color.parseColor("#888888"));
                if (containerNoExpiration != null) containerNoExpiration.addView(cardView);
            } else if (days < 0) {
                int absDays = Math.abs(days);
                tvExpiry.setText("Expired " + absDays + (absDays == 1 ? " day ago" : " days ago"));
                tvExpiry.setTextColor(Color.parseColor("#C0392B"));
                if (containerExpired != null) containerExpired.addView(cardView);
            } else if (days == 0) {
                tvExpiry.setText("Expires today");
                tvExpiry.setTextColor(Color.parseColor("#C0392B"));
                if (containerExpiresToday != null) containerExpiresToday.addView(cardView);
            } else if (days == 1) {
                tvExpiry.setText(DateParser.formatDisplayDate(item.getExpiryDate()));
                tvExpiry.setTextColor(Color.parseColor("#888888"));
                if (containerExpiresTomorrow != null) containerExpiresTomorrow.addView(cardView);
            } else if (days <= 7) {
                tvExpiry.setText(DateParser.formatDisplayDate(item.getExpiryDate()));
                tvExpiry.setTextColor(Color.parseColor("#888888"));
                if (containerExpiresLater != null) containerExpiresLater.addView(cardView);
            } else {
                tvExpiry.setText(DateParser.formatDisplayDate(item.getExpiryDate()));
                tvExpiry.setTextColor(Color.parseColor("#888888"));
                if (containerExpiresMuchLater != null) containerExpiresMuchLater.addView(cardView);
            }
        }

        // Toggle section headers visibility
        if (tvHeaderExpired != null && containerExpired != null) {
            boolean show = containerExpired.getChildCount() > 0;
            tvHeaderExpired.setVisibility(show ? View.VISIBLE : View.GONE);
            containerExpired.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (tvHeaderExpiresToday != null && containerExpiresToday != null) {
            boolean show = containerExpiresToday.getChildCount() > 0;
            tvHeaderExpiresToday.setVisibility(show ? View.VISIBLE : View.GONE);
            containerExpiresToday.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (tvHeaderExpiresTomorrow != null && containerExpiresTomorrow != null) {
            boolean show = containerExpiresTomorrow.getChildCount() > 0;
            tvHeaderExpiresTomorrow.setVisibility(show ? View.VISIBLE : View.GONE);
            containerExpiresTomorrow.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (tvHeaderExpiresLater != null && containerExpiresLater != null) {
            boolean show = containerExpiresLater.getChildCount() > 0;
            tvHeaderExpiresLater.setVisibility(show ? View.VISIBLE : View.GONE);
            containerExpiresLater.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (tvHeaderExpiresMuchLater != null && containerExpiresMuchLater != null) {
            boolean show = containerExpiresMuchLater.getChildCount() > 0;
            tvHeaderExpiresMuchLater.setVisibility(show ? View.VISIBLE : View.GONE);
            containerExpiresMuchLater.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (tvHeaderNoExpiration != null && containerNoExpiration != null) {
            boolean show = containerNoExpiration.getChildCount() > 0;
            tvHeaderNoExpiration.setVisibility(show ? View.VISIBLE : View.GONE);
            containerNoExpiration.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void openEditItemScreen(GroceryItem item) {
        if (item == null || item.isEmptyRow()) return;
        int index = allItems.indexOf(item);

        Intent intent = new Intent(this, NewItemActivity.class);
        intent.putExtra(NewItemActivity.EXTRA_IS_EDIT_MODE, true);
        intent.putExtra(NewItemActivity.EXTRA_ITEM_INDEX, index);
        intent.putExtra(NewItemActivity.EXTRA_ITEM_NAME, item.getName());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_EXPIRY, item.getExpiryDate());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_IMAGE_URL, item.getImageUrl());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_CATEGORY, item.getCategory());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_QUANTITY, item.getQuantity());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_UNIT, item.getUnit());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_MEMO, item.getMemo());
        startActivity(intent);
    }

    private static final Map<String, Bitmap> IMAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private void fetchImageToView(String urlStr, ImageView imageView) {
        if (urlStr == null || urlStr.trim().isEmpty() || imageView == null) {
            return;
        }

        // Instant Cache Hit (0ms latency, zero flickering)
        if (IMAGE_CACHE.containsKey(urlStr)) {
            Bitmap cachedBitmap = IMAGE_CACHE.get(urlStr);
            if (cachedBitmap != null) {
                imageView.setImageBitmap(cachedBitmap);
                imageView.clearColorFilter();
                return;
            }
        }

        // Download from network once
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                InputStream is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    IMAGE_CACHE.put(urlStr, bitmap);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        imageView.setImageBitmap(bitmap);
                        imageView.clearColorFilter();
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private class ExpiringSoonAdapter extends RecyclerView.Adapter<ExpiringSoonAdapter.ViewHolder> {
        private final List<GroceryItem> items;

        public ExpiringSoonAdapter(List<GroceryItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_owned_expiring_soon, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GroceryItem item = items.get(position);
            holder.tvName.setText(item.getName());

            holder.itemView.setOnClickListener(v -> {
                if (isSelectionModeActive) {
                    item.setSelected(!item.isSelected());
                    renderOwnedVisualView();
                } else {
                    openEditItemScreen(item);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (!isSelectionModeActive) {
                    item.setSelected(true);
                    enterSelectionMode();
                }
                return true;
            });

            int days = DateParser.getDaysUntilExpiry(item.getExpiryDate());
            if (days <= 0) {
                holder.tvBadge.setText("Today");
            } else if (days == 1) {
                holder.tvBadge.setText("Tomorrow");
            } else {
                holder.tvBadge.setText("In " + days + "d");
            }

            if (days <= 2) {
                holder.tvBadge.setTextColor(Color.parseColor("#E74C3C"));
            } else {
                holder.tvBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.dark_green));
            }

            String imgUrl = item.getImageUrl();
            if (imgUrl == null || imgUrl.trim().isEmpty()) {
                imgUrl = ProductImageResolver.resolveImageUrl(item.getName(), item.getCategory());
                item.setImageUrl(imgUrl);
            }
            fetchImageToView(imgUrl, holder.imgPhoto);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPhoto;
            TextView tvName, tvBadge;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPhoto = itemView.findViewById(R.id.imgExpiringPhoto);
                tvName = itemView.findViewById(R.id.tvExpiringName);
                tvBadge = itemView.findViewById(R.id.tvExpiringBadge);
            }
        }
    }

    private void showAddFridgeOptionsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_fridge_options, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnScanQR = dialogView.findViewById(R.id.btnOptionScanQR);
        View btnAddManually = dialogView.findViewById(R.id.btnOptionAddManually);
        TextView btnCancel = dialogView.findViewById(R.id.btnOptionsCancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnScanQR.setOnClickListener(v -> {
            dialog.dismiss();
            checkCameraPermissionAndScan();
        });

        btnAddManually.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, NewItemActivity.class));
        });

        dialog.show();
    }

    private void startQRScanner() {
        Intent intent = new Intent(this, BarcodeScannerActivity.class);
        barcodeScannerLauncher.launch(intent);
    }



    private void showAddFridgeItemDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_fridge_item, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etInput = dialogView.findViewById(R.id.etDialogInput);
        TextView btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        TextView btnAdd = dialogView.findViewById(R.id.btnDialogAdd);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String name = etInput.getText().toString().trim();
            if (!name.isEmpty()) {
                GroceryItem newItem = new GroceryItem(name, true, "");
                String autoExpiry = calculateExpiryDate(name);
                newItem.setExpiryDate(autoExpiry);
                allItems.add(newItem);
                saveItems();
                switchTab(true);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showDeleteConfirmationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_items, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView btnCancel = dialogView.findViewById(R.id.btnDeleteCancel);
        TextView btnYes = dialogView.findViewById(R.id.btnDeleteYes);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            for (int i = allItems.size() - 1; i >= 0; i--) {
                GroceryItem item = allItems.get(i);
                if (item.isOwned() == isOwnedTab && item.isSelected() && !item.isEmptyRow()) {
                    allItems.remove(i);
                }
            }
            saveItems();
            switchTab(isOwnedTab);
            exitSelectionMode();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void exitSelectionMode() {
        isSelectionModeActive = false;
        if (adapter != null) {
            adapter.setSelectionMode(false);
        }

        View tvGroceryTitle = findViewById(R.id.tvGroceryTitle);
        if (tvGroceryTitle != null) {
            tvGroceryTitle.setVisibility(View.VISIBLE);
        }

        ImageButton btnToggleViewMode = findViewById(R.id.btnToggleViewMode);
        if (btnToggleViewMode != null) {
            btnToggleViewMode.setVisibility(isOwnedTab ? View.VISIBLE : View.GONE);
        }

        View llSelectionControls = findViewById(R.id.llSelectionControls);
        if (llSelectionControls != null) {
            llSelectionControls.setVisibility(View.GONE);
        }

        ImageButton btnAddOwned = findViewById(R.id.btnAddOwned);
        if (btnAddOwned != null) {
            btnAddOwned.setVisibility(isOwnedTab ? View.VISIBLE : View.GONE);
        }

        for (GroceryItem item : allItems) {
            item.setSelected(false);
        }

        CheckBox cbSelectAll = findViewById(R.id.cbSelectAll);
        if (cbSelectAll != null) {
            cbSelectAll.setChecked(false);
        }

        if (isOwnedTab && isVisualOwnedMode) {
            renderOwnedVisualView();
        } else if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void saveItems() {
        JSONArray jsonArray = new JSONArray();
        for (GroceryItem item : allItems) {
            if (item.getName() != null && !item.isEmptyRow()) {
                try {
                    jsonArray.put(item.toJSON());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        prefs.edit().putString("grocery_list", jsonArray.toString()).apply();
    }

    private void loadItems() {
        allItems.clear();
        String jsonStr = prefs.getString("grocery_list", "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                GroceryItem item = GroceryItem.fromJSON(obj);
                if (item.getImageUrl() == null || item.getImageUrl().trim().isEmpty()) {
                    item.setImageUrl(ProductImageResolver.resolveImageUrl(item.getName(), item.getCategory()));
                }
                allItems.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemAdded(String name, int position, boolean focusNextLine) {
        if (position < 0 || position >= displayedItems.size()) return;
        GroceryItem itemAtPos = displayedItems.get(position);

        if (!itemAtPos.isEmptyRow() && name.equals(itemAtPos.getName())) {
            return;
        }

        itemAtPos.setName(name);
        itemAtPos.setEmptyRow(false);
        itemAtPos.setOwned(false);
        itemAtPos.setStriked(false);
        
        if (!allItems.contains(itemAtPos)) {
            allItems.add(itemAtPos);
        }
        saveItems();
        
        int emptyCount = 0;
        for (GroceryItem item : displayedItems) { 
            if (item.isEmptyRow()) emptyCount++; 
        }
        if (emptyCount == 0) {
             displayedItems.add(new GroceryItem());
        }
        
        adapter.notifyDataSetChanged();
        
        if (focusNextLine) {
            RecyclerView rvGroceryList = findViewById(R.id.rvGroceryList);
            rvGroceryList.post(() -> {
                int nextPos = position + 1;
                if (nextPos < displayedItems.size()) {
                    rvGroceryList.scrollToPosition(nextPos);
                    rvGroceryList.postDelayed(() -> {
                        RecyclerView.ViewHolder holder = rvGroceryList.findViewHolderForAdapterPosition(nextPos);
                        if (holder instanceof GroceryAdapter.ViewHolder) {
                            GroceryAdapter.ViewHolder vh = (GroceryAdapter.ViewHolder) holder;
                            vh.etGroceryInput.setEnabled(true);
                            vh.etGroceryInput.setFocusable(true);
                            vh.etGroceryInput.setFocusableInTouchMode(true);
                            vh.etGroceryInput.requestFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.showSoftInput(vh.etGroceryInput, InputMethodManager.SHOW_IMPLICIT);
                            }
                        }
                    }, 50);
                }
            });
        }
    }

    @Override
    public void onItemEdited(GroceryItem item, String newName, int position) {
        item.setName(newName);
        saveItems();
        adapter.notifyItemChanged(position);
    }

    private String calculateExpiryDate(String itemName) {
        String lowerName = itemName.toLowerCase();
        int daysToAdd = -1;
        
        Map<String, Integer> expiryDB = new HashMap<>();
        expiryDB.put("milk", 14);
        expiryDB.put("beef", 3);
        expiryDB.put("chicken", 2);
        expiryDB.put("broccoli", 7);
        expiryDB.put("tomato", 10);
        expiryDB.put("egg", 21);
        expiryDB.put("cheese", 30);
        expiryDB.put("apple", 21);
        expiryDB.put("banana", 5);
        
        for (Map.Entry<String, Integer> entry : expiryDB.entrySet()) {
            if (lowerName.contains(entry.getKey())) {
                daysToAdd = entry.getValue();
                break;
            }
        }
        
        if (daysToAdd != -1) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, daysToAdd);
            return String.format("%02d/%02d/%d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
        }
        
        return ""; // Empty means needs manual addition
    }

    @Override
    public void onItemMovedToOwned(GroceryItem item) {
        if (!item.isStriked()) return;
        
        item.setOwned(true);
        item.setStriked(false); // Reset striked state when moved to Owned
        
        String autoExpiry = calculateExpiryDate(item.getName());
        item.setExpiryDate(autoExpiry);
        
        saveItems();

        int index = displayedItems.indexOf(item);
        if (index != -1) {
            displayedItems.remove(index);
            if (!isOwnedTab) {
                displayedItems.add(new GroceryItem());
                adapter.notifyItemRemoved(index);
                adapter.notifyItemInserted(displayedItems.size() - 1);
            } else {
                adapter.notifyItemRemoved(index);
            }
        } else {
            switchTab(isOwnedTab);
        }
    }

    @Override
    public void onSetExpiryDate(GroceryItem item, int position) {
        openEditItemScreen(item);
    }

    private void enterSelectionMode() {
        isSelectionModeActive = true;
        if (adapter != null) {
            adapter.setSelectionMode(true);
        }

        View tvGroceryTitle = findViewById(R.id.tvGroceryTitle);
        if (tvGroceryTitle != null) {
            tvGroceryTitle.setVisibility(View.GONE);
        }

        ImageButton btnToggleViewMode = findViewById(R.id.btnToggleViewMode);
        if (btnToggleViewMode != null) {
            btnToggleViewMode.setVisibility(View.GONE);
        }

        ImageButton btnAddOwned = findViewById(R.id.btnAddOwned);
        if (btnAddOwned != null) {
            btnAddOwned.setVisibility(View.GONE);
        }

        View llSelectionControls = findViewById(R.id.llSelectionControls);
        if (llSelectionControls != null) {
            llSelectionControls.setVisibility(View.VISIBLE);
        }

        ImageButton btnMoveSelected = findViewById(R.id.btnMoveSelected);
        if (btnMoveSelected != null) {
            btnMoveSelected.setVisibility(isOwnedTab ? View.VISIBLE : View.GONE);
        }

        if (isOwnedTab && isVisualOwnedMode) {
            renderOwnedVisualView();
        } else if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemLongClicked(GroceryItem item, int position) {
        if (item.isEmptyRow()) return;

        item.setSelected(true);
        enterSelectionMode();
    }

}
