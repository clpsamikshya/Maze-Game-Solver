package com.example.maze;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout rootLayout;
    private TextView tvGreeting;
    private TextView tvCurrentLevel;
    private TextView tvMoves;
    private TextView tvAlgorithmTime; // added timer TextView
    private MazeView mazeView;
    private Maze maze;
    private DBHelper dbHelper;
    private int currentLevel = 1;
    private int mazeSize;
    private boolean isMazeSolved = false;
    private Spinner spinnerAlgorithm;
    private int userId;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int algorithmSeconds = 0; // seconds elapsed for current algorithm run

    private static final int MAX_LEVEL = 8;
    private static final int REQUEST_CODE_LEVEL_SELECT = 100;
    private static final String PREFS_NAME = "MazePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        tvMoves = findViewById(R.id.tvMoves);
        tvAlgorithmTime = findViewById(R.id.tvTimer); // link timer TextView
        spinnerAlgorithm = findViewById(R.id.spinnerAlgorithm);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"A*", "Greedy First Search"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlgorithm.setAdapter(adapter);

        Button btnReset = findViewById(R.id.btnReset);
        Button btnSolve = findViewById(R.id.btnSolve);
        Button btnSave = findViewById(R.id.saveButton);
        Button btnLevelSelect = findViewById(R.id.btnLevelSelect);
        Button btnLogout = findViewById(R.id.btnLogout);

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("loggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = prefs.getInt("loggedInUserId", -1);
        dbHelper = new DBHelper(this);

        // Fetch and display the user's full name
        String fullName = dbHelper.getUserFullName(userId);
        tvGreeting.setText("Hi! " + fullName);

        int selectedLevel = getIntent().getIntExtra("level_number", -1);
        int unlockedLevel = dbHelper.getMaxUnlockedLevel(userId);

        int lastPlayed = getLastPlayedLevel(userId);
        if (selectedLevel != -1 && selectedLevel >= 1 && selectedLevel <= unlockedLevel && selectedLevel <= MAX_LEVEL) {
            currentLevel = selectedLevel;
        } else if (lastPlayed >= 1 && lastPlayed <= unlockedLevel && lastPlayed <= MAX_LEVEL) {
            currentLevel = lastPlayed;
        } else {
            currentLevel = 1;
        }

        saveLastPlayedLevel(userId, currentLevel);
        tvCurrentLevel.setText("Level: " + currentLevel);
        tvMoves.setText("Moves: 0");
        tvAlgorithmTime.setText("Time: 0 sec"); // initialize timer
        mazeSize = calculateMazeSize(currentLevel);

        generateMaze();

        btnReset.setOnClickListener(v -> {
            isMazeSolved = false;
            generateMaze();
            mazeView.resetPlayer();
            tvMoves.setText("Moves: 0");
            resetAlgorithmTimer(); // reset timer on reset
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
                        dbHelper.completeLevel(userId, currentLevel);
                        Toast.makeText(MainActivity.this, "Progress Saved!", Toast.LENGTH_SHORT).show();
                        loadNextLevel();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        btnLevelSelect.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LevelSelectActivity.class);
            startActivityForResult(intent, REQUEST_CODE_LEVEL_SELECT);
        });

        // LOGOUT BUTTON UPDATED WITH CONFIRMATION DIALOG
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                                .edit()
                                .clear()
                                .apply();

                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build();
                        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);

                        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                            mGoogleSignInClient.revokeAccess().addOnCompleteListener(task2 -> {
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        });
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
            mazeView.setMovable(false);
            onLevelCompleted();
        });

        mazeView.setOnMoveListener(count -> tvMoves.setText("Moves: " + count));

        RelativeLayout.LayoutParams mazeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        mazeParams.addRule(RelativeLayout.ABOVE, R.id.btnLevelSelect);

        rootLayout.addView(mazeView, mazeParams);

        findViewById(R.id.saveButton).bringToFront();
        findViewById(R.id.btnReset).bringToFront();
        findViewById(R.id.btnSolve).bringToFront();
        findViewById(R.id.btnLevelSelect).bringToFront();
        spinnerAlgorithm.bringToFront();
    }

    private void solveMazeAnimated() {
        String algorithm = spinnerAlgorithm.getSelectedItem().toString();

        mazeView.setAlgorithm(algorithm.equals("Greedy First Search")); // this will automatically call updateAlgorithmColors()

        // Reset move count and timer for this algorithm run
        mazeView.resetMoveCount();
        tvMoves.setText("Moves: 0");
        resetAlgorithmTimer();
        startAlgorithmTimer();

        Solver solver = new Solver(maze);
        List<Solver.Step> steps;

        if (algorithm.equals("Greedy First Search")) {
            steps = solver.solveWithGreedyAnimated(
                    maze.grid[0][0],
                    maze.grid[mazeSize - 1][mazeSize - 1]
            );
        } else {
            steps = solver.solveWithAStarAnimated(
                    maze.grid[0][0],
                    maze.grid[mazeSize - 1][mazeSize - 1]
            );
        }

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
                        mazeView.addAnimatedStep(cell); // increments moveCount only for final path
                    }

                    stopAlgorithmTimer(); // ✅ stop timer when algorithm finishes

                    isMazeSolved = true;
                    mazeView.setMovable(false);
                    Toast.makeText(MainActivity.this,
                            "Maze solved with " + (algorithm.equals("A*") ? "A*" : "Greedy First Search"),
                            Toast.LENGTH_SHORT).show();
                }
            }, i * delay);
        }
    }

    // Timer helper methods
    private void startAlgorithmTimer() {
        stopAlgorithmTimer();
        algorithmSeconds = 0;
        tvAlgorithmTime.setText("Time: 0 sec");

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                algorithmSeconds++;
                tvAlgorithmTime.setText("Time: " + algorithmSeconds + " sec");
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopAlgorithmTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void resetAlgorithmTimer() {
        stopAlgorithmTimer();
        algorithmSeconds = 0;
        tvAlgorithmTime.setText("Time: 0 sec ");
    }

    private void onLevelCompleted() {
        Toast.makeText(this, "Level " + currentLevel + " Completed!", Toast.LENGTH_SHORT).show();
    }

    private void loadNextLevel() {
        if (currentLevel < MAX_LEVEL) {
            currentLevel++;
            saveLastPlayedLevel(userId, currentLevel);
            mazeSize = calculateMazeSize(currentLevel);
            isMazeSolved = false;
            tvCurrentLevel.setText("Level: " + currentLevel);
            tvMoves.setText("Moves: 0");
            resetAlgorithmTimer();
            new Handler().postDelayed(this::generateMaze, 300);
        } else {
            Toast.makeText(this, "Congrats! You finished the hardest level!", Toast.LENGTH_LONG).show();
        }
    }

    private void saveLastPlayedLevel(int userId, int level) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt("lastPlayedLevel_" + userId, level)
                .apply();
    }

    private int getLastPlayedLevel(int userId) {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt("lastPlayedLevel_" + userId, 1);
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
        tvCurrentLevel.setText("Level: " + currentLevel);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_LEVEL_SELECT && resultCode == RESULT_OK && data != null) {
            int selectedLevel = data.getIntExtra("level_number", currentLevel);
            int unlockedLevel = dbHelper.getMaxUnlockedLevel(userId);
            if (selectedLevel >= 1 && selectedLevel <= unlockedLevel && selectedLevel <= MAX_LEVEL) {
                currentLevel = selectedLevel;
                saveLastPlayedLevel(userId, currentLevel);
                mazeSize = calculateMazeSize(currentLevel);
                tvCurrentLevel.setText("Level: " + currentLevel);
                tvMoves.setText("Moves: 0");
                resetAlgorithmTimer();
                generateMaze();
            } else {
                Toast.makeText(this, "Level is locked or invalid!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
