package com.campsruntool.yh.campusrun;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class CampusRunGlassBackgroundView extends View {

    private final Paint paintBlob1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBlob2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBlob3 = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float blob1X, blob1Y;
    private float blob2X, blob2Y;
    private float blob3X, blob3Y;

    private ValueAnimator animator;
    private float animProgress;

    public CampusRunGlassBackgroundView(Context context) {
        super(context);
        init();
    }

    public CampusRunGlassBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CampusRunGlassBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        startAnimation(w, h);
    }

    private void startAnimation(int w, int h) {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(12000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            animProgress = (float) animation.getAnimatedValue();
            updateBlobPositions(w, h, animProgress);
            invalidate();
        });
        animator.start();

        updateBlobPositions(w, h, 0f);
    }

    private void updateBlobPositions(int w, int h, float t) {
        float r1 = Math.min(w, h) * 0.35f;
        float r2 = Math.min(w, h) * 0.30f;
        float r3 = Math.min(w, h) * 0.28f;

        blob1X = w * 0.25f + (float) (Math.sin(t * Math.PI * 2) * w * 0.15f);
        blob1Y = h * 0.20f + (float) (Math.cos(t * Math.PI * 2) * h * 0.10f);

        blob2X = w * 0.75f + (float) (Math.cos(t * Math.PI * 2 + 1.5f) * w * 0.12f);
        blob2Y = h * 0.45f + (float) (Math.sin(t * Math.PI * 2 + 1.5f) * h * 0.15f);

        blob3X = w * 0.40f + (float) (Math.sin(t * Math.PI * 2 + 3.0f) * w * 0.18f);
        blob3Y = h * 0.75f + (float) (Math.cos(t * Math.PI * 2 + 3.0f) * h * 0.10f);

        paintBlob1.setShader(new RadialGradient(blob1X, blob1Y, r1,
                0x4060A5FA, 0x0060A5FA, Shader.TileMode.CLAMP));
        paintBlob2.setShader(new RadialGradient(blob2X, blob2Y, r2,
                0x35A855F5, 0x00A855F5, Shader.TileMode.CLAMP));
        paintBlob3.setShader(new RadialGradient(blob3X, blob3Y, r3,
                0x30EC4899, 0x00EC4899, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(blob1X, blob1Y,
                Math.min(getWidth(), getHeight()) * 0.35f, paintBlob1);
        canvas.drawCircle(blob2X, blob2Y,
                Math.min(getWidth(), getHeight()) * 0.30f, paintBlob2);
        canvas.drawCircle(blob3X, blob3Y,
                Math.min(getWidth(), getHeight()) * 0.28f, paintBlob3);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }
}
