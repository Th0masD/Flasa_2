package com.example.flasa2;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {

    public static final int REQUEST_CODE = 100;

    public static boolean checkAndRequestPermissions(Activity activity) {

        List<String> permissions = new ArrayList<>();

        // Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {

                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {

                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        // Android < 12
        else {

            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissions.isEmpty()) {

            ActivityCompat.requestPermissions(
                    activity,
                    permissions.toArray(new String[0]),
                    REQUEST_CODE
            );

            return false;
        }

        return true;
    }
}
