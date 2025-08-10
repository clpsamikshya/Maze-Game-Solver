package com.example.maze;

import java.util.*;

public class Maze {
    public int rows, cols;
    public Cell[][] grid;
    private int level;

    public Maze(int rows, int cols, int level) {
        this.rows = rows;
        this.cols = cols;
        this.level = level;
        grid = new Cell[rows][cols];

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = new Cell(r, c);

        if (level <= 2) {
            generateMazeDFS();  // Simple DFS for early levels
        } else {
            generateMazePrim();  // Complex maze for higher levels
        }

        // Add loops based on level to create multiple solution paths
        int loopsToAdd = Math.min(level * 5, (rows * cols) / 4);
        addLoops(loopsToAdd);

        grid[0][0].walls.put(Direction.TOP, false); // Entrance open
        grid[rows - 1][cols - 1].walls.put(Direction.BOTTOM, false);// open ending
    }

    // ===================== DFS GENERATION =====================
    private void generateMazeDFS() {
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

    // ===================== PRIM’S GENERATION =====================
    private void generateMazePrim() {
        Set<Cell> visited = new HashSet<>();
        List<Edge> walls = new ArrayList<>();
        Cell start = grid[0][0];
        visited.add(start);

        for (Cell neighbor : getUnvisitedNeighbors(start)) {
            walls.add(new Edge(start, neighbor));
        }

        Random rand = new Random();

        while (!walls.isEmpty()) {
            Edge edge = walls.remove(rand.nextInt(walls.size()));
            Cell current = edge.to;

            if (!visited.contains(current)) {
                removeWalls(edge.from, current);
                visited.add(current);

                for (Cell neighbor : getUnvisitedNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        walls.add(new Edge(current, neighbor));
                    }
                }
            }
        }
    }

    private static class Edge {
        Cell from, to;

        Edge(Cell from, Cell to) {
            this.from = from;
            this.to = to;
        }
    }

    // ===================== ADD LOOPS =====================
    public void addLoops(int extraConnections) {
        Random rand = new Random();
        int attempts = 0;
        int added = 0;
        int maxAttempts = extraConnections * 10;

        while (added < extraConnections && attempts < maxAttempts) {
            attempts++;

            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);

            // Skip start and end cells for consistency
            if ((r == 0 && c == 0) || (r == rows - 1 && c == cols - 1)) continue;

            Cell cell = grid[r][c];

            // Shuffle directions to randomize wall removal
            List<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
            Collections.shuffle(directions);

            boolean removedWall = false;
            for (Direction dir : directions) {
                int nr = r + dir.rowOffset();
                int nc = c + dir.colOffset();

                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    Cell neighbor = grid[nr][nc];

                    // Only remove wall if it exists
                    if (cell.walls.get(dir)) {
                        cell.walls.put(dir, false);
                        neighbor.walls.put(dir.opposite(), false);
                        removedWall = true;
                        break;  // Remove only one wall per attempt
                    }
                }
            }

            if (removedWall) added++;
        }
    }

    // ===================== COMMON HELPERS =====================
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

        // Mark cells visited for Prim's algorithm (not necessary for DFS)
        a.visited = true;
        b.visited = true;
    }
}
