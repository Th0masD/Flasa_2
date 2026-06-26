package com.example.flasa2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BleServerManager {


    private BluetoothGattService batteryService;
    private BluetoothGattService mainService;

    private static final String TAG = "BleServerManager";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothManager bluetoothManager;
    private BluetoothDevice connectedDevice;

    private BluetoothGattServer gattServer;
    private BluetoothLeAdvertiser advertiser;

    private boolean isAdvertising = false;
    private boolean serviceReady = false;

    private BleCallback callback;

    private final Map<String, BluetoothDevice> connectedDevices = new HashMap<>();

    public BleServerManager(Context context, BluetoothAdapter adapter) {
        this.context = context;
        this.bluetoothAdapter = adapter;
        this.bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public void setCallback(BleCallback callback) {
        this.callback = callback;
    }

    // ---------------- START ----------------

    public void start() {

        stop(); // clean state

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback);

        if (gattServer == null) {
            Log.e(TAG, "GATT server is NULL");
            return;
        }


        mainService = createMainService();
        batteryService = createBatteryService();


        Log.e(TAG, "ADDING mainService");

        gattServer.addService(mainService);

    }

    // ---------------- STOP ----------------

    public void stop() {

        Log.e(TAG, "STOP BLE SERVER");

        isAdvertising = false;
        serviceReady = false;

        if (advertiser != null) {
            try {
                advertiser.stopAdvertising(advertiseCallback);
            } catch (Exception e) {
                Log.e(TAG, "stopAdvertising error", e);
            }
            advertiser = null;
        }

        if (gattServer != null) {
            try {
                gattServer.close();
            } catch (Exception e) {
                Log.e(TAG, "gattServer close error", e);
            }
            gattServer = null;
        }
    }



    // ---------------- SERVICE ----------------
    private BluetoothGattCharacteristic notifyChar;

    private BluetoothGattService createMainService() {

        BluetoothGattService service =
                new BluetoothGattService(
                        Constants.HEART_RATE_SERVICE_UUID,
                        BluetoothGattService.SERVICE_TYPE_PRIMARY
                );

        BluetoothGattCharacteristic writeChar1 =
                new BluetoothGattCharacteristic(
                        Constants.HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID,
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_WRITE
                );

        notifyChar =
                new BluetoothGattCharacteristic(
                        Constants.BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ
                );

        BluetoothGattDescriptor configDescriptor =
                new BluetoothGattDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                        BluetoothGattDescriptor.PERMISSION_READ
                                | BluetoothGattDescriptor.PERMISSION_WRITE
                );

        notifyChar.addDescriptor(configDescriptor);


        service.addCharacteristic(writeChar1);
        service.addCharacteristic(notifyChar);

        return service;
    }

    private BluetoothGattService createBatteryService() {

        BluetoothGattService batteryService =
                new BluetoothGattService(
                        Constants.BATTERY_SERVICE_UUID,
                        BluetoothGattService.SERVICE_TYPE_PRIMARY
                );

        BluetoothGattCharacteristic batteryChar =
                new BluetoothGattCharacteristic(
                        Constants.BATTERY_LEVEL_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ
                                | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ
                );

        BluetoothGattDescriptor configDescriptor =
                new BluetoothGattDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                        BluetoothGattDescriptor.PERMISSION_READ
                                | BluetoothGattDescriptor.PERMISSION_WRITE
                );

        batteryChar.addDescriptor(configDescriptor);

     //   batteryChar.setValue(new byte[]{85});

        batteryService.addCharacteristic(batteryChar);

        return batteryService;
    }


    private byte getBatteryLevel() {
        android.os.BatteryManager batteryManager =
                (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        int level = batteryManager.getIntProperty(
                android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        return (byte) level;
    }


    // ---------------- GATT CALLBACK ----------------

    private final BluetoothGattServerCallback gattServerCallback =
            new BluetoothGattServerCallback() {

                @Override
                public void onServiceAdded(int status, BluetoothGattService service) {

                    Log.e(TAG, "onServiceAdded status=" + status);

                    if (status == BluetoothGatt.GATT_SUCCESS) {

                        if (service.getUuid().equals(mainService.getUuid())) {

                            Log.e(TAG, "ADDING BATTERY SERVICE");
                            gattServer.addService(batteryService);

                        } else if (service.getUuid().equals(batteryService.getUuid())) {

                            serviceReady = true;
                            startAdvertising();
                        }
                    }
                }

                @Override
                public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {

                    if (newState == BluetoothProfile.STATE_CONNECTED) {

                        //connectedDevices.put(device.getAddress(), device);
                        connectedDevice = device;

                        if (callback != null) callback.onConnected();

                        Log.d(TAG, "Client connected: " + device.getAddress());
                    }


                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        connectedDevices.remove(device.getAddress());

                        if (callback != null) callback.onDisconnected();

                        Log.d(TAG, "Client disconnected");
                    }
                }


                @Override
                public void onDescriptorWriteRequest(
                        BluetoothDevice device,
                        int requestId,
                        BluetoothGattDescriptor descriptor,
                        boolean preparedWrite,
                        boolean responseNeeded,
                        int offset,
                        byte[] value) {

                    if (descriptor.getUuid().equals(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))) {

                        if (Arrays.equals(value,
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {

                            Log.e(TAG, "Notifications enabled");
                        }
                    }

                    gattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_SUCCESS, 0, null);
                }





                @Override
                public void onCharacteristicReadRequest(
                        BluetoothDevice device,
                        int requestId,
                        int offset,
                        BluetoothGattCharacteristic characteristic) {

                    if (characteristic == null) {
                        return;
                    }

                    byte battery = getBatteryLevel();

                    characteristic.setValue(new byte[]{battery});

                    gattServer.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            characteristic.getValue()
                    );
                }


                @Override
                public void onCharacteristicWriteRequest(
                        BluetoothDevice device,
                        int requestId,
                        BluetoothGattCharacteristic characteristic,
                        boolean preparedWrite,
                        boolean responseNeeded,
                        int offset,
                        byte[] value) {

                    // Aby 2X nezapisoval
                    if (preparedWrite) return;
                    if (offset != 0) return;

                    String data = new String(value);

                    Log.d(TAG, "WRITE ONLY ONCE: " + data);

                    if (callback != null) {
                        callback.onDataReceived(
                                characteristic.getUuid().toString(),
                                value
                        );
                    }



                    Log.d(TAG, "Received: " + data);



                    if (responseNeeded && gattServer != null) {
                        gattServer.sendResponse(
                                device,
                                requestId,
                                android.bluetooth.BluetoothGatt.GATT_SUCCESS,
                                0,
                                null
                        );
                    }
                }
            };


    public void sendNotification(String text) {

        if (connectedDevice != null && notifyChar != null) {

            notifyChar.setValue(text.getBytes());

            gattServer.notifyCharacteristicChanged(
                    connectedDevice,
                    notifyChar,
                    false
            );

            Log.d(TAG, "Notification sent: " + text);
        }


    }

    // ---------------- ADVERTISING ----------------

    private void startAdvertising() {

        if (!serviceReady) {
            Log.w(TAG, "Service not ready yet");
            return;
        }

        if (isAdvertising) {
            Log.w(TAG, "Already advertising");
            return;
        }

        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        if (advertiser == null) {
            Log.e(TAG, "Advertising not supported");
            return;
        }

        AdvertiseSettings settings =
                new AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setConnectable(true)
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                        .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(Constants.HEART_RATE_SERVICE_UUID))
                .build();

        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();


        advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback);

        isAdvertising = true;

        Log.e(TAG, "Advertising started request");
    }

    private final AdvertiseCallback advertiseCallback =
            new AdvertiseCallback() {

                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.d(TAG, "Advertising started OK");
                }

                @Override
                public void onStartFailure(int errorCode) {
                    Log.e(TAG, "Advertising failed: " + errorCode);
                    isAdvertising = false;
                }
            };
}