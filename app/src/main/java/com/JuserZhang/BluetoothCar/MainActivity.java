package com.JuserZhang.BluetoothCar;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import com.JuserZhang.BluetoothCar.util.Logger;
import com.JuserZhang.BluetoothCar.widget.Direction;
import com.JuserZhang.BluetoothCar.widget.Rudder;
import com.JuserZhang.BluetoothCar.widget.Rudder.RudderListener;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements RudderListener, SensorEventListener {
    // 调试用
    private static final String TAG = "MainActivity";
    // 蓝牙UUID
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    // 协议常量
    private static byte[] data = new byte[]{0x00, 0x0D, 0x0A};

    // 提示内容
    private TextView mTextView = null;
    // 虚拟摇杆
    private Rudder mRudder = null;

    // 加载动画
    private ImageView mWheelView = null;
    private Animation mAnimation = null;

    // 蓝牙API
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothSocket mSocket = null;
    private OutputStream mOutS = null;

    // 加速(重力)传感器API
    private SensorManager mSensorManager = null;
    private Sensor mSensor = null;

    // 加速(重力)传感器开关
    private ToggleButton mToggle = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置无标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        initUi();
    }

    @Override
    protected void onStart() {
        super.onStart();

        initDevice();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 初始化传感器
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onPause() {
        // 注销传感器
        mSensorManager.unregisterListener(this);

        super.onPause();
    }

    @Override
    protected void onStop() {
        close();

        super.onStop();
    }

    /**
     * 初始化UI
     */
    private void initUi() {
        mTextView = (TextView) findViewById(R.id.text_view);
        mRudder = (Rudder) findViewById(R.id.rudder);
        mWheelView = (ImageView) findViewById(R.id.wheel_view);
        mToggle = (ToggleButton) findViewById(R.id.toggle);

        mToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 注册传感器
                    mSensorManager.registerListener(MainActivity.this, mSensor, SensorManager.SENSOR_DELAY_GAME);
                } else {
                    // 注销传感器
                    mSensorManager.unregisterListener(MainActivity.this);
                    data[0] = 0x00;
                    writeStream(data);
                }
            }
        });

        // 设置监听器
        mRudder.setOnRudderListener(this);

        // 加载动画
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);

        // 设置匀速旋转速率
        mAnimation.setInterpolator(new LinearInterpolator());
    }

    /**
     * 初始化设备
     */
    private void initDevice() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mBluetoothDevice = bundle.getParcelable("device");
            if (mBluetoothDevice != null) {
                Logger.d(TAG, mBluetoothDevice.getName() + "------" + mBluetoothDevice.getAddress());
                new Thread(mConnRun).start();
            }
        }
    }

    /**
     * 连接蓝牙
     */
    private Runnable mConnRun = new Runnable() {

        @Override
        public void run() {
            connect();
        }
    };

    /**
     * 建立连接
     */
    private void connect() {
        // 转化格式
        UUID uuid = UUID.fromString(SPP_UUID);

        try {
            // 发起远程服务连接
            mSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Logger.e(TAG, e1.getMessage());
                }
            }
        }

        try {
            // 连接
            mSocket.connect();
        } catch (IOException e) {
            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Logger.e(TAG, e1.getMessage());
                }
            }
        }

        try {
            // 获取输出流
            mOutS = mSocket.getOutputStream();
        } catch (IOException e) {
            if (mOutS != null) {
                try {
                    mOutS.close();
                } catch (IOException e1) {
                    Logger.e(TAG, e.getMessage());
                }
            }

            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Logger.e(TAG, e.getMessage());
                }
            }
        }
    }

    /**
     * 关闭流
     */
    private void close() {
        if (mOutS != null) {
            try {
                mOutS.close();
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * 发送数据
     *
     * @param data
     */
    private void writeStream(byte[] data) {
        try {
            if (mOutS != null) {
                // 写入数据
                mOutS.write(data);
                // 将缓冲区的数据发送出去，此方法必须调用
                mOutS.flush();
            }
        } catch (IOException e) {
            // UI控件必须在main线程执行，runOnUiThread内部实现即Handler消息处理机制
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                 //   Toaster.shortToastShow(MainActivity.this, "连接超时，被服务器君抛弃了::>_<::");
                    // 结束程序
                  //  MainActivity.this.finish();
                }
            });
        }
    }

    @Override
    public void onSteeringWheelChanged(int action, Direction direction) {
        if (action == Rudder.ACTION_RUDDER) {
            switch (direction) {

                case LEFT_DOWN_DIR:
                    Logger.d(TAG, "[1] --> left down...");
                    mTextView.setText("左后方");
                    data[0] = 0x07;
                    break;

                case LEFT_DIR:
                    Logger.d(TAG, "[2] --> turn left...");
                    mTextView.setText("左转");
                    data[0] = 0x03;
                    break;

                case LEFT_UP_DIR:
                    Logger.d(TAG, "[3] --> left up...");
                    mTextView.setText("左前方");
                    data[0] = 0x05;
                    break;

                case UP_DIR:
                    Logger.d(TAG, "[4] --> go forward...");
                    mTextView.setText("前进");
                    data[0] = 0x01;
                    break;

                case RIGHT_UP_DIR:
                    Logger.d(TAG, "[5] --> right up...");
                    mTextView.setText("右前方");
                    data[0] = 0x06;
                    break;

                case RIGHT_DIR:
                    Logger.d(TAG, "[6] --> turn right...");
                    mTextView.setText("右转");
                    data[0] = 0x04;
                    break;

                case RIGHT_DOWN_DIR:
                    Logger.d(TAG, "[7] --> right down...");
                    mTextView.setText("右后方");
                    data[0] = 0x08;
                    break;

                case DOWN_DIR:
                    Logger.d(TAG, "[8] --> go backward...");
                    mTextView.setText("后退");
                    data[0] = 0x02;
                    break;

                default:
                    break;
            }
            writeStream(data);

        } else if (action == Rudder.ACTION_STOPPED) {
            Logger.d(TAG, "[9] --> keep stopped..");
            mTextView.setText("暂停");
            data[0] = 0x00;
            writeStream(data);
        }
    }

    @Override
    public void onAnimated(boolean isAnim) {
        if (isAnim) {
            mWheelView.startAnimation(mAnimation);
        } else {
            mWheelView.clearAnimation();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == null) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float gx = event.values[0];
            float gy = event.values[1];
            float gz = event.values[2];

            if (gx < -3) {
                Logger.d(TAG, "[10] --> go forward...");
                mTextView.setText("重力感应:前进");
                data[0] = 0x01;
                writeStream(data);
            } else if (gx > 6) {
                Logger.d(TAG, "[11] --> go backward...");
                mTextView.setText("重力感应:后退");
                data[0] = 0x02;
                writeStream(data);
            } else if (gy < -4) {
                Logger.d(TAG, "[12] --> turn left...");
                mTextView.setText("重力感应:左转");
                data[0] = 0x03;
                writeStream(data);
            } else if (gy > 4) {
                Logger.d(TAG, "[13] --> turn right...");
                mTextView.setText("重力感应:右转");
                data[0] = 0x04;
                writeStream(data);
            } else {
                Logger.d(TAG, "[14] --> gx:" + gx + "------gy:" + gy + "------gz:" + gz);
                mTextView.setText("重力感应:暂停");
                data[0] = 0x00;
                writeStream(data);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
