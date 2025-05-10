package com.example.smartlockapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class LocksActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locks);

        Button lock1 = findViewById(R.id.lock1Btn);
        lock1.setOnClickListener(v -> {
            Intent intent = new Intent(LocksActivity.this, ControlActivity.class);
            startActivity(intent);
        });
    }
}
