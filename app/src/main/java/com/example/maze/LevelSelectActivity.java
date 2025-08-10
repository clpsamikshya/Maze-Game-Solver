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

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class LevelSelectActivity extends AppCompatActivity {

    private ListView listView;
    private DBHelper dbHelper;
    private int maxUnlockedLevel;
    private Button btnResetProgress;

    private static final int TOTAL_LEVELS = 8; // Total available levels

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        listView = findViewById(R.id.levelListView);
        btnResetProgress = findViewById(R.id.btnResetProgress);
        dbHelper = new DBHelper(this);

        // 🔙 Back button setup
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {

            setResult(RESULT_CANCELED);
            finish();
        });

        btnResetProgress.setOnClickListener(v -> {
            dbHelper.resetProgress();
            Toast.makeText(this, "Progress reset! Restarting screen...", Toast.LENGTH_SHORT).show();
            recreate(); // Restart activity to refresh level list
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

                textView.setTextColor(0xFFFFFFFF); // White text for all

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
}
