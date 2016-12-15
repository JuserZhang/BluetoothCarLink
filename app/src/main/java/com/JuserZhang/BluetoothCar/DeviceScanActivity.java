package com.JuserZhang.BluetoothCar;

import com.JuserZhang.BluetoothCar.adapter.DeviceListAdapter;
import com.JuserZhang.BluetoothCar.util.Logger;
import com.JuserZhang.BluetoothCar.widget.WhorlView;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.widget.ListView;

public class DeviceScanActivity extends ListActivity {
    // 调试用
    private static final String TAG = "DeviceScanActivity";

    // 开启蓝牙请求码
    private static final int REQUEST_ENABLE = 0;

    // 停止扫描蓝牙消息头
    private static final int WHAT_CANCEL_DISCOVERY = 1;
    // 更新列表消息头
    private static final int WHAT_DEVICE_UPDATE = 2;

    // 扫描间隔时间
    private static final int SCAN_PERIOD = 30 * 1000;

    private DeviceListAdapter mLeDeviceListAdapter = null;
    // 蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;
    // 螺纹进度条
    private WhorlView mWhorlView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mLeDeviceListAdapter = new DeviceListAdapter(this);
        // 设置列表适配器，注：调用此方法必须继承ListActivity
        setListAdapter(mLeDeviceListAdapter);

        scanDevice(true);
        mWhorlView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        scanDevice(false);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregReceiver();

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 线程自杀
        if (requestCode == REQUEST_ENABLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            Process.killProcess(Process.myPid());
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) {
            return;
        }
        // 执行Intent跳转并携带数据
        Intent intent = new Intent(this, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("device", device);
        intent.putExtras(bundle);

        scanDevice(false);
        startActivity(intent);
        finish();
    }

    /**
     * 消息处理者
     */
    private Handler mHandler = new Handler(new Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {

                case WHAT_DEVICE_UPDATE:
                    mLeDeviceListAdapter.addDevice((BluetoothDevice) msg.obj);
                    // 刷新列表
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    break;

                case WHAT_CANCEL_DISCOVERY:
                    // 隐藏View并消除所占空间
                    mWhorlView.setVisibility(View.GONE);
                    break;

                default:
                    break;
            }
            return false;
        }
    });

    /**
     * 初始化
     */
    private void init() {
        mWhorlView = (WhorlView) findViewById(R.id.whorl_view);
        // 开启动画
        mWhorlView.start();

        // 初始化本地蓝牙设备
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 检测蓝牙设备是否开启，如果未开启，发起Intent并回调
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE);
        }

        registerReceiver();
    }

    /**
     * 是否扫描蓝牙设备
     *
     * @param enable
     */
    private void scanDevice(boolean enable) {
        if (enable) {

            Logger.d(TAG, "[1]--> startDiscovery()");

            // 开启扫描
            mBluetoothAdapter.startDiscovery();

            // 延时30s后取消扫描动作
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mBluetoothAdapter.cancelDiscovery();

                    Logger.d(TAG, "[2]--> cancelDiscovery()");

                    // 发送消息
                    mHandler.sendEmptyMessage(WHAT_CANCEL_DISCOVERY);
                }
            }, SCAN_PERIOD);
        } else {

            Logger.d(TAG, "[3]--> cancelDiscovery()");

            // 停止扫描
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * 注册广播接收器
     */
    private void registerReceiver() {
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    /**
     * 注销广播接收器
     */
    private void unregReceiver() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    /**
     * 广播接收器接收返回的蓝牙信息
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND == action) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Logger.d(TAG, "[4] --> " + device.getName() + "------" + device.getAddress());

                if (device != null) {
                    //发送消息
                    mHandler.sendMessage(mHandler.obtainMessage(WHAT_DEVICE_UPDATE, device));
                }
            }
        }
    };
}
