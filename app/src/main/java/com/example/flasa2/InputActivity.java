package com.example.flasa2;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.activity.OnBackPressedCallback;
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

    //Ked strati focus apk
    private boolean wentToBackground = false;
    private boolean resetting = false;


    //Citane charaktristiky status a battery kazdych 5sek
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable readStatusRunnable = new Runnable() {
        @Override
        public void run() {

            // Spusti 5-sekundový timeout
            handler.removeCallbacks(serverTimeoutRunnable);
            handler.postDelayed(serverTimeoutRunnable, 5000);

            bleManager.readBattery();
            bleManager.readStatus();

            handler.postDelayed(this, 5000);
        }
    };

    //Kontrola ci odpovedal server na citanie Statusu
    private final Runnable serverTimeoutRunnable = () -> {
        runOnUiThread(() ->
                txtStav.setText("Server neodpovedá"));

        //Nastavi Ovladacie tlacidlo na Sive
        btnAlarm.setText(". . . .");
        btnAlarm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));

        // Odpoj BLE spojenie so serverom
        bleManager.disconnect();

    };


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


        // Zablokovanie systémového tlačidla Back
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
            @Override
                    public void handleOnBackPressed() {
                        // Nerob nič
                    }
                });



        //Zobrazi aktualne telefonne cislo
        SharedPreferences preferences = getSharedPreferences("FLASA_SETTINGS", MODE_PRIVATE);
        String phoneNumber = preferences.getString("phone_number", "");
        txtTelcislo.setText(phoneNumber);

        //Nastavi Ovladacie tlacidlo na Sive
        btnAlarm.setText(". . . .");
        btnAlarm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));


        //Testovacia obrazovka sa pouziva v rezime testujem
        testScreen = findViewById(R.id.testScreen);
        Button buttonCancelTest = findViewById(R.id.buttonCancelTest);
        buttonCancelTest.setOnClickListener(v -> {

            bleManager.writeNumber("OF:" + phoneNumber);
            testScreen.setVisibility(View.GONE);
        });



        String address = getIntent().getStringExtra("device_address");

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        device = adapter.getRemoteDevice(address);
        bleManager = new BleManager(this, adapter);


        bleManager.setCallback(new BleCallback() {
            @Override
            public void onConnected() {
               // runOnUiThread(() ->
              //          Toast.makeText(InputActivity.this,
               //                 "Connected", Toast.LENGTH_SHORT).show());
               // txtStav.setText("Connected");
            }

            @Override
            public void onDisconnected() {
                handler.removeCallbacks(readStatusRunnable); //vypnutie citanie statusu kazdych 5sek
            }

            @Override
            public void onServicesDiscovered() {

                handler.removeCallbacks(readStatusRunnable);

                // prvé čítanie okamžite
                bleManager.readBattery();
                bleManager.readStatus();

                // potom každých 5 sekúnd
                handler.postDelayed(readStatusRunnable, 5000);
            }


            @Override
            public void onDataReceived(String uuid, byte[] data) {

            }


            @Override
            public void onNotificationReceived(String message) {
                //toneG.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, 100);
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
                //txtNotify.setText(message);

            }



            @Override
            public void onBatteryReceived(int batteryLevel) {
                Log.d(TAG, "Vypis hodnoty baterie");

                runOnUiThread(() ->
                        txtBattery.setText(batteryLevel + "%"));
            }

            @Override
            public void onStatusReceived(String message) {

                String mStav;

                Log.d(TAG, "Vypis stavu");

                handler.removeCallbacks(serverTimeoutRunnable);
                runOnUiThread(() -> txtStav.setText(message));


                String[] separated = message.split(":");
                mStav = separated[0];


                if (mStav == null) return;

                switch (mStav) {
                    case "ON":

                        runOnUiThread(() -> {
                            btnAlarm.setBackgroundTintList(
                                    ColorStateList.valueOf(
                                            ContextCompat.getColor(InputActivity.this, android.R.color.holo_red_dark)
                                    )
                            );
                            btnAlarm.setText("ARMED");
                        });

                        break;


                    case "OF":

                        runOnUiThread(() -> {
                            btnAlarm.setBackgroundTintList(
                                    ColorStateList.valueOf(
                                            ContextCompat.getColor(InputActivity.this, android.R.color.holo_green_dark)
                                    )
                            );
                            btnAlarm.setText("DISARMED");
                        });

                        break;

                }
            }

            @Override
            public void onError(String message) {

            }
        });

      //  bleManager.disconnect();   // refresh cache + close starý GATT
        bleManager.disconnect();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bleManager.connect(device);
        }, 500);



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem settings = menu.findItem(R.id.action_settings);
        settings.setIcon(R.drawable.baseline_settings_24);
        return true;
    }

/*
    @Override
    protected void onStop() {
        super.onStop();

        // Activity už nie je viditeľná.
        // Home alebo prepnutie do inej aplikácie.
        if (!isChangingConfigurations() && !isFinishing()) {
            wentToBackground = true;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (wentToBackground && !resetting) {

            resetting = true;
            wentToBackground = false;

            // Odpojenie BLE
            if (bleManager != null) {
                bleManager.disconnect();
            }

            // Otvor DiscoveryActivity a odstráň InputActivity
            // aj ostatné Activity z back stacku.
            Intent intent = new Intent(
                    InputActivity.this,
                    DiscoveryActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);

            finish();
        }
    }
*/

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

                    String savedPhone = preferences.getString("phone_number", "");

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

                if (menuItem.getItemId() == R.id.menu_connect) {

                    finish();
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

            //    btnAlarm.setText("ARMED");
            //    btnAlarm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                        //Nastavi Ovladacie tlacidlo na Sive
                        btnAlarm.setText(". . . .");
                btnAlarm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));


                bleManager.writeNumber("ON:" + phoneNumber);

            } else {


           //     btnAlarm.setText("DISARMED");
           //     btnAlarm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_green_dark)));

                //Nastavi Ovladacie tlacidlo na Sive
                btnAlarm.setText(". . . .");
                btnAlarm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));

                bleManager.writeNumber("OF:" + phoneNumber);
            }


        }


    }



}


