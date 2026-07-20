package com.example.flasa2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class InputActivity extends AppCompatActivity {

    private BleManager bleManager;

    private EditText inputNumber;
    private Button btnSend, btnBattery, btnArmed, btnDisarmed,btnTest;
    private TextView txtBattery;
    private TextView txtNotify;
    private TextView txtPrikaz;

    private BluetoothDevice device;

    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);



        inputNumber = findViewById(R.id.inputNumber);
        btnSend = findViewById(R.id.btnSend);
        btnBattery = findViewById(R.id.btnBattery);
        btnArmed = findViewById(R.id.btnArmed);
        btnDisarmed = findViewById(R.id.btnDisarmed);
        btnTest = findViewById(R.id.btnTest);

        txtBattery = findViewById(R.id.txtBattery);
        txtNotify = findViewById(R.id.txtNotify);
        txtPrikaz = findViewById(R.id.txtPrikaz);

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

            }



            @Override
            public void onNotificationReceived(String message) {
                toneG.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, 100);
                txtNotify.setText(message);
            }


            @Override
            public void onBatteryReceived(int batteryLevel) {
                runOnUiThread(() ->
                        txtBattery.setText(batteryLevel + "%"));
            }

            @Override
            public void onError(String message) {

            }
        });

        bleManager.disconnect();   // refresh cache + close starý GATT
        bleManager.connect(device);

/*
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
    */

    }



    public void onClick(View view) {

        int id = view.getId();

        if (id == R.id.btnSend) {
            String number = inputNumber.getText().toString().trim();
            bleManager.writeNumber(number);

        } else if (id == R.id.btnBattery) {
            bleManager.readBattery();

        } else if (id == R.id.btnArmed) {
            String number = inputNumber.getText().toString().trim();
            bleManager.writeNumber("ON:" + number);
            txtPrikaz.setText("ARMED");
            txtPrikaz.setTextColor(Color.RED);

        } else if (id == R.id.btnDisarmed) {
            String number = inputNumber.getText().toString().trim();
            bleManager.writeNumber("OF:" + number);
            txtPrikaz.setText("DISARMED");
            txtPrikaz.setTextColor(Color.GREEN);

        } else if (id == R.id.btnTest) {
            String number = inputNumber.getText().toString().trim();
            bleManager.writeNumber("TS:" + number);
            txtPrikaz.setText("TESTING");
            txtPrikaz.setTextColor(Color.BLUE);
        }
    }



}
