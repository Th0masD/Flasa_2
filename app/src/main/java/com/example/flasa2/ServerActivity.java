package com.example.flasa2;

import static android.content.ContentValues.TAG;

import static java.lang.Math.abs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;




import androidx.appcompat.app.AppCompatActivity;

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


    int pocitadlo_nakolnu = 5;  //Pocita kolko krat sa zavola gyroskop
    private SensorManager sensorManager;
    Sensor acclerometer;

    private boolean sensorEnabled = false; //Zapinanie vypinavie senzoru

    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);


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

                //     mMobilCislo1.setText("ZAP " + mobilcislo);
                //       toneG.startTone(ToneGenerator.TONE_DTMF_0, 200);
                //        mediaPlayer = MediaPlayer.create(PeripheralRoleActivity.this, R.raw.strazim);
                //        mediaPlayer.start();

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
                //    mMobilCislo1.setText("VYP ");
                //      toneG.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 500);
                //      toneG.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 4000);
                //     mediaPlayer = MediaPlayer.create(PeripheralRoleActivity.this, R.raw.nestrazim);
                //    mediaPlayer.start();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtPrikaz.setTextColor(Color.GREEN);
                        txtPrikaz.setText("DISARMED");
                    }
                });



                TestNaklonu = false;
                stopSensor();

                //    String msg2 = mobilcislo;
                //  sensorManager.unregisterListener(PeripheralRoleActivity.this);

                //    Log.v(MainActivity.TAG, msg2);
                //    showMsgText(msg2);

                break;

            case "SM":
               // SendSMS();

                break;

            case "TS":
                ZapVyp = false;
                TestNaklonu = true;
                PrvyKrat = true;

                //     mMobilCislo1.setText("TEST" + mobilcislo);
                //     toneG.startTone(ToneGenerator.TONE_DTMF_9, 200);
                //        mediaPlayer = MediaPlayer.create(PeripheralRoleActivity.this, R.raw.testujem);
                //        mediaPlayer.start();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtPrikaz.setText("TESTING");
                    }
                });


                startSensor();


                //tomas
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                acclerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.registerListener(ServerActivity.this, acclerometer, SensorManager.SENSOR_DELAY_NORMAL);


                break;


        }


    }







    //tomas
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        txtNaklonRealtime.setText("Value: " + String.format("%.1f  %.1f  %.1f", sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]));
        zmenaX = sensorEvent.values[0];
        zmenaY = sensorEvent.values[1];
        zmenaZ = sensorEvent.values[2];


        if (PrvyKrat == true)
        {
            zmenaXzaciatocna = sensorEvent.values[0];
            zmenaYzaciatocna = sensorEvent.values[1];
            zmenaZzaciatocna = sensorEvent.values[2];
            txtNaklonZaciatocna.setText("Zacia: " + String.format("%.1f  %.1f  %.1f", sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]));
            PrvyKrat = false;
        }


        if (((abs(zmenaXzaciatocna-zmenaX)>1) || (abs(zmenaYzaciatocna-zmenaY)>1) || (abs(zmenaZzaciatocna-zmenaZ)>1)) && TestNaklonu)
        {


            if(pocitadlo_nakolnu==0) {
               // notifyCharacteristicChanged();
               // bleServerManager.sendNotification("Test");
                toneG.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, 100);
                pocitadlo_nakolnu=3;
            }
            pocitadlo_nakolnu--;
            //  sensorManager.unregisterListener(PeripheralRoleActivity.this);
        }

        if (((abs(zmenaXzaciatocna-zmenaX)>1) || (abs(zmenaYzaciatocna-zmenaY)>1) || (abs(zmenaZzaciatocna-zmenaZ)>1)) && ZapVyp)
        {
            //      ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
           // zavolajCislo();
            //  SendSMS();
            //  sensorManager.unregisterListener(PeripheralRoleActivity.this);
        }
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



    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleServerManager.stop();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bleServerManager.stop();
        sensorManager.unregisterListener(this);
        }

    @Override
    protected void onStop() {
        super.onStop();
        bleServerManager.stop();
        sensorManager.unregisterListener(this);
    }

}