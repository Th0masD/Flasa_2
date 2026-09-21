package com.example.flasa2;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.content.SharedPreferences;
import android.text.InputType;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;


public class InputActivity extends AppCompatActivity {

    private BleManager bleManager;

    private Button btnAlarm, btnArmed, btnDisarmed,btnTest;
    private TextView txtBattery;
    private TextView txtNotify;
    private TextView txtTelcislo;
    private TextView txtStav;

    private View testScreen;  //Testovacia obrazovka

    private BluetoothDevice device;

    boolean armed = false;


    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);


        txtBattery = findViewById(R.id.txtBattery);
        btnAlarm = findViewById(R.id.btnAlarm);

        txtNotify = findViewById(R.id.txtNotify);
        txtTelcislo = findViewById(R.id.txtTelcislo);
        txtStav = findViewById(R.id.txtStav);

        //Horna lista
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("FLASA");
        }


        //Zobrazi aktualne telefonne cislo
        SharedPreferences preferences = getSharedPreferences("FLASA_SETTINGS", MODE_PRIVATE);
        String phoneNumber = preferences.getString("phone_number", "");
        txtTelcislo.setText(phoneNumber);


        //Testovacia obrazovka
        testScreen = findViewById(R.id.testScreen);

        Button buttonCancelTest = findViewById(R.id.buttonCancelTest);

        buttonCancelTest.setOnClickListener(v -> {

            bleManager.writeNumber("OF:" + phoneNumber);
            testScreen.setVisibility(View.GONE);
        });



        String address = getIntent().getStringExtra("device_address");

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        BluetoothAdapter adapter = bluetoothManager.getAdapter();

        device = adapter.getRemoteDevice(address);

        bleManager = new BleManager(this, adapter);


        bleManager.setCallback(new BleCallback() {
            @Override
            public void onConnected() {
               // runOnUiThread(() ->
              //          Toast.makeText(InputActivity.this,
               //                 "Connected", Toast.LENGTH_SHORT).show());
                txtStav.setText("Connected");
            }

            @Override
            public void onDisconnected() {
                txtStav.setText("Disconnected");
            }

            @Override
            public void onServicesDiscovered() {

                Log.e(TAG, "Discovered serices");
                bleManager.readBattery();
            }


            @Override
            public void onDataReceived(String uuid, byte[] data) {

            }


            @Override
            public void onNotificationReceived(String message) {
                //toneG.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, 100);
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
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


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem settings = menu.findItem(R.id.action_settings);
        settings.setIcon(R.drawable.baseline_settings_24);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {

            Toolbar toolbar = findViewById(R.id.toolbar);

            View settingsView = toolbar.findViewById(R.id.action_settings);

            PopupMenu popup = new PopupMenu(this, settingsView);

            popup.getMenuInflater().inflate(
                    R.menu.settings_menu,
                    popup.getMenu()
            );

            popup.setOnMenuItemClickListener(menuItem -> {


                if (menuItem.getItemId() == R.id.menu_phone) {

                    SharedPreferences preferences =
                            getSharedPreferences("FLASA_SETTINGS", MODE_PRIVATE);

                    String savedPhone =
                            preferences.getString("phone_number", "");

                    EditText input = new EditText(this);
                    input.setInputType(InputType.TYPE_CLASS_PHONE);
                    input.setSingleLine(true);
                    input.setHint("09...");

                    // Zobraz uložené číslo
                    input.setText(savedPhone);
                    input.setSelection(input.length());

                    new AlertDialog.Builder(this)
                            .setTitle("Telefónne číslo")
                            .setView(input)
                            .setPositiveButton("Uložiť", (dialog, which) -> {

                                String phoneNumber = input.getText().toString().trim();

                                preferences.edit().putString("phone_number", phoneNumber).apply();

                                // Zobrazenie čísla priamo v aktivite
                                txtTelcislo.setText(phoneNumber);

                            })
                            .setNegativeButton("Zrušiť", null)
                            .show();

                    return true;
                }




                if (menuItem.getItemId() == R.id.menu_test) {

                    SharedPreferences preferences = getSharedPreferences("FLASA_SETTINGS", MODE_PRIVATE);
                    String phoneNumber = preferences.getString("phone_number", "");
                    bleManager.writeNumber("TS:" + phoneNumber);
                    testScreen.setVisibility(View.VISIBLE);
                    return true;
                }

                if (menuItem.getItemId() == R.id.menu_info) {
                    // Info
                    return true;
                }

                return false;
            });

            popup.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    public void onClick(View view) {

        int id = view.getId();

        //Ulozenie tel cisla do pamete
        SharedPreferences preferences = getSharedPreferences("FLASA_SETTINGS", MODE_PRIVATE);
        String phoneNumber = preferences.getString("phone_number", "");



        if (id == R.id.btnAlarm) {
            armed = !armed;

            if (armed) {

                btnAlarm.setText("ARMED");
                btnAlarm.setBackgroundTintList(
                        ColorStateList.valueOf(
                                ContextCompat.getColor(this, android.R.color.holo_red_dark)
                        )
                );

                //String number = inputNumber.getText().toString().trim();
                bleManager.writeNumber("ON:" + phoneNumber);

            } else {


                btnAlarm.setText("DISARMED");
                btnAlarm.setBackgroundTintList(
                        ColorStateList.valueOf(
                                ContextCompat.getColor(this, android.R.color.holo_green_dark)
                        )
                );

                bleManager.writeNumber("OF:" + phoneNumber);
            }


        }


    }



}
