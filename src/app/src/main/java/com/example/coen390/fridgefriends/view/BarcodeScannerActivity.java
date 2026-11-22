package com.example.coen390.fridgefriends.view;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.coen390.fridgefriends.R;
import com.example.coen390.fridgefriends.model.DateParser;
import com.example.coen390.fridgefriends.model.ExpiryDateParser;
import com.google.common.util.concurrent.ListenableFuture;
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
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BarcodeScannerActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeScannerActivity";
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

    // View References
    private DecoratedBarcodeView barcodeScannerView;
    private PreviewView previewView;
    private ViewfinderOverlayView viewfinderOverlay;
    private View cardProductPreview, cardDatePreview, boxDateReticle;
    private ImageView imgProductThumb;
    private TextView tvProductBrand, tvProductName, tvScanInstruction, tvScannedDateText;
    private TextView pillBarcode, pillExpiration;
    private Button btnNextScan;
    private ImageButton btnTorchToggle;

    // State Variables
    private String recognizedFullName = "";
    private String scannedExpiryDate = "";
    private String currentScannedImageUrl = "";
    private boolean isLookupInProgress = false;
    private boolean isOcrInProgress = false;
    private boolean isScanningPaused = false;
    private boolean isTorchOn = false;

    // CameraX and ML Kit
    private ProcessCameraProvider cameraProvider;
    private CameraControl cameraControl;
    private CameraInfo cameraInfo;
    private TextRecognizer textRecognizer;
    private ExecutorService cameraExecutor;
    private long lastFrameAnalyzedTime = 0L;

    // Multi-Frame Consensus Buffer (3 of 5 frames match)
    private final Deque<String> frameBuffer = new ArrayDeque<>();
    private static final int BUFFER_SIZE = 5;
    private static final int CONSENSUS_THRESHOLD = 3;

    private static final Map<String, String[]> PLU_DATABASE = new HashMap<>();
    static {
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
        cameraExecutor = Executors.newSingleThreadExecutor();

        barcodeScannerView = findViewById(R.id.barcodeScannerView);
        previewView = findViewById(R.id.previewView);
        viewfinderOverlay = findViewById(R.id.viewfinderOverlay);
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
        btnTorchToggle = findViewById(R.id.btnTorchToggle);
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
                returnResultAndFinish();
            }
        });

        // Flashlight / Torch Toggle Button
        btnTorchToggle.setOnClickListener(v -> toggleTorch());

        // Tap-to-focus on CameraX PreviewView
        setupTapToFocus();

        barcodeScannerView.initializeFromIntent(getIntent());
        barcodeScannerView.setStatusText("");

        View.OnClickListener manualDatePickerListener = v -> openManualDatePicker();
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

    private void toggleTorch() {
        isTorchOn = !isTorchOn;
        if (currentMode == ScanMode.EXPIRATION && cameraControl != null) {
            cameraControl.enableTorch(isTorchOn);
        } else if (currentMode == ScanMode.BARCODE && barcodeScannerView != null) {
            if (isTorchOn) {
                barcodeScannerView.setTorchOn();
            } else {
                barcodeScannerView.setTorchOff();
            }
        }
        btnTorchToggle.setColorFilter(isTorchOn ? Color.YELLOW : Color.WHITE);
    }

    private void setupTapToFocus() {
        previewView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (cameraControl != null) {
                    MeteringPointFactory factory = previewView.getMeteringPointFactory();
                    MeteringPoint point = factory.createPoint(event.getX(), event.getY());
                    FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build();
                    cameraControl.startFocusAndMetering(action);
                }
                v.performClick();
                return true;
            }
            return false;
        });
    }

    private void switchToMode(ScanMode mode) {
        this.currentMode = mode;
        View pillExpirationLayout = findViewById(R.id.pillExpirationLayout);
        ImageView ivPillExpirationIcon = findViewById(R.id.ivPillExpirationIcon);

        if (mode == ScanMode.BARCODE) {
            stopCameraX();
            barcodeScannerView.setVisibility(View.VISIBLE);
            barcodeScannerView.resume();
            previewView.setVisibility(View.GONE);
            viewfinderOverlay.setVisibility(View.GONE);
            boxDateReticle.setVisibility(View.GONE);

            pillBarcode.setBackgroundResource(R.drawable.bg_pill_button);
            pillBarcode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dark_green, null)));
            pillBarcode.setTextColor(getResources().getColor(R.color.white, null));

            if (pillExpirationLayout != null) pillExpirationLayout.setBackground(null);
            if (pillExpiration != null) pillExpiration.setTextColor(Color.parseColor("#D0D0D0"));
            if (ivPillExpirationIcon != null) ivPillExpirationIcon.setColorFilter(Color.parseColor("#D0D0D0"));

            tvScanInstruction.setText("Point at barcode");
            cardDatePreview.setVisibility(View.GONE);

            if (!recognizedFullName.isEmpty()) {
                cardProductPreview.setVisibility(View.VISIBLE);
                btnNextScan.setVisibility(View.VISIBLE);
            } else {
                btnNextScan.setVisibility(View.GONE);
            }
        } else {
            // Expiration Mode with CameraX
            barcodeScannerView.pause();
            barcodeScannerView.setVisibility(View.GONE);
            previewView.setVisibility(View.VISIBLE);
            viewfinderOverlay.setVisibility(View.VISIBLE);
            boxDateReticle.setVisibility(View.GONE);

            if (pillExpirationLayout != null) {
                pillExpirationLayout.setBackgroundResource(R.drawable.bg_pill_button);
                pillExpirationLayout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dark_green, null)));
            }
            if (pillExpiration != null) pillExpiration.setTextColor(getResources().getColor(R.color.white, null));
            if (ivPillExpirationIcon != null) ivPillExpirationIcon.setColorFilter(getResources().getColor(R.color.white, null));

            pillBarcode.setBackground(null);
            pillBarcode.setTextColor(Color.parseColor("#D0D0D0"));

            tvScanInstruction.setText("Align expiry date inside box");
            cardProductPreview.setVisibility(View.GONE);

            if (!scannedExpiryDate.isEmpty()) {
                tvScannedDateText.setText(DateParser.formatDisplayDate(scannedExpiryDate));
                cardDatePreview.setVisibility(View.VISIBLE);
                btnNextScan.setVisibility(View.VISIBLE);
            } else {
                cardDatePreview.setVisibility(View.GONE);
                btnNextScan.setVisibility(View.GONE);
            }

            frameBuffer.clear();
            isScanningPaused = false;
            startCameraX();
        }
    }

    private void startCameraX() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraXUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Error starting CameraX", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraXUseCases() {
        if (cameraProvider == null) return;
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(new Size(1280, 720))
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::processCameraXImageFrame);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        try {
            Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            cameraControl = camera.getCameraControl();
            cameraInfo = camera.getCameraInfo();
            if (isTorchOn && cameraControl != null) {
                cameraControl.enableTorch(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind CameraX use cases", e);
        }
    }

    private void stopCameraX() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    /**
     * CameraX ImageAnalysis Frame Analyzer:
     * 1. Uses STRATEGY_KEEP_ONLY_LATEST.
     * 2. Throttles frame analysis to ~3 frames per second (~330ms).
     * 3. Crops the ImageProxy bitmap to the exact 300dp x 80dp viewfinder reticle bounding box.
     * 4. Passes cropped bitmap to ML Kit Text Recognition.
     */
    private void processCameraXImageFrame(ImageProxy imageProxy) {
        if (isScanningPaused || currentMode != ScanMode.EXPIRATION || isOcrInProgress) {
            imageProxy.close();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameAnalyzedTime < 330) { // ~3 frames per second
            imageProxy.close();
            return;
        }
        lastFrameAnalyzedTime = currentTime;
        isOcrInProgress = true;

        try {
            Bitmap originalBmp = imageProxy.toBitmap();
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            imageProxy.close();

            if (originalBmp == null) {
                isOcrInProgress = false;
                return;
            }

            // Rotate bitmap to align with view orientation
            Bitmap rotatedBmp = rotateBitmap(originalBmp, rotationDegrees);

            // Crop to exact 300dp x 80dp cutout rectangle of viewfinder overlay
            RectF normCutout = viewfinderOverlay.getNormalizedCutoutRect();
            Bitmap croppedBmp = cropBitmapToNormalizedRect(rotatedBmp, normCutout);

            InputImage inputImage = InputImage.fromBitmap(croppedBmp, 0);
            textRecognizer.process(inputImage)
                    .addOnSuccessListener(visionText -> {
                        String text = visionText.getText();
                        if (text != null && !text.isEmpty()) {
                            String parsedDate = ExpiryDateParser.parseExpiryDate(text);
                            if (parsedDate != null) {
                                processMultiFrameConsensus(parsedDate);
                            }
                        }
                        isOcrInProgress = false;
                    })
                    .addOnFailureListener(e -> isOcrInProgress = false);
        } catch (Exception e) {
            Log.e(TAG, "Error in OCR frame processing", e);
            isOcrInProgress = false;
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || bitmap == null) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap cropBitmapToNormalizedRect(Bitmap source, RectF normRect) {
        int w = source.getWidth();
        int h = source.getHeight();

        int left = (int) (normRect.left * w);
        int top = (int) (normRect.top * h);
        int width = (int) (normRect.width() * w);
        int height = (int) (normRect.height() * h);

        // Clamp boundaries
        left = Math.max(0, Math.min(left, w - 1));
        top = Math.max(0, Math.min(top, h - 1));
        width = Math.max(1, Math.min(width, w - left));
        height = Math.max(1, Math.min(height, h - top));

        return Bitmap.createBitmap(source, left, top, width, height);
    }

    /**
     * Multi-Frame Consensus Stabilization:
     * Maintains a rolling buffer of the last 5 frames.
     * Locks in only when the exact same normalized date (yyyy-MM-dd) is detected in >= 3 of last 5 frames.
     * Triggers brief haptic vibration upon lock-in.
     */
    private void processMultiFrameConsensus(String detectedDateYmd) {
        if (isScanningPaused) return;

        synchronized (frameBuffer) {
            if (frameBuffer.size() >= BUFFER_SIZE) {
                frameBuffer.pollFirst();
            }
            frameBuffer.addLast(detectedDateYmd);

            Map<String, Integer> counts = new HashMap<>();
            for (String d : frameBuffer) {
                counts.put(d, counts.getOrDefault(d, 0) + 1);
            }

            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() >= CONSENSUS_THRESHOLD) {
                    String lockedDate = entry.getKey();
                    frameBuffer.clear();
                    triggerHapticVibration();
                    runOnUiThread(() -> onExpiryDateConsensusLocked(lockedDate));
                    break;
                }
            }
        }
    }

    private void triggerHapticVibration() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(100);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Pauses scanning and displays confirmation dialog with quick adjustment buttons and manual DatePickerDialog fallback.
     */
    private void onExpiryDateConsensusLocked(String lockedYmd) {
        isScanningPaused = true;
        showConfirmationDialog(lockedYmd);
    }

    private void showConfirmationDialog(String initialYmd) {
        final Calendar calendar = Calendar.getInstance();
        try {
            String[] parts = initialYmd.split("-");
            if (parts.length == 3) {
                calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            }
        } catch (Exception ignored) {}

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_confirm_expiry_date);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCancelable(false);

        TextView tvDialogDetectedDate = dialog.findViewById(R.id.tvDialogDetectedDate);
        TextView tvDialogFormattedDate = dialog.findViewById(R.id.tvDialogFormattedDate);
        Button btnPlus1Day = dialog.findViewById(R.id.btnPlus1Day);
        Button btnPlus3Days = dialog.findViewById(R.id.btnPlus3Days);
        Button btnPlus1Week = dialog.findViewById(R.id.btnPlus1Week);
        Button btnManualDatePicker = dialog.findViewById(R.id.btnManualDatePicker);
        Button btnDialogRescan = dialog.findViewById(R.id.btnDialogRescan);
        Button btnDialogConfirm = dialog.findViewById(R.id.btnDialogConfirm);

        Runnable updateDialogDisplay = () -> {
            String currentYmd = String.format(Locale.US, "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH));
            tvDialogDetectedDate.setText(currentYmd);
            tvDialogFormattedDate.setText(DateParser.formatDisplayDate(currentYmd));
        };

        updateDialogDisplay.run();

        // One-tap Quick Adjustments
        btnPlus1Day.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDialogDisplay.run();
        });

        btnPlus3Days.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 3);
            updateDialogDisplay.run();
        });

        btnPlus1Week.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 7);
            updateDialogDisplay.run();
        });

        // Manual DatePickerDialog Fallback
        btnManualDatePicker.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    BarcodeScannerActivity.this,
                    R.style.CustomDatePickerDialog,
                    (view, y, m, d) -> {
                        calendar.set(Calendar.YEAR, y);
                        calendar.set(Calendar.MONTH, m);
                        calendar.set(Calendar.DAY_OF_MONTH, d);
                        updateDialogDisplay.run();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Rescan Action
        btnDialogRescan.setOnClickListener(v -> {
            dialog.dismiss();
            synchronized (frameBuffer) {
                frameBuffer.clear();
            }
            isScanningPaused = false;
        });

        // Confirm Action
        btnDialogConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            scannedExpiryDate = String.format(Locale.US, "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH));
            tvScannedDateText.setText(DateParser.formatDisplayDate(scannedExpiryDate));
            cardDatePreview.setVisibility(View.VISIBLE);
            btnNextScan.setVisibility(View.VISIBLE);

            returnResultAndFinish();
        });

        dialog.show();
    }

    private void openManualDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this, R.style.CustomDatePickerDialog,
                (view1, y, m, d) -> {
                    scannedExpiryDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                    tvScannedDateText.setText(DateParser.formatDisplayDate(scannedExpiryDate));
                    cardDatePreview.setVisibility(View.VISIBLE);
                    btnNextScan.setVisibility(View.VISIBLE);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void returnResultAndFinish() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SCANNED_NAME, recognizedFullName);
        resultIntent.putExtra(EXTRA_SCANNED_EXPIRY, scannedExpiryDate);
        resultIntent.putExtra(EXTRA_SCANNED_IMAGE_URL, currentScannedImageUrl);
        setResult(RESULT_OK, resultIntent);
        finish();
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

        if (PLU_DATABASE.containsKey(cleanCode)) {
            String[] info = PLU_DATABASE.get(cleanCode);
            if (info != null) {
                showProductPreview(info[0], info[1], null);
                return;
            }
        }

        if (LOCAL_PRODUCT_DB.containsKey(cleanCode)) {
            String[] info = LOCAL_PRODUCT_DB.get(cleanCode);
            if (info != null) {
                showProductPreview(info[0], info[1], null);
                return;
            }
        }

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

            new Handler(Looper.getMainLooper()).post(() ->
                    showProductPreview("Scanned Item", "Product #" + code.substring(Math.max(0, code.length() - 4)), null)
            );
        }).start();
    }

    private static final Map<String, String> PRODUCT_IMAGE_MAP = new HashMap<>();
    static {
        PRODUCT_IMAGE_MAP.put("cookie", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=200");
        PRODUCT_IMAGE_MAP.put("cookies", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=200");
        PRODUCT_IMAGE_MAP.put("biscuit", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=200");
        PRODUCT_IMAGE_MAP.put("bread", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=200");
        PRODUCT_IMAGE_MAP.put("milk", "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=200");
        PRODUCT_IMAGE_MAP.put("cheese", "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=200");
        PRODUCT_IMAGE_MAP.put("apple", "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=200");
        PRODUCT_IMAGE_MAP.put("banana", "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=200");
        PRODUCT_IMAGE_MAP.put("tomato", "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=200");
        PRODUCT_IMAGE_MAP.put("sauce", "https://images.unsplash.com/photo-1572449043416-55f4685c9bb7?w=200");
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
        }

        cardProductPreview.setVisibility(View.VISIBLE);
        btnNextScan.setVisibility(View.VISIBLE);
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
            if (currentMode == ScanMode.BARCODE) {
                barcodeScannerView.resume();
            } else {
                isScanningPaused = false;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentMode == ScanMode.BARCODE) {
            barcodeScannerView.pause();
        } else {
            isScanningPaused = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCameraX();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
