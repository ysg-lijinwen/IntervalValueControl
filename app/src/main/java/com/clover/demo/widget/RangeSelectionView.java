package com.clover.demo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.clover.demo.R;

import java.math.BigDecimal;

/**
 * Description:区间滑动取值控件
 * Created by Kevin.Li on 2018-01-11.
 */
public class RangeSelectionView extends View {
    private Paint paintBackground;//背景线的画笔
    private Paint paintCircle;//起始点圆环的画笔
    private Paint paintWhileCircle;//起始点内圈白色区域的画笔
    private Paint paintStartText;//起点数值的笔
    private Paint paintEndText;//终点数值的画笔
    private Paint paintResultText;//顶部结果数值的画笔
    private Paint paintConnectLine;//起始点连接线的画笔

    private int height = 0;//控件的高度
    private int width = 0;//控件的宽度

    private float centerVertical = 0;//y轴的中间位置

    private float backLineWidth = 5;//底线的宽度

    private float marginHorizontal = 1;//横向边距

    private float marginTop = 60;//文字距基线顶部的距离
    private float marginBottom = 40;//文字距基线底部的距离

    private float pointStart = 0;//起点的X轴位置

    private float pointEnd = 0;//始点的Y轴位置

    private float circleRadius = 30;//起始点圆环的半径

    private float numStart = 0;//数值的开始值

    private float numEnd = 0;//数值的结束值

    private int textSize = 35;//文字的大小

    private String leftUnit;//左侧单位
    private String rightUnit;//右侧单位

    private String strConnector = " - ";//连接符

    private boolean isRunning = false;//是否可以滑动

    private boolean isStart = true;//起点还是终点 true：起点；false：终点。

    private boolean isInteger = false;//是否保留整形
    private boolean isShowResult = true;//是否显示结果值，默认显示。

    private int precision = 2;//保留精度，默认为2。
    //进度范围
    private float startNum = 0.00F;
    private float endNum = 100.00F;

    private int startValueColor;//开始文字颜色
    private int endValueColor;//终点文字颜色
    private int resultValueColor;//结果值文字颜色

    private int backLineColor;//基线颜色
    private int connectLineColor;//连接线颜色
    private int circleColor;//外圆填充色
    private int whileCircleColor;//圆形填充色
    private float scaling; //取值比例
    int paddingStart;
    int paddingEnd;

    private OnChangeListener mOnChangeListener;

    public RangeSelectionView(Context context) {
        super(context);
        init();
    }

    public RangeSelectionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public RangeSelectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        handleAttrs(context, attrs, defStyleAttr);
        init();
    }

    private void handleAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeSelectionView, defStyleAttr, 0);

        backLineColor = ta.getColor(R.styleable.RangeSelectionView_backLineColor, Color.CYAN);
        connectLineColor = ta.getColor(R.styleable.RangeSelectionView_connectLineColor, Color.BLUE);
        circleColor = ta.getColor(R.styleable.RangeSelectionView_circleColor, Color.BLUE);
        whileCircleColor = ta.getColor(R.styleable.RangeSelectionView_whileCircleColor, Color.WHITE);

        startValueColor = ta.getColor(R.styleable.RangeSelectionView_startValueColor, Color.MAGENTA);
        endValueColor = ta.getColor(R.styleable.RangeSelectionView_endValueColor, Color.MAGENTA);
        resultValueColor = ta.getColor(R.styleable.RangeSelectionView_resultValueColor, Color.MAGENTA);
        isShowResult = ta.getBoolean(R.styleable.RangeSelectionView_isShowResult, true);
        isInteger = ta.getBoolean(R.styleable.RangeSelectionView_isInteger, false);
        precision = ta.getInteger(R.styleable.RangeSelectionView_valuePrecision, 2);
        startNum = ta.getFloat(R.styleable.RangeSelectionView_startValue, startNum);
        endNum = ta.getFloat(R.styleable.RangeSelectionView_endValue, endNum);
        if (ta.getString(R.styleable.RangeSelectionView_leftUnit) != null) {
            leftUnit = ta.getString(R.styleable.RangeSelectionView_leftUnit);
        }
        if (ta.getString(R.styleable.RangeSelectionView_rightUnit) != null) {
            rightUnit = ta.getString(R.styleable.RangeSelectionView_rightUnit);
        }

        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //获取控件的宽高、中线位置、起始点、起始数值
        height = MeasureSpec.getSize(heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        paddingStart = getPaddingStart();
        paddingEnd = getPaddingEnd();

        centerVertical = height / 2;

        pointStart = marginHorizontal + paddingStart + circleRadius;
        pointEnd = width - marginHorizontal - paddingEnd - circleRadius;
        initBaseData();
    }

    /**
     * 初始化基础值
     */
    private void initBaseData() {
        // （父级控件宽度-左右边距-圆直径）/（结束值-起点值）
        scaling = (width - 2 * marginHorizontal - (paddingStart + paddingEnd) - 2 * circleRadius) / (endNum - startNum);

        numStart = getProgressNum(pointStart);
        numEnd = getProgressNum(pointEnd);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //如果点击的点在第一个圆内就是起点,并且可以滑动
                if (event.getX() >= (pointStart - circleRadius) && event.getX() <= (pointStart + circleRadius)) {
                    isRunning = true;
                    isStart = true;

                    pointStart = event.getX();
                    //如果点击的点在第二个圆内就是终点,并且可以滑动
                } else if (event.getX() <= (pointEnd + circleRadius) && event.getX() >= (pointEnd - circleRadius)) {
                    isRunning = true;
                    isStart = false;

                    pointEnd = event.getX();
                } else {
                    //如果触控点不在圆环内，则不能滑动
                    isRunning = false;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (isRunning) {
                    if (isStart) {
                        //起点滑动时，重置起点的位置和进度值
                        pointStart = event.getX();
                        if (pointStart < marginHorizontal + paddingStart + circleRadius) {
                            pointStart = marginHorizontal + paddingStart + circleRadius;
                            numStart = startNum;
                        } else {
                            if (pointStart + circleRadius < pointEnd - circleRadius) {//防止起点不动而值增加的问题
                                numStart = getProgressNum(pointStart);
                            }
                        }
                    } else {
                        //始点滑动时，重置始点的位置和进度值
                        pointEnd = event.getX();
                        if (pointEnd > width - marginHorizontal - paddingEnd - circleRadius) {
                            pointEnd = width - marginHorizontal - paddingEnd - circleRadius;
                            numEnd = endNum;
                        } else {
                            if (pointEnd < marginHorizontal + paddingStart + 3 * circleRadius) {//防止终点和起点在起始点相连时，往左移动，终点不动，而值减小的问题。
                                pointEnd = marginHorizontal + paddingStart + 3 * circleRadius;
                            }
                            numEnd = getProgressNum(pointEnd);
                        }
                    }

                    flushState();//刷新状态
                }

                break;
            case MotionEvent.ACTION_UP:

                flushState();
                break;
        }

        return true;
    }

    /**
     * 刷新状态和屏蔽非法值
     */
    private void flushState() {

        //起点非法值
        if (pointStart < marginHorizontal + paddingStart + circleRadius) {
            pointStart = marginHorizontal + paddingStart + circleRadius;
        }
        //终点非法值
        if (pointEnd > width - marginHorizontal - paddingEnd - circleRadius) {
            pointEnd = width - marginHorizontal - paddingEnd - circleRadius;
        }

        //防止起点位置大于终点位置（规定：如果起点位置大于终点位置，则将起点位置放在终点位置前面,即：终点可以推着起点走，而起点不能推着终点走）
        if (pointStart + circleRadius > pointEnd - circleRadius) {
            pointStart = pointEnd - 2 * circleRadius;
            numStart = getProgressNum(pointStart);//更新起点值
        }

        //防止终点把起点推到线性范围之外
        if (pointEnd < marginHorizontal + paddingStart + 3 * circleRadius) {
            pointEnd = marginHorizontal + paddingStart + 3 * circleRadius;
            pointStart = marginHorizontal + paddingStart + circleRadius;
        }

        invalidate();//这个方法会导致onDraw方法重新绘制
        if (mOnChangeListener != null) {// call back listener.
            if (isInteger) {
                mOnChangeListener.leftCursor(String.valueOf((int) numStart));
                mOnChangeListener.rightCursor(String.valueOf((int) numEnd));
            } else {
                mOnChangeListener.leftCursor(String.valueOf(numStart));
                mOnChangeListener.rightCursor(String.valueOf(numEnd));
            }
        }
    }

    //计算进度数值
    private float getProgressNum(float progress) {
        if (progress == marginHorizontal + paddingStart + circleRadius) {// 处理边界问题
            return startNum;
        }
        if (progress == width - marginHorizontal - paddingEnd - circleRadius) {
            return endNum;
        }
        // （坐标点-左边距-圆半径）/比例 + 起始值
//        float value = (progress - marginHorizontal - circleRadius) / scaling + startNum;
//        return value < startNum ? startNum : value > endNum ? endNum : value;
        return (progress - marginHorizontal - paddingEnd - circleRadius) / scaling + startNum;
    }

    /**
     * 初始化画笔
     */
    private void init() {

        paintBackground = new Paint();
        paintBackground.setColor(backLineColor);
        paintBackground.setStrokeWidth(backLineWidth);
        paintBackground.setAntiAlias(true);

        paintCircle = new Paint();
        paintCircle.setColor(circleColor);
        paintCircle.setStrokeWidth(backLineWidth);
        paintCircle.setStyle(Paint.Style.STROKE);
        paintCircle.setAntiAlias(true);

        paintWhileCircle = new Paint();
        paintWhileCircle.setColor(whileCircleColor);
        paintCircle.setStyle(Paint.Style.FILL);
        paintWhileCircle.setAntiAlias(true);

        paintStartText = new Paint();
        paintStartText.setColor(startValueColor);
        paintStartText.setTextSize(textSize);
        paintStartText.setAntiAlias(true);

        paintEndText = new Paint();
        paintEndText.setColor(endValueColor);
        paintEndText.setTextSize(textSize);
        paintEndText.setAntiAlias(true);
        paintEndText.setTextAlign(Paint.Align.RIGHT);

        paintResultText = new Paint();
        paintResultText.setColor(resultValueColor);
        paintResultText.setTextSize(textSize);
        paintResultText.setAntiAlias(true);

        paintConnectLine = new Paint();
        paintConnectLine.setColor(connectLineColor);
        paintConnectLine.setStrokeWidth(backLineWidth + 5);
        paintConnectLine.setAntiAlias(true);

    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);

        //背景线
        canvas.drawLine(marginHorizontal + paddingStart, centerVertical, width - marginHorizontal - paddingEnd, centerVertical, paintBackground);
        //起点位置的外圈圆
        canvas.drawCircle(pointStart, centerVertical, circleRadius, paintCircle);
        //起点位置的内圈圆
        canvas.drawCircle(pointStart, centerVertical, circleRadius - backLineWidth, paintWhileCircle);
        //终点位置的外圈圆
        canvas.drawCircle(pointEnd, centerVertical, circleRadius, paintCircle);
        //终点位置的内圈圆
        canvas.drawCircle(pointEnd, centerVertical, circleRadius - backLineWidth, paintWhileCircle);
        //起始点连接线
        canvas.drawLine(pointStart + circleRadius, centerVertical, pointEnd - circleRadius, centerVertical, paintConnectLine);
        //起点数值
        canvas.drawText(assembleStartText(), pointStart - circleRadius, centerVertical + marginBottom + circleRadius, paintStartText);
        //终点数值
        canvas.drawText(assembleEndText(), pointEnd + circleRadius, centerVertical + marginBottom + circleRadius, paintEndText);
        if (isShowResult) {
            //结果值
            canvas.drawText(assembleResultText(), marginHorizontal + paddingStart, centerVertical - marginTop, paintResultText);
        }
    }

    /**
     * 处理起点值精度
     */
    private float handleNumStartPrecision(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(precision, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    /**
     * 处理终点值精度
     */
    private float handleNumEndPrecision(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(precision, BigDecimal.ROUND_HALF_DOWN);
        return bd.floatValue();
    }

    /**
     * 组装起点文字
     */
    private String assembleStartText() {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(leftUnit)) sb.append(leftUnit);
        //必须在此调用String.valueOf()来提前转化为String，否则会因为append()重载而导致整形无效的问题。
        sb.append(isInteger ? String.valueOf((int) numStart) : String.valueOf(handleNumStartPrecision(numStart)));
        if (!TextUtils.isEmpty(rightUnit)) sb.append(" ").append(rightUnit);
        return sb.toString();
    }

    /**
     * 组装终点文字
     */
    private String assembleEndText() {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(leftUnit)) sb.append(leftUnit);
        sb.append(isInteger ? String.valueOf((int) numEnd) : handleNumEndPrecision(numEnd));
        if (!TextUtils.isEmpty(rightUnit)) sb.append(" ").append(rightUnit);
        return sb.toString();
    }

    /**
     * 组装结果值
     */
    private String assembleResultText() {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(leftUnit)) sb.append(leftUnit);
        sb.append(isInteger ? String.valueOf((int) numStart) : handleNumStartPrecision(numStart));
        if (!TextUtils.isEmpty(rightUnit)) sb.append(" ").append(rightUnit);
        sb.append(strConnector);
        if (!TextUtils.isEmpty(leftUnit)) sb.append(leftUnit);
        sb.append(isInteger ? String.valueOf((int) numEnd) : handleNumEndPrecision(numEnd));
        if (!TextUtils.isEmpty(rightUnit)) sb.append(" ").append(rightUnit);
        return sb.toString();
    }

    /**
     * 左侧单位
     */
    public RangeSelectionView setLeftUnit(String leftUnit) {
        this.leftUnit = leftUnit;
        return this;
    }

    /**
     * 右侧单位
     */
    public RangeSelectionView setRightUnit(String rightUnit) {
        this.rightUnit = rightUnit;
        return this;
    }


    /**
     * 是否保留整形
     */
    public RangeSelectionView setInteger(boolean integer) {
        isInteger = integer;
        return this;
    }

    /**
     * 是否显示结果值，默认显示。
     */
    public RangeSelectionView setShowResult(boolean showResult) {
        isShowResult = showResult;
        return this;
    }

    /**
     * 保留精度，默认为2。
     */
    public RangeSelectionView setPrecision(int precision) {
        this.precision = precision;
        return this;
    }

    /**
     * 起始值
     */
    public RangeSelectionView setStartNum(float startNum) {
        this.startNum = startNum;
        return this;
    }

    /**
     * 结束值
     */
    public RangeSelectionView setEndNum(float endNum) {
        this.endNum = endNum;
        return this;
    }


    /**
     * 开始文字颜色
     */
    public RangeSelectionView setStartValueColor(int startValueColor) {
        this.startValueColor = startValueColor;
        return this;
    }

    /**
     * 终点文字颜色
     */
    public RangeSelectionView setEndValueColor(int endValueColor) {
        this.endValueColor = endValueColor;
        return this;
    }

    /**
     * 结果值文字颜色
     */
    public RangeSelectionView setResultValueColor(int resultValueColor) {
        this.resultValueColor = resultValueColor;
        return this;
    }

    /**
     * 基线颜色
     */
    public RangeSelectionView setBackLineColor(int backLineColor) {
        this.backLineColor = backLineColor;
        return this;
    }

    /**
     * 连接线颜色
     */
    public RangeSelectionView setConnectLineColor(int connectLineColor) {
        this.connectLineColor = connectLineColor;
        return this;
    }

    /**
     * 外圆填充色
     */
    public RangeSelectionView setCircleColor(int circleColor) {
        this.circleColor = circleColor;
        return this;
    }

    /**
     * 圆形填充色
     */
    public RangeSelectionView setWhileCircleColor(int whileCircleColor) {
        this.whileCircleColor = whileCircleColor;
        return this;
    }

    /**
     * 通知刷新
     */
    public void notifyRefresh() {
        init();
        initBaseData();
        invalidate();//这个方法会导致onDraw方法重新绘制
    }

    /**
     * 主要充值起点和终点的画笔值
     */
    public void reSetValue() {
        pointStart = marginHorizontal + paddingStart + circleRadius;
        pointEnd = width - marginHorizontal - paddingEnd - circleRadius;
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        mOnChangeListener = onChangeListener;
    }

    public interface OnChangeListener {
        void leftCursor(String resultValue);

        void rightCursor(String resultValue);
    }
}
