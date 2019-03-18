package com.dalimao.didi.common.location;

/**
 * 位置变化监听
 * Created by liuguangli on 17/3/18.
 */
public interface CommonOnLocationChangeListener {
    void  onLocationChange(double latitude, double longitude, float rotation, String addr);
}
