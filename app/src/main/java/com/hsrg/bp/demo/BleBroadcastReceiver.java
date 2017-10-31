package com.hsrg.bp.demo;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 蓝牙广播接收
 * Created by hs-301 on 2017/10/31.
 */

public class BleBroadcastReceiver extends BroadcastReceiver
{
//    private String mDeviceAddress;
    private boolean result;
    public static BluetoothGattCharacteristic mNotifyCharacteristic;
    public static  ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    public static ArrayList<BluetoothGattCharacteristic> charas;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
        {
            result = true;
//            mBluetoothLeService.write(mNotifyCharacteristic, ParserHelper.setBpmWriteData(161, 60));

        } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
        {
            result = false;
            MainActivity.mBluetoothLeService.close();
//            mBluetoothLeService.connect(mDeviceAddress);
        } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
        {
            // 显示所有的支持服务的特点和用户界面。
            List<BluetoothGattService> supportedGattServices = MainActivity.mBluetoothLeService.getSupportedGattServices();
            displayGattServices(MainActivity.mBluetoothLeService.getSupportedGattServices());
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

    // 我们是注定的expandablelistview数据结构
    private void displayGattServices(List<BluetoothGattService> gattServices) {
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
                MainActivity.mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            MainActivity.mBluetoothLeService.readCharacteristic(characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
        {
            mNotifyCharacteristic = characteristic;
            MainActivity.mBluetoothLeService.setCharacteristicNotification(characteristic, true);
        }

        startBackData();
    }

    /**
     * 开始接受设备返回的数据
     */
    private void startBackData() {
        BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(3).get(0);
        MainActivity.mBluetoothLeService.setCharacteristicNotification(characteristic, true);
    }

}
