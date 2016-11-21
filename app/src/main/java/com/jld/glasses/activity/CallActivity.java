package com.jld.glasses.activity;

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
import com.jld.glasses.util.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import static com.jld.glasses.BluetoothService.RECEIVE_MESSAGE;

public class CallActivity extends AppCompatActivity implements Conn.MyServiceConnection {

    private Conn mConn;
    private Handler mServiceHandler;
    public static final String TAG = "CallActivity";
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case RECEIVE_MESSAGE:
                    String obj = (String) msg.obj;
                    try {
                        JSONObject jsonObject = new JSONObject(obj);
                        String number = jsonObject.getString("number");
                        String State = jsonObject.getString("state");
                        String Result = jsonObject.getString("result");
                        if ("glasses_05".equals(number) && "1".equals(State)) {//请求

                            if (!TextUtils.isEmpty(Result)) {
                                mTv_number.setText(Result);
                            } else {
                                mTv_number.setText(getString(R.string.current_number_null));
                            }
                        } else if ("glasses_05".equals(number) && "0".equals(State)) {//设置
                            if (!TextUtils.isEmpty(Result) && "1".equals(Result)) {
                                ToastUtil.showToast(CallActivity.this, getString(R.string.change_win), 3000);
                                mTv_number.setText(mChangeNum);
                            } else {
                                ToastUtil.showToast(CallActivity.this, getString(R.string.change_fail), 3000);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };
    private BluetoothService.myBinder mBinder;
    private Gson mGson;
    private TextView mTv_number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        //将activity添加到application里面，以便finish
        MyApplication.setSubActivity(this);
        mGson = new Gson();
        mConn = new Conn(this);
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        initView();
    }

    public void initView() {
        View mTitleBar = findViewById(R.id.call_title_bar);
        LinearLayout mLinearLayout = (LinearLayout) mTitleBar.findViewById(R.id.linearLayout);
        TextView mTextView = (TextView) mTitleBar.findViewById(R.id.textView);
        ImageView mBack = (ImageView) mTitleBar.findViewById(R.id.iv_bar_back);
        mTextView.setVisibility(View.VISIBLE);
        mBack.setVisibility(View.VISIBLE);
        mLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        TextView dev_name = (TextView) findViewById(R.id.tv_dev_name);
        dev_name.setText(getString(R.string.connect_dev) + SettingActivity.mBt_name);
        mTv_number = (TextView) findViewById(R.id.tv_current_number);
        Button btn_set = (Button) findViewById(R.id.btn_call_setting);
        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeDialog();
            }
        });
    }

    public String mChangeNum;
    AlertDialog mChangeDialog;

    public void changeDialog() {
        LayoutInflater inflater = LayoutInflater.from(CallActivity.this);
        View view = inflater.inflate(R.layout.dialog_glasses_setting, null);
        TextView mTitleName = (TextView) view.findViewById(R.id.tv_dialog_title_name);
        mTitleName.setText(R.string.call_dialog_title_name);
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
                mChangeNum = devName.getText().toString();
                if (!TextUtils.isEmpty(mChangeNum)) {
                    mChangeDialog.dismiss();
                    HashMap<Object, Object> map = new HashMap<>();
                    map.put("number", "phone_05");
                    map.put("call_number", mChangeNum);
                    map.put("state", "0");
                    mySendMessage(mGson.toJson(map));
                } else {
                    ToastUtil.showToast(CallActivity.this, getString(R.string.input_num), 3000);
                }
            }
        });
        mChangeDialog = new AlertDialog.Builder(this, R.style.CustomDialog).create();
        mChangeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);//无标题栏
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

    public void mySendMessage(String str) {
        Message message = mServiceHandler.obtainMessage();
        message.what = BluetoothService.SEND_MESSAGE;
        message.obj = str;
        mServiceHandler.sendMessage(message);
    }

    //绑定service
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mBinder = (BluetoothService.myBinder) iBinder;
        mBinder.setHandler(mHandler);
        mServiceHandler = mBinder.getHandler();
        //请求当前号码
        HashMap<Object, Object> map = new HashMap<>();
        map.put("number", "phone_05");
        map.put("state", "1");
        mySendMessage(mGson.toJson(map));
    }

    //解绑service
    @Override
    protected void onDestroy() {
        if (mConn != null)
            unbindService(mConn);
        super.onDestroy();
    }
}
