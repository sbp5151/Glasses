package com.jld.glasses.model;

/**
 * 项目名称：Glasses
 * 晶凌达科技有限公司所有，
 * 受到法律的保护，任何公司或个人，未经授权不得擅自拷贝。
 *
 * @creator boping
 * @create-time 2016/10/25 9:07
 */
public class GlassesInfoItem {

    private String devName;//设备名称
    private String devSn;//设备SN
    private String devVersions;//硬件版本
    private String androidVersions;//安卓版本
    private String storeSize;//存储空间
    private String freeSize;//剩余空间
    private String freeEle;//剩余电量

    public GlassesInfoItem(String devName, String devSn, String devVersions, String androidVersions, String storeSize, String freeSize, String freeEle) {
        this.devName = devName;
        this.devSn = devSn;
        this.devVersions = devVersions;
        this.androidVersions = androidVersions;
        this.storeSize = storeSize;
        this.freeSize = freeSize;
        this.freeEle = freeEle;
    }

    public String getDevName() {
        return devName;
    }

    public void setDevName(String devName) {
        this.devName = devName;
    }

    public String getDevSn() {
        return devSn;
    }

    public void setDevSn(String devSn) {
        this.devSn = devSn;
    }

    public String getDevVersions() {
        return devVersions;
    }

    public void setDevVersions(String devVersions) {
        this.devVersions = devVersions;
    }

    public String getAndroidVersions() {
        return androidVersions;
    }

    public void setAndroidVersions(String androidVersions) {
        this.androidVersions = androidVersions;
    }

    public String getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(String storeSize) {
        this.storeSize = storeSize;
    }

    public String getFreeSize() {
        return freeSize;
    }

    public void setFreeSize(String freeSize) {
        this.freeSize = freeSize;
    }

    public String getFreeEle() {
        return freeEle;
    }

    public void setFreeEle(String freeEle) {
        this.freeEle = freeEle;
    }
}
