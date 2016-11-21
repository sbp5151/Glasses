package com.jld.glasses;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.jld.glasses.activity.MainActivity;
import com.jld.glasses.model.MainItem;
import com.jld.glasses.util.LogUtil;

import java.util.ArrayList;

import static com.jld.glasses.activity.MainActivity.REFRESH;

public class BluetoothReceiver extends BroadcastReceiver {

    public static final String TAG = "BluetoothReceiver";
    private static Handler mSendHandler;
    private static Handler mServiceHandler;
    private ArrayList<String> mAddress = new ArrayList<>();
    public static final int CONNECT_CHANGE = 0x61;//蓝牙连接状态改变

    public static void sendHandler(Handler mHandler) {
        mSendHandler = mHandler;
    }

    public static void sendServiceHandler(Handler mHandler) {
        mServiceHandler = mHandler;
    }

    public BluetoothReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        LogUtil.d(TAG, "action:" + action);
        int connectState;
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {//发现蓝牙设备
            // 获取查找到的蓝牙设备
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // 获取蓝牙设备的连接状态
            connectState = device.getBondState();
            switch (connectState) {
                // 未配对
                case BluetoothDevice.BOND_NONE:
                    LogUtil.d(TAG, "未配对：" + device.getName());
                    LogUtil.d(TAG, "未配对：" + device.getAddress());
                    synchronized ("BOND_NONE") {
                        LogUtil.d(TAG, "进-----");
                        if (mSendHandler != null && !TextUtils.isEmpty(device.getAddress()) && !mAddress.contains(device.getAddress())) {
                            send_code(new MainItem(device.getName(), "5", "", device.getAddress()), MainActivity.RET_DEV_NAME, -1);
                            mAddress.add(device.getAddress());
                        }
                        LogUtil.d(TAG, "出----------");
                    }
                    break;
                // 已配对
                case BluetoothDevice.BOND_BONDED:
                    LogUtil.d(TAG, "已配对：" + device.getName());
                    synchronized ("BOND_BONDED") {
                        if (mSendHandler != null && !TextUtils.isEmpty(device.getAddress()) && !mAddress.contains(device.getAddress())) {
                            send_code(new MainItem(device.getName(), "3", "", device.getAddress()), MainActivity.RET_DEV_NAME, -1);
                            mAddress.add(device.getAddress());
                        }
                    }
                    break;
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {   //搜索结束
            LogUtil.d(TAG, "搜索结束");
            if (mSendHandler != null)
                mSendHandler.sendEmptyMessage(MainActivity.REFRESH_FINISH);
        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {   //开始搜索
            LogUtil.d(TAG, "开始搜索");
            mAddress.clear();
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {   //绑定状态改变
            LogUtil.d(TAG, "绑定状态改变");
            if (mSendHandler != null)
                mSendHandler.sendEmptyMessage(REFRESH);
        } else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {//蓝牙连接状态改变
            LogUtil.d(TAG, "蓝牙连接状态改变");
            if (mServiceHandler != null)
                mServiceHandler.sendEmptyMessage(CONNECT_CHANGE);
            if (mSendHandler != null)
                mSendHandler.sendEmptyMessage(CONNECT_CHANGE);
        }
    }

    private void send_code(Object str, int what, int arg1) {
        if (mSendHandler == null)
            return;
        Message message = mSendHandler.obtainMessage();
        if (str != null)
            message.obj = str;
        if (arg1 != -1)
            message.arg1 = arg1;
        message.what = what;
        mSendHandler.sendMessage(message);
    }
}
