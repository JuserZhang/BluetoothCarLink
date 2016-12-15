package com.JuserZhang.BluetoothCar.widget;

import com.JuserZhang.BluetoothCar.util.MathUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

/**
 * 自定义方向盘类
 */
public class Rudder extends SurfaceView implements Runnable, Callback {
    private Direction mDirection = Direction.DEFAULT;

    // 摇杆事件
    public static final int ACTION_RUDDER = 0x01;
    // 按钮事件（未实现）
    public static final int ACTION_STOPPED = 0x02;

    private SurfaceHolder mHolder = null;
    private Thread mThread = null;
    private Paint mPaint = null;

    private int wheelColor = Color.rgb(220, 220, 209);
    private int rockerColor = Color.rgb(0, 153, 0);

    // 摇杆起始位置
    private Point mCtrlPoint = new Point(260, 260);
    // 摇杆位置
    private Point mRockerPosition = new Point(mCtrlPoint);

    private boolean isStop = false;

    // 摇杆活动范围半径
    private int mRangeRadius = 220;
    // 摇杆半径
    private int mRudderRadius = 50;

    // 事件回调接口
    private RudderListener mListener = null;

    public Rudder(Context context) {
        this(context, null);
    }

    public Rudder(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Rudder(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    /**
     * 回调接口
     */
    public interface RudderListener {

        /**
         * @param action
         * @param direction
         */
        void onSteeringWheelChanged(int action, Direction direction);

        /**
         * @param isAnim
         */
        void onAnimated(boolean isAnim);
    }

    /**
     * 设置回调接口
     *
     * @param listener
     */
    public void setOnRudderListener(RudderListener listener) {
        mListener = listener;
    }

    @Override
    public void run() {
        Canvas canvas = null;

        while (!isStop) {
            try {
                canvas = mHolder.lockCanvas();

                // 清屏
                canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);

                // 绘制摇杆活动范围
                mPaint.setColor(wheelColor);
                canvas.drawCircle(mCtrlPoint.x, mCtrlPoint.y, mRangeRadius, mPaint);

                // 绘制摇杆
                mPaint.setColor(rockerColor);
                canvas.drawCircle(mRockerPosition.x, mRockerPosition.y, mRudderRadius, mPaint);

                SystemClock.sleep(50);
            } catch (Exception e) {
                isStop = true;
            } finally {
                if (canvas != null) {
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isStop = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int len = MathUtil.getLength(mCtrlPoint.x, mCtrlPoint.y, event.getX(), event.getY());

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mListener != null) {
                mListener.onAnimated(true);
            }
            // 如果屏幕接触点不在摇杆挥动范围内，则不处理
            if (len > mRangeRadius) {
                return true;
            }
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {

            if (len <= mRangeRadius) {
                // 如果手指在摇杆活动范围内，则摇杆处于手指触摸位置
                mRockerPosition.set((int) event.getX(), (int) event.getY());
            } else {
                // 设置摇杆位置，使其处于手指触摸方向的摇杆活动范围边缘
                mRockerPosition = MathUtil.getBorderPoint(mCtrlPoint,
                        new Point((int) event.getX(), (int) event.getY()),
                        mRangeRadius);
            }

            float radian = MathUtil.getRadian(mCtrlPoint, new Point((int) event.getX(), (int) event.getY()));
            int angle = MathUtil.getAngleConvert(radian);
            checkDirection(angle);
        }
        // 如果手指离开屏幕，则摇杆返回初始位置
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mListener != null) {
                mListener.onAnimated(false);
                mListener.onSteeringWheelChanged(ACTION_STOPPED, Direction.DEFAULT);
            }
            mRockerPosition.set(mCtrlPoint.x, mCtrlPoint.y);
        }
        return true;
    }

    /**
     * 触摸角度
     *
     * @param angle
     */
    private void checkDirection(int angle) {
        if (angle > 202.5f && angle < 247.5f) {
            mDirection = Direction.LEFT_DOWN_DIR;
        } else if (angle > 157.5f && angle < 202.5f) {
            mDirection = Direction.LEFT_DIR;
        } else if (angle > 112.5f && angle < 157.5f) {
            mDirection = Direction.LEFT_UP_DIR;
        } else if (angle > 67.5f && angle < 112.5f) {
            mDirection = Direction.UP_DIR;
        } else if (angle > 22.5f && angle < 67.5f) {
            mDirection = Direction.RIGHT_UP_DIR;
        } else if (angle > 292.5f && angle < 337.5f) {
            mDirection = Direction.RIGHT_DOWN_DIR;
        } else if (angle > 247.5f && angle < 292.5f) {
            mDirection = Direction.DOWN_DIR;
        } else {
            mDirection = Direction.RIGHT_DIR;
        }

        if (mListener != null) {
            mListener.onSteeringWheelChanged(ACTION_RUDDER, mDirection);
        }
    }

    /**
     * 初始化
     */
    private void init() {
        setKeepScreenOn(true);

        mHolder = getHolder();
        // 设置回调
        mHolder.addCallback(this);

        mThread = new Thread(this);

        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        // 抗锯齿
        mPaint.setAntiAlias(true);

        // 设置聚焦
        setFocusable(true);
        setFocusableInTouchMode(true);
        setZOrderOnTop(true);

        // 设置背景透明
        mHolder.setFormat(PixelFormat.TRANSPARENT);
    }
}
