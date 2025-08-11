
package com.example.maze;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class LevelSelectActivity extends AppCompatActivity {

    private ListView listView;
    private DBHelper dbHelper;
    private int maxUnlockedLevel;
    private Button btnResetProgress;

    private static final int TOTAL_LEVELS = 8; // Total available levels
    private boolean progressReset = false;  // flag to track reset

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        if (savedInstanceState != null) {
            progressReset = savedInstanceState.getBoolean("progressReset", false);
        }

        listView = findViewById(R.id.levelListView);
        btnResetProgress = findViewById(R.id.btnResetProgress);
        dbHelper = new DBHelper(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (progressReset) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("level_number", 1);
                setResult(RESULT_OK, resultIntent);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        });

        btnResetProgress.setOnClickListener(v -> {
            dbHelper.resetProgress();
            Toast.makeText(this, "Progress reset! Restarting screen...", Toast.LENGTH_SHORT).show();
            progressReset = true;
            recreate();
        });

        loadLevelList();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedLevel = position + 1;
            if (selectedLevel <= maxUnlockedLevel) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("level_number", selectedLevel);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(LevelSelectActivity.this, "Level locked! Complete previous levels first.", Toast.LENGTH_SHORT).show();
            }
        });

        // Add this OnBackPressedCallback to handle system back and swipe back gesture
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (progressReset) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("level_number", 1);
                    setResult(RESULT_OK, resultIntent);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
            }
        });
    }

    private void loadLevelList() {
        maxUnlockedLevel = dbHelper.getMaxUnlockedLevel();

        List<String> levels = new ArrayList<>();
        for (int i = 1; i <= TOTAL_LEVELS; i++) {
            levels.add("Level " + i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, levels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);

                int levelNumber = position + 1;
                textView.setTextColor(0xFF2C3E50);

                if (levelNumber > maxUnlockedLevel) {
                    textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.lock, 0);
                    textView.setCompoundDrawablePadding(16);
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
                return view;
            }
        };

        listView.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("progressReset", progressReset);
    }
}



