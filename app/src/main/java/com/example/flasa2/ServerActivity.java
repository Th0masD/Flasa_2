package com.example.flasa2;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ServerActivity extends AppCompatActivity {

    private BleServerManager bleServerManager;
    private Button btnNotify;
    private TextView txtReceived;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        btnNotify = findViewById(R.id.btnNotify);

        txtReceived = findViewById(R.id.txtReceived);
        txtStatus = findViewById(R.id.txtStatus);

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        BluetoothAdapter adapter = bluetoothManager.getAdapter();

        bleServerManager = new BleServerManager(this, adapter);

        bleServerManager.setCallback(new BleCallback() {
            @Override
            public void onConnected() {
                runOnUiThread(() ->
                        txtStatus.setText("Client connected"));
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() ->
                        txtStatus.setText("Client disconnected"));
            }

            @Override
            public void onServicesDiscovered() {
                // server side not needed
            }

            @Override
            public void onDataReceived(String uuid, byte[] data) {

                String value = new String(data);

                runOnUiThread(() ->
                        txtReceived.setText("Received: " + value));
            }

            @Override
            public void onNotificationReceived(String message) {

            }


            @Override
            public void onBatteryReceived(int batteryLevel) {
                // optional server UI
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        txtStatus.setText("Error: " + message));
            }
        });

        btnNotify.setOnClickListener(v -> {
            Log.d(TAG, "Notify Button");
            bleServerManager.sendNotification("Test");

        });


        bleServerManager.start();
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleServerManager.stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        bleServerManager.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bleServerManager.stop();
    }

}