package com.JuserZhang.BluetoothCar.adapter;

import java.util.ArrayList;
import java.util.List;
import com.JuserZhang.BluetoothCar.R;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * 自定义适配器类
 */
public class DeviceListAdapter extends BaseAdapter {
    private List<BluetoothDevice> mDevices = null;
    private LayoutInflater mLayoutInflater = null;

    /**
     * 构造函数
     *
     */
    public DeviceListAdapter(Context context) {
        mDevices = new ArrayList<>();
        // 获取系统布局映射服务
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * 将蓝牙设备添加到列表中
     *
     * @param device
     */
    public void addDevice(BluetoothDevice device) {
        // 去重
        if (!mDevices.contains(device)) {
            mDevices.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mDevices.get(position);
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.list_item, viewGroup, false);

            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        String deviceName = mDevices.get(i).getName();

        //使用TextUtils.isEmpty简单化代码
        //android的官方API解释：Returns true if the string is null or 0-length
        if (!TextUtils.isEmpty(deviceName)) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText("未知设备");
        }

        viewHolder.deviceAddress.setText(mDevices.get(i).getAddress());

        return view;
    }

    /**
     * ViewHolder提高加载性能
     *
     * 就是一个持有者的类，他里面一般没有方法，只有属性，
     * 作用就是一个临时的储存器，把你getView方法中每次返回的View存起来，可以下次再用。
     * 这样做的好处就是不必每次都到布局文件中去拿到你的View，提高了效率
     */
    private static class ViewHolder {
        private TextView deviceName = null;
        private TextView deviceAddress = null;
    }
}
