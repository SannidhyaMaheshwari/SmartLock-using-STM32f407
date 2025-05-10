package com.example.smartlockapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;

public class TempPasswordActivity extends AppCompatActivity {
    private ProgressBar loadingSpinner;
    private EditText tempPassInput;
    private Button submitBtn;
    private final String API_URL = "http://192.168.123.4:8000/set_temp_password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_password);

        loadingSpinner = findViewById(R.id.loadingSpinner);
        tempPassInput = findViewById(R.id.tempPassInput);
        submitBtn = findViewById(R.id.submitTempBtn);

        submitBtn.setOnClickListener(v -> {
            String value = tempPassInput.getText().toString();
            if (value.length() != 4 || !value.matches("\\d{4}")) {
                Toast.makeText(this, "❌ Enter a 4-digit number", Toast.LENGTH_SHORT).show();
                return;
            }

            sendTempPassword(value);
        });
    }

    private void sendTempPassword(String value) {
        runOnUiThread(() -> {
            loadingSpinner.setVisibility(View.VISIBLE);
            tempPassInput.setEnabled(false);
            submitBtn.setEnabled(false);
        });

        new Thread(() -> {
            try {
                String url = API_URL + "?value=" + value;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).get().build();
                Response response = client.newCall(request).execute();

                runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    tempPassInput.setEnabled(true);
                    submitBtn.setEnabled(true);

                    if (response.isSuccessful()) {
                        Toast.makeText(this, "✅ Temp password set", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity
                    } else {
                        Toast.makeText(this, "❌ Failed to set temp password", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    tempPassInput.setEnabled(true);
                    submitBtn.setEnabled(true);
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
