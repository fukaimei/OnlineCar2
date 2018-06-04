package com.fukaimei.onlinecar;

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
import com.fukaimei.onlinecar.BicycleSharing.BicycleActivity;
import com.fukaimei.onlinecar.alipay.task.AlipayTask;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TakeTaxActivity extends Activity implements OnClickListener {
    private final static String TAG = "TakeTaxActivity";
    private EditText et_departure;
    private EditText et_destination;
    private TextView tv_travel;
    private Button btn_travel;
    private int mStep = 0;
    private SpeechSynthesizer mCompose;
    private LatLng mUserPos;
    private LatLng mDriverPos;
    private BitmapDescriptor icon_car;
    private int mDelayTime = 10000;
    private boolean bFinish = false;
    PopupMenu popup = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_take_tax);
        // 获取相应的动态权限
        xPermissions();
        et_departure = (EditText) findViewById(R.id.et_departure);
        et_destination = (EditText) findViewById(R.id.et_destination);
        tv_travel = (TextView) findViewById(R.id.tv_travel);
        btn_travel = (Button) findViewById(R.id.btn_travel);
        btn_travel.setOnClickListener(this);
        initLocation();
        initVoiceSetting();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_travel) {
            if (mStep == 0) {  //开始打车
                btn_travel.setTextColor(getResources().getColor(R.color.dark_grey));
                btn_travel.setEnabled(false);
                speaking("等待司机接单");
                mHandler.postDelayed(mAccept, mDelayTime);
            } else if (mStep == 1) {  //支付车费

                AlertDialog.Builder builder = new AlertDialog.Builder(TakeTaxActivity.this);
                builder.setIcon(R.drawable.icon_car);
                builder.setTitle("请选择支付车费方式");
                final String[] pay = {"支付宝支付", "微信支付", "银联支付"};
                //    设置一个单项选择下拉框
                /**
                 * 第一个参数指定我们要显示的一组下拉单选框的数据集合
                 * 第二个参数代表索引，指定默认哪一个单选框被勾选上，0表示默认'支付宝支付' 会被勾选上
                 * 第三个参数给每一个单选项绑定一个监听器
                 */
                builder.setSingleChoiceItems(pay, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(TakeTaxActivity.this, "您选择的支付方式为：" + pay[which], Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setPositiveButton("确定支付", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String desc = String.format("从%s到%s的打车费", et_departure.getText().toString(), et_destination.getText().toString());
                        new AlipayTask(TakeTaxActivity.this, 1).execute("快滴打车-打车费", desc, "0.01");
                        bFinish = false;
                    }
                });
                builder.setNegativeButton("取消支付", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
            }
            mStep++;
        }
    }

    private Handler mHandler = new Handler();
    private double latitude_offset;
    private double longitude_offset;
    private Runnable mAccept = new Runnable() {
        @Override
        public void run() {
            double latitude = mUserPos.latitude + getRandomDecimal();
            double longitude = mUserPos.longitude + getRandomDecimal();
            Log.d(TAG, "latitude=" + latitude + ", longitude=" + longitude);
            latitude_offset = latitude - mUserPos.latitude;
            longitude_offset = longitude - mUserPos.longitude;
            mDriverPos = new LatLng(latitude, longitude);
            rereshCar(mDriverPos);
            speaking("司机马上过来");
            mHandler.postDelayed(mRefresh, mDelayTime / 100);
        }
    };

    private double getRandomDecimal() {
        return 0.05 - Math.random() * 200 % 10.0 / 100.0;
    }

    private Runnable mRefresh = new Runnable() {
        private int i = 0;

        @Override
        public void run() {
            if (i++ < 100) {
                double new_latitude = mUserPos.latitude + latitude_offset * (100 - i) / 100.0;
                double new_longitude = mUserPos.longitude + longitude_offset * (100 - i) / 100.0;
                rereshCar(new LatLng(new_latitude, new_longitude));
                mHandler.postDelayed(this, mDelayTime / 100);
            } else {
                speaking("快车已经到达，请上车");
                mHandler.postDelayed(mTravel, mDelayTime);
            }
        }
    };

    private Runnable mTravel = new Runnable() {
        private int i = 0;

        @Override
        public void run() {
            if (i++ < 100) {
                double new_latitude = mUserPos.latitude + latitude_offset * i / 100.0;
                double new_longitude = mUserPos.longitude + longitude_offset * i / 100.0;
                rereshCar(new LatLng(new_latitude, new_longitude));
                mHandler.postDelayed(this, mDelayTime / 100);
            } else {
                speaking("已经到达目的地，欢迎下次再来乘车");
                btn_travel.setTextColor(getResources().getColor(R.color.black));
                btn_travel.setEnabled(true);
                btn_travel.setText("支付车费");
                bFinish = true;
            }
        }
    };

    private void rereshCar(LatLng pos) {
        mMapLayer.clear();
        OverlayOptions ooCar = new MarkerOptions().draggable(false)
                .visible(true).icon(icon_car).position(pos);
        mMapLayer.addOverlay(ooCar);
    }

    private void initVoiceSetting() {
        mCompose = SpeechSynthesizer.createSynthesizer(this, mComposeInitListener);
        mCompose.setParameter(SpeechConstant.PARAMS, null);
        mCompose.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mCompose.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        SharedPreferences shared = getSharedPreferences(VoiceSettingsActivity.PREFER_NAME, MODE_PRIVATE);
        mCompose.setParameter(SpeechConstant.SPEED, shared.getString("speed_preference", "50"));
        mCompose.setParameter(SpeechConstant.PITCH, shared.getString("pitch_preference", "50"));
        mCompose.setParameter(SpeechConstant.VOLUME, shared.getString("volume_preference", "50"));
        mCompose.setParameter(SpeechConstant.STREAM_TYPE, shared.getString("stream_preference", "3"));
        mCompose.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bFinish != true) {
            mStep = 0;
            mMapLayer.clear();
            tv_travel.setText("准备出发");
            btn_travel.setText("开始叫车");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompose.stopSpeaking();
        mCompose.destroy();
    }

    private void speaking(String text) {
        tv_travel.setText(text);
        int code = mCompose.startSpeaking(text, mComposeListener);
        if (code != ErrorCode.SUCCESS) {
            Toast.makeText(this, "语音合成失败,错误码: " + code, Toast.LENGTH_SHORT).show();
        }
    }

    //初始化监听
    private InitListener mComposeInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                Toast.makeText(TakeTaxActivity.this, "语音初始化失败,错误码: " + code, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private SynthesizerListener mComposeListener = new SynthesizerListener() {
        @Override
        public void onBufferProgress(int arg0, int arg1, int arg2, String arg3) {
        }

        @Override
        public void onCompleted(SpeechError arg0) {
        }

        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
        }

        @Override
        public void onSpeakBegin() {
        }

        @Override
        public void onSpeakPaused() {
        }

        @Override
        public void onSpeakProgress(int arg0, int arg1, int arg2) {
        }

        @Override
        public void onSpeakResumed() {
        }
    };

    // 以下主要是定位用到的代码
    private MapView mMapView;
    private BaiduMap mMapLayer;
    private LocationClient mLocClient;
    private boolean isFirstLoc = true;// 是否首次定位
    private double m_latitude;
    private double m_longitude;
    //定位图层显示方式
    private MyLocationConfiguration.LocationMode locationMode;

    private void initLocation() {
        mMapView = (MapView) findViewById(R.id.mv_dongdong);
        mMapView.setVisibility(View.INVISIBLE);
        mMapLayer = mMapView.getMap();
        mMapLayer.setMyLocationEnabled(true);
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(new MyLocationListenner());
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        mLocClient.setLocOption(option);
        mLocClient.start();
        icon_car = BitmapDescriptorFactory.fromResource(R.drawable.car_small);
    }

    public class MyLocationListenner implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || mMapView == null) {
                Log.d(TAG, "location is null or mMapView is null");
                return;
            }
            m_latitude = location.getLatitude();
            m_longitude = location.getLongitude();
            Log.d(TAG, "m_latitude=" + m_latitude + ", m_longitude=" + m_longitude);
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(100).latitude(m_latitude).longitude(m_longitude).build();
            mMapLayer.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                mUserPos = new LatLng(m_latitude, m_longitude);
                MapStatus mMapStatus;//地图当前状态
                MapStatusUpdate mMapStatusUpdate;//地图将要变化成的状态
                mMapStatus = new MapStatus.Builder().overlook(-45).build();
                mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                mMapLayer.setMapStatus(mMapStatusUpdate);
                MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(mUserPos, 18);
                mMapLayer.animateMapStatus(update);
                mMapView.setVisibility(View.VISIBLE);
                //定义Maker坐标点
                LatLng point = new LatLng(m_latitude + 0.00056, m_longitude);
                //构建Marker图标
                BitmapDescriptor bitmap = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option);
                //定义Maker坐标点
                LatLng point1 = new LatLng(m_latitude, m_longitude + 0.0003);
                //构建Marker图标
                BitmapDescriptor bitmap1 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option1 = new MarkerOptions()
                        .position(point1)
                        .icon(bitmap1);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option1);
                //定义Maker坐标点
                LatLng point2 = new LatLng(m_latitude + 0.0005, m_longitude + 0.001);
                //构建Marker图标
                BitmapDescriptor bitmap2 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option2 = new MarkerOptions()
                        .position(point2)
                        .icon(bitmap2);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option2);
                //定义Maker坐标点
                LatLng point3 = new LatLng(m_latitude + 0.0003, m_longitude + 0.0001);
                //构建Marker图标
                BitmapDescriptor bitmap3 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option3 = new MarkerOptions()
                        .position(point3)
                        .icon(bitmap3);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option3);
                //定义Maker坐标点
                LatLng point4 = new LatLng(m_latitude + 0.0006, m_longitude);
                //构建Marker图标
                BitmapDescriptor bitmap4 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option4 = new MarkerOptions()
                        .position(point4)
                        .icon(bitmap4);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option4);
                //定义Maker坐标点
                LatLng point5 = new LatLng(m_latitude, m_longitude + 0.0004);
                //构建Marker图标
                BitmapDescriptor bitmap5 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option5 = new MarkerOptions()
                        .position(point5)
                        .icon(bitmap5);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option5);

                //定义Maker坐标点
                LatLng point6 = new LatLng(m_latitude, m_longitude - 0.0008);
                //构建Marker图标
                BitmapDescriptor bitmap6 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option6 = new MarkerOptions()
                        .position(point6)
                        .icon(bitmap6);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option1);
                //定义Maker坐标点
                LatLng point7 = new LatLng(m_latitude - 0.0005, m_longitude + 0.0004);
                //构建Marker图标
                BitmapDescriptor bitmap7 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option7 = new MarkerOptions()
                        .position(point7)
                        .icon(bitmap7);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option7);
                //定义Maker坐标点
                LatLng point8 = new LatLng(m_latitude - 0.0003, m_longitude - 0.0005);
                //构建Marker图标
                BitmapDescriptor bitmap8 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option8 = new MarkerOptions()
                        .position(point8)
                        .icon(bitmap8);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option8);
                //定义Maker坐标点
                LatLng point9 = new LatLng(m_latitude - 0.0003, m_longitude);
                //构建Marker图标
                BitmapDescriptor bitmap9 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option9 = new MarkerOptions()
                        .position(point9)
                        .icon(bitmap9);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option9);
                //定义Maker坐标点
                LatLng point10 = new LatLng(m_latitude, m_longitude - 0.0003);
                //构建Marker图标
                BitmapDescriptor bitmap10 = BitmapDescriptorFactory
                        .fromResource(R.drawable.car_small);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option10 = new MarkerOptions()
                        .position(point10)
                        .icon(bitmap10);
                //在地图上添加Marker，并显示
                mMapLayer.addOverlay(option10);
            }

        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    public void getMyLocation() {
        LatLng latLng = new LatLng(m_latitude, m_longitude);
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mMapLayer.setMapStatus(msu);
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
                                mMapLayer.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                                break;
                            case R.id.id_map_site:
                                mMapLayer.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                                break;
                            case R.id.id_map_traffic:
                                if (mMapLayer.isTrafficEnabled()) {
                                    mMapLayer.setTrafficEnabled(false);
                                    item.setTitle("实时交通(off)");
                                } else {
                                    mMapLayer.setTrafficEnabled(true);
                                    item.setTitle("实时交通(on)");
                                }
                                break;
                            case R.id.id_map_mlocation:
                                getMyLocation();
                                break;
                        }
                        return true;
                    }
                });
        popup.show();
    }

    public void onBicycle(View v) {
        Intent intent = new Intent(TakeTaxActivity.this, BicycleActivity.class);
        startActivity(intent);
    }

    // 定义获取动态权限
    private void xPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.RECORD_AUDIO}, 2);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS}, 3);
        }
    }

    /**
     * 重写onRequestPermissionsResult方法
     * 获取动态权限请求的结果,再开启定位
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initLocation();
        } else {
//            Toast.makeText(this, "您拒绝了定位权限", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {
//            Toast.makeText(this, "您拒绝了录音权限", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {
//            Toast.makeText(this, "您拒绝了访问SD卡权限", Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
