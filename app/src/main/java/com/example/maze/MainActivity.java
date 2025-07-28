package com.example.maze;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout rootLayout;
    private MazeView mazeView;
    private Maze maze;
    private final int mazeSize = 10;  // You can change maze size here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnSolve = findViewById(R.id.btnSolve);

        generateMaze();

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateMaze();
            }
        });

        btnSolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solveMazeAnimated();
            }
        });
    }

    private void generateMaze() {
        maze = new Maze(mazeSize, mazeSize);

        if (mazeView != null) {
            rootLayout.removeView(mazeView);
        }

        mazeView = new MazeView(this, maze, null); // Initially no path

        // Layout params to position the MazeView above buttons
        RelativeLayout.LayoutParams mazeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        mazeParams.addRule(RelativeLayout.ABOVE, R.id.btnReset);

        rootLayout.addView(mazeView, mazeParams);
    }

    private void solveMazeAnimated() {
        Solver solver = new Solver(maze);
        List<Cell> animatedPath = solver.solveAnimated(
                maze.grid[0][0],
                maze.grid[mazeSize - 1][mazeSize - 1]
        );

        final Handler handler = new Handler();
        final int delay = 50; // milliseconds between steps

        mazeView.setPath(null); // Clear static path
        mazeView.clearAnimatedSteps(); // Start fresh
        mazeView.invalidate();

        for (int i = 0; i < animatedPath.size(); i++) {
            final Cell step = animatedPath.get(i);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mazeView.addAnimatedStep(step);
                }
            }, i * delay);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("loggedIn", false)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

}

