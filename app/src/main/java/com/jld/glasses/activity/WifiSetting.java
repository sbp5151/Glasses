package com.jld.glasses.activity;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jld.glasses.BluetoothService;
import com.jld.glasses.MyApplication;
import com.jld.glasses.R;
import com.jld.glasses.util.Conn;
import com.jld.glasses.util.DialogUtil;
import com.jld.glasses.util.LogUtil;
import com.jld.glasses.util.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import static com.jld.glasses.BluetoothService.RECEIVE_MESSAGE;

public class WifiSetting extends AppCompatActivity implements View.OnClickListener,Conn.MyServiceConnection{

    private TextView mConnectState;
    private TextView mSSID;
    private Handler mServiceHandler;
    public static final String TAG = "WifiSetting";

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case RECEIVE_MESSAGE:
                    String str = (String) msg.obj;
                    try {
                        JSONObject json = new JSONObject(str);
                        String number = json.getString("number");
                        String state = json.getString("state");
                        String result = json.getString("result");
                        if ("glasses_04".equals(number) && "1".equals(state)) {//wifi请求返回
                            if (TextUtils.isEmpty(result)) {
                                mConnectState.setText(getString(R.string.unConnect));
                            } else {
                                mConnectState.setText(getString(R.string.connected));
                                mSSID.setText(result);
                            }
                            if (mDialog != null && mDialog.isShowing())
                                mDialog.dismiss();
                        }
                        if ("glasses_04".equals(number) && "0".equals(state)) {//wifi请求返回
                            if ("1".equals(result)) {
                                mConnectState.setText(getString(R.string.connected));
                                mSSID.setText(wifiName);
                            } else {
                                ToastUtil.showToast(WifiSetting.this, getString(R.string.wifi_set_fail), 3000);
                            }
                            if (mSetDialog != null && mSetDialog.isShowing())
                                mSetDialog.dismiss();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };
    private Conn mConn;
    private Gson mGson;
    private Dialog mDialog;
    private Dialog mSetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setting);
        //将activity添加到application里面，以便finish
        MyApplication.setSubActivity(this);
        initView();
        mGson = new Gson();
        //绑定蓝牙服务器
        mConn = new Conn(this);
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        mDialog = DialogUtil.createLoadingDialog(this, getString(R.string.load_ing), false);
        mDialog.show();
    }

    public void initView() {
//      titleBar
        View mTitleBar = findViewById(R.id.setting_title_bar);
        TextView mBarName = (TextView) mTitleBar.findViewById(R.id.tv_bar_title_name);
        mBarName.setText(getString(R.string.wifi_info));

        LinearLayout mLinearLayout = (LinearLayout) mTitleBar.findViewById(R.id.linearLayout);
        TextView mTextView = (TextView) mTitleBar.findViewById(R.id.textView);
        ImageView mBack = (ImageView) mTitleBar.findViewById(R.id.iv_bar_back);
        mTextView.setVisibility(View.VISIBLE);
        mBack.setVisibility(View.VISIBLE);
        mLinearLayout.setOnClickListener(this);

        mConnectState = (TextView) findViewById(R.id.tv_connect_state);
        mSSID = (TextView) findViewById(R.id.tv_ssid);
        Button mBtnSetting = (Button) findViewById(R.id.btn_wifi_setting);
        mBtnSetting.setOnClickListener(this);
        TextView mDevName = (TextView) findViewById(R.id.tv_dev_name);
        mDevName.setText(getString(R.string.connect_dev) + SettingActivity.mBt_name);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        BluetoothService.myBinder mBinder = (BluetoothService.myBinder) iBinder;
        mBinder.setHandler(mHandler);
        mServiceHandler = mBinder.getHandler();
        //发送wifi请求信息
        HashMap<Object, Object> mMap = new HashMap<>();
        mMap.put("number", "phone_04");
        mMap.put("state", "1");
        String json = mGson.toJson(mMap);
        mySendMessage(json);
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.linearLayout:
                finish();
                break;
            case R.id.btn_wifi_setting:
                changeDialog();
                break;
        }
    }

    public AlertDialog mChangeDialog;
    public String wifiName;

    public void changeDialog() {
        LayoutInflater inflater = LayoutInflater.from(WifiSetting.this);
        LogUtil.d(TAG, "inflater:" + inflater);
        View view = inflater.inflate(R.layout.dialog_wifi_setting, null);
        LogUtil.d(TAG, "view:" + view);

        final EditText ssid = (EditText) view.findViewById(R.id.et_set_ssid);
        final EditText password = (EditText) view.findViewById(R.id.et_set_password);
        Button btnCancel = (Button) view.findViewById(R.id.btn_cancel);
        ImageButton close = (ImageButton) view.findViewById(R.id.ib_dialog_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChangeDialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChangeDialog.dismiss();
            }
        });
        Button btnConfirm = (Button) view.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiName = ssid.getText().toString();
                String sPassword = password.getText().toString();
                if (TextUtils.isEmpty(wifiName)) {
                    ToastUtil.showToast(WifiSetting.this, getString(R.string.input_ssid), 3000);
                    return;
                }
                if (TextUtils.isEmpty(sPassword)) {
                    ToastUtil.showToast(WifiSetting.this, getString(R.string.input_password), 3000);
                    return;
                }
                mChangeDialog.dismiss();
                HashMap<Object, Object> map = new HashMap<>();
                map.put("number", "phone_04");
                map.put("ssid", wifiName);
                map.put("password", password.getText().toString());
                map.put("state", "0");
                String json = mGson.toJson(map);
                mySendMessage(json);
                mSetDialog = DialogUtil.createLoadingDialog(WifiSetting.this, getString(R.string.load_ing), false);
                mSetDialog.show();
            }
        });
        mChangeDialog = new AlertDialog.Builder(this,R.style.CustomDialog).create();
        mChangeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);//无标题栏
//        mChangeDialog.setContentView(view, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
//                RelativeLayout.LayoutParams.MATCH_PARENT));
        mChangeDialog.setView(view);
        Window window = mChangeDialog.getWindow();
        // 设置显示动画
        //window.setWindowAnimations(R.style.main_menu_animstyle);
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mChangeDialog.show();
    }

    public void mySendMessage(String str) {
        Message message = mServiceHandler.obtainMessage();
        message.what = BluetoothService.SEND_MESSAGE;
        message.obj = str;
        mServiceHandler.sendMessage(message);
    }

    @Override
    protected void onDestroy() {
        if(mConn!=null)
            unbindService(mConn);
        super.onDestroy();
    }
}
