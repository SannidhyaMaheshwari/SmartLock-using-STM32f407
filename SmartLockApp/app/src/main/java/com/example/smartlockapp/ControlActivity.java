package com.example.smartlockapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ControlActivity extends AppCompatActivity {

    private static final String LOCK_URL = "http://192.168.123.4:8000/lock";
    private static final String UNLOCK_URL = "http://192.168.123.4:8000/unlock";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        Button lockBtn = findViewById(R.id.lockBtn);
        Button unlockBtn = findViewById(R.id.unlockBtn);
        Button viewLogsBtn = findViewById(R.id.viewLogsBtn);

        lockBtn.setOnClickListener(v -> {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            saveAppLog("🔒 Locked via App @ " + time);
            new SendGetRequestTask().execute(LOCK_URL);
        });

        Button tempPasswordBtn = findViewById(R.id.tempPasswordBtn);
        tempPasswordBtn.setOnClickListener(v -> {
            startActivity(new Intent(ControlActivity.this, TempPasswordActivity.class));
        });

        unlockBtn.setOnClickListener(v -> {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            saveAppLog("🔓 Unlocked via App @ " + time);
            new SendGetRequestTask().execute(UNLOCK_URL);
        });

        viewLogsBtn.setOnClickListener(v -> {
            startActivity(new Intent(ControlActivity.this, LogsActivity.class));
        });
    }

    private void saveAppLog(String log) {
        try {
            FileOutputStream fos = openFileOutput("log_history.txt", MODE_APPEND);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write("[APP] " + log);
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class SendGetRequestTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int responseCode = conn.getResponseCode();
                return responseCode == 200;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String message = success ? "✅ Request successful" : "❌ Failed to contact server";
            Toast.makeText(ControlActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
