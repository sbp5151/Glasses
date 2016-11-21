package com.jld.glasses.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.jld.glasses.R;
import com.jld.glasses.util.LogUtil;

public class CodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);
        ImageView mImageView = (ImageView) findViewById(R.id.imageView);
        Intent intent = getIntent();
        String mAddress = intent.getStringExtra("address");
        LogUtil.d("mAddressssssss:" + mAddress);
        Bitmap image = null;

    }


}
