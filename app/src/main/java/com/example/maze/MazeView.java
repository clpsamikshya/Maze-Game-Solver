package com.example.maze;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MazeView extends View {
    private Maze maze;
    private List<Cell> path;
    private List<Cell> animatedSteps = new ArrayList<>();
    private List<Cell> userSteps = new ArrayList<>();
    private Paint wallPaint, pathPaint, animPaint, borderPaint, playerPaint, userPathPaint;
    private Cell playerPosition;
    private int cellSize;

    public MazeView(Context context, Maze maze, List<Cell> path) {
        super(context);
        this.maze = maze;
        this.path = path;

        wallPaint = new Paint();
        wallPaint.setColor(Color.DKGRAY);
        wallPaint.setStrokeWidth(6f);
        wallPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(12f);
        borderPaint.setAntiAlias(true);

        pathPaint = new Paint();
        pathPaint.setColor(Color.parseColor("#2C3E50"));
        pathPaint.setStyle(Paint.Style.FILL);
        pathPaint.setAntiAlias(true);

        playerPaint = new Paint();
        playerPaint.setColor(Color.parseColor("#3A506B")); // Dot color updated
        playerPaint.setAntiAlias(true);

        userPathPaint = new Paint();
        userPathPaint.setColor(Color.parseColor("#2C3E50"));
        userPathPaint.setStyle(Paint.Style.FILL);
        userPathPaint.setAntiAlias(true);

        playerPosition = maze.grid[0][0];
        userSteps.add(playerPosition);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (maze == null || maze.grid == null) return;

        cellSize = Math.min(getWidth() / maze.cols, getHeight() / maze.rows);
        int mazeWidth = cellSize * maze.cols;
        int mazeHeight = cellSize * maze.rows;
        int offsetX = (getWidth() - mazeWidth) / 2;
        int offsetY = (getHeight() - mazeHeight) / 2;

        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                Cell cell = maze.grid[r][c];
                int x = offsetX + c * cellSize;
                int y = offsetY + r * cellSize;

                if (cell.walls.get(Direction.TOP))
                    canvas.drawLine(x, y, x + cellSize, y, wallPaint);
                if (cell.walls.get(Direction.RIGHT))
                    canvas.drawLine(x + cellSize, y, x + cellSize, y + cellSize, wallPaint);
                if (cell.walls.get(Direction.BOTTOM))
                    canvas.drawLine(x + cellSize, y + cellSize, x, y + cellSize, wallPaint);
                if (cell.walls.get(Direction.LEFT))
                    canvas.drawLine(x, y + cellSize, x, y, wallPaint);
            }
        }

        // Draw user's path
        for (Cell cell : userSteps) {
            int x = offsetX + cell.col * cellSize;
            int y = offsetY + cell.row * cellSize;
            canvas.drawRoundRect(new RectF(x + 10, y + 10, x + cellSize - 10, y + cellSize - 10), 12f, 12f, userPathPaint);
        }

        // Draw animated solution path if needed
        if (animatedSteps != null) {
            for (Cell cell : animatedSteps) {
                int x = offsetX + cell.col * cellSize;
                int y = offsetY + cell.row * cellSize;
                canvas.drawRoundRect(new RectF(x + 6, y + 6, x + cellSize - 6, y + cellSize - 6), 20f, 20f, pathPaint);
            }
        }

        // Draw player
        if (playerPosition != null) {
            float px = offsetX + playerPosition.col * cellSize + cellSize / 2f;
            float py = offsetY + playerPosition.row * cellSize + cellSize / 2f;
            canvas.drawCircle(px, py, cellSize / 4f, playerPaint);
        }
    }

    public void setPath(List<Cell> path) {
        this.path = path;
        invalidate();
    }

    public void addAnimatedStep(Cell cell) {
        animatedSteps.add(cell);
        invalidate();
    }

    public void resetAnimatedSteps() {
        animatedSteps.clear();
        invalidate();
    }

    public void clearAnimatedSteps() {
        animatedSteps.clear();
    }

    private float downX, downY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float upX = event.getX();
                float upY = event.getY();
                float dx = upX - downX;
                float dy = upY - downY;

                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0) movePlayer("RIGHT");
                    else movePlayer("LEFT");
                } else {
                    if (dy > 0) movePlayer("DOWN");
                    else movePlayer("UP");
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void movePlayer(String direction) {
        int row = playerPosition.row;
        int col = playerPosition.col;
        Cell next = null;

        switch (direction) {
            case "UP":
                if (!playerPosition.walls.get(Direction.TOP) && row > 0) next = maze.grid[row - 1][col];
                break;
            case "DOWN":
                if (!playerPosition.walls.get(Direction.BOTTOM) && row < maze.rows - 1) next = maze.grid[row + 1][col];
                break;
            case "LEFT":
                if (!playerPosition.walls.get(Direction.LEFT) && col > 0) next = maze.grid[row][col - 1];
                break;
            case "RIGHT":
                if (!playerPosition.walls.get(Direction.RIGHT) && col < maze.cols - 1) next = maze.grid[row][col + 1];
                break;
        }

        if (next != null) {
            playerPosition = next;
            userSteps.add(playerPosition);
            invalidate();

            if (playerPosition == maze.grid[maze.rows - 1][maze.cols - 1]) {
                Toast.makeText(getContext(), "Maze Solved!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
