package com.example.coen390.fridgefriends.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ViewfinderOverlayView extends View {

    private final Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF cutoutRect = new RectF();
    private float cutoutWidthPx;
    private float cutoutHeightPx;

    public ViewfinderOverlayView(Context context) {
        super(context);
        init();
    }

    public ViewfinderOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ViewfinderOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        scrimPaint.setColor(Color.parseColor("#B0000000")); // Darkened semi-transparent overlay
        scrimPaint.setStyle(Paint.Style.FILL);

        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        borderPaint.setColor(Color.WHITE); // White border reticle
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(3));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(spToPx(14));
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(6f, 0f, 2f, Color.parseColor("#80000000"));

        cutoutWidthPx = dpToPx(300);
        cutoutHeightPx = dpToPx(80);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float left = (w - cutoutWidthPx) / 2f;
        float top = (h - cutoutHeightPx) / 2f - dpToPx(20); // Slightly above absolute center
        float right = left + cutoutWidthPx;
        float bottom = top + cutoutHeightPx;
        cutoutRect.set(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        // 1. Draw darkened scrim over whole view
        canvas.drawRect(0, 0, width, height, scrimPaint);

        // 2. Cut out center rectangle (300dp x 80dp)
        float cornerRadius = dpToPx(12);
        canvas.drawRoundRect(cutoutRect, cornerRadius, cornerRadius, clearPaint);

        // 3. Draw reticle border
        canvas.drawRoundRect(cutoutRect, cornerRadius, cornerRadius, borderPaint);

        // 4. Draw helper instruction text below box
        String helperText = "Align expiry date inside box";
        float textY = cutoutRect.bottom + dpToPx(32);
        canvas.drawText(helperText, width / 2f, textY, textPaint);
    }

    public RectF getCutoutRect() {
        return new RectF(cutoutRect);
    }

    public RectF getNormalizedCutoutRect() {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return new RectF(0.1f, 0.4f, 0.9f, 0.6f);
        }
        return new RectF(
                cutoutRect.left / (float) width,
                cutoutRect.top / (float) height,
                cutoutRect.right / (float) width,
                cutoutRect.bottom / (float) height
        );
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
