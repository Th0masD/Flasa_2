package com.example.flasa2;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;

    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private List<String> deviceNames = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private ListView listView;

    private static final long FLASA_SCAN_TIMEOUT = 8000; // 8 sekúnd

    private boolean flasaFound = false;

    private final Handler scanHandler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        listView = findViewById(R.id.listDevices);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                deviceNames
        );

        listView.setAdapter(adapter);

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        bluetoothAdapter = bluetoothManager.getAdapter();
        scanner = bluetoothAdapter.getBluetoothLeScanner();

        startScan();

        listView.setOnItemClickListener(
                (parent, view, position, id) -> {

                    BluetoothDevice device =
                            deviceList.get(position);

                    Intent intent =
                            new Intent(
                                    DiscoveryActivity.this,
                                    InputActivity.class
                            );

                    intent.putExtra(
                            "device_address",
                            device.getAddress()
                    );

                    startActivity(intent);
                }
        );
    }

    @SuppressLint("MissingPermission")
    private void startScan() {

        flasaFound = false;

        deviceList.clear();
        deviceNames.clear();

        adapter.notifyDataSetChanged();

        Log.d("DiscoveryActivity",
                "Starting BLE scan for FLASA");

        scanner.startScan(scanCallback);

        // FLASA hľadáme maximálne 8 sekúnd
        scanHandler.postDelayed(
                stopFlasaScan,
                FLASA_SCAN_TIMEOUT
        );
    }

    private final Runnable stopFlasaScan = new Runnable() {

        @SuppressLint("MissingPermission")
        @Override
        public void run() {

            if (!flasaFound) {

                Log.d("DiscoveryActivity",
                        "FLASA not found after 8 seconds");

                scanner.stopScan(scanCallback);

                adapter.notifyDataSetChanged();
            }
        }
    };

    private final ScanCallback scanCallback =
            new ScanCallback() {

                @SuppressLint("MissingPermission")
                @Override
                public void onScanResult(
                        int callbackType,
                        ScanResult result) {

                    BluetoothDevice device =
                            result.getDevice();

                    String name = device.getName();

                    Log.d("DiscoveryActivity",
                            "BLE device: "
                                    + name
                                    + " / "
                                    + device.getAddress());

                    // -----------------------------------------
                    // FLASA FOUND
                    // -----------------------------------------

                    if ("FLASA".equals(name)) {

                        if (flasaFound) {
                            return;
                        }

                        flasaFound = true;

                        Log.d("DiscoveryActivity",
                                "FLASA FOUND - opening InputActivity");

                        // Zrušíme timeout
                        scanHandler.removeCallbacks(
                                stopFlasaScan
                        );

                        // Zastavíme BLE scan
                        scanner.stopScan(scanCallback);

                        // Automaticky otvoríme InputActivity
                        Intent intent =
                                new Intent(
                                        DiscoveryActivity.this,
                                        InputActivity.class
                                );

                        intent.putExtra(
                                "device_address",
                                device.getAddress()
                        );

                        startActivity(intent);

                        return;
                    }

                    // -----------------------------------------
                    // OSTATNÉ ZARIADENIA
                    // -----------------------------------------

                    if (!deviceList.contains(device)) {

                        deviceList.add(device);

                        if (name == null) {
                            name = "Unknown device";
                        }

                        deviceNames.add(
                                name
                                        + "\n"
                                        + device.getAddress()
                        );

                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {

                    Log.e("DiscoveryActivity",
                            "BLE scan failed: "
                                    + errorCode);
                }
            };

    @Override
    protected void onDestroy() {

        super.onDestroy();

        // Zrušíme prípadný timeout
        scanHandler.removeCallbacks(
                stopFlasaScan
        );

        // Zastavíme scan
        if (scanner != null) {

            try {
                scanner.stopScan(scanCallback);
            } catch (Exception e) {

                Log.e(
                        "DiscoveryActivity",
                        "Error stopping scan",
                        e
                );
            }
        }
    }
}
