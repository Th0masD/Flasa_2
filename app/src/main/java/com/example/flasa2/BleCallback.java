package com.example.flasa2;

public interface BleCallback {

    void onConnected();

    void onDisconnected();

    void onServicesDiscovered();

    void onDataReceived(String uuid, byte[] data);

    void onBatteryReceived(int batteryLevel);

    void onNotificationReceived(String message);

    void onError(String message);
}
