package com.view.anymo.anymowheelview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * Created by Anymo on 2018/11/16.
 */

public class HWheelView extends View {
    private static final String TAG = "EasyPickerView";
    // 文字大小
    private int textSize;
    // 颜色，默认Color.BLACK
    private int textColor;
    // 文字之间的间隔，默认10dp
    private int textPadding;
    // 文字最大放大比例，默认2.0f
    private float textMaxScale;
    // 是否循环模式，默认是
    private boolean isRecycle;
    // 正常状态下最多显示几个文字，默认3（偶数时，边缘的文字会截断）
    private int maxShowNum;

    private TextPaint textPaintCenter;
    private TextPaint textPaintOthers;
    private Paint.FontMetrics fm;

    private Scroller scroller;
    private VelocityTracker velocityTracker;
    private int minimumVelocity;
    private int maximumVelocity;
    //scaledTouchSlop 是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件。如果小于这个距离就不触发移动控件
    private int scaledTouchSlop;

    // 数据
    private ArrayList<String> dataList = new ArrayList<>();
    // 中间x坐标
    private int cx;
    // 中间y坐标
    private int cy;
    // 文字最大宽度
    private float maxTextWidth;
    // 文字高度
    private int textHeight;
    // 实际内容宽度
    private int contentWidth;
    // 实际内容高度
    private int contentHeight;

    // 按下时的x坐标
    private float downX;
    // 本次滑动的x坐标偏移值
    private float offsetX;
    // 在fling之前的offsetX
    private float oldOffsetX;
    // 当前选中项（中间那个）
    private int curIndex;
    //每一次滑动的个数
    private int offsetIndex;

    // 回弹距离
    private float bounceDistance;
    // 是否正处于滑动状态
    private boolean isSliding = false;
    //中间上线
    private int centerTopX;
    //中间下线
    private int centerBottomX;

    public HWheelView(Context context) {
        this(context, null);
    }

    public HWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WheelView, defStyleAttr, 0);
        textSize = a.getDimensionPixelSize(R.styleable.WheelView_textSize, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));
        textColor = a.getColor(R.styleable.WheelView_textColor, Color.BLACK);
        textPadding = a.getDimensionPixelSize(R.styleable.WheelView_textPadding, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));
        textMaxScale = a.getFloat(R.styleable.WheelView_textMaxScale, 2.0f);
        isRecycle = a.getBoolean(R.styleable.WheelView_isRecycle, true);
        maxShowNum = a.getInteger(R.styleable.WheelView_maxShowNum, 4);
        a.recycle();

        textPaintCenter = new TextPaint();
        textPaintCenter.setColor(textColor);
        textPaintCenter.setTextSize(textSize);
        textPaintCenter.setAntiAlias(true);

        textPaintOthers = new TextPaint();
        textPaintOthers.setColor(Color.RED);
        textPaintOthers.setTextSize(textSize);
        textPaintOthers.setAntiAlias(true);

        fm = textPaintCenter.getFontMetrics();
        textHeight = (int) (fm.bottom - fm.top);
        //Scroller和OverScroller这两个类是AndroidUI框架下实现滚动效果最关键的类，ScrollView内部的实现也是使用的OverScroller
        scroller = new Scroller(context);
        //ViewConfiguration滑动参数设置类
        //configuration.getScaledTouchSlop() //获得能够进行手势滑动的距离
        //configuration.getScaledMinimumFlingVelocity()//获得允许执行一个fling手势动作的最小速度值
        //configuration.getScaledMaximumFlingVelocity()//获得允许执行一个fling手势动作的最大速度值
        minimumVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        maximumVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
        scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        contentWidth = (int) (maxTextWidth * textMaxScale) + getPaddingLeft() + getPaddingRight();
        Log.i(TAG, "onMeasure: contentWidth: " + contentWidth + " width: " + width);
        if (mode != MeasureSpec.EXACTLY) { // wrap_content
            width = contentWidth;
            if (maxShowNum%2 == 0) {//偶数
                centerTopX = (int) (width * (maxShowNum/2 - 1) + contentWidth/2);
                centerBottomX = (int) (width * (maxShowNum/2) + contentWidth/2);
            }else {//奇数
                centerTopX = (int) (width * (maxShowNum/2));
                centerBottomX = (int) (width * (maxShowNum/2 + 1));
            }
        }

        mode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        contentHeight = textHeight + textPadding;
        if (mode != MeasureSpec.EXACTLY) { // wrap_content
            height = contentHeight + getPaddingTop() + getPaddingBottom();
        }

        cx = contentWidth*maxShowNum / 2;
        cy = height / 2;

        Log.i(TAG, "onMeasure: width: " + width + " height: " + height);
        setMeasuredDimension((width)*maxShowNum, height);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        addVelocityTracker(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) {//判断是否停止滑动，没有就强制停止
                    scroller.forceFinished(true);
                    finishScroll();
                }
                downX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                offsetX = event.getX() - downX;
                //getScaledTouchSlop是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件。如果小于这个距离就不触发移动控件
                if (isSliding || Math.abs(offsetX) > scaledTouchSlop) {
                    isSliding = true;
                    reDraw();
                }
                break;

            case MotionEvent.ACTION_UP:
                int scrollXVelocity = 2 * getScrollXVelocity() / 3;
                if (Math.abs(scrollXVelocity) > minimumVelocity) {//当滑动速率大于最小速率是就继续滑动控件，否则停止滑动
                    oldOffsetX = offsetX;
                    scroller.fling(0, 0, scrollXVelocity, 0, 0, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
                    invalidate();
                } else {
                    finishScroll();
                }

                // 没有滑动，则判断点击事件
                if (!isSliding) {
                    if (downX < contentWidth / 3)
                        moveBy(-1);
                    else if (downX > 2 * contentWidth / 3)
                        moveBy(1);
                }

                isSliding = false;
                recycleVelocityTracker();
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.drawLine(centerTopX, 0, centerTopX, getHeight(), textPaintCenter);
        canvas.drawLine(centerBottomX, 0, centerBottomX, getHeight(), textPaintCenter);
        canvas.restore();
        if (null != dataList && dataList.size() > 0) {
            // 绘制文字，从当前中间项往前、后一共绘制maxShowNum个字
            int size = dataList.size();
            int half = maxShowNum / 2 + 1;
            for (int i = -half; i <= half; i++) {
                int index = curIndex - offsetIndex + i;
                if (isRecycle) {
                    if (index < 0)
                        index = (index + 1) % dataList.size() + dataList.size() - 1;
                    else if (index > dataList.size() - 1)
                        index = index % dataList.size();
                }
                Log.i(TAG, "onDraw: index: " + index);
                if (index >= 0 && index < size) {
                    // 计算每个字的中间y坐标
                    int tempY = cx + i * contentWidth;//距离中线多远
                    tempY += offsetX % contentWidth;
                    // 绘制
                    Paint.FontMetrics tempFm = textPaintCenter.getFontMetrics();
                    String text = dataList.get(index);
                    float textWidth = textPaintCenter.measureText(text);//文字大小

                    if (i == 0) {
                        if (tempY - contentWidth/2 <= centerTopX) {
                            canvas.save();
                            canvas.clipRect(tempY - textWidth/2 - centerTopX, 0, centerTopX, getHeight());
                            canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintOthers);
                            canvas.restore();
                            canvas.save();
                            canvas.clipRect(centerTopX, 0, tempY + (textWidth)/2, getHeight());
                            canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintCenter);
                            canvas.restore();
                        }else if (tempY + contentWidth/2 >= centerBottomX) {
                            Log.i(TAG, "onDraw: ---------" + text);
                            canvas.save();
                            canvas.clipRect(centerBottomX, 0, tempY + textWidth/2, getHeight());
                            canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintOthers);
                            canvas.restore();
                            canvas.save();
                            canvas.clipRect(tempY - textWidth/2 - centerBottomX, 0, centerBottomX, getHeight());
                            canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintCenter);
                            canvas.restore();
                        }else {
                            canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintOthers);
                        }
                    } else if (tempY + contentWidth/2 >= centerTopX && tempY + contentWidth/2 <= centerBottomX){
                        canvas.save();
                        canvas.clipRect(tempY - textWidth/2- centerTopX, 0, centerTopX, getHeight());
                        canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintOthers);
                        canvas.restore();
                        canvas.save();
                        canvas.clipRect(centerTopX, 0, tempY + textWidth/2, getHeight());
                        canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintCenter);
                        canvas.restore();
                    }else if (tempY + contentWidth/2 >= centerBottomX && tempY - contentWidth/2 <= centerBottomX) {
                        Log.i(TAG, "onDraw: ---------" + text);
                        canvas.save();
                        canvas.clipRect(centerBottomX, 0, tempY + textWidth/2, getHeight());
                        canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintOthers);
                        canvas.restore();
                        canvas.save();
                        canvas.clipRect(tempY - textWidth/2 - centerBottomX, 0, centerBottomX, getHeight());
                        canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintCenter);
                        canvas.restore();
                    }else {
                        canvas.drawText(text, tempY - textWidth/2, cy - (tempFm.ascent + tempFm.descent) / 2, textPaintOthers);
                    }
                }
            }
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {//计算偏移量
            offsetX = oldOffsetX + scroller.getCurrX();
            if (!scroller.isFinished())
                reDraw();
            else
                finishScroll();
        }
    }

    private void addVelocityTracker(MotionEvent event) {
        if (velocityTracker == null)
            //VelocityTracker从字面意思理解那就是速度追踪器了
            velocityTracker = VelocityTracker.obtain();

        velocityTracker.addMovement(event);
    }

    //记得资源回收
    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private int getScrollXVelocity() {
        //帮助你追踪一个touch事件（flinging事件和其他手势事件）的速率。当你要跟踪一个touch事件的时候，使用obtain()方法得到这个类的实例，然后 addMovement(MotionEvent)函数将你接受到的motion event加入到VelocityTracker类实例中。当你使用到速率时，使用computeCurrentVelocity(int)初始化速率的单位，并获得当前的事件的速率，然后使用getXVelocity() 或getXVelocity()获得横向和竖向的速率。
        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
        int velocityX = (int) velocityTracker.getXVelocity();
        return velocityX;
    }

    private void reDraw() {
        // curIndex需要偏移的量
        int i = (int) (offsetX / (contentWidth));
        //(curIndex - i >= 0 && curIndex - i < dataList.size())这个是为了限制在不是无限循环的情况下，做的边界判断
        if (isRecycle || (curIndex - i >= 0 && curIndex - i < dataList.size())) {
            if (offsetIndex != i) {//每一次offsetY刷新时，会不断的去计算滑动的个数
                offsetIndex = i;
            }
            postInvalidate();
        } else {
            finishScroll();
        }
    }

    private void finishScroll() {
        // 判断结束滑动后应该停留在哪个位置
        int centerPadding = contentWidth + textPadding;//每个字的cell高度
        float v = offsetX % centerPadding;
        if (v > 0.5f * centerPadding)//超过字的一半就算下一个（这是向下滑的时候）
            ++offsetIndex;
        else if (v < -0.5f * centerPadding)//超过字的一半就算下一个（这是向上滑的时候）
            --offsetIndex;

        // 重置curIndex
        curIndex = getNowIndex(-offsetIndex);

        // 计算回弹的距离
        bounceDistance = offsetIndex * centerPadding - offsetX;
        offsetX += bounceDistance;

        // 重绘
        reset();
        postInvalidate();
    }

    // 重置curIndex
    private int getNowIndex(int offsetIndex) {
        int index = curIndex + offsetIndex;
        if (isRecycle) {
            if (index < 0)
                index = (index + 1) % dataList.size() + dataList.size() - 1;
            else if (index > dataList.size() - 1)
                index = index % dataList.size();
        } else {
            if (index < 0)
                index = 0;
            else if (index > dataList.size() - 1)
                index = dataList.size() - 1;
        }
        return index;
    }

    private void reset() {
        offsetX = 0;
        oldOffsetX = 0;
        offsetIndex = 0;
        bounceDistance = 0;
    }

    /**
     * 设置要显示的数据
     *
     * @param dataList 要显示的数据
     */
    public void setDataList(ArrayList<String> dataList) {
        this.dataList.clear();
        this.dataList.addAll(dataList);

        // 更新maxTextWidth
        if (null != dataList && dataList.size() > 0) {
            int size = dataList.size();
            for (int i = 0; i < size; i++) {
                float tempWidth = textPaintCenter.measureText(dataList.get(i));//适配最大宽度的文字
                if (tempWidth > maxTextWidth)
                    maxTextWidth = tempWidth;
            }
            curIndex = 0;
        }
        requestLayout();
        invalidate();
    }

    /**
     * 获取当前状态下，选中的下标
     *
     * @return 选中的下标
     */
    public int getCurIndex() {
        return getNowIndex(-offsetIndex);
    }

    /**
     * 滚动到指定位置
     *
     * @param index 需要滚动到的指定位置
     */
    public void moveTo(int index) {
        if (index < 0 || index >= dataList.size() || curIndex == index)
            return;

        if (!scroller.isFinished())
            scroller.forceFinished(true);

        finishScroll();

        int dx = 0;
        int centerPadding = contentWidth + textPadding;
        if (!isRecycle) {
            dx = (curIndex - index) * centerPadding;
        } else {
            int offsetIndex = curIndex - index;
            int d1 = Math.abs(offsetIndex) * centerPadding;
            int d2 = (dataList.size() - Math.abs(offsetIndex)) * centerPadding;

            if (offsetIndex > 0) {
                if (d1 < d2)
                    dx = d1; // ascent
                else
                    dx = -d2; // descent
            } else {
                if (d1 < d2)
                    dx = -d1; // descent
                else
                    dx = d2; // ascent
            }
        }
        //startX 水平方向滚动的偏移值，以像素为单位。正值表明滚动将向左滚动
        //startY 垂直方向滚动的偏移值，以像素为单位。正值表明滚动将向上滚动
        //dx 水平方向滑动的距离，正值会使滚动向左滚动
        //dy 垂直方向滑动的距离，正值会使滚动向上滚动
        scroller.startScroll(0, 0, dx, 0, 500);
        invalidate();
    }

    /**
     * 滚动指定的偏移量
     *
     * @param offsetIndex 指定的偏移量
     */
    public void moveBy(int offsetIndex) {
        moveTo(getNowIndex(offsetIndex));
    }

}
