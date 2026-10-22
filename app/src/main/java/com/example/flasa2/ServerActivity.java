package com.example.flasa2;

import static android.content.ContentValues.TAG;

import static java.lang.Math.abs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;




import androidx.appcompat.app.AppCompatActivity;

import java.io.DataOutputStream;

public class ServerActivity extends AppCompatActivity implements SensorEventListener{

    private BleServerManager bleServerManager;
    private Button btnNotify;
    private TextView txtReceived;
    private TextView txtStatus;
    private TextView txtNaklonRealtime;
    private TextView txtNaklonZaciatocna;
    private TextView txtPrikaz;


    public boolean ZapVyp = false;
    public boolean TestNaklonu = false;
    public boolean PrvyKrat = true;
    private Button mZavolaCislo1;
    public String mobilcislo;
    public String mPrikaz;
    public String mStav;


    float zmenaY;
    float zmenaYzaciatocna;

    float zmenaX;
    float zmenaXzaciatocna;

    float zmenaZ;
    float zmenaZzaciatocna;

    private int pocitadlo_zobrazenia = 0;

    int pocitadlo_nakolnu = 5;  //Pocita kolko krat sa zavola gyroskop
    private SensorManager sensorManager;
    Sensor acclerometer;

    private boolean sensorEnabled = false; //Zapinanie vypinavie senzoru

    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    private CallStateManager callStateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        btnNotify = findViewById(R.id.btnNotify);

        txtReceived = findViewById(R.id.txtReceived);
        txtPrikaz = findViewById(R.id.txtPrikaz);
        txtStatus = findViewById(R.id.txtStatus);
        txtNaklonRealtime = findViewById(R.id.txtNaklonRealtime);
        txtNaklonZaciatocna = findViewById(R.id.txtNaklonZaciatocna);

        //Senzory naklonu inicializacia
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acclerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        startSensor();


        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

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

                Log.d("BLE_RECEIVE", "uuid=" + uuid + " value=" + value);

                SpracovaniePrikazuClienta(value);

                bleServerManager.sendNotification("ACK:" + value); //Posle clientovy ACK ze prijal prikaz

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
            public void onStatusReceived(String message) {
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


        // Telefonovanaie manager
        /*
            callStateManager = new CallStateManager();

            callStateManager.start(this, new CallStateManager.CallListener() {
                @Override
                public void onRinging() {
                    Log.d("CALL", "Volá...");
                }
                @Override
                public void onAnswered() {
                    Log.d("CALL", "Zdvihnuté");
                }
                @Override
                public void onEnded(CallStateManager.CallResult result) {
                    Log.d("CALL", "Výsledok: " + result);
                }
            });
            */
    }


    private void SpracovaniePrikazuClienta(String value){

     //   Log.d(TAG, "SpracovaniePrikazu:" + value);

        String[] separated = value.split(":");

        mStav = separated[0];

        if (separated.length > 1) {
            mobilcislo = separated[1];
        } else {
            mobilcislo = "";
        }

        Log.d(TAG, "SpracovaniePrikazu:" + mStav + mobilcislo);

        if (mStav == null) return;

        switch (mStav) {
            case "ON":
                ZapVyp = true;
                TestNaklonu = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtPrikaz.setTextColor(Color.RED);
                        txtPrikaz.setText("ARMED");
                    }
                });

                pocitadlo_nakolnu=5;
                startSensor();

                break;

            case "OF":
                ZapVyp = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtPrikaz.setTextColor(Color.GREEN);
                        txtPrikaz.setText("DISARMED");
                    }
                });

                TestNaklonu = false;
                stopSensor();
                break;

            case "SM":
               // SendSMS();
                break;

            case "TS":
                ZapVyp = false;
                TestNaklonu = true;
                PrvyKrat = true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtPrikaz.setText("TESTING");
                    }
                });

                startSensor();
                break;

        }

    }


    //tomas
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        pocitadlo_zobrazenia++;

        // Ak je zvyšok po delení 5 rovný 0, ide o 5., 10., 15., 20... vypis
        if (pocitadlo_zobrazenia % 5 == 0) {
            txtNaklonRealtime.setText("Value: " + String.format("%.1f  %.1f  %.1f", sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]));
            pocitadlo_zobrazenia = 0;
        }

        zmenaX = sensorEvent.values[0];
        zmenaY = sensorEvent.values[1];
        zmenaZ = sensorEvent.values[2];

        // Pri zapnuti rezimu TESTUJEM sa ulozia hodnoty naklonu. Nasledne sa porovnavaju s realnymi. Pri zmene o 1 sa spusti Alarm.
        if (PrvyKrat == true)
        {
            zmenaXzaciatocna = sensorEvent.values[0];
            zmenaYzaciatocna = sensorEvent.values[1];
            zmenaZzaciatocna = sensorEvent.values[2];
            txtNaklonZaciatocna.setText("Zacia: " + String.format("%.1f  %.1f  %.1f", sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]));
            PrvyKrat = false;
        }

        // Ak je zapnuty rezim Testujem
        if (((abs(zmenaXzaciatocna-zmenaX)>1) || (abs(zmenaYzaciatocna-zmenaY)>1) || (abs(zmenaZzaciatocna-zmenaZ)>1)) && TestNaklonu)
        {


            if(pocitadlo_nakolnu==0) {

                bleServerManager.sendNotification("Test");
              //  toneG.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, 100);
                pocitadlo_nakolnu=3;
            }
            pocitadlo_nakolnu--;

        }

        // Ak je zapnuty rezim Armed
        if (((abs(zmenaXzaciatocna-zmenaX)>1) || (abs(zmenaYzaciatocna-zmenaY)>1) || (abs(zmenaZzaciatocna-zmenaZ)>1)) && ZapVyp)
        {
            //      ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            airplaneOff();
            //  SendSMS();

            stopSensor();
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}   //Cakaj kym sa vypne AirplaneMode

            spustiProces(); // zavola a po 12sekundach hovor zrusi rpicom caka ky to skonci a nic nerobi

            ZapVyp = false;
            startSensor();

            //CallManager.makeCall(this, mobilcislo);
            //sensorManager.unregisterListener(ServerActivity.this);
        }
    }


    public void spustiProces() {
        new Thread(() -> {

            // 1. Zavolanie hovoru
            RootCallHelper.makeCallAndAutoHangup(ServerActivity.this, mobilcislo);

            // 2. Blokujúce čakanie na pozadí, kým sa zmení flag v triede RootCallHelper
            while (!RootCallHelper.isHangupFinished) {
                try {
                    Thread.sleep(500); // Kontrola každých 500 ms (šetrenie CPU)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 3. Kód po ukončení zavesenia
            Log.i("ServerActivity", "Hovor bol zavesený, pokračujem v ďalšom kóde...");

        }).start();
    }


    private void startSensor() {
        if (!sensorEnabled && sensorManager != null && acclerometer != null) {
            sensorManager.registerListener(ServerActivity.this, acclerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorEnabled = true;
        }
    }

    private void stopSensor() {
        if (sensorEnabled && sensorManager != null) {
            sensorManager.unregisterListener(ServerActivity.this);
            sensorEnabled = false;
        }
    }

    public void airplaneOff() {

        try {

            Process process = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            os.writeBytes("settings put global airplane_mode_on 0\n");

            os.writeBytes(
                    "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false\n");

            os.writeBytes("exit\n");
            os.flush();

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleServerManager.stop();
        sensorManager.unregisterListener(this);

        if (callStateManager != null) {
            callStateManager.stop(this);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        }

    @Override
    protected void onStop() {
        super.onStop();
     //   bleServerManager.stop();
     //   sensorManager.unregisterListener(this);
    }

}