
package com.example.maze;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout rootLayout;
    private TextView tvCurrentLevel;
    private MazeView mazeView;
    private Maze maze;
    private DBHelper dbHelper;
    private int currentLevel = 1;
    private int mazeSize;
    private boolean isMazeSolved = false;

    private static final int MAX_LEVEL = 8;
    private static final int REQUEST_CODE_LEVEL_SELECT = 100;

    private static final String PREFS_NAME = "MazePrefs";
    private static final String KEY_LAST_PLAYED_LEVEL = "lastPlayedLevel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
//        LinearLayout topBar = findViewById(R.id.topBar);
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnSolve = findViewById(R.id.btnSolve);
        Button btnSave = findViewById(R.id.saveButton);
        Button btnLevelSelect = findViewById(R.id.btnLevelSelect);
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setEnabled(true);
        btnLogout.setOnClickListener(v -> {

            getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });


        btnLevelSelect.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LevelSelectActivity.class);
            startActivityForResult(intent, REQUEST_CODE_LEVEL_SELECT);
        });

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("loggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }


        dbHelper = new DBHelper(this);
        int selectedLevel = getIntent().getIntExtra("level_number", -1);
        int unlockedLevel = dbHelper.getMaxUnlockedLevel();

        if (selectedLevel != -1 && selectedLevel <= unlockedLevel + 1 && selectedLevel <= MAX_LEVEL) {
            currentLevel = selectedLevel;
        } else {
            int lastPlayed = getLastPlayedLevel();
            if (lastPlayed >= 1 && lastPlayed <= unlockedLevel + 1 && lastPlayed <= MAX_LEVEL) {
                currentLevel = lastPlayed;
            } else {
                currentLevel = 1;
            }
        }

        saveLastPlayedLevel(currentLevel);

        tvCurrentLevel.setText("Level " + currentLevel);
        mazeSize = calculateMazeSize(currentLevel);

        generateMaze();

        btnReset.setOnClickListener(v -> {
            isMazeSolved = false;
            generateMaze();
        });

        btnSolve.setOnClickListener(v -> solveMazeAnimated());

        btnSave.setOnClickListener(v -> {
            if (!isMazeSolved) {
                Toast.makeText(MainActivity.this, "Complete the level before saving.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Save Progress")
                    .setMessage("Do you want to save your progress?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.completeLevel(currentLevel);
                        Toast.makeText(MainActivity.this, "Progress Saved!", Toast.LENGTH_SHORT).show();

                        // Instead of opening LevelSelectActivity, just load next level directly
                        loadNextLevel();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private int calculateMazeSize(int level) {
        return 7 + (level - 1) * 2;
    }

    private void generateMaze() {
        maze = new Maze(mazeSize, mazeSize, currentLevel);

        if (mazeView != null) {
            rootLayout.removeView(mazeView);
        }

        mazeView = new MazeView(this, maze, null);
        mazeView.setOnMazeSolvedListener(() -> {
            isMazeSolved = true;
            onLevelCompleted();
        });

        RelativeLayout.LayoutParams mazeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        mazeParams.addRule(RelativeLayout.ABOVE, R.id.btnLevelSelect);

        rootLayout.addView(mazeView, mazeParams);

        // Bring buttons to front so they remain clickable
        Button btnSave = findViewById(R.id.saveButton);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnSolve = findViewById(R.id.btnSolve);
        Button btnLevelSelect = findViewById(R.id.btnLevelSelect);

        btnSave.bringToFront();
        btnReset.bringToFront();
        btnSolve.bringToFront();
        btnLevelSelect.bringToFront();
    }

    private void solveMazeAnimated() {
        Solver solver = new Solver(maze);
        List<Cell> animatedPath;

        if (currentLevel <= 3) {
            animatedPath = solver.solveWithDijkstra(
                    maze.grid[0][0],
                    maze.grid[mazeSize - 1][mazeSize - 1]
            );
        } else {
            animatedPath = solver.solveWithAStar(
                    maze.grid[0][0],
                    maze.grid[mazeSize - 1][mazeSize - 1]
            );
        }

        final Handler handler = new Handler();
        final int delay = 50;

        mazeView.setPath(null);
        mazeView.clearAnimatedSteps();
        mazeView.setSolvingMode(true);
        mazeView.invalidate();

        for (int i = 0; i < animatedPath.size(); i++) {
            final Cell step = animatedPath.get(i);
            handler.postDelayed(() -> {
                mazeView.addAnimatedStep(step);
                if (step == animatedPath.get(animatedPath.size() - 1)) {
                    isMazeSolved = true;
                    Toast.makeText(MainActivity.this, "Level " + currentLevel + " Completed!", Toast.LENGTH_SHORT).show();
                }
            }, i * delay);
        }
    }

    private void onLevelCompleted() {
        Toast.makeText(MainActivity.this, "Level " + currentLevel + " Completed!", Toast.LENGTH_SHORT).show();
    }

    private void loadNextLevel() {
        if (currentLevel < MAX_LEVEL) {
            currentLevel++;
            saveLastPlayedLevel(currentLevel);
            mazeSize = calculateMazeSize(currentLevel);
            isMazeSolved = false;
            tvCurrentLevel.setText("Level " + currentLevel);
            new Handler().postDelayed(this::generateMaze, 300);
        } else {
            Toast.makeText(MainActivity.this, "Congrats! You finished the hardest level!", Toast.LENGTH_LONG).show();
        }
    }

    private void saveLastPlayedLevel(int level) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_LAST_PLAYED_LEVEL, level)
                .apply();
    }

    private int getLastPlayedLevel() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_LAST_PLAYED_LEVEL, 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentLevel", currentLevel);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentLevel = savedInstanceState.getInt("currentLevel", 1);
        tvCurrentLevel.setText("Level " + currentLevel);
    }

//    private void logout() {
//        // Clear login data (adjust the prefs name if needed)
//        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
//                .edit()
//                .clear()
//                .apply();
//
//        // Redirect to LoginActivity
//        Intent intent = new Intent(this, LoginActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);
//        finish();
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_LEVEL_SELECT && resultCode == RESULT_OK && data != null) {
            int selectedLevel = data.getIntExtra("level_number", currentLevel);
            int unlockedLevel = dbHelper.getMaxUnlockedLevel();

            // Validate level selection
            if (selectedLevel >= 1 && selectedLevel <= unlockedLevel + 1 && selectedLevel <= MAX_LEVEL) {
                currentLevel = selectedLevel;
                saveLastPlayedLevel(currentLevel);
                mazeSize = calculateMazeSize(currentLevel);
                tvCurrentLevel.setText("Level " + currentLevel);
                generateMaze();
            } else {
                Toast.makeText(this, "Level is locked or invalid!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}