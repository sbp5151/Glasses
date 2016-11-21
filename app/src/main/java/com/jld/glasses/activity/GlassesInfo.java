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
import static com.jld.glasses.R.id.textView;

public class GlassesInfo extends BaseActivity implements View.OnClickListener, Conn.MyServiceConnection {

    private TextView mDevName;
    private TextView mDevSn;
    private TextView mDevVersions;
    private TextView mAdVersions;
    private TextView mStorageSize;
    private TextView mStorageResidue;
    private TextView mStorageElectric;
    private Button mChange;
    private JSONObject mJson;
    private BluetoothService.myBinder mBinder;

    private Handler mServiceHandler;
    private Conn mConn;
    private Gson mGson;
    public static final String TAG = "GlassesInfoItem";
    public static final int DIALOG_DISMESS = 0x31;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case RECEIVE_MESSAGE:
                    String obj = (String) msg.obj;
                    LogUtil.d(TAG, "str:" + obj);
                    try {
                        JSONObject mJson = new JSONObject(obj);
                        String mNumber = mJson.getString("number");
                        if ("glasses_03".equals(mNumber)) {
                            String mResult = mJson.getString("result");
                            String str;
                            if ("1".equals(mResult)) {//修改成功
                                str = getString(R.string.change_win);
                                if (mDevName != null) {
                                    SettingActivity.mBt_name = mChangeName;
                                    mDevName.setText(mChangeName);
                                }
                            } else {//修改失败
                                str = getString(R.string.change_fail);
                            }
                            ToastUtil.showToast(GlassesInfo.this, str, 3000);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mHandler.sendEmptyMessage(DIALOG_DISMESS);
                    break;
                case DIALOG_DISMESS:
                    if (mDialog != null && mDialog.isShowing())
                        mDialog.dismiss();
                    break;
            }
        }
    };
    private Dialog mDialog;
    private String mChangeName;
    private AlertDialog mChangeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glasses_info);
        Intent intent = getIntent();
        String json = intent.getStringExtra("mJson");
        //将activity添加到application里面，以便finish
        MyApplication.setSubActivity(this);
        mGson = new Gson();
        try {
            mJson = new JSONObject(json);
            initView();
        } catch (JSONException e) {
            ToastUtil.showToast(this, getString(R.string.load_fail), 3000);
            e.printStackTrace();
        }
        Intent mIntent = new Intent(this, BluetoothService.class);
        mConn = new Conn(this);
        bindService(mIntent, mConn, Context.BIND_AUTO_CREATE);
        mDialog = DialogUtil.createLoadingDialog(this, getString(R.string.dialog_text), false);
    }

    public void initView() throws JSONException {
        //titleBar
        View mTitleBar = findViewById(R.id.glasses_info_title_bar);
        TextView mTitleName = (TextView) mTitleBar.findViewById(R.id.tv_bar_title_name);
        mTitleName.setText(getString(R.string.glasses_info));
        LinearLayout mLinearLayout = (LinearLayout) mTitleBar.findViewById(R.id.linearLayout);
        ImageView mBack = (ImageView) mTitleBar.findViewById(R.id.iv_bar_back);
        TextView mTextView = (TextView) mTitleBar.findViewById(textView);
        mTextView.setVisibility(View.VISIBLE);
        mBack.setVisibility(View.VISIBLE);
        mLinearLayout.setOnClickListener(this);

        Button mRight = (Button) mTitleBar.findViewById(R.id.btn_bar_right);
        mRight.setVisibility(View.INVISIBLE);
        //content
//        ListView infos = (ListView) findViewById(R.id.lv_glasses_info);

        //设备名称
        mDevName = (TextView) findViewById(R.id.tv_dev_name);
        mDevName.setText(mJson.getString("dev_name"));
        //修改名称
        mChange = (Button) findViewById(R.id.btn_change_name);
        mChange.setOnClickListener(this);
        //设备SN
        mDevSn = (TextView) findViewById(R.id.tv_dev_sn);
        mDevSn.setText(mJson.getString("dev_sn"));

        //硬件版本
        mDevVersions = (TextView) findViewById(R.id.tv_dev_versions);
        mDevVersions.setText(mJson.getString("hw_version"));

        //安卓版本
        mAdVersions = (TextView) findViewById(R.id.tv_android_versions);
        mAdVersions.setText(mJson.getString("ad_version"));

        //存储空间
        mStorageSize = (TextView) findViewById(R.id.tv_storage_size);
        mStorageSize.setText(mJson.getString("memory_size"));

        //剩余空间
        mStorageResidue = (TextView) findViewById(R.id.tv_storage_residue);
        mStorageResidue.setText(mJson.getString("usable_mem"));

        //剩余电量
        mStorageElectric = (TextView) findViewById(R.id.tv_storage_electric);
        mStorageElectric.setText(mJson.getString("usable_ele"));
        //连接对象
        TextView tv_connect = (TextView) findViewById(R.id.tv_connect_name);
        tv_connect.setText(getString(R.string.connect_dev) + SettingActivity.mBt_name);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_change_name:
                changeDialog();
                break;
            case R.id.linearLayout:
                finish();
                break;
        }
    }

    public void changeDialog() {
        LayoutInflater inflater = LayoutInflater.from(GlassesInfo.this);
        LogUtil.d(TAG, "inflater:" + inflater);
        View view = inflater.inflate(R.layout.dialog_glasses_setting, null);
        LogUtil.d(TAG, "view:" + view);

        final EditText devName = (EditText) view.findViewById(R.id.et_dev_name);
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
                String name = devName.getText().toString();
                if (TextUtils.isEmpty(name)) {
                    ToastUtil.showToast(GlassesInfo.this, getString(R.string.input_glasses_name), 3000);
                } else {
                    mChangeDialog.dismiss();
                    mChangeName = name;
                    changeWifi(name);
                }
            }
        });
        mChangeDialog = new AlertDialog.Builder(this, R.style.CustomDialog).create();
        mChangeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);//无标题栏
        mChangeDialog.setCanceledOnTouchOutside(false);//触摸不消失
//        dialog.setContentView(view, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
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


    public void changeWifi(String mChangeName) {
        if (!TextUtils.isEmpty(mChangeName)) {
            if (mServiceHandler != null) {//发送修改设备名称信息

                HashMap<Object, Object> mMap = new HashMap<>();
                mMap.put("number", "phone_03");
                mMap.put("dev_name", mChangeName);
                String mJson = mGson.toJson(mMap);
                Message message = mServiceHandler.obtainMessage();
                message.obj = mJson;
                message.what = BluetoothService.SEND_MESSAGE;
                mServiceHandler.sendMessage(message);
                mDialog.show();
                mHandler.sendEmptyMessageDelayed(DIALOG_DISMESS, 1000 * 6);
            } else {
                ToastUtil.showToast(GlassesInfo.this, getString(R.string.bind_error), 3000);
            }
        } else
            ToastUtil.showToast(GlassesInfo.this, getString(R.string.input_name), 3000);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mBinder = (BluetoothService.myBinder) iBinder;
        mBinder.setHandler(mHandler);
        mServiceHandler = mBinder.getHandler();
    }

    @Override
    protected void onDestroy() {
        if (mConn != null)
            unbindService(mConn);
        super.onDestroy();

    }
}
