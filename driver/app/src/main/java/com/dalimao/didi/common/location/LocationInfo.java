package com.dalimao.didi.common.location;



import java.io.Serializable;

/**
 * 位置点
 * Created by liuguangli on 17/2/17.
 */
public class LocationInfo implements Serializable {

    private String  name;
    private double  latitude;
    private double longitude;

    public LocationInfo(String name, double latitude, double longtitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longtitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
