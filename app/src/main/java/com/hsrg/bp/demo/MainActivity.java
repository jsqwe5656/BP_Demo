package com.hsrg.bp.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity
{
    //蓝牙适配器
    private BluetoothAdapter mBleAdapter;
    //蓝牙管理器
    BluetoothManager bluetoothManager;

    private boolean mScanning;
    private Handler mHandler;

    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private boolean result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mHandler = new Handler();
        MainActivityPermissionsDispatcher.getPermissionWithPermissionCheck(this);
        bleInit();
        initGPS();
    }

    /**
     * 按钮点击事件
     */
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.button:                   //开始测量
                break;
            case R.id.button2:                  //结束测量
                break;
            case R.id.button3:                  //开启语音
                break;
            case R.id.button4:                  //关闭语音
                break;
            case R.id.button5:                  //打开定位设置
                initGPS();
                break;
            case R.id.button6:                  //开启蓝牙
                bleInit();
                break;
            case R.id.button7:                  //搜索设备
                scanLeDevice(true);
                break;
        }
    }

    /**
     * 初始化蓝牙
     */
    private void bleInit() {
        mBleAdapter = bluetoothManager.getAdapter();
        if (mBleAdapter == null || !mBleAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        }
    }

    /**
     * 监听GPS
     */
    private void initGPS() {
        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        // 判断GPS模块是否开启，如果没有则开启
        if (!locationManager
                .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
        {
            Toast.makeText(MainActivity.this, "请打开GPS", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("请打开GPS");
            dialog.setTitle("提示");
            dialog.setPositiveButton("确定",
                    new android.content.DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                            // 转到手机设置界面，用户设置GPS
                            Intent intent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 1); // 设置完成后返回到原来的界面

                        }
                    });
            dialog.setNeutralButton("取消", new android.content.DialogInterface.OnClickListener()
            {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            });
            dialog.show();
        } else
        {
            // 弹出Toast
            Toast.makeText(MainActivity.this, "GPS is ready",
                    Toast.LENGTH_LONG).show();
            // 弹出对话框
            new AlertDialog.Builder(this).setMessage("GPS is ready")
                    .setPositiveButton("OK", null).show();
        }
    }

    //搜索时长
    private static final long SCAN_PERIOD = 5000;
    /**
     * 搜索设备
     */
    private void scanLeDevice(final boolean enable) {
        if (enable)
        {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run() {
                    mScanning = false;
                    mBleAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBleAdapter.startLeScan(mLeScanCallback);
        } else
        {
            mScanning = false;
            mBleAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * 蓝牙搜索回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    Log.e("zbf", device.getName() + "," + device.getAddress() + "," + rssi);
                    String deviceName = device.getName() + "";
                    if (deviceName.startsWith("BPM-"))
                    {
                        scanLeDevice(false);
                        connectDevice(device);
                    }
                }
            });
        }
    };


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.e("zbf", "初始化蓝牙服务");
            if (!mBluetoothLeService.initialize())
            {
                Log.e("zbf", "无法初始化蓝牙");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
//            mBluetoothLeService.connect(mDeviceAddress);
            result = mBluetoothLeService.connect(mDeviceAddress);
            //开始搜索，搜索到开始连接
            scanLeDevice(true);
        }
        //断开连接
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /**
     * 链接设备
     */
    private void connectDevice(BluetoothDevice device) {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        mDeviceAddress = device.getAddress();
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case 0:
                if (resultCode == RESULT_OK)
                {
                    Toast.makeText(MainActivity.this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                } else
                {
                    Toast.makeText(MainActivity.this, "蓝牙开启失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case 1:
                bleInit();
                break;
        }
    }

    @NeedsPermission(value = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, maxSdkVersion = 26)
    void getPermission() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

//    @OnShowRationale({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
//    void showPermission(final PermissionRequest request) {
//    }
}
