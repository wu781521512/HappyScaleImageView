package com.example.happyproject;


import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;

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
    private int mTouchSlop;
    private float maxScaleX;  //宽度最大放大倍数
    private float maxScaleY;  //长度最大放大倍数
    private float minScaleX;  //宽度最小缩放倍数
    private float minScaleY;  //长度最小缩放倍数
    private RectF originBounds = new RectF();
    private RectF realBounds = new RectF();

    private float[] currentValues = new float[9];
    private float move_end_x;
    private float move_end_y;
    private float move_start_x;
    private float move_start_y;
    private int mLastPointerCount;
    private boolean isCanDrag = true;
    private float[] originValue = new float[9];

    public HappyScaleImageView(Context context) {
        this(context, null);
    }

    public HappyScaleImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HappyScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化ImageView状态和设置手势监听
     */
    private void init(Context context) {
        //默认设置ImageView的ScaleType为matrix
        setScaleType(ScaleType.MATRIX);
        scaleMatrix = new Matrix();
        originScaleMatrix = new Matrix();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
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

                scaleMatrix.mapRect(realBounds,originBounds);
                checkBoundsIsOutside();
                current_state = STATE_AMPLIFYING;
                setImageMatrix(scaleMatrix);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                super.onScaleEnd(detector);
            }
        });
    }


    /**
     * 处理双击事件
     */
    private void handleDoubleTapEvent() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
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
                        realBounds.set(originBounds);
                        originScaleMatrix.getValues(currentValues);
                        current_state = STATE_NORMAL;
                        break;
                }
                return true;
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
            originBounds.set(0, 0, originBitmapWidth, originBitmapHeight);

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
            //获取matrix转换后的图片边界到realBounds中
            scaleMatrix.getValues(currentValues);
            float[] values = new float[9];
            scaleMatrix.getValues(values);
            originScaleMatrix.getValues(originValue);
            scaleMatrix.mapRect(realBounds, originBounds);
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

        float averageX = 0;
        float averageY = 0;
        int pointerCount = event.getPointerCount();
        for (int j = 0; j < pointerCount; j++) {
            averageX += event.getX(j);
            averageY += event.getY(j);
        }

        averageX /= pointerCount;
        averageY /= pointerCount;
        if (mLastPointerCount != pointerCount) {
            isCanDrag = false;
            move_start_x = averageX;
            move_start_y = averageY;
        }

        mLastPointerCount = pointerCount;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                move_end_x = averageX;
                move_end_y = averageY;
                float dx = move_end_x - move_start_x;
                float dy = move_end_y - move_start_y;

                if (!isCanDrag) {
                    isCanDrag = Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
                }
                if (isCanDrag) {
                    scaleMatrix.postTranslate(dx, dy);
                    checkBoundsIsOutside();
                    setImageMatrix(scaleMatrix);
                }
                move_start_x = move_end_x;
                move_start_y = move_end_y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount = 0;
                break;
        }
        return true;
    }

    /**
     * 检测移动后位置是否会让左右出现白边
     */
    private void checkBoundsIsOutside() {
        if (realBounds.left > 0) {
            //左边移动到了边界内，会出现白边，移回去
            scaleMatrix.postTranslate(-realBounds.left,0);
        }

        if (realBounds.right < getWidth()) {
            //右边移动到了边界内，移回去
            scaleMatrix.postTranslate(getWidth()-realBounds.right,0);
        }

        if (realBounds.top > getHeight()/3) {
            //上边移动到了原始位置下方
            scaleMatrix.postTranslate(0,-(realBounds.top-getHeight()/3));
        }

        if (realBounds.bottom < getHeight()/3*2) {
            scaleMatrix.postTranslate(0,getHeight()/3*2 - realBounds.bottom);
        }

        scaleMatrix.mapRect(realBounds,originBounds);
    }
}
