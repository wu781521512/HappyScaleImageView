package com.example.happyproject;


import android.content.Context;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

public class HappyScaleImageView extends android.support.v7.widget.AppCompatImageView implements ViewTreeObserver.OnGlobalLayoutListener, View.OnTouchListener {

    //为图片执行缩放的matrix
    private Matrix scaleMatrix;

    //原始的缩放比例matrix，用于双击缩小到原大小
    private Matrix originScaleMatrix;

    //标志，检测onGlobalLayout是不是第一次调用
    private boolean isOnce = true;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private static final int STATE_AMPLIFYING = 0; //放大的状态
    private static final int STATE_NORMAL = 1;  //常规的状态
    private int current_state = STATE_NORMAL;  //当前图片所处的状态
    private final int mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    private float hasScaleX;
    private float hasScaleY;
    private float maxScaleX;  //宽度最大放大倍数
    private float maxScaleY;  //长度最大放大倍数
    private float minScaleX;  //宽度最小缩放倍数
    private float minScaleY;  //长度最小缩放倍数


    float[] currentValues = new float[9];
    private float move_end_x;
    private float move_end_y;
    private float move_start_x;
    private float move_start_y;
    private boolean isScaleEnd = true;
    private boolean preMotionIsScale;
    private boolean isScaling;

    public HappyScaleImageView(Context context) {
        this(context, null);
    }

    public HappyScaleImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HappyScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化ImageView状态和设置手势监听
     */
    private void init() {
        //默认设置ImageView的ScaleType为matrix
        setScaleType(ScaleType.MATRIX);
        scaleMatrix = new Matrix();
        originScaleMatrix = new Matrix();

        handleDoubleTapEvent();
        handleScaleTapEvent();

        setOnTouchListener(this);
    }

    /**
     * 处理缩放事件
     */
    private void handleScaleTapEvent() {
        final float[] finalScaleX = new float[1];
        final float[] finalScaleY = new float[1];
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleMatrix.getValues(currentValues);
                float currentX = detector.getCurrentSpanX() - detector.getPreviousSpanX();
                float currentY = detector.getCurrentSpanY() - detector.getPreviousSpanY();
                //想再放大
                if (currentX >= 0) {
                    if (currentValues[0] * detector.getCurrentSpanX() / detector.getPreviousSpanX() > maxScaleX) {
                        finalScaleX[0] = maxScaleX / currentValues[0];
                    } else {
                        finalScaleX[0] = detector.getCurrentSpanX() / detector.getPreviousSpanX();
                    }
                } else {
                    //想缩小
                    if (currentValues[0] * detector.getCurrentSpanX() / detector.getPreviousSpanX() > minScaleX) {
                        finalScaleX[0] = detector.getCurrentSpanX() / detector.getPreviousSpanX();
                    } else {
                        finalScaleX[0] = minScaleX / currentValues[0];
                    }
                }


                //想再放大
                if (currentY >= 0) {
                    if (currentValues[4] * detector.getCurrentSpanY() / detector.getPreviousSpanY() > maxScaleY) {
                        finalScaleY[0] = maxScaleX / currentValues[4];
                    } else {
                        finalScaleY[0] = detector.getCurrentSpanY() / detector.getPreviousSpanY();
                    }
                } else {
                    //想缩小
                    if (currentValues[4] * detector.getCurrentSpanY() / detector.getPreviousSpanY() > minScaleY) {
                        finalScaleY[0] = detector.getCurrentSpanY() / detector.getPreviousSpanY();
                    } else {
                        finalScaleY[0] = minScaleX / currentValues[4];
                    }
                }

                scaleMatrix.postScale(finalScaleX[0], finalScaleY[0], detector.getFocusX(), detector.getFocusY());
                current_state = STATE_AMPLIFYING;
                setImageMatrix(scaleMatrix);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                super.onScaleEnd(detector);
                isScaling = false;
            }
        });
    }


    /**
     * 处理双击事件
     */
    private void handleDoubleTapEvent() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener());
        gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                switch (current_state) {
                    case STATE_NORMAL:
                        //正常状态，双击后放大

                        scaleMatrix.postScale(maxScaleX / currentValues[0], maxScaleY / currentValues[4], getWidth() / 2f, getHeight() / 2f);
                        setImageMatrix(scaleMatrix);
                        scaleMatrix.getValues(currentValues);
                        current_state = STATE_AMPLIFYING;
                        break;
                    case STATE_AMPLIFYING:
                        //放大状态，双击后缩小到常规状态
                        setImageMatrix(originScaleMatrix);
                        originScaleMatrix.getValues(currentValues);
                        current_state = STATE_NORMAL;
                        break;
                }
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        //在这里获取图片加载完后的宽高

        if (isOnce) {
            //第一次调用，获取控件大小(这个控件定位为match_parent情况下使用，也就是获取屏幕宽高)
            int originViewWidth = getWidth();
            int originViewHeight = getHeight();

            //获取图片大小

            int originBitmapWidth = getDrawable().getIntrinsicWidth();
            int originBitmapHeight = getDrawable().getIntrinsicHeight();

            //设置最大缩放倍数
            maxScaleX = originViewWidth * 2f / originBitmapWidth;
            maxScaleY = originViewHeight * 2f / originBitmapHeight;

            //最终缩放效果，宽度和控件相等，高度为控件的1/3
            //如果图片宽高都要大于控件大小
            float scaleWidth = originViewWidth * 1f / originBitmapWidth;
            float scaleHeight = originViewHeight / 3f / originBitmapHeight;


            minScaleX = scaleWidth;
            minScaleY = scaleHeight;

            scaleMatrix.preScale(scaleWidth, scaleHeight);
            scaleMatrix.postTranslate(0f, originViewHeight / 3f);

            scaleMatrix.getValues(currentValues);
            float[] values = new float[9];
            scaleMatrix.getValues(values);

            originScaleMatrix.setValues(values);
            setImageMatrix(scaleMatrix);
            isOnce = false;
        }
    }


    /**
     * 重写onTouchListener的onTouch事件，将事件转交给双击和缩放手势
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (gestureDetector.onTouchEvent(event))
            return true;
        scaleGestureDetector.onTouchEvent(event);


        int pointerCount = event.getPointerCount();

        if (isScaling && SystemClock.uptimeMillis() - scaleGestureDetector.getEventTime() > 100)
            return true;
        else if (pointerCount == 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    move_start_x = event.getX();
                    move_start_y = event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    move_end_x = event.getX();
                    move_end_y = event.getY();
                    if (Math.abs(move_end_x - move_start_x) > mTouchSlop || Math.abs(move_end_y - move_start_y) > mTouchSlop) {
                        scaleMatrix.postTranslate(move_end_x - move_start_x, move_end_y - move_start_y);
                        setImageMatrix(scaleMatrix);
                        move_start_x = move_end_x;
                        move_start_y = move_end_y;
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    move_start_x = 0f;
                    move_start_y = 0f;
                    return true;
            }
        }


        return true;
    }
}
