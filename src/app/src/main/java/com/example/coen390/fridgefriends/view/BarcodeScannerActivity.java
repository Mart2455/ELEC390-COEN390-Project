package com.example.coen390.fridgefriends.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.coen390.fridgefriends.R;
import com.example.coen390.fridgefriends.model.DateParser;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BarcodeScannerActivity extends AppCompatActivity {

    public static final String EXTRA_SCANNED_NAME = "extra_scanned_name";
    public static final String EXTRA_SCANNED_EXPIRY = "extra_scanned_expiry";
    public static final String EXTRA_SCANNED_IMAGE_URL = "extra_scanned_image_url";
    public static final String EXTRA_START_MODE = "extra_start_mode";
    private static final int CAMERA_PERMISSION_REQUEST = 101;

    private enum ScanMode {
        BARCODE,
        EXPIRATION
    }

    private ScanMode currentMode = ScanMode.BARCODE;

    private DecoratedBarcodeView barcodeScannerView;
    private View cardProductPreview, cardDatePreview, boxDateReticle;
    private ImageView imgProductThumb;
    private TextView tvProductBrand, tvProductName, tvScanInstruction, tvScannedDateText;
    private TextView pillBarcode, pillExpiration;
    private Button btnNextScan;

    private String recognizedFullName = "";
    private String scannedExpiryDate = "";
    private String currentScannedImageUrl = "";
    private boolean isLookupInProgress = false;
    private boolean isOcrInProgress = false;
    private boolean isAutoFinishing = false;
    private TextRecognizer textRecognizer;

    private int countdownSeconds = 5;
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentMode == ScanMode.EXPIRATION) {
                if (countdownSeconds > 0) {
                    tvScanInstruction.setText("Point at the date (" + countdownSeconds + "s)");
                    countdownSeconds--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    tvScanInstruction.setText("Point at the date");
                    if (!scannedExpiryDate.isEmpty() && !isAutoFinishing) {
                        isAutoFinishing = true;
                        autoFinishAndAdd();
                    }
                }
            }
        }
    };

    private static final Map<String, String[]> PLU_DATABASE = new HashMap<>();
    static {
        // Produce PLU Codes on Stickers
        PLU_DATABASE.put("4017", new String[]{"Joyfully Grown", "Granny Smith Apple"});
        PLU_DATABASE.put("4011", new String[]{"Fresh Produce", "Yellow Banana"});
        PLU_DATABASE.put("4015", new String[]{"Fresh Produce", "Red Delicious Apple"});
        PLU_DATABASE.put("4016", new String[]{"Fresh Produce", "Gala Apple"});
        PLU_DATABASE.put("4020", new String[]{"Fresh Produce", "Golden Delicious Apple"});
        PLU_DATABASE.put("4021", new String[]{"Fresh Produce", "Fuji Apple"});
        PLU_DATABASE.put("4022", new String[]{"Fresh Produce", "Honeycrisp Apple"});
        PLU_DATABASE.put("4030", new String[]{"Fresh Produce", "Kiwi Fruit"});
        PLU_DATABASE.put("4032", new String[]{"Fresh Produce", "Watermelon"});
        PLU_DATABASE.put("4042", new String[]{"Fresh Produce", "Plum"});
        PLU_DATABASE.put("4046", new String[]{"Fresh Produce", "Hass Avocado"});
        PLU_DATABASE.put("4225", new String[]{"Fresh Produce", "Large Avocado"});
        PLU_DATABASE.put("4060", new String[]{"Fresh Produce", "Broccoli"});
        PLU_DATABASE.put("4062", new String[]{"Fresh Produce", "Cucumber"});
        PLU_DATABASE.put("4065", new String[]{"Fresh Produce", "Green Bell Pepper"});
        PLU_DATABASE.put("4068", new String[]{"Fresh Produce", "Green Onion"});
        PLU_DATABASE.put("4069", new String[]{"Fresh Produce", "Green Cabbage"});
        PLU_DATABASE.put("4072", new String[]{"Fresh Produce", "Russet Potato"});
        PLU_DATABASE.put("4080", new String[]{"Fresh Produce", "Asparagus"});
        PLU_DATABASE.put("4087", new String[]{"Fresh Produce", "Roma Tomato"});
        PLU_DATABASE.put("4012", new String[]{"Fresh Produce", "Navel Orange"});
        PLU_DATABASE.put("4033", new String[]{"Fresh Produce", "Fresh Lemon"});
        PLU_DATABASE.put("4048", new String[]{"Fresh Produce", "Fresh Lime"});
    }

    private static final Map<String, String[]> LOCAL_PRODUCT_DB = new HashMap<>();
    static {
        LOCAL_PRODUCT_DB.put("750220318941", new String[]{"Sunny Valley", "Pasta Sauce"});
        LOCAL_PRODUCT_DB.put("75022031894", new String[]{"Sunny Valley", "Pasta Sauce"});
        LOCAL_PRODUCT_DB.put("011110416001", new String[]{"Kroger", "Organic Gala Apples"});
        LOCAL_PRODUCT_DB.put("021130000000", new String[]{"Lucerne", "Whole Milk"});
        LOCAL_PRODUCT_DB.put("070038300001", new String[]{"Dole", "Fresh Bananas"});
        LOCAL_PRODUCT_DB.put("041520000001", new String[]{"Chiquita", "Organic Bananas"});
        LOCAL_PRODUCT_DB.put("033383000001", new String[]{"Dole", "Classic Salad"});
        LOCAL_PRODUCT_DB.put("078742351864", new String[]{"Great Value", "Diced Tomatoes"});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        barcodeScannerView = findViewById(R.id.barcodeScannerView);
        cardProductPreview = findViewById(R.id.cardProductPreview);
        cardDatePreview = findViewById(R.id.cardDatePreview);
        boxDateReticle = findViewById(R.id.boxDateReticle);
        imgProductThumb = findViewById(R.id.imgProductThumb);
        tvProductBrand = findViewById(R.id.tvProductBrand);
        tvProductName = findViewById(R.id.tvProductName);
        tvScanInstruction = findViewById(R.id.tvScanInstruction);
        tvScannedDateText = findViewById(R.id.tvScannedDateText);
        pillBarcode = findViewById(R.id.pillBarcode);
        pillExpiration = findViewById(R.id.pillExpiration);
        btnNextScan = findViewById(R.id.btnNextScan);
        ImageButton btnCloseScanner = findViewById(R.id.btnCloseScanner);

        btnCloseScanner.setOnClickListener(v -> finish());

        pillBarcode.setOnClickListener(v -> switchToMode(ScanMode.BARCODE));
        View pillExpirationLayout = findViewById(R.id.pillExpirationLayout);
        if (pillExpirationLayout != null) {
            pillExpirationLayout.setOnClickListener(v -> switchToMode(ScanMode.EXPIRATION));
        }
        if (pillExpiration != null) {
            pillExpiration.setOnClickListener(v -> switchToMode(ScanMode.EXPIRATION));
        }

        btnNextScan.setOnClickListener(v -> {
            if (currentMode == ScanMode.BARCODE) {
                switchToMode(ScanMode.EXPIRATION);
            } else {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SCANNED_NAME, recognizedFullName);
                resultIntent.putExtra(EXTRA_SCANNED_EXPIRY, scannedExpiryDate);
                resultIntent.putExtra(EXTRA_SCANNED_IMAGE_URL, currentScannedImageUrl);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        barcodeScannerView.initializeFromIntent(getIntent());
        barcodeScannerView.setStatusText(""); // Hide default status text

        View.OnClickListener manualDatePickerListener = v -> {
            if (currentMode == ScanMode.EXPIRATION) {
                final java.util.Calendar c = java.util.Calendar.getInstance();
                int year = c.get(java.util.Calendar.YEAR);
                int month = c.get(java.util.Calendar.MONTH);
                int day = c.get(java.util.Calendar.DAY_OF_MONTH);

                android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                        this, R.style.CustomDatePickerDialog,
                        (view1, y, m, d) -> {
                            scannedExpiryDate = String.format(java.util.Locale.US, "%02d/%02d/%d", d, m + 1, y);
                            tvScannedDateText.setText(DateParser.formatDisplayDate(scannedExpiryDate));
                            cardDatePreview.setVisibility(View.VISIBLE);
                            btnNextScan.setVisibility(View.VISIBLE);
                        },
                        year, month, day);
                datePickerDialog.show();
            }
        };

        boxDateReticle.setOnClickListener(manualDatePickerListener);
        cardDatePreview.setOnClickListener(manualDatePickerListener);
        tvScannedDateText.setOnClickListener(manualDatePickerListener);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }

        String startMode = getIntent().getStringExtra(EXTRA_START_MODE);
        if ("EXPIRATION".equalsIgnoreCase(startMode)) {
            switchToMode(ScanMode.EXPIRATION);
        }
    }

    private void switchToMode(ScanMode mode) {
        this.currentMode = mode;
        View pillExpirationLayout = findViewById(R.id.pillExpirationLayout);
        ImageView ivPillExpirationIcon = findViewById(R.id.ivPillExpirationIcon);

        if (mode == ScanMode.BARCODE) {
            pillBarcode.setBackgroundResource(R.drawable.bg_pill_button);
            pillBarcode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dark_green, null)));
            pillBarcode.setTextColor(getResources().getColor(R.color.white, null));

            if (pillExpirationLayout != null) pillExpirationLayout.setBackground(null);
            if (pillExpiration != null) pillExpiration.setTextColor(Color.parseColor("#D0D0D0"));
            if (ivPillExpirationIcon != null) ivPillExpirationIcon.setColorFilter(Color.parseColor("#D0D0D0"));

            tvScanInstruction.setText("Point at barcode");
            cardDatePreview.setVisibility(View.GONE);
            boxDateReticle.setVisibility(View.GONE);

            if (!recognizedFullName.isEmpty()) {
                cardProductPreview.setVisibility(View.VISIBLE);
                btnNextScan.setVisibility(View.VISIBLE);
            } else {
                btnNextScan.setVisibility(View.GONE);
            }
        } else {
            // Expiration Mode
            if (pillExpirationLayout != null) {
                pillExpirationLayout.setBackgroundResource(R.drawable.bg_pill_button);
                pillExpirationLayout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dark_green, null)));
            }
            if (pillExpiration != null) pillExpiration.setTextColor(getResources().getColor(R.color.white, null));
            if (ivPillExpirationIcon != null) ivPillExpirationIcon.setColorFilter(getResources().getColor(R.color.white, null));

            pillBarcode.setBackground(null);
            pillBarcode.setTextColor(Color.parseColor("#D0D0D0"));

            tvScanInstruction.setText("Point at the date");
            cardProductPreview.setVisibility(View.GONE);
            
            // Hide date bubble & next button by default until a valid date is detected
            cardDatePreview.setVisibility(View.GONE);
            btnNextScan.setVisibility(View.GONE);
            boxDateReticle.setVisibility(View.VISIBLE);

            if (!scannedExpiryDate.isEmpty()) {
                tvScannedDateText.setText(DateParser.formatDisplayDate(scannedExpiryDate));
                cardDatePreview.setVisibility(View.VISIBLE);
                btnNextScan.setVisibility(View.VISIBLE);
            }

            countdownSeconds = 5;
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownHandler.post(countdownRunnable);

            ocrHandler.removeCallbacks(ocrRunnable);
            ocrHandler.post(ocrRunnable);
        }
    }

    private final Handler ocrHandler = new Handler(Looper.getMainLooper());
    private final Runnable ocrRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isOcrInProgress) {
                captureAndProcessOcrFrame();
            }
            ocrHandler.postDelayed(this, 250);
        }
    };

    private void captureAndProcessOcrFrame() {
        try {
            Bitmap bitmap = null;
            View view = barcodeScannerView.getBarcodeView();
            if (view instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    if (child instanceof android.view.TextureView) {
                        bitmap = ((android.view.TextureView) child).getBitmap();
                        break;
                    }
                }
            }
            if (bitmap == null) {
                return;
            }

            if (bitmap != null) {
                isOcrInProgress = true;
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                textRecognizer.process(image)
                        .addOnSuccessListener(visionText -> {
                            String text = visionText.getText();
                            if (text != null && !text.isEmpty()) {
                                if (currentMode == ScanMode.EXPIRATION) {
                                    String extractedDate = DateParser.extractFormattedDate(text);
                                    if (extractedDate != null) {
                                        scannedExpiryDate = extractedDate;
                                        tvScannedDateText.setText(DateParser.formatDisplayDate(scannedExpiryDate));
                                        cardDatePreview.setVisibility(View.VISIBLE);
                                        btnNextScan.setVisibility(View.VISIBLE);

                                        if (countdownSeconds <= 0 && !isAutoFinishing) {
                                            isAutoFinishing = true;
                                            autoFinishAndAdd();
                                        }
                                    }
                                } else if (currentMode == ScanMode.BARCODE && !isLookupInProgress) {
                                    // Scan OCR text for produce PLU codes (e.g. 4017 on stickers)
                                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{4})\\b").matcher(text);
                                    if (m.find()) {
                                        String pluCode = m.group(1);
                                        if (PLU_DATABASE.containsKey(pluCode)) {
                                            lookupBarcode(pluCode);
                                        }
                                    } else if (text.toLowerCase().contains("granny") || text.toLowerCase().contains("apple")) {
                                        lookupBarcode("4017");
                                    }
                                }
                            }
                            isOcrInProgress = false;
                        })
                        .addOnFailureListener(e -> isOcrInProgress = false);
            }
        } catch (Exception e) {
            isOcrInProgress = false;
        }
    }

    private void autoFinishAndAdd() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SCANNED_NAME, recognizedFullName);
            resultIntent.putExtra(EXTRA_SCANNED_EXPIRY, scannedExpiryDate);
            resultIntent.putExtra(EXTRA_SCANNED_IMAGE_URL, currentScannedImageUrl);
            setResult(RESULT_OK, resultIntent);
            finish();
        }, 500);
    }

    private void startScanning() {
        barcodeScannerView.resume();
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (currentMode == ScanMode.BARCODE) {
                    if (result.getText() != null && !isLookupInProgress) {
                        lookupBarcode(result.getText().trim());
                    }
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void lookupBarcode(String code) {
        if (isLookupInProgress) return;
        isLookupInProgress = true;

        String cleanCode = code.replaceAll("[^0-9]", "");

        // Check PLU database first
        if (PLU_DATABASE.containsKey(cleanCode)) {
            String[] info = PLU_DATABASE.get(cleanCode);
            if (info != null) {
                showProductPreview(info[0], info[1], null);
                return;
            }
        }

        // Check local DB
        if (LOCAL_PRODUCT_DB.containsKey(cleanCode)) {
            String[] info = LOCAL_PRODUCT_DB.get(cleanCode);
            if (info != null) {
                showProductPreview(info[0], info[1], null);
                return;
            }
        }

        // Async API lookup via Open Food Facts API
        new Thread(() -> {
            try {
                URL url = new URL("https://world.openfoodfacts.org/api/v0/product/" + code + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    JSONObject json = new JSONObject(response);

                    if (json.optInt("status", 0) == 1 && json.has("product")) {
                        JSONObject product = json.getJSONObject("product");
                        String brandStr = product.optString("brands", "Scanned Product");
                        String brand = brandStr.split(",")[0].trim();
                        String name = product.optString("product_name", "Produce Item");
                        String imageUrl = product.optString("image_url", "");

                        if (brand.isEmpty()) brand = "Produce";
                        if (name.isEmpty()) name = "Scanned Item";

                        final String finalBrand = brand;
                        final String finalName = name;
                        final String finalImageUrl = imageUrl;

                        new Handler(Looper.getMainLooper()).post(() ->
                            showProductPreview(finalBrand, finalName, finalImageUrl)
                        );
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Fallback if not found in API
            new Handler(Looper.getMainLooper()).post(() ->
                showProductPreview("Scanned Item", "Product #" + code.substring(Math.max(0, code.length() - 4)), null)
            );
        }).start();
    }

    private static final Map<String, String> PRODUCT_IMAGE_MAP = new HashMap<>();
    static {
        // Cookies, Biscuits & Snacks
        PRODUCT_IMAGE_MAP.put("cookie", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=200");
        PRODUCT_IMAGE_MAP.put("cookies", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=200");
        PRODUCT_IMAGE_MAP.put("biscuit", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=200");
        PRODUCT_IMAGE_MAP.put("biscuits", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=200");
        PRODUCT_IMAGE_MAP.put("oreo", "https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=200");
        PRODUCT_IMAGE_MAP.put("chocolate chip", "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=200");
        PRODUCT_IMAGE_MAP.put("wafer", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=200");

        // Bakery & Bread
        PRODUCT_IMAGE_MAP.put("bread", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=200");
        PRODUCT_IMAGE_MAP.put("toast", "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=200");
        PRODUCT_IMAGE_MAP.put("bagel", "https://images.unsplash.com/photo-1585478259715-876acc5be8eb?w=200");
        PRODUCT_IMAGE_MAP.put("croissant", "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=200");
        PRODUCT_IMAGE_MAP.put("bun", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=200");

        // Dairy & Refrigerated
        PRODUCT_IMAGE_MAP.put("milk", "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=200");
        PRODUCT_IMAGE_MAP.put("cheese", "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=200");
        PRODUCT_IMAGE_MAP.put("butter", "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=200");
        PRODUCT_IMAGE_MAP.put("yogurt", "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=200");
        PRODUCT_IMAGE_MAP.put("yoghurt", "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=200");
        PRODUCT_IMAGE_MAP.put("cream", "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=200");
        PRODUCT_IMAGE_MAP.put("egg", "https://images.unsplash.com/photo-1506976785307-8732e854ad03?w=200");
        PRODUCT_IMAGE_MAP.put("eggs", "https://images.unsplash.com/photo-1506976785307-8732e854ad03?w=200");

        // Fruits
        PRODUCT_IMAGE_MAP.put("apple", "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=200");
        PRODUCT_IMAGE_MAP.put("banana", "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=200");
        PRODUCT_IMAGE_MAP.put("bananas", "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=200");
        PRODUCT_IMAGE_MAP.put("orange", "https://images.unsplash.com/photo-1547514701-42782101795e?w=200");
        PRODUCT_IMAGE_MAP.put("strawberry", "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=200");
        PRODUCT_IMAGE_MAP.put("berries", "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=200");
        PRODUCT_IMAGE_MAP.put("blueberry", "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=200");
        PRODUCT_IMAGE_MAP.put("grape", "https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=200");
        PRODUCT_IMAGE_MAP.put("grapes", "https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=200");
        PRODUCT_IMAGE_MAP.put("lemon", "https://images.unsplash.com/photo-1534531141161-e41d133a8b50?w=200");
        PRODUCT_IMAGE_MAP.put("lime", "https://images.unsplash.com/photo-1534531141161-e41d133a8b50?w=200");
        PRODUCT_IMAGE_MAP.put("watermelon", "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=200");
        PRODUCT_IMAGE_MAP.put("avocado", "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=200");

        // Vegetables
        PRODUCT_IMAGE_MAP.put("tomato", "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=200");
        PRODUCT_IMAGE_MAP.put("tomatoes", "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=200");
        PRODUCT_IMAGE_MAP.put("broccoli", "https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?w=200");
        PRODUCT_IMAGE_MAP.put("carrot", "https://images.unsplash.com/photo-1598170845058-12ef4a457c15?w=200");
        PRODUCT_IMAGE_MAP.put("carrots", "https://images.unsplash.com/photo-1598170845058-12ef4a457c15?w=200");
        PRODUCT_IMAGE_MAP.put("onion", "https://images.unsplash.com/photo-1508747703725-719777637510?w=200");
        PRODUCT_IMAGE_MAP.put("potato", "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=200");
        PRODUCT_IMAGE_MAP.put("potatoes", "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=200");
        PRODUCT_IMAGE_MAP.put("cucumber", "https://images.unsplash.com/photo-1449300079323-02e209d9d3a6?w=200");
        PRODUCT_IMAGE_MAP.put("pepper", "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=200");
        PRODUCT_IMAGE_MAP.put("spinach", "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=200");
        PRODUCT_IMAGE_MAP.put("lettuce", "https://images.unsplash.com/photo-1622206151226-18ca2c9ab4a1?w=200");
        PRODUCT_IMAGE_MAP.put("salad", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=200");

        // Meats & Fish
        PRODUCT_IMAGE_MAP.put("chicken", "https://images.unsplash.com/photo-1587593810167-a84920ea0781?w=200");
        PRODUCT_IMAGE_MAP.put("beef", "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=200");
        PRODUCT_IMAGE_MAP.put("steak", "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=200");
        PRODUCT_IMAGE_MAP.put("pork", "https://images.unsplash.com/photo-1602470520998-f4a52199a3d6?w=200");
        PRODUCT_IMAGE_MAP.put("bacon", "https://images.unsplash.com/photo-1528607929212-2636ec44253e?w=200");
        PRODUCT_IMAGE_MAP.put("sausage", "https://images.unsplash.com/photo-1585325701165-351af916e581?w=200");
        PRODUCT_IMAGE_MAP.put("salmon", "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=200");
        PRODUCT_IMAGE_MAP.put("fish", "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=200");
        PRODUCT_IMAGE_MAP.put("tuna", "https://images.unsplash.com/photo-1534604973900-c43ab4c2e0ab?w=200");

        // Pantry & Sauces
        PRODUCT_IMAGE_MAP.put("pasta sauce", "https://images.unsplash.com/photo-1572449043416-55f4685c9bb7?w=200");
        PRODUCT_IMAGE_MAP.put("sunny valley", "https://images.unsplash.com/photo-1572449043416-55f4685c9bb7?w=200");
        PRODUCT_IMAGE_MAP.put("sauce", "https://images.unsplash.com/photo-1572449043416-55f4685c9bb7?w=200");
        PRODUCT_IMAGE_MAP.put("pasta", "https://images.unsplash.com/photo-1621996346565-e3d5d6288015?w=200");
        PRODUCT_IMAGE_MAP.put("spaghetti", "https://images.unsplash.com/photo-1621996346565-e3d5d6288015?w=200");
        PRODUCT_IMAGE_MAP.put("rice", "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=200");
        PRODUCT_IMAGE_MAP.put("ketchup", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=200");
        PRODUCT_IMAGE_MAP.put("soup", "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=200");

        // Beverages & Drinks
        PRODUCT_IMAGE_MAP.put("juice", "https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?w=200");
        PRODUCT_IMAGE_MAP.put("soda", "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=200");
        PRODUCT_IMAGE_MAP.put("coke", "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=200");
        PRODUCT_IMAGE_MAP.put("water", "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=200");
        PRODUCT_IMAGE_MAP.put("coffee", "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=200");
        PRODUCT_IMAGE_MAP.put("tea", "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=200");

        // Snacks & Candy
        PRODUCT_IMAGE_MAP.put("chips", "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=200");
        PRODUCT_IMAGE_MAP.put("chocolate", "https://images.unsplash.com/photo-1511381939415-e44015466834?w=200");
        PRODUCT_IMAGE_MAP.put("candy", "https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=200");
        PRODUCT_IMAGE_MAP.put("nuts", "https://images.unsplash.com/photo-1536591375315-1989565823e3?w=200");
        PRODUCT_IMAGE_MAP.put("popcorn", "https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=200");
    }

    private void showProductPreview(String brand, String name, String imageUrl) {
        recognizedFullName = (brand.isEmpty() ? "" : brand + " ") + name;
        tvProductBrand.setText(brand);
        tvProductName.setText(name);

        String targetUrl = imageUrl;
        if (targetUrl == null || targetUrl.isEmpty()) {
            String lower = (brand + " " + name).toLowerCase();
            for (Map.Entry<String, String> entry : PRODUCT_IMAGE_MAP.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    targetUrl = entry.getValue();
                    break;
                }
            }
        }

        currentScannedImageUrl = targetUrl == null ? "" : targetUrl;

        if (targetUrl != null && !targetUrl.isEmpty()) {
            fetchImage(targetUrl);
        } else {
            fetchWikimediaImage(name);
        }

        cardProductPreview.setVisibility(View.VISIBLE);
        btnNextScan.setVisibility(View.VISIBLE);
    }

    private void fetchWikimediaImage(String queryName) {
        new Thread(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(queryName, "UTF-8");
                URL url = new URL("https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=" 
                        + encoded + "&gsrlimit=1&prop=pageimages&pithumbsize=200&format=json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    JSONObject json = new JSONObject(response);
                    if (json.has("query")) {
                        JSONObject pages = json.getJSONObject("query").getJSONObject("pages");
                        String firstKey = pages.keys().next();
                        JSONObject page = pages.getJSONObject(firstKey);
                        if (page.has("thumbnail")) {
                            String imgUrl = page.getJSONObject("thumbnail").getString("source");
                            fetchImage(imgUrl);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void fetchImage(String urlStr) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                InputStream is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (bitmap != null) {
                        imgProductThumb.setImageBitmap(bitmap);
                        imgProductThumb.clearColorFilter();
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeScannerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScannerView.pause();
    }
}
