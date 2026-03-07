package com.ost.application.ui.activity.animation; // ПАКЕТ

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.io.IOException;
import java.io.InputStream;

public class RadialGradientView extends View {

    private static final String TAG = "[RadialGradientView]";

    // Вложенные Enum'ы
    public enum AnimationState { NONE, START, REPEAT, END }
    public enum AlignType { LTR, RTL }

    float baseScaleHeight;
    float baseScaleWidth;

    float gradient1_AlphaValue;
    float gradient1_ScaleValue;
    float gradient1_alpha;
    float gradient1_x;
    float gradient1_y;

    float gradient2_alpha;
    float gradient2_x;
    float gradient2_y;

    float gradient3_AlphaValue;
    float gradient3_ScaleValue;
    float gradient3_alpha;
    float gradient3_x;
    float gradient3_y;

    AlignType mAlignType;
    AnimationState mAnimationState;

    Bitmap mBitmapGradientPatternWhite;
    int mColor1;
    int mColor2;
    int mColor3;

    ColorFilter mColorFilter1;
    ColorFilter mColorFilter2;
    ColorFilter mColorFilter3;

    boolean mFlagInit;
    boolean mFlagShowArc;
    int mGradientRadial; // Было 0, теперь будет установлено
    float mGradientScale; // Было 0.0f, теперь будет установлено

    Paint mPaint;
    Paint mPaintArc;

    ValueAnimator mRepeatAnimator;
    int mRepeatDuration;
    int mStartDuration;
    ValueAnimator mStartGradient1_Animator;
    int mStartGradient1_Delay;
    ValueAnimator mStartGradient3_Animator;

    int mViewHeight;
    int mViewWidth;

    float scale1;
    float scale2;
    float scale3;
    public boolean isAnimating = false;
    // Конструкторы
    public RadialGradientView(Context context) {
        super(context);
        Log.d(TAG, "Constructor called");
        initDefaultValues();
    }

    public RadialGradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "Constructor (Attrs) called");
        initDefaultValues();
    }

    public RadialGradientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "Constructor (Attrs, Style) called");
        initDefaultValues();
    }

    // Инициализация значений по умолчанию (вызывается из конструкторов)
    private void initDefaultValues() {
        mAlignType = AlignType.LTR;
        mFlagShowArc = false;
        mFlagInit = false;

        mColor1 = 0xFFFF0000; // Красный
        mColor2 = 0xFF00FF00; // Зеленый
        mColor3 = 0xFFFFFF00; // Желтый

        mBitmapGradientPatternWhite = null;
        mColorFilter1 = null;
        mColorFilter2 = null;
        mColorFilter3 = null;

        mStartDuration = 7000; // 2 секунды
        mStartGradient1_Delay = 5000; // 0.4 секунды
        mRepeatDuration = 5000; // 4 секунды

        baseScaleWidth = 0.0f;
        baseScaleHeight = 0.0f;

        gradient1_alpha = 0.8f;
        gradient2_alpha = 1.0f;
        gradient3_alpha = 0.9f;

        scale1 = 1.3f;
        scale2 = 1.8f;
        scale3 = 2.0f;

        gradient1_x = 0.25f;
        gradient1_y = 0.43f;
        gradient2_x = 1.2f;
        gradient2_y = -0.09f;
        gradient3_x = 0.45f;
        gradient3_y = 0.43f;

        gradient1_AlphaValue = 0.0f;
        gradient1_ScaleValue = 0.0f;
        gradient3_AlphaValue = 0.0f;
        gradient3_ScaleValue = 0.0f;

        // *** ФИКС: Инициализация mGradientRadial и mGradientScale здесь! ***
        mGradientRadial = 2048;
        mGradientScale = 1.0f;
        Log.d(TAG, "initDefaultValues(): mGradientRadial set to " + mGradientRadial + ", mGradientScale set to " + mGradientScale);
    }

    // Основной метод инициализации View
    public void init(AlignType type, int gradientPatternResId) {
        Log.d(TAG, "init() called with type: " + type.name() + ", resource ID: " + gradientPatternResId);
        if (mBitmapGradientPatternWhite != null && !mBitmapGradientPatternWhite.isRecycled()) {
            Log.d(TAG, "Recycling existing bitmap.");
            mBitmapGradientPatternWhite.recycle();
            mBitmapGradientPatternWhite = null;
        }

        mAlignType = type;

        if (type == AlignType.LTR) {
            gradient1_x = 0.25f; gradient1_y = 0.43f;
            gradient2_x = 1.2f;  gradient2_y = -0.09f;
            gradient3_x = 0.45f; gradient3_y = 0.43f;
        } else if (type == AlignType.RTL) {
            gradient1_x = 0.75f; gradient1_y = 0.43f;
            gradient2_x = -0.20000005f; gradient2_y = -0.09f;
            gradient3_x = 0.55f; gradient3_y = 0.43f;
        }

        // *** УДАЛЕНЫ СТРОКИ mGradientRadial и mGradientScale отсюда, так как они в initDefaultValues() ***

        InputStream inputStream = null;
        Bitmap gradientBitmap = null;
        try {
            inputStream = getContext().getResources().openRawResource(gradientPatternResId);
            if (inputStream != null) {
                Log.d(TAG, "Raw resource opened successfully.");
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inScaled = false;
                gradientBitmap = BitmapFactory.decodeStream(inputStream, null, o);
            } else {
                Log.e(TAG, "Raw resource returned null stream for ID: " + gradientPatternResId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening raw resource with ID " + gradientPatternResId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (gradientBitmap != null) {
            Log.d(TAG, "gradientBitmap decoded. Dimensions: " + gradientBitmap.getWidth() + "x" + gradientBitmap.getHeight());
            int width = gradientBitmap.getWidth();
            int height = gradientBitmap.getHeight();
            mBitmapGradientPatternWhite = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);

            int[] pixels = new int[width];
            for (int y = 0; y < height; y++) {
                gradientBitmap.getPixels(pixels, 0, width, 0, y, width, 1);
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = (pixels[i] & 0xFF) << 24 | 0x00FFFFFF;
                }
                mBitmapGradientPatternWhite.setPixels(pixels, 0, width, 0, y, width, 1);
            }
            gradientBitmap.recycle();
        } else {
            Log.e(TAG, "gradientBitmap is NULL after decodeStream. Check raw resource file.");
            mBitmapGradientPatternWhite = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8); // Заглушка
        }

        mPaint = new Paint();
        mPaintArc = new Paint();
        mPaintArc.setStrokeWidth(8.0f);
        mPaintArc.setStyle(Paint.Style.STROKE);

        mAnimationState = AnimationState.NONE;

        // Настройка аниматоров
        mStartGradient1_Animator = new ValueAnimator();
        mStartGradient1_Animator.setInterpolator(new LinearInterpolator());
        mStartGradient1_Animator.setRepeatCount(0);
        mStartGradient1_Animator.setFloatValues(0.0f, 1.0f);
        mStartGradient1_Animator.setDuration(mStartDuration);
        mStartGradient1_Animator.setStartDelay(mStartGradient1_Delay);
        mStartGradient1_Animator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) { Log.d(TAG, "StartGradient1_Animator: Animation Start"); }
            @Override public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "StartGradient1_Animator: Animation End. Current state: " + mAnimationState);
                if (mAnimationState == AnimationState.START) {
                    mAnimationState = AnimationState.REPEAT;
                    mRepeatAnimator.start();
                    Log.d(TAG, "StartGradient1_Animator: Transition to REPEAT state, starting repeat animator.");
                }
            }
            @Override public void onAnimationCancel(Animator animation) { Log.d(TAG, "StartGradient1_Animator: Animation Cancel"); }
            @Override public void onAnimationRepeat(Animator animation) { Log.d(TAG, "StartGradient1_Animator: Animation Repeat"); }
        });
        mStartGradient1_Animator.addUpdateListener(animation -> invalidate());

        mStartGradient3_Animator = new ValueAnimator();
        mStartGradient3_Animator.setInterpolator(new LinearInterpolator());
        mStartGradient3_Animator.setRepeatCount(0);
        mStartGradient3_Animator.setFloatValues(0.0f, 1.0f);
        mStartGradient3_Animator.setDuration(mStartDuration);
        mStartGradient3_Animator.addUpdateListener(animation -> invalidate()); // Обновление для отрисовки

        mRepeatAnimator = new ValueAnimator();
        mRepeatAnimator.setInterpolator(new LinearInterpolator());
        mRepeatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mRepeatAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mRepeatAnimator.setFloatValues(1.0f, 0.0f, 1.0f); // Пульсация от 1 до 0 и обратно
        mRepeatAnimator.setDuration(mRepeatDuration);
        mRepeatAnimator.addUpdateListener(animation -> invalidate()); // Обновление для отрисовки

        setColors(mColor1, mColor2, mColor3); // Установка начальных ColorFilter'ов

        mFlagInit = true;
        Log.d(TAG, "init() finished. mFlagInit: " + mFlagInit);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged() called.");
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw() called. mFlagInit=" + mFlagInit + ", mAnimationState=" + mAnimationState);

        if (!mFlagInit) {
            Log.d(TAG, "View not initialized. Skipping draw.");
            return;
        }
        if (mBitmapGradientPatternWhite == null || mBitmapGradientPatternWhite.isRecycled()) {
            Log.e(TAG, "mBitmapGradientPatternWhite is null or recycled. Cannot draw.");
            return;
        }

        Log.d(TAG, "View dimensions: Width=" + mViewWidth + ", Height=" + mViewHeight);
        Log.d(TAG, "mGradientRadial=" + mGradientRadial + ", mGradientScale=" + mGradientScale);
        // *** ФИКС: Теперь mGradientRadial не 0, деления на NaN не будет ***
        baseScaleWidth = (float) mViewWidth / (mGradientRadial + mGradientRadial) * mGradientScale;
        baseScaleHeight = baseScaleWidth; // Высота масштабируется так же
        Log.d(TAG, "Calculated baseScaleWidth=" + baseScaleWidth);

        // Логика анимации для обновления значений альфа и масштаба
        if (mAnimationState == AnimationState.START) {
            float animatedValue1 = (Float) mStartGradient1_Animator.getAnimatedValue();
            gradient1_AlphaValue = EasingSineFunc.getInstance().easeInOut(animatedValue1, 0.0f, 1.0f, 1.0f);
            float animatedValue3 = (Float) mStartGradient3_Animator.getAnimatedValue();
            gradient3_AlphaValue = EasingSineFunc.getInstance().easeInOut(animatedValue3, 0.0f, 1.0f, 1.0f);

            gradient1_ScaleValue = (float)(0.5 * scale1 * baseScaleWidth + (scale1 * baseScaleWidth * gradient1_AlphaValue * 0.7));
            gradient3_ScaleValue = (float)(0.5 * scale3 * baseScaleWidth + (scale3 * baseScaleWidth * gradient3_AlphaValue * 0.7));

        } else if (mAnimationState == AnimationState.REPEAT) {
            float animatedValue = (Float) mRepeatAnimator.getAnimatedValue();
            gradient1_AlphaValue = EasingSineFunc.getInstance().easeInOut(animatedValue, 0.0f, 1.0f, 1.0f);
            gradient3_AlphaValue = EasingSineFunc.getInstance().easeInOut(animatedValue, 0.0f, 1.0f, 1.0f);

            // TODO: Проверить точность коэффициентов (0.85 и 0.35) по оригинальному Smali, если анимация не идеальна
            gradient1_ScaleValue = (float)(0.85 * scale1 * baseScaleWidth + (scale1 * baseScaleWidth * gradient1_AlphaValue * 0.35));
            gradient3_ScaleValue = (float)(0.85 * scale3 * baseScaleWidth + (scale3 * baseScaleWidth * gradient3_AlphaValue * 0.35));
        }
        // В состоянии NONE/END значения alphaValue и scaleValue могут быть просто базовыми.
        // onDraw вызывается часто, поэтому важно, чтобы значения были консистентны.
        // Сейчас они сохраняют последнее анимированное значение или дефолтные 0.0f.

        Log.d(TAG, "Current animation values: G1_Alpha=" + gradient1_AlphaValue + ", G1_Scale=" + gradient1_ScaleValue +
                ", G3_Alpha=" + gradient3_AlphaValue + ", G3_Scale=" + gradient3_ScaleValue);


        final float FULL_ALPHA_FLOAT = 255.0f;

        // Рисование Градиента 3 (нижний слой)
        mPaint.setColorFilter(mColorFilter3);
        int alpha3;
        if (mAnimationState == AnimationState.START) {
            alpha3 = (int) (gradient3_alpha * gradient3_AlphaValue * FULL_ALPHA_FLOAT);
        } else if (mAnimationState == AnimationState.REPEAT) {
            alpha3 = (int) (gradient3_alpha * FULL_ALPHA_FLOAT); // В цикле альфа градиента 3 фиксирована на gradient3_alpha
        } else { // NONE or END
            alpha3 = (int) (gradient3_alpha * FULL_ALPHA_FLOAT);
        }
        mPaint.setAlpha(alpha3);
        Log.d(TAG, "G3 drawing alpha: " + alpha3);

        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.translate(mViewWidth * gradient3_x, mViewWidth * gradient3_y);
            canvas.scale(gradient3_ScaleValue, gradient3_ScaleValue, 0.0f, 0.0f);
            canvas.rotate(i * 90.0f, 0.0f, 0.0f);
            canvas.drawBitmap(mBitmapGradientPatternWhite, 0.0f, 0.0f, mPaint);

            if (mFlagShowArc) {
                mPaintArc.setColor(0xFFFFFF00); // Желтая дуга
                canvas.drawArc(-mGradientRadial, -mGradientRadial, mGradientRadial, mGradientRadial, 270.0f, 90.0f, false, mPaintArc);
            }
            canvas.restore();
        }

        // Рисование Градиента 2 (средний слой)
        mPaint.setColorFilter(mColorFilter2);
        int alpha2 = (int) (gradient2_alpha * FULL_ALPHA_FLOAT); // Альфа градиента 2 всегда фиксирована
        mPaint.setAlpha(alpha2);
        Log.d(TAG, "G2 drawing alpha: " + alpha2);

        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.translate(mViewWidth * gradient2_x, mViewWidth * gradient2_y);
            canvas.scale(scale2 * baseScaleWidth, scale2 * baseScaleWidth, 0.0f, 0.0f);
            canvas.rotate(i * 90.0f, 0.0f, 0.0f);
            canvas.drawBitmap(mBitmapGradientPatternWhite, 0.0f, 0.0f, mPaint);
            if (mFlagShowArc) {
                mPaintArc.setColor(0xFF888888); // Серая дуга
                canvas.drawArc(-mGradientRadial, -mGradientRadial, mGradientRadial, mGradientRadial, 270.0f, 90.0f, false, mPaintArc);
            }
            canvas.restore();
        }

        // Рисование Градиента 1 (верхний слой)
        mPaint.setColorFilter(mColorFilter1);
        int alpha1;
        if (mAnimationState == AnimationState.START) {
            alpha1 = (int) (gradient1_alpha * gradient1_AlphaValue * FULL_ALPHA_FLOAT);
        } else if (mAnimationState == AnimationState.REPEAT) {
            alpha1 = (int) (gradient1_alpha * FULL_ALPHA_FLOAT); // В цикле альфа градиента 1 фиксирована на gradient1_alpha
        } else { // NONE or END
            alpha1 = (int) (gradient1_alpha * FULL_ALPHA_FLOAT);
        }
        mPaint.setAlpha(alpha1);
        Log.d(TAG, "G1 drawing alpha: " + alpha1);

        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.translate(mViewWidth * gradient1_x, mViewWidth * gradient1_y);
            canvas.scale(gradient1_ScaleValue, gradient1_ScaleValue, 0.0f, 0.0f);
            canvas.rotate(i * 90.0f, 0.0f, 0.0f);
            canvas.drawBitmap(mBitmapGradientPatternWhite, 0.0f, 0.0f, mPaint);
            if (mFlagShowArc) {
                mPaintArc.setColor(0xFF00FF00); // Зеленая дуга
                canvas.drawArc(-mGradientRadial, -mGradientRadial, mGradientRadial, mGradientRadial, 270.0f, 90.0f, false, mPaintArc);
            }
            canvas.restore();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        Log.d(TAG, "onSizeChanged: Width=" + w + ", Height=" + h);
    }

    public void setAlphas(float iAlpha1, float iAlpha2, float iAlpha3) {
        gradient1_alpha = iAlpha1;
        gradient2_alpha = iAlpha2;
        gradient3_alpha = iAlpha3;
        invalidate();
    }

    public void setArcShow(boolean flag) {
        mFlagShowArc = flag;
        invalidate();
    }

    public void setColors(int iColor1, int iColor2, int iColor3) {
        if(mStartGradient3_Animator != null) {
            mStartGradient3_Animator.setCurrentPlayTime(0); // Сброс аниматора при смене цвета
        }

        mColor1 = iColor1 | 0xFF000000;
        mColor2 = iColor2 | 0xFF000000;
        mColor3 = iColor3 | 0xFF000000;

        mColorFilter1 = new PorterDuffColorFilter(mColor1, PorterDuff.Mode.SRC_IN);
        mColorFilter2 = new PorterDuffColorFilter(mColor2, PorterDuff.Mode.SRC_IN);
        mColorFilter3 = new PorterDuffColorFilter(mColor3, PorterDuff.Mode.SRC_IN);

        invalidate();
    }

    public void startAnimation() {
        Log.d(TAG, "startAnimation() called. Current state: " + mAnimationState);
        if (mAnimationState != AnimationState.START) {
            stopAnimation();
            isAnimating = true; // <-- ДОБАВЬ ЭТУ СТРОКУ
            mAnimationState = AnimationState.START;
            mStartGradient1_Animator.start();
            mStartGradient3_Animator.start();
            Log.d(TAG, "Starting START animation.");
        }
    }

    public void stopAnimation() {
        Log.d(TAG, "stopAnimation() called. Current state: " + mAnimationState);
        if (mAnimationState == AnimationState.START || mAnimationState == AnimationState.REPEAT) {
            isAnimating = false; // <-- ДОБАВЬ ЭТУ СТРОКУ
            mAnimationState = AnimationState.END;
            if(mStartGradient1_Animator != null) mStartGradient1_Animator.cancel();
            if(mStartGradient3_Animator != null) mStartGradient3_Animator.cancel(); // Если есть такой аниматор
            if(mRepeatAnimator != null) mRepeatAnimator.cancel();
            Log.d(TAG, "Animation cancelled. New state: " + mAnimationState);
        }
        invalidate();
    }

    // Метод для освобождения ресурсов (важно для Bitmap!)
    public void releaseResources() {
        Log.d(TAG, "releaseResources() called.");
        stopAnimation(); // Останавливаем анимации перед освобождением
        if (mBitmapGradientPatternWhite != null) {
            mBitmapGradientPatternWhite.recycle(); // Освобождаем память битмапа
            mBitmapGradientPatternWhite = null;
            Log.d(TAG, "Bitmap recycled.");
        }
        mColorFilter1 = null;
        mColorFilter2 = null;
        mColorFilter3 = null;
        mPaint = null;
        mPaintArc = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow() called.");
        releaseResources(); // Вызываем освобождение ресурсов при отсоединении View
    }
}