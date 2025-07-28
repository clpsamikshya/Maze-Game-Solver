package com.example.maze;

import java.util.*;

public class Maze {
    public int rows, cols;
    public Cell[][] grid;

    public Maze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        grid = new Cell[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = new Cell(r, c);
        generateMaze();
    }

    private void generateMaze() {
        Stack<Cell> stack = new Stack<>();
        Cell start = grid[0][0];
        start.visited = true;
        stack.push(start);

        while (!stack.isEmpty()) {
            Cell current = stack.peek();
            List<Cell> neighbors = getUnvisitedNeighbors(current);

            if (!neighbors.isEmpty()) {
                Cell next = neighbors.get(new Random().nextInt(neighbors.size()));
                removeWalls(current, next);
                next.visited = true;
                stack.push(next);
            } else {
                stack.pop();
            }
        }
    }

    private List<Cell> getUnvisitedNeighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();
        int r = cell.row, c = cell.col;

        if (r > 0 && !grid[r - 1][c].visited) neighbors.add(grid[r - 1][c]);
        if (r < rows - 1 && !grid[r + 1][c].visited) neighbors.add(grid[r + 1][c]);
        if (c > 0 && !grid[r][c - 1].visited) neighbors.add(grid[r][c - 1]);
        if (c < cols - 1 && !grid[r][c + 1].visited) neighbors.add(grid[r][c + 1]);

        return neighbors;
    }

    private void removeWalls(Cell a, Cell b) {
        int dx = b.col - a.col;
        int dy = b.row - a.row;

        if (dx == 1) {
            a.walls.put(Direction.RIGHT, false);
            b.walls.put(Direction.LEFT, false);
        } else if (dx == -1) {
            a.walls.put(Direction.LEFT, false);
            b.walls.put(Direction.RIGHT, false);
        } else if (dy == 1) {
            a.walls.put(Direction.BOTTOM, false);
            b.walls.put(Direction.TOP, false);
        } else if (dy == -1) {
            a.walls.put(Direction.TOP, false);
            b.walls.put(Direction.BOTTOM, false);
        }
    }
}

