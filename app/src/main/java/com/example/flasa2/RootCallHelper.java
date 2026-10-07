package com.example.flasa2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.util.Log;

import java.io.DataOutputStream;

public class RootCallHelper {

    private static final String TAG = "ROOT_CALL_HELPER";
    // Globálna premenná (volatile zaručí viditeľnosť naprieč vláknami Pouziva ju RootCallHelper)
    public static volatile boolean isHangupFinished = false;

    /**
     * Vytočí hovor a po 30 sekundách ho automaticky zavesí cez ROOT.
     *
     * @param context Context aplikácie
     * @param phoneNumber Telefónne číslo na vytočenie (napr. "+421900000000")
     */
    public static void makeCallAndAutoHangup(Context context, String phoneNumber) {

        // Pred začiatkom nastavíme stav na false
        isHangupFinished = false;

        // 1. Vytočenie hovoru cez TelecomManager
        try {
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            Uri uri = Uri.fromParts("tel", phoneNumber, null);

            // Vyžaduje oprávnenie android.permission.CALL_PHONE
            telecomManager.placeCall(uri, null);
            Log.i(TAG, "Hovor na číslo " + phoneNumber + " bol úspešne vytočený.");

        } catch (SecurityException e) {
            Log.e(TAG, "Chýba oprávnenie CALL_PHONE! Používam fallback cez Intent...", e);
            // Fallback ak chýba priame oprávnenie na placeCall
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
        }

        // 2. Naplánovanie zavesenia po 30 sekundách cez ROOT
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.i(TAG, "30 sekúnd uplynulo! Pripájam ROOT príkaz na zavesenie hovoru...");

            // Spustenie v novom vlákne, aby sme neblokovali UI vlákno pri vykonávaní shell príkazu
            new Thread(() -> {
                executeRootHangup();
                isHangupFinished = true;
                Log.i(TAG, "isHangupFinished nastavené na TRUE");
            }).start();

        }, 12000); // 30 000 ms = 30 sekúnd
    }

    /**
     * Vykoná ROOT príkaz na zavesenie hovoru.
     */
    private static void executeRootHangup() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            // KEYCODE_ENDCALL (6) simuluje stlačenie hardvérového tlačidla pre zavesenie
            os.writeBytes("input keyevent 6\n");

            // Alternatívny príkaz pre novšie Androidy (ak by keyevent zlyhal):
            // os.writeBytes("cmd telecom end-call\n");

            os.writeBytes("exit\n");
            os.flush();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Log.d(TAG, "Hovor bol úspešne zavesený cez ROOT.");
            } else {
                Log.e(TAG, "ROOT príkaz zlyhal s kódmi: " + exitCode);
            }

        } catch (Exception e) {
            Log.e(TAG, "Chyba pri vykonávaní SU príkazu na zavesenie", e);
        }
    }
}
