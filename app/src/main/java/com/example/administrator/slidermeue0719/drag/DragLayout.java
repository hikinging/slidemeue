package com.example.administrator.slidermeue0719.drag;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * @作者 海境
 * @E-Mail 812385828@qq.com
 * @创建时间 2016/7/19 9:55
 **/
public class DragLayout extends FrameLayout {

    private ViewDragHelper mDragHelper;
    private ViewGroup mLeftContent;
    private ViewGroup mMainContent;
    private int mHeight;
    private int mWidth;
    private int mRange;
    private OnDragStatusChangeListener mListener;
    private Status mStatus = Status.Close;

    /**
     * 状态枚举
     */
    public static enum Status{
        Close,Open,Draging;
    }

    public interface OnDragStatusChangeListener{
        void onClose();
        void onOpen();
        void onDraging(float percent);
    }

    public void setDragStausListener(OnDragStatusChangeListener mListener){
        this.mListener = mListener;
    }

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //a.初始化操作
        mDragHelper = ViewDragHelper.create(this, mCallback);
    }

    ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {
        //1. 根据返回结果决定当前child是否是否可以拖拽
        //child 当前被拖拽的View
        //pointer 区分多点触摸的id
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
//            Log.d("hhhh", "tryCaptureView" + child);
            //            return child == mMainContent;
            return true;
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            //当capturedChild被捕获时，调用
//            Log.d("hhhh", "onViewCaptuerd" + capturedChild);
            super.onViewCaptured(capturedChild, activePointerId);
        }


        @Override
        public int getViewHorizontalDragRange(View child) {
            //返回拖拽的范围，不对拖拽进行真正的限制,仅仅决定了动画的执行速度
            return mRange;
        }

        //2.根据建议值修正要移动到的(横向)位置
        //此时没有发生真正的移动
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            //child：当前拖拽的View
            //left: 新的位置的建议值,dx 位置变化量
//            Log.d("zzz", "clampViewPositionHorizontal:" + "old" + child.getLeft() + "dx:" + dx + "left" + left);
            if (child == mMainContent) {
                left = fixLeft(left);
            }
            return left;
        }

        //3.当view位置改变时，处理要做的事情(更新状态,伴随动画,重绘界面)
        //此时，View已经发生了位置的改变
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
//            Log.d("ooo", "left:" + left + "dx:" + dx);

            //left 新的左边值
            //dx 水平方向变化量
            int newLeft = left;
            if (changedView == mLeftContent) {
                //把当前变化量传递给mMainContent
                newLeft = mMainContent.getLeft() + dx;
            }
            //进行修正
            newLeft = fixLeft(newLeft);

            if (changedView == mLeftContent) {
                //当左面板移动之后，再强制放回去
                mLeftContent.layout(0, 0, 0 + mWidth, 0 + mHeight);
                mMainContent.layout(newLeft, 0, newLeft + mWidth, 0 + mHeight);
            }
            dispatchDragEvent(newLeft);
            //为了兼容低版本,每次修改之后,进行重绘
            invalidate();
        }


        /**
         * 4.当View被释放的时候,处理的事情(执行动画)
         * @param releasedChild 被释放的子View
         * @param xvel 水平方向的速度
         * @param yvel 竖直方向的速度
         */
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
//            Log.d("t", "x:" + xvel + " y:" + yvel);
            if (xvel == 0 && mMainContent.getLeft() > mRange / 2.0f) {
                open();
            } else if (xvel > 0) {
                open();
            } else {
                close();
            }
        }
    };

    private void dispatchDragEvent(int newLeft) {
        float percent = newLeft * 1.0f / mRange;
        if (mListener != null){
            mListener.onDraging(percent);
        }
        //更新状态,执行回调
        Status preStatus = mStatus;
        mStatus = updateStatus(percent);
        if (mStatus != preStatus){
            //状态发生变化
            if (mStatus == Status.Close) {
                //当前变为关闭状态
                if (mListener != null){
                    mListener.onClose();
                }
            }else if (mStatus == Status.Open){
                if (mListener != null){
                    mListener.onOpen();
                }
            }
        }
        // 伴随动画：
        animViews(percent);

    }

    private Status updateStatus(float percent) {
        if (percent == 0f){
            return Status.Close;
        }else if (percent == 1.0f){
            return Status.Open;
        }
        return Status.Draging;
    }

    private void animViews(float percent) {
        //  1.左面板：缩放动画，平移动画,透明度动画
        mLeftContent.setScaleX(0.5f + 0.5f * percent);
        mLeftContent.setScaleY(0.5f + 0.5f * percent);
        mLeftContent.setTranslationX(evaluate(percent, -mWidth / 2.0f, 0));
        mLeftContent.setAlpha(evaluate(percent, 0.5f, 1.0f));
        //2.主面板:缩放动画
        //1.0->0.8f
        mMainContent.setScaleX(evaluate(percent, 1.0f, 0.8f));
        mMainContent.setScaleY(evaluate(percent, 1.0f, 0.8f));
        //3.背景动画:亮度变化
        getBackground().setColorFilter((Integer)evaluateColor(percent, Color.BLACK,Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
    }

    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startValue.floatValue());
    }

    /**
     * 颜色变化过度
     * @param fraction
     * @param startValue
     * @param endValue
     * @return
     */
    public Object evaluateColor(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int) ((startA + (int) (fraction * (endA - startA))) << 24) |
                (int) ((startR + (int) (fraction * (endR - startR))) << 16) |
                (int) ((startG + (int) (fraction * (endG - startG))) << 8) |
                (int) ((startB + (int) (fraction * (endB - startB))));
    }

    //重构代码
    public void close() {
        close(true);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        //2.持续平滑动画(高频调用)
        if (mDragHelper.continueSettling(true)) {
            //如果返回true,动画还需要继续执行
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    //关闭
    private void close(boolean isSmooth) {
        int finalLeft = 0;
        if (isSmooth) {
            //1.触发一个平滑动画
            if (mDragHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)) {
                //返回true代表还没有移动到指定位置,需要刷新界面
                //参数传this(child所在的ViewGroup)
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }

    public void open() {
        open(true);
    }

    //开启
    private void open(boolean isSmooth) {
        int finalLeft = mRange;
        if (isSmooth) {
            //1.触发一个平滑动画
            if (mDragHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)) {
                //返回true代表还没有移动到指定位置,需要刷新界面
                //参数传this(child所在的ViewGroup)
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }

    /**
     * 根据范围修正左边值
     *
     * @param left
     * @return
     */
    private int fixLeft(int left) {
        if (left < 0) {
            return 0;
        } else if (left > mRange) {
            return mRange;
        }
        return left;
    }

    //b.传递触摸事件
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //传递给mDragHelper
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mDragHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回true,持续接收事件
        return true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //容错性检查(至少有2个孩子,子View必须是ViewGroup的子类)
        if (getChildCount() < 2) {
            //抛出非法状态异常
            throw new IllegalStateException("布局至少有2个孩子");
        }
        if (!(getChildAt(0) instanceof ViewGroup && getChildAt(1) instanceof ViewGroup)) {
            //非法参数异常
            throw new IllegalArgumentException("子View必须是ViewGroup的子类");
        }
        mLeftContent = (ViewGroup) getChildAt(0);
        mMainContent = (ViewGroup) getChildAt(1);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //当尺寸有变化的时候调用
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();

        mRange = (int) (mWidth * 0.6f);
    }

}
























