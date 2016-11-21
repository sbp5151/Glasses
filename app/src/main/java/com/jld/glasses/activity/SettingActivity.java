package com.jld.glasses.activity;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jld.glasses.BluetoothReceiver;
import com.jld.glasses.BluetoothService;
import com.jld.glasses.R;
import com.jld.glasses.util.Conn;
import com.jld.glasses.util.Constants;
import com.jld.glasses.util.DialogUtil;
import com.jld.glasses.util.LogUtil;
import com.jld.glasses.util.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import static com.jld.glasses.BluetoothService.BLUE_DISCONNECT;
import static com.jld.glasses.BluetoothService.BREAK_CONNECT;
import static com.jld.glasses.BluetoothService.RECEIVE_MESSAGE;
import static com.jld.glasses.R.id.btn_disconnect;
import static com.jld.glasses.activity.MainActivity.SERVICE_CODE;

public class SettingActivity extends BaseActivity implements View.OnClickListener, Conn.MyServiceConnection {

    public static final String TAG = "SettingActivity";
    private BluetoothService.myBinder mBinder;
    public static final int DISCONNECT = 0x21;
    public static final int CONNECT_ERROR = 0x22;
    public static final int DIALOG_DISMISS = 0x23;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case RECEIVE_MESSAGE:
                    String str = (String) msg.obj;
                    LogUtil.d(TAG, "str:" + str);
                    try {
                        JSONObject mJson = new JSONObject(str);
                        String number = mJson.getString("number");
                        switch (number) {
                            case "glasses_02"://眼镜信息请求返回
                                toActivity(GlassesInfo.class, str);
                                break;
                            case "glasses_00"://重新连接成功
                                mHandler.sendEmptyMessage(DIALOG_DISMISS);
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case DISCONNECT://断开连接
                    Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case CONNECT_ERROR://连接错误
                    if (!isDisconnect)
                        againConnectDialog();
                    break;
                case SERVICE_CODE:
                    int arg1 = msg.arg1;
                    if (arg1 == 3 && isAgain && mConnect_dialog.isShowing()) {//设备重新连接失败
                        mConnect_dialog.dismiss();
                        mHandler.sendEmptyMessage(btn_disconnect);
                        Intent intent1 = new Intent(SettingActivity.this, MainActivity.class);
                        startActivity(intent1);
                        finish();
                    }
                    break;
                case DIALOG_DISMISS:
                    if (mConnect_dialog != null && mConnect_dialog.isShowing()) {
                        mConnect_dialog.dismiss();
                    }
                    break;
                case BLUE_DISCONNECT://蓝牙被关闭
                    mHandler.sendEmptyMessage(DIALOG_DISMISS);
                    break;

            }
        }
    };
    private Handler mServiceHandler;
    public static String mBt_name = "";
    private Gson mGson;
    private Dialog mDialog;
    private Conn mConn;
    private TextView mTv_name;
    private AlertDialog mChangeDialog;
    private SharedPreferences mSp;
    private Dialog mConnect_dialog;
    private boolean isDisconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_setting);
        Intent mIntent = getIntent();
        mBt_name = mIntent.getStringExtra("bt_name");
        mGson = new Gson();
        mSp = getSharedPreferences(Constants.SHARE_KEY, Context.MODE_PRIVATE);

        initView();
        Intent intent = new Intent(this, BluetoothService.class);
        mConn = new Conn(this);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        mDialog = DialogUtil.createLoadingDialog(SettingActivity.this, getString(R.string.dialog_text), false);
    }


    public void initView() {
//      titleBar
        View mTitleBar = findViewById(R.id.connect_title_bar);
        ImageView iv_back = (ImageView) mTitleBar.findViewById(R.id.iv_bar_back);
        iv_back.setVisibility(View.INVISIBLE);
        TextView mBarName = (TextView) mTitleBar.findViewById(R.id.tv_bar_title_name);
        mBarName.setText(getString(R.string.app_name));
        ImageView mBack = (ImageView) mTitleBar.findViewById(R.id.iv_bar_back);
        mBack.setOnClickListener(this);

//      content
        mTv_name = (TextView) findViewById(R.id.tv_dev_name);
        Button btn_glasses = (Button) findViewById(R.id.btn_glasses_info);
        btn_glasses.setOnClickListener(this);
        Button btn_wifi = (Button) findViewById(R.id.btn_wifi_setting);
        btn_wifi.setOnClickListener(this);
        Button btn_call = (Button) findViewById(R.id.btn_call_setting);
        btn_call.setOnClickListener(this);
        Button btn_connect = (Button) findViewById(btn_disconnect);
        btn_connect.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        HashMap<Object, Object> mMap = new HashMap<>();
        String json;
        switch (id) {
            case R.id.btn_glasses_info://眼镜信息
                mMap.put("number", "phone_02");
                json = mGson.toJson(mMap);
                mySendMessage(json);
                mDialog.show();
                break;
            case R.id.btn_wifi_setting://wifi设置
                Intent intent = new Intent(SettingActivity.this, WifiSetting.class);
                startActivity(intent);
                break;
            case R.id.btn_call_setting://呼叫设置
                Intent intent1 = new Intent(SettingActivity.this, CallActivity.class);
                startActivity(intent1);
                break;
            case btn_disconnect://断开连接
                isDisconnect = true;
                mServiceHandler.sendEmptyMessage(BREAK_CONNECT);
                finish();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        BluetoothReceiver.sendHandler(mHandler);
        if (mBinder != null) {
            mBinder.setHandler(mHandler);
        } else if (mConn == null) {
            Intent intent = new Intent(this, BluetoothService.class);
            mConn = new Conn(this);
            bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        }
        mTv_name.setText(getString(R.string.connect_dev) + mBt_name);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mBinder = (BluetoothService.myBinder) iBinder;
        mBinder.setHandler(mHandler);
        mBinder.sendMainHandler(mHandler);
        mServiceHandler = mBinder.getHandler();
    }

    public void mySendMessage(String msg) {
        Message message = mServiceHandler.obtainMessage();
        message.what = BluetoothService.SEND_MESSAGE;
        message.obj = msg;
        mServiceHandler.sendMessage(message);
    }

    /**
     * 右进左出的动画方式跳转到目标activity
     */
    protected void toActivity(Class<?> aClass, String json) {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
        Intent intent = new Intent(this, aClass);
        intent.putExtra("mJson", json);
        startActivity(intent);
        overridePendingTransition(R.anim.right_in, R.anim.left_out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.d(TAG, "onStop");
    }


    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
        if (mConn != null)
            unbindService(mConn);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    //用户是否点击重新连接蓝牙设备
    boolean isAgain = false;
    /**
     * 重新连接蓝牙设备dialog
     */
    public void againConnectDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        LogUtil.d(TAG, "inflater:" + inflater);
        View view = inflater.inflate(R.layout.dialog_again_connect, null);
        LogUtil.d(TAG, "view:" + view);
        Button btnCancel = (Button) view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChangeDialog.dismiss();
                mHandler.sendEmptyMessage(btn_disconnect);
                Intent intent1 = new Intent(SettingActivity.this, MainActivity.class);
                startActivity(intent1);
                finish();
            }
        });
        Button btnConfirm = (Button) view.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChangeDialog.dismiss();
                isAgain = true;
                String address = mSp.getString(Constants.CONNECT_DEV_ADDRESS, "");
                if (TextUtils.isEmpty(address)) {
                    ToastUtil.showToast(SettingActivity.this, getString(R.string.last_connect_numm), 3000);
                    return;
                }
                if (mServiceHandler == null) {
                    ToastUtil.showToast(SettingActivity.this, getString(R.string.dialog_text), 3000);
                    return;
                }
                Message message = mServiceHandler.obtainMessage();
                message.obj = address;
                message.what = BluetoothService.AGAIN_CONNECT_DEV;
                mServiceHandler.sendMessage(message);
                mConnect_dialog = DialogUtil.createLoadingDialog(SettingActivity.this, getString(R.string.dialog_text), false);
                mConnect_dialog.setCancelable(false);
                mConnect_dialog.show();
            }
        });
        mChangeDialog = new AlertDialog.Builder(this, R.style.CustomDialog).create();
        mChangeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);//无标题栏
        mChangeDialog.setCancelable(false);//返回键不消失
        mChangeDialog.setView(view);
//        Window window = mChangeDialog.getWindow();
//        mChangeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);//指定会全局,可以在后台弹出
//        // 设置显示动画
//        //window.setWindowAnimations(R.style.main_menu_animstyle);
//        WindowManager.LayoutParams wl = window.getAttributes();
//        wl.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mChangeDialog.show();
    }
}
