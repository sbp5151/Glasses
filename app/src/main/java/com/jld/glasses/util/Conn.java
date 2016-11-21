package com.jld.glasses.util;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * 项目名称：Glasses
 * 晶凌达科技有限公司所有，
 * 受到法律的保护，任何公司或个人，未经授权不得擅自拷贝。
 *
 * @creator boping
 * @create-time 2016/10/20 13:47
 */
public class Conn implements ServiceConnection {

     public MyServiceConnection myConn;

    public Conn(MyServiceConnection myConn) {
        this.myConn = myConn;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        myConn.onServiceConnected(componentName,iBinder);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    public interface MyServiceConnection{
         void onServiceConnected(ComponentName componentName, IBinder iBinder);
    }
}
