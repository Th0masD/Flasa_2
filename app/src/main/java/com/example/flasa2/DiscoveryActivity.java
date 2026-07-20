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

        listView.setOnItemClickListener((parent, view, position, id) -> {

            BluetoothDevice device = deviceList.get(position);

            Intent intent =
                    new Intent(DiscoveryActivity.this, InputActivity.class);

            intent.putExtra("device_address", device.getAddress());

            startActivity(intent);
        });
    }

    @SuppressLint("MissingPermission")
    private void startScan() {
        scanner.startScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            BluetoothDevice device = result.getDevice();

            if (!deviceList.contains(device)) {

                deviceList.add(device);

                String name = device.getName();

                if (name == null) {
                    name = "Unknown device";
                }

                deviceNames.add(name + "\n" + device.getAddress());

                adapter.notifyDataSetChanged();
            }
        }
    };
}

