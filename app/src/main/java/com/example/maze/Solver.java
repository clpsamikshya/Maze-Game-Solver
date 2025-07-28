package com.example.maze;
import java.util.*;

public class Solver {
    private Maze maze;

    public Solver(Maze maze) {
        this.maze = maze;
    }

    public List<Cell> solveAnimated(Cell start, Cell end) {
        Queue<Cell> queue = new LinkedList<>();
        Map<Cell, Cell> cameFrom = new HashMap<>();
        Set<Cell> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);
        cameFrom.put(start, null);

        while (!queue.isEmpty()) {
            Cell current = queue.poll();

            if (current == end) break;

            for (Direction dir : Direction.values()) {
                if (!current.walls.get(dir)) {
                    Cell neighbor = getNeighbor(current, dir);
                    if (neighbor != null && !visited.contains(neighbor)) {
                        queue.add(neighbor);
                        visited.add(neighbor);
                        cameFrom.put(neighbor, current);
                    }
                }
            }
        }

        // Reconstruct path from end to start
        List<Cell> path = new ArrayList<>();
        Cell current = end;
        while (current != null) {
            path.add(0, current);  // add to the beginning
            current = cameFrom.get(current);
        }

        return path;
    }

    private Cell getNeighbor(Cell cell, Direction dir) {
        int r = cell.row, c = cell.col;
        switch (dir) {
            case TOP: r--; break;
            case RIGHT: c++; break;
            case BOTTOM: r++; break;
            case LEFT: c--; break;
        }
        if (r >= 0 && r < maze.rows && c >= 0 && c < maze.cols)
            return maze.grid[r][c];
        return null;
    }
}
