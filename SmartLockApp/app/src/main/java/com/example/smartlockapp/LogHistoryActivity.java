package com.example.smartlockapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class LogHistoryActivity extends AppCompatActivity {

    private final String logFilename = "log_history.txt";
    private ArrayList<String> fullLogs = new ArrayList<>();
    private ArrayList<String> filteredLogs = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView fullLogList;
    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_history);

        fullLogList = findViewById(R.id.fullLogList);
        searchBar = findViewById(R.id.searchBar);

        loadLogs();

        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                filteredLogs
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                String log = getItem(position);

                if (log != null && log.startsWith("[APP]")) {
                    text.setTextColor(getResources().getColor(android.R.color.holo_purple));
                } else {
                    text.setTextColor(getResources().getColor(android.R.color.white));
                }

                return view;
            }
        };

        fullLogList.setAdapter(adapter);

        // 🔍 Live filter
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLogs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void loadLogs() {
        fullLogs.clear();
        filteredLogs.clear();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(logFilename)));
            String line;
            while ((line = reader.readLine()) != null) {
                fullLogs.add(line);
                filteredLogs.add(line);
            }
            reader.close();
        } catch (Exception e) {
            fullLogs.add("❌ Error reading log file");
            filteredLogs.add("❌ Error reading log file");
        }
    }

    private void filterLogs(String query) {
        filteredLogs.clear();
        for (String log : fullLogs) {
            if (log.toLowerCase().contains(query.toLowerCase())) {
                filteredLogs.add(log);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
