package com.jld.glasses.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jld.glasses.R;
import com.jld.glasses.model.MainItem;

import java.util.ArrayList;

/**
 * 项目名称：Glasses
 * 晶凌达科技有限公司所有，
 * 受到法律的保护，任何公司或个人，未经授权不得擅自拷贝。
 *
 * @creator boping
 * @create-time 2016/10/17 14:10
 */
public class MainListAdapter extends BaseAdapter {

    private ArrayList<MainItem> mItems;
    private Context mContext;
    private LayoutInflater mInflater;

    public MainListAdapter(ArrayList<MainItem> items, Context mContext) {
        mItems = items;
        this.mContext = mContext;
        this.mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mItems.get(i);
    }

    /**
     * 获取绑定蓝牙设备数量
     *
     * @return
     */
    public int getContnetNum() {
        int num = 0;
        for (MainItem item : mItems) {
            if ("3".equals(item.getType()))
                num++;
        }
        return num;
    }

    /**
     * 获取未绑定蓝牙设备数量
     *
     * @return
     */
    public int getUnContnetNum() {
        int num = 0;
        for (MainItem item : mItems) {
            if ("5".equals(item.getType()))
                num++;
        }
        return num;
    }

    public void addItem(MainItem item) {
        String type = item.getType();
        if ("3".equals(type)) {
            int num = getContnetNum();
            mItems.add(3 + num, item);
        } else if ("5".equals(type)) {
            mItems.add(mItems.size(), item);
        } else {
            mItems.add(item);
        }
        try {
            notifyDataSetChanged();
        } catch (Exception e) {
        }
    }

    public ArrayList<MainItem> getItems() {
        return mItems;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        viewHolder mHolder = null;
        if (view == null) {
            mHolder = new viewHolder();
            view = mInflater.inflate(R.layout.main_list_item, null);
            mHolder.mLastName = (TextView) view.findViewById(R.id.tv_main_item_last_name);
            mHolder.mDevName = (TextView) view.findViewById(R.id.tv_main_dev_name);
            mHolder.mPairHint = (TextView) view.findViewById(R.id.tv_pair_ing);
            mHolder.mDevIcon = (ImageView) view.findViewById(R.id.iv_main_item_dev_icon);
            mHolder.mRightIcon = (ImageView) view.findViewById(R.id.iv_main_item_right);
            mHolder.mRelativeLayout = (RelativeLayout) view.findViewById(R.id.RelativeLayout);
            view.setTag(mHolder);
        } else {
            mHolder = (viewHolder) view.getTag();
        }
        mHolder.mDevName.setText(mItems.get(i).getName());
        mHolder.mLastName.setText(mItems.get(i).getDev_name());
        mHolder.mRightIcon.setImageResource(R.mipmap.right);
        mHolder.mPairHint.setVisibility(View.GONE);
        mHolder.mRelativeLayout.setBackgroundResource(R.drawable.click_color);
        mHolder.mRelativeLayout.setFocusable(false);
        if ("3".equals(mItems.get(i).getType()) || "5".equals(mItems.get(i).getType())) {//蓝牙设备
            mHolder.mDevIcon.setVisibility(View.VISIBLE);
            mHolder.mRightIcon.setVisibility(View.VISIBLE);
            mHolder.mRightIcon.setImageResource(R.mipmap.dev_right);
            if (mItems.get(i).isPair()){//正在配对
                mHolder.mPairHint.setVisibility(View.VISIBLE);
                mHolder.mRightIcon.setVisibility(View.INVISIBLE);
                mHolder.mRelativeLayout.setFocusable(true);
            }
        } else if ("2".equals(mItems.get(i).getType()) || "4".equals(mItems.get(i).getType())) {//已配对和未配对标题栏
            mHolder.mDevIcon.setVisibility(View.GONE);
            mHolder.mRightIcon.setVisibility(View.GONE);
            mHolder.mRelativeLayout.setBackgroundResource(R.color.color_white);
        } else {
            mHolder.mDevIcon.setVisibility(View.GONE);
            mHolder.mRightIcon.setVisibility(View.VISIBLE);
            if ("1".equals(mItems.get(i).getType())) {
                if (TextUtils.isEmpty(mItems.get(i).getDev_name())) {//上次连接设备为空
                    mHolder.mRelativeLayout.setFocusable(true);
                    mHolder.mRightIcon.setVisibility(View.GONE);
                }
            }
        }
        return view;
    }

    class viewHolder {
        public TextView mDevName;
        public TextView mLastName;
        public TextView mPairHint;
        public ImageView mDevIcon;
        public ImageView mRightIcon;
        public RelativeLayout mRelativeLayout;
    }
}
