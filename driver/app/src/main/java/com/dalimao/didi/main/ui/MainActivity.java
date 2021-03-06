package com.dalimao.didi.main.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;


import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;
import com.dalimao.didi.DidiApplication;
import com.dalimao.didi.R;
import com.dalimao.didi.account.model.AccountManagerImpl;
import com.dalimao.didi.account.ui.LoginDialog;
import com.dalimao.didi.account.ui.PhoneInputDialog;
import com.dalimao.didi.common.http.HttpConfig;
import com.dalimao.didi.common.http.OkHttpClientImpl;
import com.dalimao.didi.common.location.CommonOnLocationChangeListener;
import com.dalimao.didi.common.location.MapRouteInfo;
import com.dalimao.didi.common.location.OnRouteCompleteListener;
import com.dalimao.didi.common.location.impl.GaodeMapServiceLayerImpl;
import com.dalimao.didi.common.location.IMapServiceLayer;
import com.dalimao.didi.common.location.LocationInfo;
import com.dalimao.didi.common.location.OnSearchedListener;
import com.dalimao.didi.common.utils.DevUtil;
import com.dalimao.didi.common.utils.LogUtil;
import com.dalimao.didi.common.utils.SensorEventHelper;
import com.dalimao.didi.common.utils.ToastUtil;
import com.dalimao.didi.main.model.bean.Driver;
import com.dalimao.didi.main.model.MainManagerImpl;
import com.dalimao.didi.order.model.bean.Order;
import com.dalimao.didi.main.presenter.MainPresenterImpl;
import com.dalimao.didi.order.model.OrderManagerImpl;

import java.util.ArrayList;
import java.util.List;

import c.b.BP;
import c.b.PListener;
import cn.bmob.push.BmobPush;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobConfig;
import cn.bmob.v3.BmobInstallation;


public class MainActivity extends Activity implements IMainView {

    private static final String TAG = "MainActivity";
    private static final String KEY_START = "KEY_START";
    private static final String KEY_END = "KEY_END";
    private SensorEventHelper mSensorHelper;

    private MainPresenterImpl mMainPresenter;
    private LoginDialog mLoginDialog;
    private PhoneInputDialog mPhoneInputDialog;

    private IMapServiceLayer mMapLayer;
    private Bitmap mDriverMarkerBit;
    private LocationInfo mCurrentLocation;
    private AutoCompleteTextView mStartEdit;
    private AutoCompleteTextView mEndEdit;
    private PoiAdapter mEndAdapter;
    private TextView mCity;
    private View mCallArea;
    private View mSelectArea;
    private Button mBtnCall;
    private Button mBtnCancel;
    private Button mBtnPay;
    private View mUser;
    private Bitmap mMyLocationBitMap;
    private LocationInfo mEndLocation;
    private TextView mRouteInfo;
    private View mLoadingArea;
    private DrawerLayout mDrawerLayout;
    private boolean firstLocation = true;
    private float mCurrentCost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMainPresenter = new MainPresenterImpl(new AccountManagerImpl(OkHttpClientImpl.getInstance()),
                new MainManagerImpl(OkHttpClientImpl.getInstance()),
                new OrderManagerImpl(OkHttpClientImpl.getInstance()),
                this);

        mCurrentLocation = new LocationInfo("", 0, 0);
        initViews();
        initMap();
        mMapLayer.onCreate(savedInstanceState);


    }

    private void initViews() {
        mStartEdit = (AutoCompleteTextView) findViewById(R.id.start);
        mEndEdit = (AutoCompleteTextView) findViewById(R.id.end);
        mCity = (TextView) findViewById(R.id.city);
        mCallArea = findViewById(R.id.call_area);
        mSelectArea = findViewById(R.id.select_area);
        mBtnCall = (Button) findViewById(R.id.btn_call_driver);
        mBtnPay = (Button) findViewById(R.id.btn_pay);
        mBtnCancel = (Button) findViewById(R.id.btn_cancel);
        mRouteInfo = (TextView) findViewById(R.id.route_info);
        mUser = findViewById(R.id.im_user);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mLoadingArea = findViewById(R.id.loading_area);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btn_call_driver) {
                    callDriver();
                } else if (v.getId() == R.id.btn_cancel){
                    cancel();
                } else if (v.getId() == R.id.im_user){
                    mDrawerLayout.openDrawer(Gravity.LEFT);
                } else if (v.getId() == R.id.btn_pay) {
                    pay();
                }
            }


        };
        mBtnPay.setVisibility(View.GONE);
        mUser.setOnClickListener(onClickListener);
        mBtnPay.setOnClickListener(onClickListener);
        mBtnCall.setOnClickListener(onClickListener);
        mBtnCancel.setOnClickListener(onClickListener);
        mEndEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mMapLayer.poiSearch(s.toString(), new OnSearchedListener() {
                    @Override
                    public void onSearched(final List<LocationInfo> results) {
                        List<String> listString = new ArrayList<String>();
                        for (int i = 0; i < results.size(); i++) {
                            listString.add(results.get(i).getName());
                        }
                        if (mEndAdapter == null) {
                            mEndAdapter = new PoiAdapter(getApplicationContext(), listString);
                            mEndEdit.setAdapter(mEndAdapter);

                        } else {

                            mEndAdapter.setData(listString);
                        }
                        mEndEdit.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                mEndLocation = results.get(position);
                                ToastUtil.show(MainActivity.this, results.get(position).getName());
                                showStartEnd();
                                DevUtil.closeInputMethod(MainActivity.this);
                            }
                        });
                        mEndAdapter.notifyDataSetChanged();


                    }

                    @Override
                    public void onError(int rCode) {

                    }
                });
            }
        });
    }

    private void toUserCenter() {

    }


    /**
     *  支付
     */
    private void pay() {

        // 模拟支付成功, 通知业务服务器，支付已经成功
        mMainPresenter.paySuc();
    }

    /**
     *  取消
     */
    private void cancel() {

        mSelectArea.setVisibility(View.VISIBLE);
        mRouteInfo.setText(getString(R.string.canceling));
        mBtnCancel.setEnabled(false);
        mMainPresenter.cancelOrder();
    }


    /**
     *  呼叫司机 ／下单
     */
    private void callDriver() {
        mLoadingArea.setVisibility(View.VISIBLE);
        mBtnCall.setEnabled(false);
        mMainPresenter.callDrivers(DidiApplication.getInstance().getPushClientId(),
                mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude(),
                mEndLocation.getLatitude(),
                mEndLocation.getLongitude(),
                mStartEdit.getText().toString(),
                mEndEdit.getText().toString(),
                mCurrentCost);
    }

    /**
     *  显示起点和终点
     */
    private void showStartEnd() {
        mMapLayer.clearAllMarker();
        mMapLayer.addMarker(KEY_START, BitmapFactory.decodeResource(getResources(), R.mipmap.start), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 0);
        mMapLayer.addMarker(KEY_END, BitmapFactory.decodeResource(getResources(), R.mipmap.end), mEndLocation.getLatitude(), mEndLocation.getLongitude(), 0);

        mCallArea.setVisibility(View.VISIBLE);
        mBtnPay.setVisibility(View.GONE);
        mBtnCall.setEnabled(true);
        mBtnCall.setVisibility(View.VISIBLE);
        mBtnCancel.setVisibility(View.VISIBLE);
        // 起点－终点：路径规划
        mMapLayer.driverRoute(mCurrentLocation, mEndLocation, Color.GREEN, new OnRouteCompleteListener() {
            @Override
            public void onComplete(MapRouteInfo result) {
                String infoString = getString(R.string.route_info);
                mCurrentCost = result.getTaxiCost();
                infoString = String.format(infoString, new Float(result.getDistance()).intValue(), mCurrentCost, result.getDuration());
                mRouteInfo.setText(infoString);
                // 聚集到起点和终点
                mMapLayer.moveCamera(mCurrentLocation, mEndLocation) ;
            }
        });
    }

    private void initMap() {
        mMapLayer = new GaodeMapServiceLayerImpl(this);
        // 设置我的位置图标记
        mMyLocationBitMap = BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.navi_map_gps_locked);
        mMapLayer.setLocationStyle(mMyLocationBitMap);
        mDriverMarkerBit = BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.car);
        // 设置位置变化监听
        mMapLayer.setLocationChangeListener(new CommonOnLocationChangeListener() {
            @Override
            public void onLocationChange(double latitude, double longitude, float rotation, String addr) {

                LogUtil.d(TAG, "onLocationChange");
                mCurrentLocation.setLatitude(latitude);
                mCurrentLocation.setLongitude(longitude);
                if (firstLocation || AMapUtils.calculateLineDistance(new LatLng(latitude, longitude),new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())) > 100) {
                    // 距离变化大于 100 米 更新附近司机 && 上传自己的位置
                    mMainPresenter.getNearDrivers(latitude, longitude);
                    mMainPresenter.updateMyLocation(DidiApplication.getInstance().getPushClientId(), latitude, longitude, rotation);
                    LogUtil.d(TAG, "onLocationChange: 距离变化大于 100 米");
                    mCity.setText(mMapLayer.getCity());
                    mStartEdit.setText(addr);
                    addMyLocation();

                }

                if (firstLocation) {
                    firstLocation = false;
                    mMainPresenter.getProcessingOrder();
                }



            }
        });



        ViewGroup mapContainer = (ViewGroup) findViewById(R.id.mapContainer);
        mapContainer.addView(mMapLayer.getMap());
    }




    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mMapLayer.onResume();
        mMainPresenter.subscribe();
        mMainPresenter.getLocalAccount();
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mMapLayer.onPause();
        if (mSensorHelper != null) {
            mSensorHelper.unRegisterSensorListener();
            mSensorHelper.setCurrentMarker(null);
            mSensorHelper = null;
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapLayer.onDestroy();
        mMainPresenter.unSubscribe();
    }

    @Override
    public void showPhoneInputDialog() {
        if (mPhoneInputDialog == null || !mPhoneInputDialog.isShowing()) {
            mPhoneInputDialog = new PhoneInputDialog(this);
            mPhoneInputDialog.show();
            cancel();
        }


    }

    @Override
    public void showLoginDialog(String phone) {


        if (mPhoneInputDialog == null || !mPhoneInputDialog.isShowing()) {
            mLoginDialog = new LoginDialog(this, phone);
            mLoginDialog.show();
            cancel();
        }


    }

    @Override
    public void showNearDrivers(List<Driver> drivers) {
        LogUtil.d(TAG, "showNearDrivers");
        for (Driver driver: drivers) {
            addDriver(driver);
        }
    }

    @Override
    public void addDriver(Driver driver) {
        if (mDriverMarkerBit.isRecycled()) {
            mDriverMarkerBit = BitmapFactory.decodeResource(this.getResources(),
                    R.mipmap.car);
        }
        mMapLayer.addMarker(driver.getKey(), mDriverMarkerBit, driver.getLatitude(), driver.getLongitude(), driver.getRotation() );
        //
    }

    @Override
    public void updateDriver(Driver driver) {
        if (mDriverMarkerBit.isRecycled()) {
            mDriverMarkerBit = BitmapFactory.decodeResource(this.getResources(),
                    R.mipmap.car);
        }
        mMapLayer.updateMarker(driver.getKey(), mDriverMarkerBit, driver.getLatitude(), driver.getLongitude(), driver.getRotation() );
    }

    @Override
    public void removeDriver(Driver driver) {
       mMapLayer.removeMarker(driver.getKey());
    }

    /**
     * 司机接单
     * @param order
     */

    @Override
    public void showOrderAccepted(final Order.Data order) {
        mLoadingArea.setVisibility(View.GONE);
        mCallArea.setVisibility(View.VISIBLE);
        mBtnCall.setEnabled(false);
        if (mCurrentLocation == null || (mCurrentLocation.getLatitude() == 0 && mCurrentLocation.getLongitude() == 0)) {
            mCurrentLocation = new LocationInfo("",order.getStartLatitude(), order.getStartLongitude());
        }
        if (mEndLocation == null || (mEndLocation.getLatitude() == 0 && mEndLocation.getLongitude() == 0)) {
            mEndLocation = new LocationInfo("", order.getEndLatitude(), order.getEndLongitude());
        }

        // 清空地图上的标记
        mMapLayer.clearAllMarker();

        addMyLocation();

        // 添加司机位置
        addDriverLocation(order.getDriverUid(), order.getDriverLatitude(), order.getDriverLongitude(), order.getDriverRotation());
        // 司机－起点：路径规划
        final LocationInfo driverLocation = new LocationInfo("",order.getDriverLatitude(), order.getDriverLongitude());
        mMapLayer.driverRoute(driverLocation,
                    mCurrentLocation,
                    Color.BLUE,
                    new OnRouteCompleteListener() {
                    @Override
                      public void onComplete(MapRouteInfo result) {
                        String infoString = getString(R.string.accept_info);
                        infoString = String.format(infoString,
                                order.getDriverName(),
                                order.getCarNo(),
                                new Float(result.getDistance()).intValue(),
                                result.getDuration());
                        mRouteInfo.setText(infoString);
                        // 聚集到司机位置和我的位置
                        mMapLayer.moveCamera(mCurrentLocation, driverLocation) ;
                     }
                });


        // 起点和终点图标
        mMapLayer.addMarker(KEY_START, BitmapFactory.decodeResource(getResources(), R.mipmap.start), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 0);
        mMapLayer.addMarker(KEY_END, BitmapFactory.decodeResource(getResources(), R.mipmap.end), mEndLocation.getLatitude(), mEndLocation.getLongitude(), 0);

    }



    @Override
    public void cancelOrderFail() {

        //ToastUtil.show(this, getString(R.string.order_cancel_error));
    }

    @Override
    public void cancelOrderSuc() {

        restoreInit();

    }

    /**
     * 显示司机已经到达
     * @param order
     */
    @Override
    public void showDriverArrived(Order.Data order) {

        ToastUtil.show(this, getString(R.string.driver_arrive_pure));
        mLoadingArea.setVisibility(View.GONE);
        mCallArea.setVisibility(View.VISIBLE);
        mBtnCall.setEnabled(false);
        if (mCurrentLocation == null || (mCurrentLocation.getLatitude() == 0 && mCurrentLocation.getLongitude() == 0)) {
            mCurrentLocation = new LocationInfo("",order.getStartLatitude(), order.getStartLongitude());
        }
        if (mEndLocation == null || (mEndLocation.getLatitude() == 0 && mEndLocation.getLongitude() == 0)) {
            mEndLocation = new LocationInfo("", order.getEndLatitude(), order.getEndLongitude());
        }

        // 清空地图上的标记
        mMapLayer.clearAllMarker();

        addMyLocation();

        // 添加司机位置
        addDriverLocation(order.getDriverUid(), order.getDriverLatitude(), order.getDriverLongitude(), order.getDriverRotation());

        String infoString = getString(R.string.driver_arrive);
        infoString = String.format(infoString,
                order.getDriverName(),
                order.getCarNo());
        mRouteInfo.setText(infoString);
        // 聚集到司机位置和我的位置
        final LocationInfo driverLocation = new LocationInfo("",order.getDriverLatitude(), order.getDriverLongitude());
        mMapLayer.moveCamera(mCurrentLocation, driverLocation) ;

        // 起点和终点图标
        mMapLayer.addMarker(KEY_START, BitmapFactory.decodeResource(getResources(), R.mipmap.start), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 0);
        mMapLayer.addMarker(KEY_END, BitmapFactory.decodeResource(getResources(), R.mipmap.end), mEndLocation.getLatitude(), mEndLocation.getLongitude(), 0);
    }

    /**
     * 显示行程中
     * @param order
     */

    @Override
    public void showStroke(Order.Data order) {
        mLoadingArea.setVisibility(View.GONE);
        mCallArea.setVisibility(View.GONE);
        mBtnCall.setEnabled(false);
        mSelectArea.setVisibility(View.GONE);
        if (mCurrentLocation == null || (mCurrentLocation.getLatitude() == 0 && mCurrentLocation.getLongitude() == 0)) {
            mCurrentLocation = new LocationInfo("",order.getStartLatitude(), order.getStartLongitude());
        }
        if (mEndLocation == null || (mEndLocation.getLatitude() == 0 && mEndLocation.getLongitude() == 0)) {
            mEndLocation = new LocationInfo("", order.getEndLatitude(), order.getEndLongitude());
        }

        // 清空地图上的标记
        mMapLayer.clearAllMarker();

        addMyLocation();

        // 添加司机位置
        addDriverLocation(order.getDriverUid(), order.getDriverLatitude(), order.getDriverLongitude(), order.getDriverRotation());


        // 聚集到司机位置和终点的位置
        final LocationInfo driverLocation = new LocationInfo("",order.getDriverLatitude(), order.getDriverLongitude());
        mMapLayer.driverRoute(driverLocation,
                mEndLocation,
                Color.BLUE,
                new OnRouteCompleteListener() {
                    @Override
                    public void onComplete(MapRouteInfo result) {
                        mMapLayer.moveCamera(driverLocation, mEndLocation) ;
                    }
                });


        // 起点和终点图标
        mMapLayer.addMarker(KEY_START, BitmapFactory.decodeResource(getResources(), R.mipmap.start), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 0);
        mMapLayer.addMarker(KEY_END, BitmapFactory.decodeResource(getResources(), R.mipmap.end), mEndLocation.getLatitude(), mEndLocation.getLongitude(), 0);

    }

    @Override
    public void showArriveEnd(Order.Data data) {

        // 清空地图上的标记
        mMapLayer.clearAllMarker();
        addMyLocation();
        mLoadingArea.setVisibility(View.GONE);
        mBtnCall.setEnabled(false);
        mSelectArea.setVisibility(View.GONE);
        mCallArea.setVisibility(View.VISIBLE);
        mBtnCancel.setVisibility(View.GONE);
        mBtnCall.setVisibility(View.GONE);
        mBtnPay.setVisibility(View.VISIBLE);
        mRouteInfo.setText(String.format(getString(R.string.pay_info), data.getCost(), data.getDriverName(), data.getCarNo()));
    }

    @Override
    public void showOrderCancelFail() {

        ToastUtil.show(this, getString(R.string.cancel_fail));
    }

    @Override
    public void showPaySuc() {
        ToastUtil.show(this, getString(R.string.pay_suc));
        mLoadingArea.setVisibility(View.GONE);
        mSelectArea.setVisibility(View.VISIBLE);
        mCallArea.setVisibility(View.GONE);
    }

    @Override
    public void showPayFail() {
        ToastUtil.show(this, getString(R.string.pay_suc));

    }

    @Override
    public void showCallSuc() {
        ToastUtil.show(this, getString(R.string.show_call_suc));

        mBtnCall.setEnabled(true);
    }

    @Override
    public void showCallFail() {
        mLoadingArea.setVisibility(View.GONE);
        mRouteInfo.setVisibility(View.VISIBLE);
        mRouteInfo.setText(getString(R.string.show_call_fail));
        ToastUtil.show(this, getString(R.string.show_call_fail));
    }

    /**
     *  恢复初始化状态
     */
    private void restoreInit() {
        mCallArea.setVisibility(View.GONE);
        mMapLayer.clearAllMarker();
        addMyLocation();
        mMapLayer.moveCamera(mCurrentLocation, 16);
        mMainPresenter.getNearDrivers(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        mLoadingArea.setVisibility(View.GONE);
        mBtnCall.setEnabled(true);
        mBtnCancel.setEnabled(true);
    }

    private void addMyLocation() {
        if (mMyLocationBitMap.isRecycled()) {
            mMyLocationBitMap = BitmapFactory.decodeResource(this.getResources(),
                    R.mipmap.navi_map_gps_locked);
        }
        mMapLayer.addMyLocationMarker(mMyLocationBitMap, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
    }
    private void addDriverLocation(String key, double driverLatitude, double driverLongitude, float driverRotation) {
        if (mDriverMarkerBit.isRecycled()) {
            mDriverMarkerBit = BitmapFactory.decodeResource(this.getResources(),
                    R.mipmap.car);
        }
        mMapLayer.addMarker(key,
                mDriverMarkerBit,
                driverLatitude,
                driverLongitude,
                driverRotation);
    }

}
