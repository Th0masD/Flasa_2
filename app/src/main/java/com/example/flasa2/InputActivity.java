package com.example.flasa2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InputActivity extends AppCompatActivity {

    private BleManager bleManager;

    private EditText inputNumber;
    private Button btnSend, btnBattery;
    private TextView txtBattery;
    private TextView txtNotify;

    private BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        inputNumber = findViewById(R.id.inputNumber);
        btnSend = findViewById(R.id.btnSend);
        btnBattery = findViewById(R.id.btnBattery);
        txtBattery = findViewById(R.id.txtBattery);
        txtNotify = findViewById(R.id.txtNotify);

        String address = getIntent().getStringExtra("device_address");

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        BluetoothAdapter adapter = bluetoothManager.getAdapter();

        device = adapter.getRemoteDevice(address);

        bleManager = new BleManager(this, adapter);

        bleManager.setCallback(new BleCallback() {
            @Override
            public void onConnected() {
                runOnUiThread(() ->
                        Toast.makeText(InputActivity.this,
                                "Connected", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onServicesDiscovered() {

            }

            @Override
            public void onDataReceived(String uuid, byte[] data) {
                runOnUiThread(() -> {
                    String value = new String(data);
                    inputNumber.setText(value);
                });
            }


            @Override
            public void onNotificationReceived(String message) {
                txtNotify.setText(message);
            }


            @Override
            public void onBatteryReceived(int batteryLevel) {
                runOnUiThread(() ->
                        txtBattery.setText("Battery: " + batteryLevel + "%"));
            }

            @Override
            public void onError(String message) {

            }
        });

        bleManager.disconnect();   // refresh cache + close starý GATT
        bleManager.connect(device);

        btnSend.setOnClickListener(v -> {

            String number = inputNumber.getText().toString().trim();

            if (number.length() != 2) {
                Toast.makeText(this,
                        "Zadaj 2-miestne číslo", Toast.LENGTH_SHORT).show();
                return;
            }

            bleManager.writeNumber(number);
        });

        btnBattery.setOnClickListener(v -> {
            bleManager.readBattery();
        });
    }
}
