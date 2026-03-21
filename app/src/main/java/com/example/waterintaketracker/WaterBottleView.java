package com.example.waterintaketracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class WaterBottleView extends View {
    private static final float CAP_HEIGHT_RATIO = 0.08f;
    private static final float NECK_HEIGHT_RATIO = 0.15f;
    private static final float NECK_WIDTH_RATIO = 0.3f;
    private static final int MAX_PARTICLES = 30;

    private final RectF bottleBody = new RectF();
    private final Path bottlePath = new Path();
    private final Path wavePath = new Path();
    private final Path neckPath = new Path();
    private final Path capPath = new Path();
    private final Path highlightPath = new Path();

    private Paint bottlePaint;
    private Paint waterPaint;
    private Paint wavePaint;
    private Paint capPaint;
    private Paint highlightPaint;
    private Paint dropletPaint;

    private float waterLevel = 0.0f;
    private float targetWaterLevel = 0.0f;
    private float waveOffset = 0.0f;
    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private boolean particlesActive = false;

    private ValueAnimator waveAnimator;
    private ValueAnimator waterAnimator;
    private ValueAnimator overflowAnimator;

    private static class Particle {
        boolean active = false;
        float x, y, vx, vy, size;
        int alpha;

        void reset(float startX, float startY) {
            this.x = startX;
            this.y = startY;
            this.vx = (float) ((Math.random() * 20.0) - 10.0);
            this.vy = (float) ((Math.random() * -15.0) - 5.0);
            this.size = (float) ((Math.random() * 10.0) + 5.0);
            this.alpha = (int) (Math.random() * 55.0) + 200;
            this.active = true;
        }

        void update() {
            this.x += this.vx;
            this.y += this.vy;
            this.vy += 0.5f;
            this.alpha -= 8;
            if (this.alpha <= 0 || this.y > 2000.0f) {
                this.active = false;
            }
        }
    }

    public WaterBottleView(Context context) {
        this(context, null);
    }

    public WaterBottleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaterBottleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.bottlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.bottlePaint.setStyle(Paint.Style.STROKE);
        this.bottlePaint.setStrokeWidth(8.0f);
        this.bottlePaint.setColor(Color.parseColor("#94A3B8"));

        this.waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.waterPaint.setStyle(Paint.Style.FILL);

        this.wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.wavePaint.setStyle(Paint.Style.STROKE);
        this.wavePaint.setStrokeWidth(4.0f);
        this.wavePaint.setColor(Color.WHITE);
        this.wavePaint.setAlpha(200);

        this.capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.capPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.capPaint.setColor(Color.parseColor("#64748B"));

        this.highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.highlightPaint.setStyle(Paint.Style.STROKE);
        this.highlightPaint.setStrokeWidth(28.0f);
        this.highlightPaint.setColor(Color.WHITE);
        this.highlightPaint.setAlpha(90);
        this.highlightPaint.setStrokeCap(Paint.Cap.ROUND);

        this.dropletPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.dropletPaint.setColor(Color.parseColor("#7DD3FC"));

        for (int i = 0; i < MAX_PARTICLES; i++) {
            this.particles[i] = new Particle();
        }

        if (!isInEditMode()) {
            this.waveAnimator = ValueAnimator.ofFloat(0.0f, (float) (2 * Math.PI));
            this.waveAnimator.setDuration(2000L);
            this.waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
            this.waveAnimator.addUpdateListener(animation -> {
                this.waveOffset = (float) animation.getAnimatedValue();
                invalidate();
            });

            this.overflowAnimator = ValueAnimator.ofFloat(0.0f, 1.0f, 0.0f);
            this.overflowAnimator.setDuration(1000L);
            this.overflowAnimator.addUpdateListener(animation -> invalidate());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;

        float availableWidth = Math.max(0, (w - getPaddingLeft() - getPaddingRight()));
        float availableHeight = Math.max(0, (h - getPaddingTop() - getPaddingBottom() - 40));
        
        float bottleHeight = availableHeight * 0.85f;
        float bottleWidth = bottleHeight * 0.6f;
        
        float bottleLeft = getPaddingLeft() + (availableWidth - bottleWidth) / 2.0f;
        float bottleBottom = getPaddingTop() + availableHeight;
        float bottleTop = bottleBottom - bottleHeight;
        
        this.bottleBody.set(bottleLeft, bottleTop, bottleLeft + bottleWidth, bottleBottom);
        
        this.bottlePath.reset();
        this.bottlePath.addRoundRect(this.bottleBody, 50.0f, 50.0f, Path.Direction.CW);
        
        this.waterPaint.setShader(new LinearGradient(0, bottleBottom, 0, bottleTop, 
                Color.parseColor("#38BDF8"), Color.parseColor("#0284C7"), Shader.TileMode.CLAMP));

        float neckWidth = bottleWidth * NECK_WIDTH_RATIO;
        float neckLeft = this.bottleBody.centerX() - neckWidth / 2.0f;
        float capBottom = this.bottleBody.top - (bottleHeight * NECK_HEIGHT_RATIO);
        
        this.neckPath.reset();
        this.neckPath.addRoundRect(neckLeft, capBottom, neckLeft + neckWidth, this.bottleBody.top, 15.0f, 15.0f, Path.Direction.CW);
        
        float capWidth = neckWidth * 1.3f;
        float capLeft = this.bottleBody.centerX() - capWidth / 2.0f;
        this.capPath.reset();
        this.capPath.addRoundRect(capLeft, capBottom - (bottleHeight * CAP_HEIGHT_RATIO), capLeft + capWidth, capBottom, 10.0f, 10.0f, Path.Direction.CW);

        this.highlightPath.reset();
        this.highlightPath.moveTo(this.bottleBody.left + 26.0f, this.bottleBody.top + 46.0f);
        this.highlightPath.lineTo(this.bottleBody.left + 26.0f, this.bottleBody.bottom - 45.0f);
    }

    public void setWaterLevel(float level, boolean animate) {
        boolean overflow = this.targetWaterLevel < 1.0f && level >= 1.0f;
        this.targetWaterLevel = Math.max(0.0f, Math.min(1.0f, level));

        if (overflow && !isInEditMode()) {
            triggerOverflowEffect();
        }

        if (animate && !isInEditMode()) {
            if (this.waterAnimator != null) this.waterAnimator.cancel();
            this.waterAnimator = ValueAnimator.ofFloat(this.waterLevel, this.targetWaterLevel);
            this.waterAnimator.setDuration(800L);
            this.waterAnimator.setInterpolator(new DecelerateInterpolator());
            this.waterAnimator.addUpdateListener(animation -> {
                this.waterLevel = (float) animation.getAnimatedValue();
                invalidate();
            });
            this.waterAnimator.start();
            if (this.waveAnimator != null && !this.waveAnimator.isRunning()) {
                this.waveAnimator.start();
            }
        } else {
            this.waterLevel = this.targetWaterLevel;
            invalidate();
        }
    }

    private void triggerOverflowEffect() {
        if (this.overflowAnimator != null) {
            if (this.overflowAnimator.isRunning()) this.overflowAnimator.cancel();
            this.overflowAnimator.start();
        }
        this.particlesActive = true;
        for (Particle p : this.particles) {
            p.reset(this.bottleBody.centerX(), this.bottleBody.top - 50.0f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.bottleBody.isEmpty()) return;

        if (this.waterLevel > 0.0f) {
            canvas.save();
            canvas.clipPath(this.bottlePath);
            float waterTop = this.bottleBody.bottom - (this.bottleBody.height() * this.waterLevel);
            canvas.drawRect(this.bottleBody.left, waterTop, this.bottleBody.right, this.bottleBody.bottom, this.waterPaint);

            if (this.waterLevel > 0.05f && this.waterLevel < 0.95f && this.waveAnimator != null && this.waveAnimator.isRunning()) {
                this.wavePath.reset();
                this.wavePath.moveTo(this.bottleBody.left, waterTop);
                for (float x = this.bottleBody.left; x <= this.bottleBody.right; x += 10.0f) {
                    float y = (float) (Math.sin(x * 0.05 + this.waveOffset) * 8.0) + waterTop;
                    this.wavePath.lineTo(x, y);
                }
                canvas.drawPath(this.wavePath, this.wavePaint);
            }
            canvas.restore();
        }

        canvas.drawPath(this.bottlePath, this.bottlePaint);
        canvas.drawPath(this.neckPath, this.bottlePaint);
        
        canvas.save();
        canvas.clipPath(this.bottlePath);
        canvas.drawPath(this.highlightPath, this.highlightPaint);
        canvas.restore();
        
        canvas.drawPath(this.capPath, this.capPaint);
        canvas.drawPath(this.capPath, this.bottlePaint);

        if (this.particlesActive) {
            boolean anyActive = false;
            for (Particle p : this.particles) {
                if (p.active) {
                    p.update();
                    this.dropletPaint.setAlpha(p.alpha);
                    canvas.drawCircle(p.x, p.y, p.size, this.dropletPaint);
                    anyActive = true;
                }
            }
            this.particlesActive = anyActive;
            if (this.particlesActive) invalidate();
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
