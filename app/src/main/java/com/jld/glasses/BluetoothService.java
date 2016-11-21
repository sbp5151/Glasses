package com.jld.glasses;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;

import com.google.gson.Gson;
import com.jld.glasses.util.Constants;
import com.jld.glasses.util.LogUtil;
import com.jld.glasses.util.ToastUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static com.jld.glasses.BluetoothReceiver.CONNECT_CHANGE;
import static com.jld.glasses.activity.MainActivity.SERVICE_CODE;
import static com.jld.glasses.activity.SettingActivity.CONNECT_ERROR;
import static com.jld.glasses.activity.SettingActivity.DISCONNECT;

public class BluetoothService extends Service {
    public static final String TAG = "BluetoothService";
    private myBinder mMyBinder;
    private Handler mSendHandler;
    private Handler mMainHandler;
    private BluetoothManager mBlueManager;
    private BluetoothAdapter mBlueAdapter;
    private BluetoothDevice mGlassesDevice;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothSocket mSocket;
    private BluetoothServerSocket mServiceSocket;

    private ArrayList<String> mAddress = new ArrayList<>();
    public static final int MSG = 0x71;
    public static final int DISCOVERY = 0x73;
    public static final int BIND_DEV = 0x74;//绑定指定设备
    public static final int CONNECT_DEV = 0x75;//连接指定设备
    public static final int SEND_MESSAGE = 0x76;//发送消息
    public static final int RECEIVE_MESSAGE = 0x77;//接收消息
    public static final int BREAK_CONNECT = 0x78;//断开绑定
    public static final int BLUE_DISCONNECT = 0x79;//断开绑定
    public static final int AGAIN_CONNECT_DEV = 0x70;//重新连接设备
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            if (what != AGAIN_CONNECT_DEV && what != MSG && mBlueAdapter != null && !mBlueAdapter.isEnabled()) {//蓝牙被关闭
                mSendHandler.sendEmptyMessage(BLUE_DISCONNECT);
                tosUtil(getString(R.string.open_bluetooth));
                return;
            }
            switch (what) {
                case MSG://Toast工具
                    String str = (String) msg.obj;
                    ToastUtil.showToast(BluetoothService.this, str, 3000);
                    break;
                case BIND_DEV://绑定蓝牙设备
                    String address = (String) msg.obj;
                    boolean bind = bindDev(address);
                    LogUtil.d(TAG, "bind:" + bind);
                    break;
                case CONNECT_DEV://连接蓝牙设备
                    String address2 = (String) msg.obj;
                    LogUtil.d(TAG, "CONNECT_DEV:" + address2);
                    mGlassesDevice = mBlueAdapter.getRemoteDevice(address2);
                    if (mGlassesDevice != null) {
                        communication();
                    } else {
                        if (mMainHandler != null) {
                            ToastUtil.showToast(BluetoothService.this, getString(R.string.connect_error), 3000);
                            send_code("", SERVICE_CODE, 3);
                        }
                    }
                    break;
                case AGAIN_CONNECT_DEV://重新连接蓝牙设备
                    String address3 = (String) msg.obj;
                    LogUtil.d(TAG, "CONNECT_DEV:" + address3);
                    mGlassesDevice = mBlueAdapter.getRemoteDevice(address3);
                    if (mGlassesDevice != null) {
                        communication();
                    } else {
                        if (mMainHandler != null) {
                            ToastUtil.showToast(BluetoothService.this, getString(R.string.connect_error), 3000);
                            send_code("", SERVICE_CODE, 3);
                        }
                    }
                    break;
                case SEND_MESSAGE://发送接收到的指令
                    if (mOutput != null) {
                        sendMsg = (String) msg.obj;
                        try {
                            mOutput.write(sendMsg.getBytes());
                        } catch (IOException e) {
                            tosUtil(getString(R.string.connect_exception));
                            e.printStackTrace();
                        }
                        LogUtil.d(TAG, "发送：" + sendMsg);
                    } else {
                        tosUtil(getString(R.string.connect_exception));
                    }
                    break;
                case BREAK_CONNECT://断开连接
                    LogUtil.d(TAG, "断开连接");
                    HashMap<String, String> mMap = new HashMap();
                    mMap.put("number", "phone_00");
                    mMap.put("state", "0");
                    String json = mGson.toJson(mMap);
                    Message message = mHandler.obtainMessage();
                    message.obj = json;
                    message.what = SEND_MESSAGE;
                    mHandler.sendMessage(message);
                    send_code(null, DISCONNECT, -1);
                    stopSelf();
                    break;
                case CONNECT_CHANGE:
                    mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
                    break;
            }
        }
    };
    private OutputStream mOutput;
    private InputStream mInput;
    private Thread mSendThread;
    private Thread mClientThread;
    public String sendMsg;
    private SharedPreferences mSp;
    private AlertDialog mChangeDialog;
    private Gson mGson;
    private HashMap<Object, Object> mMap;

    @Override
    public void onCreate() {
        super.onCreate();
        mMap = new HashMap<>();
        mMap.put("number", "phone_00");
        mMap.put("state", "1");
        mGson = new Gson();
        sendMsg = mGson.toJson(mMap);
        BluetoothReceiver.sendServiceHandler(mHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(TAG, "onStartCommand");
        initBlue();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.d(TAG, "onBind");
        if (mMyBinder == null)
            mMyBinder = new myBinder();
        return mMyBinder;
    }

    public class myBinder extends Binder {
        public void setHandler(Handler mMainHandler) {
            BluetoothService.this.mSendHandler = mMainHandler;
        }

        public void sendMainHandler(Handler mMainHandler) {
            BluetoothService.this.mMainHandler = mMainHandler;
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    private void initBlue() {
        //获取BluetoothManager
        mBlueManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBlueAdapter = mBlueManager.getAdapter();
        new Thread(serviceRun).start();//开启服务端 等待被连接
        //保存设备mac
        String address = mBlueAdapter.getAddress();
        mSp = getSharedPreferences(Constants.SHARE_KEY, Context.MODE_PRIVATE);
        mSp.edit().putString(Constants.MY_DEV_ADDRESS, address).apply();
        LogUtil.d(TAG, "address:" + address);
    }

    /**
     * 建立socket连接
     */
    private void communication() {
        try {
            if (mSocket != null && mSocket.isConnected()) {
                mBlueAdapter.cancelDiscovery();
            }
            //通过其设备获取其socket
            mSocket = mGlassesDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(Constants.UUID));
            //开启客户端
            mClientThread = new Thread(clientRun);
            mClientThread.start();
        } catch (IOException e) {
            ToastUtil.showToast(BluetoothService.this, getString(R.string.connect_error), 3000);
            e.printStackTrace();
        }
    }

    boolean isConnect = false;//是否和眼镜连接
    boolean isReceive = true;//是否接收指令
    Runnable serviceRun = new Runnable() {
        @Override
        public void run() {
            BluetoothSocket mSocket = null;//被连接获取对方socket
            try {
                mServiceSocket = mBlueAdapter.listenUsingInsecureRfcommWithServiceRecord("mySocket", UUID.fromString(Constants.UUID));
                while (!isConnect) {
                    LogUtil.d(TAG, "等待被连接");
                    try {
                        mSocket = mServiceSocket.accept();
                    } catch (IOException e) {
                        LogUtil.d(TAG, "等待连接失败");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        continue;
                    }
                    //保存当前连接设备
                    BluetoothDevice remoteDevice = mSocket.getRemoteDevice();
                    String address = remoteDevice.getAddress();
                    mSp.edit().putString(Constants.CONNECT_DEV_ADDRESS, address).apply();
                    mSp.edit().putString(Constants.CONNECT_DEV_NAME, remoteDevice.getName()).apply();
                    LogUtil.d(TAG, "连接成功:" + address);
                    //获取输入输出流
                    mInput = mSocket.getInputStream();
                    mOutput = mSocket.getOutputStream();
                    isConnect = true;
                    receive(mInput);
//                    mBlueAdapter.cancelDiscovery();//被连接成功停止主动连接
                }
            } catch (IOException e) {
                LogUtil.d(TAG, e.toString());
                e.printStackTrace();
                return;
            }
        }
    };
    Runnable clientRun = new Runnable() {
        @Override
        public void run() {
            try {
                //请求连接
                LogUtil.d(TAG, "请求连接");
                try {
                    mSocket.connect();
                } catch (IOException e) {//连接失败
                    LogUtil.d(TAG, "IOException：" + e.toString());
                    tosUtil("请求连接失败");
                    send_code("", SERVICE_CODE, 3);
                    e.printStackTrace();
                    return;
                }
                LogUtil.d(TAG, "连接成功");
                //保存当前连接设备
                BluetoothDevice remoteDevice = mSocket.getRemoteDevice();
                String address = remoteDevice.getAddress();
                mSp.edit().putString(Constants.CONNECT_DEV_ADDRESS, address).apply();
                mSp.edit().putString(Constants.CONNECT_DEV_NAME, remoteDevice.getName()).apply();
                //获取眼镜蓝牙输入输出流
                mOutput = mSocket.getOutputStream();
                mInput = mSocket.getInputStream();
                isConnect = true;
                sendMsg = mGson.toJson(mMap);
                mOutput.write(sendMsg.getBytes());//发送连接请求
                receive(mInput);
//                mServiceSocket.close();//主动连接成功，停止被连接
            } catch (IOException e) {
                LogUtil.d(TAG, e.toString());
                e.printStackTrace();
            }
        }
    };

    /**
     * 接收消息
     *
     * @param mInput
     */
    public void receive(InputStream mInput) {
        int read;
        String str;
        LogUtil.d(TAG, "receive:" + isReceive);
        isReceive = true;
        while (isReceive) {
            byte[] buffer = new byte[1024];
            try {
                if ((read = mInput.read(buffer)) > 0) {
                    LogUtil.d(TAG, "read：" + read);
                    str = new String(buffer, 0, read, "UTF-8");
                    LogUtil.d(TAG, "收到" + str);
                } else
                    continue;
                if (str.equals("连接成功")) {
                    mOutput.write("连接成功".getBytes());
                }
                send_code(str, RECEIVE_MESSAGE, -1);
            } catch (IOException e) {
                isReceive = false;
                tosUtil(getString(R.string.connect_exception));
                //关闭子activity，主页面弹出对话框
                MyApplication.finishActivity();
                if (mMainHandler != null)
                    mMainHandler.sendEmptyMessage(CONNECT_ERROR);
                e.printStackTrace();
            }
        }
    }

    boolean isSend = true;

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
        isSend = false;
        isReceive = false;
        if (mBlueAdapter != null)
            mBlueAdapter.cancelDiscovery();
        if (mClientThread != null) {
            mClientThread.interrupt();
            mClientThread = null;
        }
        if (mSendThread != null) {
            mSendThread.interrupt();
            mSendThread = null;
        }
        if (mServiceSocket != null) {
            try {
                mServiceSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mServiceSocket = null;
        }
        if (mSocket != null && mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = null;
        }
        if (mInput != null) {
            try {
                mInput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInput = null;
        }
        if (mOutput != null) {
            try {
                mOutput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mOutput = null;
        }
        super.onDestroy();
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

    private void tosUtil(String str) {
        Message message = mHandler.obtainMessage();
        message.obj = str;
        message.what = MSG;
        mHandler.sendMessage(message);
    }

    @Override
    public boolean stopService(Intent name) {
        LogUtil.d(TAG, "stopService");
        return super.stopService(name);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        LogUtil.d(TAG, "unbindService");
        super.unbindService(conn);
    }

    public boolean bindDev(String address) {
        Boolean returnValue = false;
        BluetoothDevice remoteDevice = mBlueAdapter.getRemoteDevice(address);
        if (remoteDevice != null) {
            Method createBondMethod = null;
            try {
                createBondMethod = BluetoothDevice.class.getMethod("createBond");
                returnValue = (Boolean) createBondMethod.invoke(remoteDevice);

            } catch (ReflectiveOperationException e) {
                ToastUtil.showToast(BluetoothService.this, getString(R.string.bind_error), 3000);
                e.printStackTrace();
            }
        } else {
            ToastUtil.showToast(BluetoothService.this, getString(R.string.bind_error), 3000);
        }
        return returnValue.booleanValue();
    }

}
