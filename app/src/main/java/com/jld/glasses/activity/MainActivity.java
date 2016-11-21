package com.jld.glasses.activity;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.zxing.WriterException;
import com.jld.glasses.BluetoothReceiver;
import com.jld.glasses.BluetoothService;
import com.jld.glasses.adapter.MainListAdapter;
import com.jld.glasses.R;
import com.jld.glasses.model.MainItem;
import com.jld.glasses.util.Constants;
import com.jld.glasses.util.DialogUtil;
import com.jld.glasses.util.LogUtil;
import com.jld.glasses.util.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static com.jld.glasses.BluetoothReceiver.CONNECT_CHANGE;
import static com.jld.glasses.BluetoothService.BLUE_DISCONNECT;
import static com.jld.glasses.BluetoothService.SEND_MESSAGE;
import static com.jld.glasses.util.createCode.encodeAsBitmap;


public class MainActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private BluetoothService.myBinder mMyBinder;
    public static final String TAG = "MainActivity";
    public static final int SET_BLUE = 0x11;
    public static final int SET_BLUE2 = 0x12;
    public static final int RET_DEV_NAME = 0x13;
    public static final int DIALOG_DISMISS = 0x14;
    public static final int SERVICE_CODE = 0x15;
    public static final int REFRESH_FINISH = 0x16;
    public static final int REFRESH = 0x17;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case SET_BLUE:
                    setBluetooth();
                    break;
                case RET_DEV_NAME:
                    if (mAdapter != null) {
                        MainItem item = (MainItem) msg.obj;
                        mAdapter.addItem(item);
                        mItems = mAdapter.getItems();
                    }
                    break;
                case DIALOG_DISMISS:
                    if (mConnect_dialog != null && mConnect_dialog.isShowing())
                        mConnect_dialog.dismiss();
                    break;
                case SERVICE_CODE:
                    int arg1 = msg.arg1;
                    if (arg1 == 2) {//连接蓝牙设备成功，跳转页面
                        Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                        intent.putExtra("bt_name", "");
                        startActivity(intent);
                        finish();
                    } else if (arg1 == 3) {//设备连接失败
                        if (mConnect_dialog != null && mConnect_dialog.isShowing())
                            mConnect_dialog.dismiss();
                    }
                    break;
                case BluetoothService.RECEIVE_MESSAGE:
                    if (mConnect_dialog != null && mConnect_dialog.isShowing())
                        mConnect_dialog.dismiss();
                    String str = (String) msg.obj;
                    try {
                        JSONObject jsonObject = new JSONObject(str);
                        String number = jsonObject.getString("number");
                        String bt_name = jsonObject.getString("bt_name");
                        if ("glasses_00".equals(number)) {
                            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                            intent.putExtra("bt_name", bt_name);
                            startActivity(intent);
                            finish();
                            return;
                        } else if (("glasses_01".equals(number))) {//接收主动连接，发送回馈消息
                            HashMap<Object, Object> mMap = new HashMap<>();
                            mMap.put("number", "phone_01");
                            Gson mGson = new Gson();
                            String s = mGson.toJson(mMap);
                            Message message = mServiceHandler.obtainMessage();
                            message.obj = s;
                            message.what = SEND_MESSAGE;
                            mServiceHandler.sendMessage(message);
                            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                            intent.putExtra("bt_name", bt_name);
                            startActivity(intent);
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ToastUtil.showToast(MainActivity.this, getString(R.string.connect_exception), 300);
                    break;
                case BLUE_DISCONNECT://蓝牙设备断开
                    mHandler.sendEmptyMessage(DIALOG_DISMISS);
                    isBlueDisconnect = true;
                    break;
                case CONNECT_CHANGE:
                    if (mServiceHandler == null)
                        initService();
                    break;
                case REFRESH_FINISH://蓝牙设备刷新完成
                    mProgressBar.setVisibility(View.GONE);
                    mBtnRefresh.setVisibility(View.VISIBLE);
                    break;
                case REFRESH://刷新蓝牙设备
                    if (!mBluetoothAdapter.isDiscovering()) {
                        deleteDev();
                        mBluetoothAdapter.startDiscovery();
                        mProgressBar.setVisibility(View.VISIBLE);
                        mBtnRefresh.setVisibility(View.GONE);
                    }
                    break;
            }
        }
    };
    private conn mConn;
    private ArrayList<MainItem> mItems;
    private MainListAdapter mAdapter;
    private Handler mServiceHandler;
    private Dialog mConnect_dialog;
    private SharedPreferences mSp;
    private int mDpi;
    private boolean isBlueDisconnect;//蓝牙设备是否断开
    private ProgressBar mProgressBar;
    private Button mBtnRefresh;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSp = getSharedPreferences(Constants.SHARE_KEY, Context.MODE_PRIVATE);
        DisplayMetrics metric = new DisplayMetrics();
        mDpi = metric.densityDpi;
        initView();
        BluetoothReceiver.sendHandler(mHandler);
        mHandler.sendEmptyMessageDelayed(SET_BLUE,500);//防止点击断开连接service没有destroy
    }

    private void initView() {
        View mTitle = findViewById(R.id.main_title_bar);
        TextView mTvTitle = (TextView) mTitle.findViewById(R.id.tv_bar_title_name);
        mProgressBar = (ProgressBar) mTitle.findViewById(R.id.pb_refresh);
        mTvTitle.setText(getString(R.string.app_name));
        mBtnRefresh = (Button) mTitle.findViewById(R.id.btn_bar_right);
        mBtnRefresh.setVisibility(View.VISIBLE);
        mBtnRefresh.setOnClickListener(this);

        ListView mListView = (ListView) findViewById(R.id.lv_main);
        mItems = new ArrayList<>();
        mItems.add(new MainItem(getString(R.string.my_code), "0", ""));
        mItems.add(new MainItem(getString(R.string.last_connect), "1", mSp.getString(Constants.CONNECT_DEV_NAME, "")));
        mItems.add(new MainItem(getString(R.string.already_dev), "2", ""));
        mItems.add(new MainItem(getString(R.string.unready_dev), "4", ""));
        mAdapter = new MainListAdapter(mItems, this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }

    /**
     * 判断蓝牙是否打开
     */
    private void setBluetooth() {
        LogUtil.d(TAG, "setBluetooth:");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {  //Device support Bluetooth
            //确认开启蓝牙
            if (!mBluetoothAdapter.isEnabled()) {
                //请求用户开启
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, RESULT_FIRST_USER);
                //使蓝牙设备可见，方便配对
                Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
                startActivity(in);
            } else {
                initService();
                startDiscovery(false);
            }
        }
    }

    private void initService() {
        LogUtil.d(TAG, "initService");
        Intent intent_start = new Intent(this, BluetoothService.class);
        Intent intent_bind = new Intent(this, BluetoothService.class);
        startService(intent_start);
        mConn = new conn();
        bindService(intent_bind, mConn, Context.BIND_AUTO_CREATE);
    }

    private boolean isCanceled;//用户是否取消

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d(TAG, "requestCode:" + requestCode);
        LogUtil.d(TAG, "resultCode:" + resultCode);
        if (requestCode == RESULT_FIRST_USER) {
            if (resultCode == RESULT_OK) {  // YES 用户允许
                initService();
                isCanceled = false;
            }
            if (resultCode == RESULT_CANCELED) {// NO 用户取消
                isCanceled = true;
            }
        }
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_bar_right://刷新蓝牙设备
                if (mServiceHandler == null) {
                    if (!isCanceled)
                        ToastUtil.showToast(MainActivity.this, getString(R.string.dialog_text), 3000);
                    else
                        ToastUtil.showToast(MainActivity.this, getString(R.string.open_bluetooth), 3000);
                } else {
                    startDiscovery(true);
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mItems = mAdapter.getItems();
        MainItem item = mItems.get(i);
        String type = item.getType();
        LogUtil.d(TAG, "onItemClick:" + item.getName());
        switch (type) {
            case "0"://我的二维码
                String mAddress = mSp.getString(Constants.MY_DEV_ADDRESS, "");
                LogUtil.d(TAG, "mAddress:" + mAddress);
                if (!TextUtils.isEmpty(mAddress)) {
                    try {
                        Bitmap image = encodeAsBitmap(mAddress, 230);
                        showCodeDialog(image);
                    } catch (WriterException e) {
                        ToastUtil.showToast(this, "解析错误", 300);
                        e.printStackTrace();
                    }
                } else
                    ToastUtil.showToast(MainActivity.this, getString(R.string.dialog_text), 3000);
                break;
            case "1"://上次连接
                String address = mSp.getString(Constants.CONNECT_DEV_ADDRESS, "");
                if (TextUtils.isEmpty(address)) {
                    ToastUtil.showToast(MainActivity.this, getString(R.string.last_connect_numm), 3000);
                    return;
                }
                if (mServiceHandler == null) {
                    if (!isCanceled)
                        ToastUtil.showToast(MainActivity.this, getString(R.string.dialog_text), 3000);
                    else
                        ToastUtil.showToast(MainActivity.this, getString(R.string.open_bluetooth), 3000);
                    return;
                }
                Message message = mServiceHandler.obtainMessage();
                message.obj = address;
                message.what = BluetoothService.CONNECT_DEV;
                mServiceHandler.sendMessage(message);
                mConnect_dialog = DialogUtil.createLoadingDialog(MainActivity.this, getString(R.string.dialog_text), false);
                mConnect_dialog.show();
                break;
            case "3"://已配对设备
                if (mServiceHandler == null) {
                    if (!isCanceled)
                        ToastUtil.showToast(MainActivity.this, getString(R.string.dialog_text), 3000);
                    else
                        ToastUtil.showToast(MainActivity.this, getString(R.string.open_bluetooth), 3000);
                    return;
                }
                Message message1 = mServiceHandler.obtainMessage();
                message1.obj = item.getAddress();
                message1.what = BluetoothService.CONNECT_DEV;
                mServiceHandler.sendMessage(message1);
                mConnect_dialog = DialogUtil.createLoadingDialog(MainActivity.this, getString(R.string.dialog_text), false);
                mConnect_dialog.show();
                break;
            case "5"://未配对设备
                if (mServiceHandler == null) {
                    if (!isCanceled)
                        ToastUtil.showToast(MainActivity.this, getString(R.string.dialog_text), 3000);
                    else
                        ToastUtil.showToast(MainActivity.this, getString(R.string.open_bluetooth), 3000);
                    return;
                }
                Message message2 = mServiceHandler.obtainMessage();
                message2.obj = item.getAddress();
                message2.what = BluetoothService.BIND_DEV;
                mServiceHandler.sendMessage(message2);
                mItems.get(i).setPair(true);//正在配对
                mAdapter.notifyDataSetChanged();
                break;

        }
    }

    /**
     * 绑定service
     */
    private class conn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mMyBinder = (BluetoothService.myBinder) iBinder;
            mMyBinder.setHandler(mHandler);
            mServiceHandler = mMyBinder.getHandler();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    }

    public void showCodeDialog(Bitmap bitmap) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.code_layout, null);
        ImageView iv_code = (ImageView) view.findViewById(R.id.iv_code);
        iv_code.setImageBitmap(bitmap);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialog).create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setView(view);
        dialog.show();
    }

    /**
     * 清空所有蓝牙设备
     */
    public void deleteDev() {
        mItems = mAdapter.getItems();
        for (int i = 0; i < mItems.size(); i++) {
            MainItem item = mItems.get(i);
            if ("3".equals(item.getType()) || "5".equals(item.getType())) {
                mItems.remove(item);
                i--;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                ToastUtil.showToast(this, getString(R.string.open_bluetooth), 3000);
                return;
            } else {//刷新蓝牙设备
                startDiscovery(false);
            }
        }
    }

    /**
     * 刷新蓝牙设备
     */
    public void startDiscovery(boolean isClick) {
        if (mBluetoothAdapter.isDiscovering()&&isClick) {
            ToastUtil.showToast(this, getString(R.string.dev_refresh_ing), 3000);
        } else {
            deleteDev();
            mBluetoothAdapter.startDiscovery();
            mProgressBar.setVisibility(View.VISIBLE);
            mBtnRefresh.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConn != null)
            unbindService(mConn);
    }
}
