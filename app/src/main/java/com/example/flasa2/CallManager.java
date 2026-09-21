package com.example.flasa2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CallManager {

    public static final int CALL_REQUEST_CODE = 200;

    /**
     * Skontroluje, či máme povolenie na volanie
     */
    public static boolean hasCallPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Vypýta si povolenie
     */
    public static void requestCallPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.CALL_PHONE},
                CALL_REQUEST_CODE
        );
    }

    /**
     * Spustí priamy hovor
     */
    public static void makeCall(Activity activity, String phoneNumber) {

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return;
        }

        if (hasCallPermission(activity)) {

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + Uri.encode(phoneNumber)));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                activity.startActivity(callIntent);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

        } else {
            requestCallPermission(activity);
        }
    }

    /**
     * Otvorí dialer bez potreby permission
     */
    public static void openDialer(Activity activity, String phoneNumber) {

        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        activity.startActivity(dialIntent);
    }
}
