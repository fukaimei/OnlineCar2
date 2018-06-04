package com.fukaimei.onlinecar.BicycleSharing;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.fukaimei.onlinecar.BicycleSharing.util.Utils;
import com.fukaimei.onlinecar.R;
import com.fukaimei.onlinecar.TakeTaxActivity;
import com.fukaimei.scanqrcodetest.FindScanActivity;

public class BicycleActivity extends Activity {

    private static final int BAIDU_READ_PHONE_STATE = 100;

    private MapView mMapView = null;
    private BaiduMap mBaiduMap;
    private LocationClient mlocationClient;
    private MylocationListener mlistener;
    private Context context;
    private Button mQRcode;

    private double mLatitude;
    private double mLongitude;
    private float mCurrentX;

    private ImageButton mGetMylocationBN;

    PopupMenu popup = null;

    //自定义图标
    private BitmapDescriptor mIconLocation;

    private MyOrientationListener myOrientationListener;
    //定位图层显示方式
    private MyLocationConfiguration.LocationMode locationMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_bicycle_main);
        this.context = this;

        initView();
        //判断是否为Android 6.0 以上的系统版本，如果是，需要动态添加权限
        if (Build.VERSION.SDK_INT >= 23) {
            showContacts();
        } else {
            initLocation();//initLocation为定位方法
        }

    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.id_bmapView);

        mBaiduMap = mMapView.getMap();
        //根据给定增量缩放地图级别
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(18.0f);
        mBaiduMap.setMapStatus(msu);
        MapStatus mMapStatus;//地图当前状态
        MapStatusUpdate mMapStatusUpdate;//地图将要变化成的状态
        mMapStatus = new MapStatus.Builder().overlook(-45).build();
        mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);
        mGetMylocationBN = (ImageButton) findViewById(R.id.id_bn_getMyLocation);
        mGetMylocationBN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMyLocation();
            }
        });

        mQRcode = (Button) findViewById(R.id.sweep_QRcode);
        mQRcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BicycleActivity.this, FindScanActivity.class);
                startActivity(intent);
            }
        });

    }

    /**
     * 定位方法
     */
    private void initLocation() {
        locationMode = MyLocationConfiguration.LocationMode.NORMAL;

        //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        mlocationClient = new LocationClient(this);
        mlistener = new MylocationListener();

        //注册监听器
        mlocationClient.registerLocationListener(mlistener);
        //配置定位SDK各配置参数，比如定位模式、定位时间间隔、坐标系类型等
        LocationClientOption mOption = new LocationClientOption();
        //设置坐标类型
        mOption.setCoorType("bd09ll");
        //设置是否需要地址信息，默认为无地址
        mOption.setIsNeedAddress(true);
        //设置是否打开gps进行定位
        mOption.setOpenGps(true);
        //设置扫描间隔，单位是毫秒，当<1000(1s)时，定时定位无效
        int span = 1000;
        mOption.setScanSpan(span);
        //设置 LocationClientOption
        mlocationClient.setLocOption(mOption);

        //初始化图标,BitmapDescriptorFactory是bitmap 描述信息工厂类.
        mIconLocation = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_geo);

        myOrientationListener = new MyOrientationListener(context);
        //通过接口回调来实现实时方向的改变
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        //开启定位
        mBaiduMap.setMyLocationEnabled(true);
        if (!mlocationClient.isStarted()) {
            mlocationClient.start();
        }
        myOrientationListener.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //停止定位
        mBaiduMap.setMyLocationEnabled(false);
        mlocationClient.stop();
        myOrientationListener.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    public void getMyLocation() {
        LatLng latLng = new LatLng(mLatitude, mLongitude);
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.setMapStatus(msu);
    }

    public void onPopupMenuClick(View v) {
        // 创建PopupMenu对象
        popup = new PopupMenu(this, v);
        // 将R.menu.menu_main菜单资源加载到popup菜单中
        getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
        // 为popup菜单的菜单项单击事件绑定事件监听器
        popup.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.id_map_common:
                                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                                break;
                            case R.id.id_map_site:
                                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                                break;
                            case R.id.id_map_traffic:
                                if (mBaiduMap.isTrafficEnabled()) {
                                    mBaiduMap.setTrafficEnabled(false);
                                    item.setTitle("实时交通(off)");
                                } else {
                                    mBaiduMap.setTrafficEnabled(true);
                                    item.setTitle("实时交通(on)");
                                }
                                break;
                            case R.id.id_map_mlocation:
                                getMyLocation();
                                break;
                            case R.id.id_map_model_common:
                                //普通模式
                                locationMode = MyLocationConfiguration.LocationMode.NORMAL;
                                break;
                            case R.id.id_map_model_following:
                                //跟随模式
                                locationMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                                break;
                            case R.id.id_map_model_compass:
                                //罗盘模式
                                locationMode = MyLocationConfiguration.LocationMode.COMPASS;
                                break;
                        }
                        return true;
                    }
                });
        popup.show();
    }

    /**
     * 所有的定位信息都通过接口回调来实现
     */
    public class MylocationListener implements BDLocationListener {
        //定位请求回调接口
        private boolean isFirstIn = true;

        //定位请求回调函数,这里面会得到定位信息
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            //BDLocation 回调的百度坐标类，内部封装了如经纬度、半径等属性信息
            //MyLocationData 定位数据,定位数据建造器
            /**
             * 可以通过BDLocation配置如下参数
             * 1.accuracy 定位精度
             * 2.latitude 百度纬度坐标
             * 3.longitude 百度经度坐标
             * 4.satellitesNum GPS定位时卫星数目 getSatelliteNumber() gps定位结果时，获取gps锁定用的卫星数
             * 5.speed GPS定位时速度 getSpeed()获取速度，仅gps定位结果时有速度信息，单位公里/小时，默认值0.0f
             * 6.direction GPS定位时方向角度
             * */
            mLatitude = bdLocation.getLatitude();
            mLongitude = bdLocation.getLongitude();
            MyLocationData data = new MyLocationData.Builder()
                    .direction(mCurrentX)//设定图标方向
                    .accuracy(bdLocation.getRadius())//getRadius 获取定位精度,默认值0.0f
                    .latitude(mLatitude)//百度纬度坐标
                    .longitude(mLongitude)//百度经度坐标
                    .build();
            //设置定位数据, 只有先允许定位图层后设置数据才会生效，参见 setMyLocationEnabled(boolean)
            mBaiduMap.setMyLocationData(data);
            //配置定位图层显示方式,三个参数的构造器
            /**
             * 1.定位图层显示模式
             * 2.是否允许显示方向信息
             * 3.用户自定义定位图标
             * */
            MyLocationConfiguration configuration
                    = new MyLocationConfiguration(locationMode, true, mIconLocation);
            //设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效，参见 setMyLocationEnabled(boolean)
            mBaiduMap.setMyLocationConfigeration(configuration);
            //判断是否为第一次定位,是的话需要定位到用户当前位置
            if (isFirstIn) {
                //地理坐标基本数据结构
                LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                //描述地图状态将要发生的变化,通过当前经纬度来使地图显示到该位置
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                //改变地图状态
                mBaiduMap.setMapStatus(msu);
                isFirstIn = false;
                Utils.showToast2(context, "您当前的位置为：" + bdLocation.getAddrStr());

                //定义Maker坐标点
                LatLng point = new LatLng(mLatitude + 0.00056, mLongitude);
                //构建Marker图标
                BitmapDescriptor bitmap = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option);
                //定义Maker坐标点
                LatLng point1 = new LatLng(mLatitude, mLongitude + 0.0003);
                //构建Marker图标
                BitmapDescriptor bitmap1 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option1 = new MarkerOptions()
                        .position(point1)
                        .icon(bitmap1);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option1);
                //定义Maker坐标点
                LatLng point2 = new LatLng(mLatitude + 0.0005, mLongitude + 0.001);
                //构建Marker图标
                BitmapDescriptor bitmap2 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option2 = new MarkerOptions()
                        .position(point2)
                        .icon(bitmap2);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option2);
                //定义Maker坐标点
                LatLng point3 = new LatLng(mLatitude + 0.0003, mLongitude + 0.0001);
                //构建Marker图标
                BitmapDescriptor bitmap3 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option3 = new MarkerOptions()
                        .position(point3)
                        .icon(bitmap3);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option3);
                //定义Maker坐标点
                LatLng point4 = new LatLng(mLatitude + 0.0006, mLongitude);
                //构建Marker图标
                BitmapDescriptor bitmap4 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option4 = new MarkerOptions()
                        .position(point4)
                        .icon(bitmap4);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option4);
                //定义Maker坐标点
                LatLng point5 = new LatLng(mLatitude, mLongitude + 0.0004);
                //构建Marker图标
                BitmapDescriptor bitmap5 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option5 = new MarkerOptions()
                        .position(point5)
                        .icon(bitmap5);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option5);

                //定义Maker坐标点
                LatLng point6 = new LatLng(mLatitude, mLongitude - 0.0008);
                //构建Marker图标
                BitmapDescriptor bitmap6 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option6 = new MarkerOptions()
                        .position(point6)
                        .icon(bitmap6);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option1);
                //定义Maker坐标点
                LatLng point7 = new LatLng(mLatitude - 0.0005, mLongitude + 0.0004);
                //构建Marker图标
                BitmapDescriptor bitmap7 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option7 = new MarkerOptions()
                        .position(point7)
                        .icon(bitmap7);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option7);
                //定义Maker坐标点
                LatLng point8 = new LatLng(mLatitude - 0.0003, mLongitude - 0.0005);
                //构建Marker图标
                BitmapDescriptor bitmap8 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option8 = new MarkerOptions()
                        .position(point8)
                        .icon(bitmap8);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option8);
                //定义Maker坐标点
                LatLng point9 = new LatLng(mLatitude - 0.0003, mLongitude);
                //构建Marker图标
                BitmapDescriptor bitmap9 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option9 = new MarkerOptions()
                        .position(point9)
                        .icon(bitmap9);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option9);
                //定义Maker坐标点
                LatLng point10 = new LatLng(mLatitude, mLongitude - 0.0003);
                //构建Marker图标
                BitmapDescriptor bitmap10 = BitmapDescriptorFactory
                        .fromResource(R.drawable.bicycle_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option10 = new MarkerOptions()
                        .position(point10)
                        .icon(bitmap10);
                //在地图上添加Marker，并显示
                mBaiduMap.addOverlay(option10);
            }
        }
    }

    public void onTax(View v) {
        Intent intent = new Intent(BicycleActivity.this, TakeTaxActivity.class);
        startActivity(intent);
    }

    /**
     * Android 6.0 以上的版本的定位方法
     */
    public void showContacts() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "没有权限,请手动开启定位权限", Toast.LENGTH_SHORT).show();
            // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
            ActivityCompat.requestPermissions(BicycleActivity.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE
            }, BAIDU_READ_PHONE_STATE);
        } else {
            initLocation();
        }
    }

    //Android 6.0 以上的版本申请权限的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // requestCode即所声明的权限获取码，在checkSelfPermission时传入
            case BAIDU_READ_PHONE_STATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获取到权限，作相应处理（调用定位SDK应当确保相关权限均被授权，否则可能引起定位失败）
                    initLocation();
                } else {
                    // 没有获取到权限，做特殊处理
                    Toast.makeText(getApplicationContext(), "获取位置权限失败，请手动开启", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

}