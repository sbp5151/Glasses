package com.jld.glasses;

import android.app.Activity;
import android.app.Application;

/**
 * 项目名称：Glasses
 * 晶凌达科技有限公司所有，
 * 受到法律的保护，任何公司或个人，未经授权不得擅自拷贝。
 *
 * @creator boping
 * @create-time 2016/10/24 10:30
 */
public class MyApplication extends Application {

    private static Activity subActivity;
    public static void setSubActivity(Activity activity) {
        subActivity = activity;
    }
    public static void finishActivity() {
        if (subActivity != null) {
            subActivity.finish();
        }
    }
}
