package com.example.flasa2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnServer;
    private Button btnClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnServer = findViewById(R.id.btnServer);
        btnClient = findViewById(R.id.btnClient);

        if (!PermissionUtil.checkAndRequestPermissions(this)) {
            return;
        }

        btnServer.setOnClickListener(v -> {
            Intent intent =
                    new Intent(MainActivity.this, ServerActivity.class);
            startActivity(intent);
        });

        btnClient.setOnClickListener(v -> {
            Intent intent =
                    new Intent(MainActivity.this, ClientActivity.class);
            startActivity(intent);
        });
    }
}