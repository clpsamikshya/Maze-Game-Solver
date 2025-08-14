package com.example.maze;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnRegister, btnViewUsers;
    private SignInButton btnGoogleSignIn;
    private DBHelper dbHelper;
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnViewUsers = findViewById(R.id.btnViewUsers);

        dbHelper = new DBHelper(this);

        // Normal registration
        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            long id = dbHelper.registerUser(name, email, password);
            if (id != -1) {
                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Registration Failed: Email may already exist", Toast.LENGTH_SHORT).show();
            }
        });

        // Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // View Users dialog
        btnViewUsers.setOnClickListener(v -> viewUsersDialog());
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(task2 -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String name = account.getDisplayName();
                String email = account.getEmail();
                String googleId = account.getId();

                dbHelper.registerGoogleUser(name, email, googleId);

                Toast.makeText(this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                finish();
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
        }
    }

    // Fixed View Users dialog with Update/Delete
    private void viewUsersDialog() {
        Cursor cursor = dbHelper.getAllUsers();
        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No registered users found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] usersArray = new String[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            String email = cursor.getString(1);
            usersArray[i] = name + " (" + email + ")";
            i++;
        }
        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("Registered Users")
                .setItems(usersArray, (dialog, which) -> {
                    Cursor c = dbHelper.getAllUsers();
                    c.moveToPosition(which);
                    String selectedName = c.getString(0);
                    String selectedEmail = c.getString(1);
                    c.close();

                    // Options for user
                    String[] options = {"Update", "Delete"};
                    new AlertDialog.Builder(this)
                            .setTitle("Choose Action for " + selectedName)
                            .setItems(options, (d, optionIndex) -> {
                                if (optionIndex == 0) {
                                    showUpdateUserDialog(selectedName, selectedEmail);
                                } else if (optionIndex == 1) {
                                    boolean deleted = dbHelper.deleteUser(selectedEmail);
                                    if (deleted)
                                        Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                                    else
                                        Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                                }
                            }).show();
                }).show();
    }

    private void showUpdateUserDialog(String currentName, String currentEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update User");

        View view = getLayoutInflater().inflate(R.layout.dialog_update_user, null);
        EditText etName = view.findViewById(R.id.etUpdateName);
        EditText etEmail = view.findViewById(R.id.etUpdateEmail);

        etName.setText(currentName);
        etEmail.setText(currentEmail);

        builder.setView(view);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean updated = dbHelper.updateUser(currentEmail, newName, newEmail);
            if (updated)
                Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Failed to update user", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
