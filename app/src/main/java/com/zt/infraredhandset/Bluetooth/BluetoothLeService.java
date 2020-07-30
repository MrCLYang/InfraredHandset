/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zt.infraredhandset.Bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.zt.infraredhandset.FrameUtils.DataUtils;


import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public  class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private String strData;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    public  boolean isHexReceive = false;
    private static final UUID SERVICE_UUID = UUID.fromString("0000ff12-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFY_UUID =  UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    private static final UUID WRITE_UUID =  UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");

    public BluetoothGattCharacteristic mWriteCharateristic, mNotifyCharateristic;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.lancens.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.lancens.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.lancens.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.lancens.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.lancens.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            System.out.println("BluetoothLeService onConnectionStateChange");
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            System.out.println("BluetoothService  onServicesDiscovered ");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("BluetoothService  onServicesDiscovered  GATT_SUCCESS");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("BluetoothLeService  onCharacteristicRead ");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("BluetoothLeService  onCharacteristicRead   GATT_SUCCESS ");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            System.out.println("BluetoothLeService onCharacteristicChanged ");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("BluetoothLeService wrrite ");
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }}
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

  //  private int packCount = 0;
    private StringBuilder packBuffer = new StringBuilder();
   // private int packIndex = 0;	//数据包标识
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);


            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            //如果是十六进制开关,则进入十六进制显示
                if (data != null && data.length >4 ) {
                    Log.e("lanya","收到+++++   "+ DataUtils.bytesToHexString(data));

                  try{
                    String str = DataUtils.bytesToHexString(data);
                    int count = Integer.parseInt(str.substring(0, 2));	//数据包总数
                    int index= Integer.parseInt(str.substring(2, 4));    //包顺序
                    if (index<=count&&index!=0){
                       // packCount++;
                        if(index<=count){

                            packBuffer.append(str.substring(4));
                            if (index==count){
                                intent.putExtra(EXTRA_DATA, packBuffer.toString());
                                packBuffer = new StringBuilder();
                                sendBroadcast(intent);
                            }/*else if (index==count-1){
                                intent.putExtra(EXTRA_DATA, packBuffer.toString());
                                packBuffer = new StringBuilder();
                                sendBroadcast(intent);
                            }*/
                            /*if(index<packCount)
                                packIndex -= (packCount-index)*36;
                            packBuffer.insert(packIndex, str.substring(4,str.length()));
                            packIndex =packBuffer.length();
                            if(packCount==count){
                                intent.putExtra(EXTRA_DATA, packBuffer.toString());
                                Log.e("lanya" ,packBuffer.toString());
                                packCount = 0;
                                packIndex = 0;
                               packBuffer = new StringBuilder();
                                sendBroadcast(intent);

                            }*/

                        }
                    }else{
                        packBuffer = new StringBuilder();
                    }
                    }

                    catch (Exception e){
                      Log.e("lanya",e.toString());
                        packBuffer = new StringBuilder();
                  }

        }

    }

    private int c = 0;


    public  class LocalBinder extends Binder {
      public  BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("BluetoothLeService onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            System.out.println(TAG + " Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void discoverServices() {
        if (mBluetoothGatt != null) {
            System.out.println("BluetoothLeService discoverServices");
            mBluetoothGatt.discoverServices();
        }
    }

    public void setServives() {
        if (mBluetoothGatt != null) {
            BluetoothGattService service = mBluetoothGatt.getService(SERVICE_UUID);
            if (service != null) {
                mWriteCharateristic = service.getCharacteristic(WRITE_UUID);
                mNotifyCharateristic  = service.getCharacteristic(NOTIFY_UUID);
                setCharacteristicNotification(mNotifyCharateristic,true );
            }
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        System.out.println("BluetoothLeService >>>> setCharacteristicNotification() ");
       boolean isEnableNotification =  mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
   /*   BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);*/

        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

       /* if(isEnableNotification) {
            List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
            if(descriptorList != null && descriptorList.size() > 0) {
                for(BluetoothGattDescriptor descriptor : descriptorList) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }*/
    }

    //写数据到ble
    public void writeCharacteristic(BluetoothGattCharacteristic writeCharacteristic) {
        if ( mBluetoothAdapter == null||mBluetoothGatt == null) {

            return;
        }
        //mBluetoothGatt.setCharacteristicNotification(mNotifyCharateristic,true);
        //setCharacteristicNotification(mWriteCharateristic,true);
       boolean status= mBluetoothGatt.writeCharacteristic(writeCharacteristic);
        if (!status)

        Toast.makeText(this,"蓝牙发送失败，请重启蓝牙", Toast.LENGTH_SHORT).show();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    private int count = 10000;
    private byte[] data = new byte[]
            {0x00, 0x01, 0x02, 0x03, 0x04,
                    0x05, 0x06, 0x07, 0x08, 0x09,
                    0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
                    0x0F, 0x10, 0x11, 0x12, 0x13};
    private int delayMillis = 10;
    private long time = 0;
    private boolean isTest = false;


    public void sendData() {
        System.out.println(">>>>>>>>>>writeData time=" + time);

        if (mBluetoothAdapter == null || mBluetoothGatt == null || mWriteCharateristic == null) {
            return;
        }
        long m = Math.abs(System.currentTimeMillis() - time);
        if (m < delayMillis) {
            try {
                Thread.sleep(delayMillis - m);
                System.out.println(">>>>>>>>>>delayWriteMillis=" + (delayMillis - m));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        time = System.currentTimeMillis();
        data[0]++;
        count--;
        mWriteCharateristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(mWriteCharateristic);
    }

    public void setDelayMillis(int delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void setTest(boolean test) {
        this.isTest = test;
    }

    public int getCount() {
        return count;
    }

    public boolean isTest() {
        return isTest;
    }

    public void setCount(int count) {
        this.count = count;
    }

}
