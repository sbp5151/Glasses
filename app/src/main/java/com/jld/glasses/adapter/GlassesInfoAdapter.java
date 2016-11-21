package com.jld.glasses.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.jld.glasses.model.GlassesInfoItem;

/**
 * 项目名称：Glasses
 * 晶凌达科技有限公司所有，
 * 受到法律的保护，任何公司或个人，未经授权不得擅自拷贝。
 *
 * @creator boping
 * @create-time 2016/10/25 9:07
 */
public class GlassesInfoAdapter extends BaseAdapter {
    public GlassesInfoItem mItem;
    public GlassesInfoAdapter(GlassesInfoItem item) {
        mItem = item;
    }
    @Override
    public int getCount() {
        return 0;
    }
    @Override
    public Object getItem(int i) {
        return null;
    }
    @Override
    public long getItemId(int i) {
        return 0;
    }
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {


        return null;
    }
}
