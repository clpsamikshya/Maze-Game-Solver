package com.example.maze;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    private final String PREFS_NAME = "LoginPrefs";
    private final String USERNAME = "admin";  // Hardcoded username
    private final String PASSWORD = "1234";   // Hardcoded password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Auto-login if already logged in
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean("loggedIn", false).apply();

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();

            if (username.equals(USERNAME) && password.equals(PASSWORD)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("loggedIn", true);
                editor.apply();
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                goToMain();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Prevent going back to login
    }
}
