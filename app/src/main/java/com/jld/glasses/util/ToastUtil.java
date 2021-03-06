package com.jld.glasses.util;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;


/**
 * Toast工具 防止toast点击多次显示
 */
public class ToastUtil {
    private static Toast mToast;
    private static Toast mCustomToast;
    private static Handler mHandler = new Handler();
    private static Runnable r = new Runnable() {
        public void run() {
            mToast.cancel();
        }
    };
    private static Runnable runnable = new Runnable() {
        public void run() {
            mCustomToast.cancel();
        }
    };

    public static void showToast(Context mContext, String text, int duration) {

        mHandler.removeCallbacks(r);
        if (mToast != null)
            mToast.setText(text);
        else
            mToast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
        mHandler.postDelayed(r, duration);

        mToast.show();
    }

    public static void showToast(Context mContext, int resId, int duration) {
        showToast(mContext, mContext.getResources().getString(resId), duration);
    }



}
