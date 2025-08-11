package com.example.maze;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashSet;
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

    private static final int MAX_LEVEL = 8; // Increased to 8
    private static final int REQUEST_CODE_LEVEL_SELECT = 100;

    private static final String PREFS_NAME = "MazePrefs";
    private static final String KEY_LAST_PLAYED_LEVEL = "lastPlayedLevel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel);

        Button btnReset = findViewById(R.id.btnReset);
        Button btnSolve = findViewById(R.id.btnSolve);
        Button btnSave = findViewById(R.id.saveButton);
        Button btnLevelSelect = findViewById(R.id.btnLevelSelect);
        Button btnLogout = findViewById(R.id.btnLogout);

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

        generateMaze(); // mazeView created here

        btnReset.setOnClickListener(v -> {
            isMazeSolved = false;
            generateMaze();
            mazeView.resetMaze();
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

        // ✅ Now it's safe to set the move listener
        mazeView.setOnMoveListener(count -> {
            TextView moveCounter = findViewById(R.id.tvMoves);
            moveCounter.setText("Moves: " + count);
        });

        RelativeLayout.LayoutParams mazeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        mazeParams.addRule(RelativeLayout.ABOVE, R.id.btnLevelSelect);

        rootLayout.addView(mazeView, mazeParams);

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
        List<Solver.Step> steps = solver.solveWithAStarAnimated(
                maze.grid[0][0],
                maze.grid[mazeSize - 1][mazeSize - 1]
        );

        final Handler handler = new Handler();
        final int delay = 100;

        mazeView.resetAnimatedSteps();
        mazeView.setSolvingMode(true);
        mazeView.setOpenSet(new HashSet<>());
        mazeView.setClosedSet(new HashSet<>());

        for (int i = 0; i < steps.size(); i++) {
            final int index = i;
            handler.postDelayed(() -> {
                Solver.Step step = steps.get(index);
                mazeView.setOpenSet(step.openSet);
                mazeView.setClosedSet(step.closedSet);
                mazeView.setPlayerPosition(step.current);
                if (step.finalPath != null) {
                    mazeView.resetAnimatedSteps();
                    for (Cell cell : step.finalPath) {
                        mazeView.addAnimatedStep(cell);
                    }
                    isMazeSolved = true;
                    Toast.makeText(MainActivity.this, "Shortest path found!", Toast.LENGTH_SHORT).show();
                }
            }, i * delay);
        }
    }

    private void onLevelCompleted() {
        Toast.makeText(this, "Level " + currentLevel + " Completed!", Toast.LENGTH_SHORT).show();
    }

    private void loadNextLevel() {
        if (currentLevel < MAX_LEVEL) {
            currentLevel++;
            saveLastPlayedLevel(currentLevel);
            mazeSize = calculateMazeSize(currentLevel);
            isMazeSolved = false;
            tvCurrentLevel.setText("Level " + currentLevel);

            // Reset moves counter to 0
            TextView moveCounter = findViewById(R.id.tvMoves);
            moveCounter.setText("Moves: 0");

            new Handler().postDelayed(this::generateMaze, 300);
        } else {
            Toast.makeText(this, "Congrats! You finished the hardest level!", Toast.LENGTH_LONG).show();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_LEVEL_SELECT && resultCode == RESULT_OK && data != null) {
            int selectedLevel = data.getIntExtra("level_number", currentLevel);
            int unlockedLevel = dbHelper.getMaxUnlockedLevel();
            if (selectedLevel >= 1 && selectedLevel <= unlockedLevel + 1 && selectedLevel <= MAX_LEVEL) {
                currentLevel = selectedLevel;
                saveLastPlayedLevel(currentLevel);
                mazeSize = calculateMazeSize(currentLevel);
                tvCurrentLevel.setText("Level " + currentLevel);

                // Reset moves counter to 0
                TextView moveCounter = findViewById(R.id.tvMoves);
                moveCounter.setText("Moves: 0");

                generateMaze();
            } else {
                Toast.makeText(this, "Level is locked or invalid!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
