package com.jld.glasses.model;

/**
 * 项目名称：Glasses
 * 晶凌达科技有限公司所有，
 * 受到法律的保护，任何公司或个人，未经授权不得擅自拷贝。
 *
 * @creator boping
 * @create-time 2016/10/17 14:11
 */
public class MainItem {

    private String name;//设备名称
    private String type;//item类型 0为二维码 1为上次连接 2为已配对标题栏 3为已配对设备 4为未配对标题栏 5为未配对设备
    private String last_name;//上次连接设备名称
    private String address;//设备mac地址
    private boolean isPair = false;//是否正在绑定

    public MainItem(String name, String type, String dev_name, String address) {
        this.name = name;
        this.type = type;
        this.last_name = dev_name;
        this.address = address;
    }

    public MainItem(String name, String type, String dev_name) {
        this.name = name;
        this.type = type;
        this.last_name = dev_name;
    }

    public boolean isPair() {
        return isPair;
    }

    public void setPair(boolean pair) {
        isPair = pair;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDev_name() {
        return last_name;
    }

    public void setDev_name(String dev_name) {
        this.last_name = dev_name;
    }
}
