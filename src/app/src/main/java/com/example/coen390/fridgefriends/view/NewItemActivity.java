package com.example.coen390.fridgefriends.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.coen390.fridgefriends.R;
import com.example.coen390.fridgefriends.model.DateParser;
import com.example.coen390.fridgefriends.model.GroceryItem;
import com.example.coen390.fridgefriends.model.ProductImageResolver;

import org.json.JSONArray;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

public class NewItemActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_NAME = "extra_item_name";
    public static final String EXTRA_ITEM_EXPIRY = "extra_item_expiry";
    public static final String EXTRA_ITEM_IMAGE_URL = "extra_item_image_url";
    public static final String EXTRA_IS_EDIT_MODE = "extra_is_edit_mode";
    public static final String EXTRA_ITEM_INDEX = "extra_item_index";
    public static final String EXTRA_ITEM_CATEGORY = "extra_item_category";
    public static final String EXTRA_ITEM_QUANTITY = "extra_item_quantity";
    public static final String EXTRA_ITEM_UNIT = "extra_item_unit";
    public static final String EXTRA_ITEM_MEMO = "extra_item_memo";

    private ImageView imgNewItemPhoto;
    private View cardProductPhoto, btnAddPhoto;
    private ImageButton btnDeletePhoto;
    private EditText etNewItemName, etNewItemMemo;
    private TextView tvNewItemExpiry, tvQtyValue, tvUnitText;
    private TextView btnQtyMinus, btnQtyPlus;
    private Spinner spNewItemType;
    private Button btnSaveToFridge, btnAddtoGroceryList;

    private String currentImageUrl = "";
    private String currentExpiryDate = "";
    private int quantity = 1;
    private String[] units = {"pcs", "kg", "L", "pack", "box"};
    private int unitIndex = 0;
    private boolean isEditMode = false;
    private int itemIndex = -1;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.get("data") instanceof Bitmap) {
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        imgNewItemPhoto.setImageBitmap(bitmap);
                        cardProductPhoto.setVisibility(View.VISIBLE);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> barcodeScannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String scannedName = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_SCANNED_NAME);
                    String scannedExpiry = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_SCANNED_EXPIRY);
                    String scannedImageUrl = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_SCANNED_IMAGE_URL);

                    if (scannedName != null && !scannedName.isEmpty()) {
                        etNewItemName.setText(scannedName);
                    }
                    if (scannedExpiry != null && !scannedExpiry.isEmpty() && !scannedExpiry.equalsIgnoreCase("no expiration")) {
                        currentExpiryDate = scannedExpiry;
                        tvNewItemExpiry.setText(DateParser.formatDisplayDate(scannedExpiry));
                    }
                    if (scannedImageUrl != null && !scannedImageUrl.isEmpty()) {
                        currentImageUrl = scannedImageUrl;
                        fetchImage(scannedImageUrl);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);

        ImageButton btnBack = findViewById(R.id.btnNewItemBack);
        TextView tvNewItemTitle = findViewById(R.id.tvNewItemTitle);
        cardProductPhoto = findViewById(R.id.cardProductPhoto);
        imgNewItemPhoto = findViewById(R.id.imgNewItemPhoto);
        btnDeletePhoto = findViewById(R.id.btnDeletePhoto);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        Button btnScanItem = findViewById(R.id.btnScanItem);

        btnScanItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, BarcodeScannerActivity.class);
            barcodeScannerLauncher.launch(intent);
        });

        etNewItemName = findViewById(R.id.etNewItemName);
        tvNewItemExpiry = findViewById(R.id.tvNewItemExpiry);
        spNewItemType = findViewById(R.id.spNewItemType);
        btnQtyMinus = findViewById(R.id.btnQtyMinus);
        tvQtyValue = findViewById(R.id.tvQtyValue);
        btnQtyPlus = findViewById(R.id.btnQtyPlus);
        tvUnitText = findViewById(R.id.tvUnitText);
        etNewItemMemo = findViewById(R.id.etNewItemMemo);
        btnSaveToFridge = findViewById(R.id.btnSaveToFridge);
        btnAddtoGroceryList = findViewById(R.id.btnAddtoGroceryList);
        View rlExpirationDate = findViewById(R.id.rlExpirationDate);

        btnBack.setOnClickListener(v -> finish());

        // Check if opened in Edit Mode
        isEditMode = getIntent().getBooleanExtra(EXTRA_IS_EDIT_MODE, false);
        itemIndex = getIntent().getIntExtra(EXTRA_ITEM_INDEX, -1);

        Button btnDeleteNewItem = findViewById(R.id.btnDeleteNewItem);

        if (isEditMode) {
            if (tvNewItemTitle != null) tvNewItemTitle.setText("Edit Item");
            btnSaveToFridge.setText("Save Changes");
            if (btnDeleteNewItem != null) btnDeleteNewItem.setVisibility(View.VISIBLE);
        }

        if (btnDeleteNewItem != null) {
            btnDeleteNewItem.setOnClickListener(v -> showDeleteConfirmationDialog());
        }

        // Setup Category Spinner
        String[] categories = {"Condiments & Sauces", "Dairy & Refrigerated", "Produce", "Meat & Seafood", "Bakery & Bread", "Beverages", "Snacks & Candy", "Pantry", "Other"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_right, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNewItemType.setAdapter(spinnerAdapter);

        // Intent Extras from Scanner or Edit Mode
        String name = getIntent().getStringExtra(EXTRA_ITEM_NAME);
        String expiry = getIntent().getStringExtra(EXTRA_ITEM_EXPIRY);
        String imageUrl = getIntent().getStringExtra(EXTRA_ITEM_IMAGE_URL);
        String category = getIntent().getStringExtra(EXTRA_ITEM_CATEGORY);
        int qty = getIntent().getIntExtra(EXTRA_ITEM_QUANTITY, 1);
        String unit = getIntent().getStringExtra(EXTRA_ITEM_UNIT);
        String memo = getIntent().getStringExtra(EXTRA_ITEM_MEMO);

        if (name != null && !name.isEmpty()) {
            etNewItemName.setText(name);
        }

        if (expiry != null && !expiry.isEmpty() && !expiry.equalsIgnoreCase("no expiration")) {
            currentExpiryDate = expiry;
            tvNewItemExpiry.setText(DateParser.formatDisplayDate(expiry));
        } else {
            currentExpiryDate = "";
            tvNewItemExpiry.setText("No expiry set");
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            currentImageUrl = imageUrl;
            fetchImage(imageUrl);
        } else {
            cardProductPhoto.setVisibility(View.GONE);
        }

        if (category != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(category)) {
                    spNewItemType.setSelection(i);
                    break;
                }
            }
        }

        if (qty > 0) {
            quantity = qty;
            tvQtyValue.setText(String.valueOf(quantity));
        }

        if (unit != null && !unit.isEmpty()) {
            for (int i = 0; i < units.length; i++) {
                if (units[i].equalsIgnoreCase(unit)) {
                    unitIndex = i;
                    tvUnitText.setText(units[unitIndex]);
                    break;
                }
            }
        }

        if (memo != null && !memo.isEmpty()) {
            etNewItemMemo.setText(memo);
        }

        // Add Photo click
        btnAddPhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        btnDeletePhoto.setOnClickListener(v -> {
            cardProductPhoto.setVisibility(View.GONE);
            currentImageUrl = "";
        });

        // Expiration Date click opens options popup
        rlExpirationDate.setOnClickListener(v -> showExpirationOptionsDialog());

        // Quantity controls
        btnQtyMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQtyValue.setText(String.valueOf(quantity));
            } else {
                showDeleteConfirmationDialog();
            }
        });
        btnQtyPlus.setOnClickListener(v -> {
            quantity++;
            tvQtyValue.setText(String.valueOf(quantity));
        });

        tvUnitText.setOnClickListener(v -> {
            unitIndex = (unitIndex + 1) % units.length;
            tvUnitText.setText(units[unitIndex]);
        });

        // Save to Fridge button
        btnSaveToFridge.setOnClickListener(v -> saveItemAndFinish(true));

        // Add to Grocery List button
        btnAddtoGroceryList.setOnClickListener(v -> saveItemAndFinish(false));
    }

    private void showDeleteConfirmationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_items, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView btnCancel = dialogView.findViewById(R.id.btnDeleteCancel);
        TextView btnYes = dialogView.findViewById(R.id.btnDeleteYes);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            deleteItemAndFinish();
        });

        dialog.show();
    }

    private void showExpirationOptionsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_expiration_options, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View btnOptionScanDate = dialogView.findViewById(R.id.btnOptionScanDate);
        View btnOptionCalendar = dialogView.findViewById(R.id.btnOptionCalendar);
        TextView btnCancel = dialogView.findViewById(R.id.btnExpirationCancel);

        btnOptionScanDate.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, BarcodeScannerActivity.class);
            intent.putExtra(BarcodeScannerActivity.EXTRA_START_MODE, "EXPIRATION");
            barcodeScannerLauncher.launch(intent);
        });

        btnOptionCalendar.setOnClickListener(v -> {
            dialog.dismiss();
            showManualDatePicker();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showManualDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.CustomDatePickerDialog,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    currentExpiryDate = String.format(Locale.US, "%02d/%02d/%d", dayOfMonth, monthOfYear + 1, year1);
                    tvNewItemExpiry.setText(DateParser.formatDisplayDate(currentExpiryDate));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void deleteItemAndFinish() {
        SharedPreferences prefs = getSharedPreferences("GroceryPrefs", MODE_PRIVATE);
        String jsonString = prefs.getString("grocery_list", "[]");
        try {
            JSONArray array = new JSONArray(jsonString);
            if (isEditMode && itemIndex >= 0 && itemIndex < array.length()) {
                array.remove(itemIndex);
            }
            prefs.edit().putString("grocery_list", array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, GroceryListActivity.class);
        intent.putExtra("OPEN_OWNED_TAB", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchImage(String urlStr) {
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
                        imgNewItemPhoto.setImageBitmap(bitmap);
                        cardProductPhoto.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void saveItemAndFinish(boolean isOwned) {
        String name = etNewItemName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter an item name", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spNewItemType.getSelectedItem() != null ? spNewItemType.getSelectedItem().toString() : "Other";
        String memo = etNewItemMemo.getText().toString().trim();
        String unit = tvUnitText.getText().toString();

        if (currentImageUrl == null || currentImageUrl.trim().isEmpty()) {
            currentImageUrl = ProductImageResolver.resolveImageUrl(name, category);
        }

        GroceryItem item = new GroceryItem(name, isOwned, currentExpiryDate, currentImageUrl, category, quantity, unit, memo);

        // Save to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("GroceryPrefs", MODE_PRIVATE);
        String jsonString = prefs.getString("grocery_list", "[]");
        try {
            JSONArray array = new JSONArray(jsonString);
            if (isEditMode && itemIndex >= 0 && itemIndex < array.length()) {
                array.put(itemIndex, item.toJSON());
            } else {
                array.put(item.toJSON());
            }
            prefs.edit().putString("grocery_list", array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, GroceryListActivity.class);
        intent.putExtra("OPEN_OWNED_TAB", isOwned);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
