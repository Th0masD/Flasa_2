package com.example.flasa2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.BluetoothManager;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class BleManager {

    private static final String TAG = "BleManager";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;

    private BluetoothGatt bluetoothGatt;
    private BleCallback callback;

    private final Queue<Runnable> operationQueue = new LinkedList<>();
    private boolean isOperationRunning = false;

    public BleManager(Context context, BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }

/*
    public interface BleCallback {
        void onNotificationReceived(String message);
    }
*/
    public void setCallback(BleCallback callback) {
        this.callback = callback;
    }

    public void connect(BluetoothDevice device) {

        Log.d(TAG, "CONNECT: " + device.getAddress());

        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);

    }

    public void disconnect() {
        Log.d(TAG, "FULL BLE DISCONNECT");

        operationQueue.clear();
        isOperationRunning = false;

        BluetoothGatt gatt = bluetoothGatt;

        if (gatt != null) {
            try {
                gatt.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "disconnect error", e);
            }
        }

        // ZATIAĽ NEVOLAJ gatt.close()
        bluetoothGatt = null;

        Log.d(TAG, "BLE disconnect requested");
    }


    public void readBattery() {

        if (bluetoothGatt == null) return;

        BluetoothGattService service =
                bluetoothGatt.getService(Constants.BATTERY_SERVICE_UUID);

        Log.d(TAG, "Battery service = " + service);


        if (service == null) return;

        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(
                        Constants.BATTERY_LEVEL_UUID);

        //Log.d(TAG, "Battery char UUID = " + characteristic.getUuid());
        //Log.d(TAG, "Battery char instance = " + characteristic);


        if (characteristic == null) return;

        operationQueue.add(() -> {
            isOperationRunning = true;

            boolean success = bluetoothGatt.readCharacteristic(characteristic);

            if (!success) {
                isOperationRunning = false;
                nextOperation();
            }
        });

        nextOperation();
    }

    public void readStatus() {

        if (bluetoothGatt == null) return;

        BluetoothGattService service = bluetoothGatt.getService(Constants.HEART_RATE_SERVICE_UUID);

        Log.d(TAG, "READ STATUS");


        if (service == null) return;

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID);

       // Log.d(TAG, "Heart rate char UUID = " + characteristic.getUuid());
       // Log.d(TAG, "Heart rate char instance = " + characteristic);


        if (characteristic == null) return;

        operationQueue.add(() -> {
            isOperationRunning = true;

            boolean success = bluetoothGatt.readCharacteristic(characteristic);

            if (!success) {
                isOperationRunning = false;
                nextOperation();
            }
        });

        nextOperation();
    }

    public void writeNumber(String number) {

        if (bluetoothGatt == null) return;

        BluetoothGattService service = bluetoothGatt.getService(Constants.HEART_RATE_SERVICE_UUID);

        Log.d(TAG, "BLEMANAGER writenumber" + number);

        if (service == null) return;

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID);

        if (characteristic == null) return;

        operationQueue.add(() -> {
            isOperationRunning = true;

            characteristic.setValue(number.getBytes());

            boolean success = bluetoothGatt.writeCharacteristic(characteristic);

            if (!success) {
                isOperationRunning = false;
                nextOperation();
            }
        });

        nextOperation();
    }

    private void nextOperation() {

        if (isOperationRunning) return;

        Runnable operation = operationQueue.poll();

        if (operation != null) {
            operation.run();
        }
    }


    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {


                @Override
                public void onConnectionStateChange(
                        BluetoothGatt gatt,
                        int status,
                        int newState) {

                    Log.d(TAG, "STATE: gatt="
                            + System.identityHashCode(gatt)
                            + " status=" + status
                            + " state=" + newState);

                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        Log.d(TAG, "DISCONNECTED");

                        try {
                            gatt.close();
                        } catch (Exception e) {
                            Log.e(TAG, "close error", e);
                        }

                        if (bluetoothGatt == gatt) {
                            bluetoothGatt = null;
                        }

                        if (callback != null) {
                            callback.onDisconnected();
                        }
                    }

                    else if (newState == BluetoothProfile.STATE_CONNECTED) {

                        Log.d(TAG, "CONNECTED");

                        bluetoothGatt = gatt;

                        gatt.discoverServices();
                    }
                }

                @Override
                public void onCharacteristicChanged(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic) {

                    Log.d(TAG, "!!! NOTIFICATION RECEIVED !!! UUID="
                            + characteristic.getUuid());

                    byte[] data = characteristic.getValue();
                    String message = new String(data);

                    Log.e(TAG, "Notification received: " + message);

                    if (message.startsWith("ACK:")) {
                        Log.d(TAG, "SERVER POTVRDIL PRIJATIE: " + message);

                        // tu môžeš napr. nastaviť boolean / oznámiť UI
                    }
                    else Log.d(TAG, "SERVER NEPOTVRDIL PRIJATIE: " + message);


                    if (callback != null) {
                        callback.onNotificationReceived(message);
                    }

                }



                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    Log.d(TAG, "SERVICES: status=" + status);

                    for (BluetoothGattService s : gatt.getServices()) {
                        Log.d(TAG, "Service: " + s.getUuid());

                        for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                            Log.d(TAG, "Char: " + c.getUuid() + " / " + c);
                        }
                    }

                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.e(TAG, "Service discovery failed: " + status);
                        return;
                    }

                    Log.d(TAG, "Services discovered");

                    BluetoothGattService service =
                            gatt.getService(Constants.HEART_RATE_SERVICE_UUID);

                    if (service != null) {

                        BluetoothGattCharacteristic notifyChar =
                                service.getCharacteristic(
                                        Constants.BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID);

                        if (notifyChar != null) {

                            gatt.setCharacteristicNotification(notifyChar, true);

                            BluetoothGattDescriptor descriptor =
                                    notifyChar.getDescriptor(
                                            UUID.fromString(
                                                    "00002902-0000-1000-8000-00805f9b34fb"));

                            if (descriptor != null) {

                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                                // TU už callback NEVOLAJ
                                gatt.writeDescriptor(descriptor);

                                return;
                            }
                        }
                    }

                    // Ak nebolo treba zapisovať descriptor,
                    // služby sú pripravené okamžite.
                    if (callback != null) {
                        callback.onServicesDiscovered();
                    }
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt,
                                              BluetoothGattDescriptor descriptor,
                                              int status) {

                    Log.d(TAG, "Descriptor write finished, status = " + status);

                    if (status == BluetoothGatt.GATT_SUCCESS) {

                        if (callback != null) {
                            callback.onServicesDiscovered();
                        }

                    } else {
                        Log.e(TAG, "Descriptor write failed: " + status);
                    }
                }


                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                    isOperationRunning = false;

                    if (status == BluetoothGatt.GATT_SUCCESS) {



                        if (Constants.BATTERY_LEVEL_UUID.equals(characteristic.getUuid())) {


                            int battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

                            if (callback != null) {
                                callback.onBatteryReceived(battery);
                            }
                        }

                        else if (Constants.HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {


                            String status1 = characteristic.getStringValue(0);

                            if (callback != null) {
                                callback.onStatusReceived(status1);
                            }
                        }


                    }

                    nextOperation();
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  int status) {

                    isOperationRunning = false;

                    Log.d(TAG, "Write success");

                    nextOperation();
                }
            };

}