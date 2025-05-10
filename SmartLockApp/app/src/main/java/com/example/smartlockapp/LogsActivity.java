package com.example.smartlockapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class LogsActivity extends AppCompatActivity {

    private static final String STATUS_URL = "http://192.168.123.4:8000/lock_status";
    private final LinkedList<String> logs = new LinkedList<>();
    private ArrayAdapter<String> adapter;
    private Handler handler;
    private Runnable pollTask;
    private final String logFilename = "log_history.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        ListView logList = findViewById(R.id.logList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logs);
        logList.setAdapter(adapter);

        // Poll every second
        handler = new Handler();
        pollTask = new Runnable() {
            @Override
            public void run() {
                fetchStatus();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(pollTask);

        // View saved history
        Button historyBtn = findViewById(R.id.viewHistoryBtn);
        historyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LogsActivity.this, LogHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void fetchStatus() {
        new Thread(() -> {
            try {
                URL url = new URL(STATUS_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                StringBuilder result = new StringBuilder();
                int data;
                while ((data = reader.read()) != -1) {
                    result.append((char) data);
                }

                JSONObject json = new JSONObject(result.toString());
                int status = json.getInt("lock_status");

                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String statusText = (status == 0 ? "🔒 Locked" : "🔓 Unlocked") + " @ " + timestamp;

                runOnUiThread(() -> {
                    logs.addFirst(statusText);
                    if (logs.size() > 5) logs.removeLast();
                    adapter.notifyDataSetChanged();
                });

                saveLogToFile(statusText);

            } catch (Exception e) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String errorText = "❌ Error @ " + timestamp;

                runOnUiThread(() -> {
                    logs.addFirst(errorText);
                    if (logs.size() > 5) logs.removeLast();
                    adapter.notifyDataSetChanged();
                });

                saveLogToFile(errorText);
            }
        }).start();
    }

    private void saveLogToFile(String log) {
        try {
            FileOutputStream fos = openFileOutput(logFilename, MODE_APPEND);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(log);
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollTask);
    }
}
