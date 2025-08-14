package com.example.maze;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnSignUp;
    private SignInButton btnGoogleLogin;
    private DBHelper dbHelper;

    private final String PREFS_NAME = "LoginPrefs";
    private final String HARDCODED_USERNAME = "admin"; // Hardcoded username
    private final String HARDCODED_PASSWORD = "1234";  // Hardcoded password
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGoogleLogin = findViewById(R.id.btnGoogleSignIn);

        dbHelper = new DBHelper(this);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Auto-login if already logged in
        if (prefs.getBoolean("loggedIn", false)) {
            goToMain();
            return;
        }

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleLogin.setOnClickListener(v -> {
            // Always sign out first to show chooser every time
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                mGoogleSignInClient.revokeAccess().addOnCompleteListener(task2 -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                });
            });
        });

        // Normal Login
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check hardcoded account
            if (username.equals(HARDCODED_USERNAME) && password.equals(HARDCODED_PASSWORD)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("loggedIn", true);
                editor.putString("loggedInUser", username);
                editor.putInt("loggedInUserId", -1);
                editor.apply();
                Toast.makeText(this, "Login successful (Hardcoded user)!", Toast.LENGTH_SHORT).show();
                goToMain();
                return;
            }

            // Check registered users in DB
            boolean isValid = dbHelper.validateUser(username, password);
            if (isValid) {
                int userId = dbHelper.getUserIdByEmail(username);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("loggedIn", true);
                editor.putString("loggedInUser", username);
                editor.putInt("loggedInUserId", userId);
                editor.apply();
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                goToMain();
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        // Sign Up button
        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    // Google Sign-In result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String googleId = account.getId();
                    String email = account.getEmail();

                    if (dbHelper.googleUserExists(email)) {
                        long userId = dbHelper.getUserIdByGoogleId(googleId);
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("loggedIn", true);
                        editor.putString("loggedInUser", account.getDisplayName());
                        editor.putInt("loggedInUserId", (int) userId);
                        editor.apply();
                        goToMain();
                    } else {
                        Toast.makeText(this, "Google account not registered", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
