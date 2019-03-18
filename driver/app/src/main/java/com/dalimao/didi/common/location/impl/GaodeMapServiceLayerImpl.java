package com.dalimao.didi.common.location.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.LatLngBounds;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolylineOptions;
import com.amap.api.services.a.as;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.TMC;
import com.amap.api.services.route.WalkRouteResult;
import com.dalimao.didi.common.location.CommonOnLocationChangeListener;
import com.dalimao.didi.common.location.IMapServiceLayer;
import com.dalimao.didi.common.location.LocationInfo;
import com.dalimao.didi.common.location.MapRouteInfo;
import com.dalimao.didi.common.location.OnRouteCompleteListener;
import com.dalimao.didi.common.location.OnSearchedListener;
import com.dalimao.didi.common.utils.LogUtil;
import com.dalimao.didi.common.utils.SensorEventHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高德地图的实现
 * Created by liuguangli on 17/3/18.
 */

public class GaodeMapServiceLayerImpl implements IMapServiceLayer {

    private static final String TAG = "GaodeMapServiceLayerImpl";
    private static final String KEY_MY_MARKERE = "-1";
    private Context mContext;
    //位置定位对象
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    // 地图视图对象
    private MapView mapView;
    // 地图管理对象
    private AMap aMap;
    // 地图位置变化回调对象
    private LocationSource.OnLocationChangedListener mMapLocationChangeListener;
    private boolean firstLocation = true;
    private Bitmap mLocationBitmap;
    private SensorEventHelper mSensorHelper;
    // 管理地图标记集合
    private Map<String, Marker> markerMap = new HashMap<>();
    // 业务层使用通用的监听器
    CommonOnLocationChangeListener mLocationChangeListener;
    private String mCity;
    private RouteSearch mRouteSearch;

    public GaodeMapServiceLayerImpl(Context context) {

        // 创建地图对象
        mapView = new MapView(context);
        // 获取地图管理器
        aMap = mapView.getMap();
        // 创建定位对象
        mlocationClient = new AMapLocationClient(context);
        mLocationOption = new AMapLocationClientOption();
        //设置为高精度定位模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位参数
        mlocationClient.setLocationOption(mLocationOption);

        // 传感器对象
        mSensorHelper = new SensorEventHelper(context);
        mSensorHelper.registerSensorListener();
        mContext = context;
    }


    public void setUpLocation() {

        //设置监听器

        mlocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                // 定位变化位置
                if (mMapLocationChangeListener != null) {
                    // 地图已经激活，通知蓝点实时更新
                    mMapLocationChangeListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
                    LogUtil.d(TAG, "onLocationChanged");
                    if (firstLocation) {
                        firstLocation = false;
                        aMap.animateCamera(CameraUpdateFactory.zoomTo(16));
                    }

                    mCity = aMapLocation.getCity();

                    if (mLocationChangeListener != null) {
                        mLocationChangeListener.onLocationChange(aMapLocation.getLatitude(), aMapLocation.getLongitude(), mSensorHelper.getRotation(), aMapLocation.getPoiName());
                    }

                }
            }
        });

        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        mlocationClient.startLocation();
    }

    public void stopLocation() {
        mlocationClient.stopLocation();
    }

    private void setUpMap() {

        // 设置地图激活（加载监听）
        aMap.setLocationSource(new LocationSource() {
            @Override
            public void activate(OnLocationChangedListener onLocationChangedListener) {
                mMapLocationChangeListener = onLocationChangedListener;
                LogUtil.d(TAG, "activate");
            }

            @Override
            public void deactivate() {

            }
        });
        // 设置默认定位按钮是否显示，这里先不想业务使用方开放
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false，这里先不想业务使用方开放
        aMap.setMyLocationEnabled(true);


    }

    @Override
    public View getMap() {

        return mapView;
    }


    @Override
    public void setLocationStyle(Bitmap bitmap) {
        mLocationBitmap = bitmap;
    }


    public void addMyLocationMarker(Bitmap bitmap, double latitude, double longitude) {
        addMarker(KEY_MY_MARKERE, bitmap, latitude, longitude, 0);
        Marker marker = markerMap.get(KEY_MY_MARKERE);
        // 传感器控制我的位置标记的旋转角度
        mSensorHelper.setCurrentMarker(marker);
    }

    @Override
    public void addMarker(String key, Bitmap bitmap, double latitude, double longitude, float rotation) {

        Marker storedMarker = markerMap.get(key);
        if (storedMarker != null) {
            storedMarker.setPosition(new LatLng(latitude, longitude));
            storedMarker.setRotateAngle(rotation);
        } else {
            MarkerOptions options = new MarkerOptions();
            BitmapDescriptor des = BitmapDescriptorFactory.fromBitmap(bitmap);
            options.icon(des);
            options.anchor(0.5f, 0.5f);
            options.position(new LatLng(latitude, longitude));
            Marker marker = aMap.addMarker(options);
            marker.setRotateAngle(rotation);
            markerMap.put(key, marker);


        }


    }

    @Override
    public void updateMarker(String key, Bitmap bitmap, double latitude, double longitude, float rotation) {
        Marker storedMarker = markerMap.get(key);
        if (storedMarker != null) {
            storedMarker.setPosition(new LatLng(latitude, longitude));
            storedMarker.setRotateAngle(rotation);
        }
    }

    @Override
    public boolean removeMarker(String key) {
        Marker storedMarker = markerMap.get(key);
        if (storedMarker != null) {
            storedMarker.remove();
            markerMap.remove(key);
            return true;
        }
        return false;
    }

    @Override
    public String getCity() {
        return mCity;
    }

    @Override
    public void poiSearch(String key, final OnSearchedListener listener) {


        if (!TextUtils.isEmpty(key)) {
            InputtipsQuery inputquery = new InputtipsQuery(key, "");
            Inputtips inputTips = new Inputtips(mContext, inputquery);
            inputTips.setInputtipsListener(new Inputtips.InputtipsListener() {
                @Override
                public void onGetInputtips(List<Tip> tipList, int rCode) {
                    if (rCode == AMapException.CODE_AMAP_SUCCESS) {// 正确返回
                        List<LocationInfo> locationInfos = new ArrayList<LocationInfo>();

                        for (int i = 0; i < tipList.size(); i++) {
                            Tip tip = tipList.get(i);
                            locationInfos.add(new LocationInfo(tip.getName(), tip.getPoint().getLatitude(), tip.getPoint().getLongitude()));
                        }
                        listener.onSearched(locationInfos);
                    } else {
                        listener.onError(rCode);
                    }
                }
            });
            inputTips.requestInputtipsAsyn();
        }
    }

    @Override
    public void moveCamera(LocationInfo locationInfo, int scale) {
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationInfo.getLatitude(), locationInfo.getLongitude()), scale));
    }

    @Override
    public void moveCamera(LocationInfo locationInfo1, LocationInfo locationInfo2) {


        try {
            LatLng latLng = new LatLng(locationInfo1.getLatitude(), locationInfo1.getLongitude());
            LatLng latLng1 = new LatLng(locationInfo2.getLatitude(), locationInfo2.getLongitude());
            LatLngBounds latLngBounds = new LatLngBounds(latLng, latLng1);
            aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 20));
        } catch (com.amap.api.maps2d.AMapException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void driverRoute(final LocationInfo start, LocationInfo end, final int color, final OnRouteCompleteListener listener) {
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                new LatLonPoint(start.getLatitude(), start.getLongitude()), new LatLonPoint(end.getLatitude(), end.getLongitude()));
        // 驾车路径规划
        RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.DrivingDefault, null,
                null, "");
        // 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
        // 异步路径规划驾车模式查询
        if (mRouteSearch == null) {
            mRouteSearch = new RouteSearch(mContext);
        }
        mRouteSearch.calculateDriveRouteAsyn(query);
        mRouteSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
            @Override
            public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

            }

            @Override
            public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
                // 获取第一条路径
                DrivePath drivePath = driveRouteResult.getPaths()
                        .get(0);
                PolylineOptions polylineOptions =  new PolylineOptions();

                // 路径起点
                LatLonPoint  startPoint = driveRouteResult.getStartPos();
                // 路径中间步骤
                List<DriveStep> drivePaths = drivePath.getSteps();
                // 路径终点
                LatLonPoint endPoint = driveRouteResult.getTargetPos();
                // 绘制路径
                polylineOptions.add(new LatLng(startPoint.getLatitude(), startPoint.getLongitude()));
                for (DriveStep step : drivePaths) {
                    List<LatLonPoint> latlonPoints = step.getPolyline();
                    for (LatLonPoint latlonpoint : latlonPoints) {
                        polylineOptions.add(new LatLng(latlonpoint.getLatitude(), latlonpoint.getLongitude()));

                    }
                }
                polylineOptions.color(color);
                polylineOptions.add(new LatLng(endPoint.getLatitude(), endPoint.getLongitude()));
                aMap.addPolyline(polylineOptions);
                  if (listener != null) {

                      MapRouteInfo info = new MapRouteInfo();
                      info.setTaxiCost(driveRouteResult.getTaxiCost());
                      info.setDuration(10 + new Long(drivePath.getDuration()/1000 * 60).intValue());
                      info.setDistance(0.5f + drivePath.getDistance()/1000);
                      listener.onComplete(info);
                  }
            }

            @Override
            public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

            }

            @Override
            public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

            }
        });
    }


    @Override
    public void clearAllMarker() {
        aMap.clear();
        markerMap.clear();
    }

    @Override
    public void setLocationChangeListener(CommonOnLocationChangeListener listener) {
        this.mLocationChangeListener = listener;
    }


    @Override
    public void onCreate(Bundle savedState) {
        mapView.onCreate(savedState);
        setUpMap();

    }

    @Override
    public void onResume() {
        mapView.onResume();
        setUpLocation();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        mlocationClient.stopLocation();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        mlocationClient.onDestroy();
    }


}
