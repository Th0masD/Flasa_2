package com.example.flasa2;

import java.util.UUID;

public class Constants {

    // hlavná service
    public static final UUID HEART_RATE_SERVICE_UUID =
            UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");

    // Baterka service
    public static final UUID BATTERY_SERVICE_UUID =
            UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    // sem budeme zapisovať 8-miestne číslo
    public static final UUID HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID =
            UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    // Poslie notifikaciu Serveru pri TESTUJEM
    public static final UUID BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID =
            UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");


    // sem budeme čítať batériu
    public static final UUID BATTERY_LEVEL_UUID =
            UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");



    // descriptor pre notifications
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

}