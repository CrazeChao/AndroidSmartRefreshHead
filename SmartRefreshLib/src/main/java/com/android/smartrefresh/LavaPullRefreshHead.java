package com.android.smartrefresh;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.constant.RefreshState;
import com.scwang.smart.refresh.layout.constant.SpinnerStyle;
import com.scwang.smart.refresh.layout.simple.SimpleComponent;
import com.scwang.smart.refresh.layout.util.SmartUtil;

import java.util.LinkedList;
import java.util.Queue;

import static com.scwang.smart.refresh.layout.constant.RefreshState.RefreshReleased;
import static com.scwang.smart.refresh.layout.constant.RefreshState.Refreshing;

/**
 *
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class LavaPullRefreshHead extends SimpleComponent implements RefreshHeader {
    protected boolean mManualNormalColor;
    protected boolean mManualAnimationColor;
    protected Paint mPaint;
    protected int mNormalColor = 0xffeeeeee;
    protected int mAnimatingColor = 0xffe75946;
    protected float mCircleSpacing;
    protected long mStartTime = 0;
    protected boolean mIsStarted = false;
    protected long mLastDragTime;
    protected TimeInterpolator mInterpolator = new AccelerateDecelerateInterpolator();
    //</editor-fold>
    //<editor-fold desc="构造方法">
    public LavaPullRefreshHead(Context context) {
        this(context, null);
    }

    public LavaPullRefreshHead(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        final View thisView = this;
        thisView.setMinimumHeight(SmartUtil.dp2px(60));
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BallPulseFooter);
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mSpinnerStyle = SpinnerStyle.Translate;
        mSpinnerStyle = SpinnerStyle.values[ta.getInt(R.styleable.BallPulseFooter_srlClassicsSpinnerStyle, mSpinnerStyle.ordinal)];

        if (ta.hasValue(R.styleable.BallPulseFooter_srlNormalColor)) {
            setNormalColor(ta.getColor(R.styleable.BallPulseFooter_srlNormalColor, 0));
        }
        if (ta.hasValue(R.styleable.BallPulseFooter_srlAnimatingColor)) {
            setAnimatingColor(ta.getColor(R.styleable.BallPulseFooter_srlAnimatingColor, 0));
        }
        if (ta.hasValue(R.styleable.BallPulseFooter_srlAnimatingBottomPadding)){
           mPaddingBottom =  ta.getDimensionPixelSize(R.styleable.BallPulseFooter_srlAnimatingBottomPadding,0);
        }
        if (ta.hasValue(R.styleable.BallPulseFooter_srlAnimatingTopPadding)){
           mPaddingTop = ta.getDimensionPixelSize(R.styleable.BallPulseFooter_srlAnimatingTopPadding,0);
        }
        if (ta.hasValue(R.styleable.BallPulseFooter_srlAnimatingHeight)){
            int animHeight = ta.getDimensionPixelSize(R.styleable.BallPulseFooter_srlAnimatingHeight,0);
            viewHeight = (int)(animHeight+mPaddingTop+mPaddingBottom);
        }
        ta.recycle();
        mCircleSpacing = SmartUtil.dp2px(4);
    }
    private int viewHeight;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getLayoutParams().height = viewHeight;
    }

    /**
     * 点的数量
     * */
    int mNumberOfPoints = 3;//3
    int mPointInterval = 38;//38
    int mMaxExtrude = 10; //10
    long repeatTime = 600;//600
    /**
     * 暴露参数
     *
     * 动画 顶部padding
     * */
    float mPaddingTop;

    /**
     * 暴露参数
     * 动画 底部padding
     * */
    float mPaddingBottom;

    float mCircleRadius = 10;

    private Queue<Ramen> remins = new LinkedList<>();
    /**
     * 轮训时间
     * */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        remins.clear();
        int width = mPointInterval*mNumberOfPoints;
        int left = (w-width)/2;
        for (int i = 0; i < mNumberOfPoints; i++) {
            remins.add(new Ramen((i*mPointInterval)+left+mCircleRadius,mCircleRadius,mPaddingTop+mCircleRadius,h-(mPaddingBottom+mCircleRadius),mMaxExtrude));
        }
    }

    protected  void compensationForMissingTime(){
        final long now = System.currentTimeMillis();
        long inval = 0;
        if (mLastDragTime != -1 &&(inval = (now - mLastDragTime)) > 50){
            mLastDragTime = -1;
            mStartTime = mStartTime + inval;
        }
    }



    @Override
    protected void dispatchDraw(Canvas canvas) {  //反复出发dispatchDraw 来
        final View thisView = this;
        final int width = thisView.getWidth();
        final int height = thisView.getHeight();
        final long now = System.currentTimeMillis();
       compensationForMissingTime();
        //时间补偿
        final long timeline = now - mStartTime;
        int i = 0;
        for(Ramen ramen:remins){
            float progress = getProgressRepeat(timeline-(i++)*100);
            final float progressRepeat = getRepeat(progress);
            ramen.setAnimationProgress(progressRepeat);
            ramen.draw(canvas,mPaint);
        }
        super.dispatchDraw(canvas);


        if (mIsStarted) {
            thisView.invalidate();
        }
    }

    private float getProgressRepeat(long timeline) {
        final long timeProgress = timeline % (repeatTime*2);
        return (Math.abs(timeProgress - repeatTime) + 0f) / repeatTime;
    }
    public float getRepeat(float progress){
        return interpolator.getInterpolation(progress);
    }
    TimeInterpolator interpolator =  new AccelerateDecelerateInterpolator();



    @Override
    public void onStartAnimator(@NonNull RefreshLayout layout, int height, int maxDragHeight) {
        if (mIsStarted) return;
        final View thisView = this;
        thisView.invalidate();
        mIsStarted = true;
        if (mStartTime == -1){
            mStartTime = System.currentTimeMillis();
        }
        mPaint.setColor(mAnimatingColor);
    }


    @Override
    public int onFinish(@NonNull RefreshLayout layout, boolean success) {
        mIsStarted = false;
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                mStartTime = -1;
            }
        },200);
        mPaint.setColor(mNormalColor);
        return 0;
    }

    @Override
    @Deprecated
    public void setPrimaryColors(@ColorInt int... colors) {
        if (!mManualAnimationColor && colors.length > 1) {
            setAnimatingColor(colors[0]);
            mManualAnimationColor = false;
        }
        if (!mManualNormalColor) {
            if (colors.length > 1) {
                setNormalColor(colors[1]);
            } else if (colors.length > 0) {
                setNormalColor(ColorUtils.compositeColors(0x99ffffff,colors[0]));
            }
            mManualNormalColor = false;
        }
    }

    public LavaPullRefreshHead setSpinnerStyle(SpinnerStyle mSpinnerStyle) {
        this.mSpinnerStyle = mSpinnerStyle;
        return this;
    }

    public LavaPullRefreshHead setNormalColor(@ColorInt int color) {
        mNormalColor = color;
        mManualNormalColor = true;
        if (!mIsStarted) {
            mPaint.setColor(color);
        }
        return this;
    }

    public LavaPullRefreshHead setAnimatingColor(@ColorInt int color) {
        mAnimatingColor = color;
        mManualAnimationColor = true;
        if (mIsStarted) {
            mPaint.setColor(color);
        }
        return this;
    }

    @Override
    public void onStateChanged(@NonNull RefreshLayout refreshLayout, @NonNull RefreshState oldState, @NonNull RefreshState newState) {
        super.onStateChanged(refreshLayout, oldState, newState);
        mGlobalState = newState;
    }

    RefreshState mGlobalState;

    @Override
    public void computeScroll() {
        super.computeScroll();
        float y =  getTranslationY();
        if (y >getHeight()&& (mGlobalState == RefreshReleased|| mGlobalState == Refreshing)){
            setHeightCompensation((getHeight()-y));
            this.postInvalidateOnAnimation();
        }
    }

    @Override
    public void onReleased(@NonNull RefreshLayout refreshLayout, int height, int maxDragHeight) {
        super.onReleased(refreshLayout, height, maxDragHeight);
        this.postInvalidateOnAnimation();
    }

    @Override
    public void onMoving(boolean isDragging, float percent, int offset, int height, int maxDragHeight) {
        super.onMoving(isDragging, percent, offset, height, maxDragHeight);
        if (mStartTime == -1){
            mStartTime = System.currentTimeMillis();
        }
        if (isDragging){
            compensationForMissingTime();
            if (offset > height)setHeightCompensation((height-offset));
            if (offset<=height)setHeightCompensation(0);
            mLastDragTime = System.currentTimeMillis();
            this.invalidate();
        }

    }
    public void setHeightCompensation(float height){
        for(Ramen ramen:remins){
            ramen.setHeightCompensation(height);
        }
    }


    /**
     * 圆边拉面
     * 动画上到下运行
     *
     */
    public static class Ramen{
        public void draw(Canvas canvas, Paint paint){
            draw(canvas,paint,true);
        }
        public void draw(Canvas canvas, Paint paint, boolean havExtrude){
            if (havExtrude){
                synCurrentState();
            }
            canvas.save();
            canvas.translate(getTranslateX(),getTranslateY()); //画布偏移
            path.reset();
            path.moveTo(topRect.left,topRect.centerY());
            path.arcTo(topRect,180,180);
            path.lineTo(bottomRect.right,bottomRect.centerY());
            path.arcTo(bottomRect,0,180);
            path.close();
            canvas.drawPath(path,paint);
            canvas.restore();
        }

        /**
         * @author lzc
         * @param x 中心点横向偏移量
         * @param radius 基圆的半径
         * @param starty 运动参考系顶部位置
         * @param endy 运动参考系底部
         * @param mMaxExtrude 最大张开距离
         *
         * */
        public Ramen(float x, float radius, float starty, float endy, float mMaxExtrude) {
            this.x = x;
            this.radius = radius;
            this.starty = starty;
            this.endy = endy;
            this.mMaxExtrude = mMaxExtrude;
            synCurrentState();
        }

        private void synCurrentState() {
            if (topRect == null)topRect = new RectF();
            if (bottomRect == null)bottomRect = new RectF();
            float extrude= getCurrentExtrude();
            topRect.set(-radius,-(extrude/2)-radius,radius,-(extrude/2)+radius);
            bottomRect.set(-radius,(extrude/2)-radius,radius,(extrude/2)+radius);
        }

        /**
         * 高度补偿
         * */
        public void setHeightCompensation(float height){
            mCompensationHeight=height;
        }



        /**
         * 横向中心位置
         * */
        public float getTranslateX() {
            return x;
        }
        /**
         * 运动纵向 当前的中心位置
         * */
        public float getTranslateY() {
            return (endy - starty) *animationProgress +starty+mCompensationHeight;
        }

        /**
         * 获取当前拉伸像素
         * */
        public float getCurrentExtrude(){
            return (0.5f- Math.abs(animationProgress-0.5f))*2*mMaxExtrude;
        }

        public Ramen setAnimationProgress(float animationProgress) {
            this.animationProgress = animationProgress;
            return this;
        }

        Path path = new Path();
        float x;//横向位置
        float radius;//半径
        float starty;//纵向运动区间
        float endy;//纵向运动区间
        float mMaxExtrude;//最大拉伸
        float animationProgress;//动画进度 0 -1
        RectF topRect,bottomRect;
        float mCompensationHeight;

    }

}
