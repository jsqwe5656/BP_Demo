package com.hsrg.bp.demo;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * 血压测试界面
 */
public class BPM_Activity extends AppCompatActivity
{
    //蓝牙服务
    private BluetoothLeService mBluetoothLeService;
    private boolean result;
    private String mDeviceAddress;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private ArrayList<BluetoothGattCharacteristic> charas;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean flg;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    //搜索完成
    private boolean scannDone;

    /**
     * 代码管理服务生命周期。以bond 的方式启动服务
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.e("zbf", "初始化蓝牙服务");

            if (!mBluetoothLeService.initialize())
            {
                Log.e("zbf", "无法初始化蓝牙");
                finish();
            }

            // 自动连接到装置上成功启动初始化。
            //00:15:83:00:6B:F6
            result = mBluetoothLeService.connect(mDeviceAddress);

            //开始搜索，搜索到开始连接
            scanLeDevice(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            //断开链接
            mBluetoothLeService.disconnect();
            mBluetoothLeService = null;
        }
    };

    // 处理各种事件的服务了。
    // action_gatt_connected连接到服务器：关贸总协定。
    // action_gatt_disconnected：从关贸总协定的服务器断开。
    // action_gatt_services_discovered：关贸总协定的服务发现。
    // action_data_available：从设备接收数据。这可能是由于阅读或通知操作。
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
    {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {
                result = true;
                mBluetoothLeService.write(mNotifyCharacteristic, ParserHelper.setBpmWriteData(161, 60));

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                result = false;
                mBluetoothLeService.close();
                mBluetoothLeService.connect(mDeviceAddress);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                // 显示所有的支持服务的特点和用户界面。
                List<BluetoothGattService> supportedGattServices = mBluetoothLeService.getSupportedGattServices();
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                for (int i = 0; i < supportedGattServices.size(); i++)
                {
                    Log.e("a", "1:BluetoothGattService UUID=:" + supportedGattServices.get(i).getUuid());
                    List<BluetoothGattCharacteristic> cs = supportedGattServices.get(i).getCharacteristics();
                    for (int j = 0; j < cs.size(); j++)
                    {
                        Log.e("a", "2:   BluetoothGattCharacteristic UUID=:" + cs.get(j).getUuid());

                        List<BluetoothGattDescriptor> ds = cs.get(j).getDescriptors();
                        for (int f = 0; f < ds.size(); f++)
                        {
                            Log.e("a", "3:      BluetoothGattDescriptor UUID=:" + ds.get(f).getUuid());

                            byte[] value = ds.get(f).getValue();

                            Log.e("a", "4:     			value=:" + Arrays.toString(value));
                            Log.e("a", "5:     			value=:" + Arrays.toString(ds.get(f).getCharacteristic().getValue()));
                        }
                    }
                }
                //				startBackData();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
            {
                //实时赋值
//                useData(intent);
            } else if (BluetoothLeService.ACTION_RSSI.equals(action))
            {
                Log.e("zbf", "信号强度--------->" + intent.getStringExtra(BluetoothLeService.ACTION_DATA_RSSI));
            }
        }
    };


    // 我们是注定的expandablelistview数据结构
    //  在UI。
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void displayGattServices(List<BluetoothGattService> gattServices)
    {
        if (gattServices == null)
            return;
        String uuid = null;
        String unknownServiceString = "service_UUID";
        String unknownCharaString = "characteristic_UUID";

        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();

        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // 循环遍历服务
        for (BluetoothGattService gattService : gattServices)
        {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            currentServiceData.put("NAME", SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put("UUID", uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            charas = new ArrayList<BluetoothGattCharacteristic>();

            // 循环遍历特征
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
            {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                currentCharaData.put("NAME", SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put("UUID", uuid);

                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        final BluetoothGattCharacteristic characteristic = charas.get(charas.size() - 1);
        final int charaProp = characteristic.getProperties();

        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0)
        {
            if (mNotifyCharacteristic != null)
            {
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
        {
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(
                    characteristic, true);
        }

        startBackData();

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        // 初始化一个蓝牙适配器。对API 18级以上，可以参考 bluetoothmanager。
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    /**
     * 重新注册广播接收器
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        /**
         * 注册广播
         */
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null)
        {
            Log.e("a", "来了");
            result = mBluetoothLeService.connect(mDeviceAddress);
            Log.e("a", "连接请求的结果=" + result);

        }
    }

    /**
     * 销毁广播接收器
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        flg = false;
        unregisterReceiver(mGattUpdateReceiver);
    }

    /**
     * 销毁方法调用时结束服务
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mBluetoothLeService.close();
        try
        {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            unbindService(mServiceConnection);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 注册广播
     *
     * @return 意图
     */
    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RSSI);
        return intentFilter;
    }

    /**
     * 开始接受设备返回的数据
     */
    private void startBackData()
    {
        BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(3).get(0);
        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
    }

    //搜索设备
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            //停止后一个预定义的扫描周期扫描。
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 8000);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else
        {
            mScanning = false;
            scannDone = true;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

        }

        invalidateOptionsMenu();
    }

    // 扫描装置的回调。
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord)
        {
            runOnUiThread(new Runnable()
            {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void run()
                {
                    HashMap<String, Object> map = new HashMap<String, Object>();

                    Log.e("a", "RSSI=:" + rssi + "");

                    Log.e("a", "发现蓝牙" + device.getAddress() + "状态" + device.getBondState() + "type" + device.getType() + device.describeContents() + "name" + device.getName());
                    try
                    {
                        String name = device.getName();
                        if (name.equals("Technaxx BP") || name.equals("BPM-188"))
                        {
                            mDeviceAddress = device.getAddress();
                            //停止搜索
                            scanLeDevice(false);
//                            tv_dia_connect.setText(getResources().getString(R.string.dialog_tv_connecting));
                            //连接
                            mBluetoothLeService.connect(mDeviceAddress);
                        }
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }


                }
            });
        }
    };


}
