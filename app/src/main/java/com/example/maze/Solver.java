package com.example.maze;

import java.util.*;

public class Solver {

    private Maze maze;

    public Solver(Maze maze) {
        this.maze = maze;
    }

    public static class Step {
        public Set<Cell> openSet;
        public Set<Cell> closedSet;
        public Cell current;
        public List<Cell> finalPath; // non-null only at last step

        public Step(Set<Cell> openSet, Set<Cell> closedSet, Cell current, List<Cell> finalPath) {
            this.openSet = new HashSet<>(openSet);
            this.closedSet = new HashSet<>(closedSet);
            this.current = current;
            this.finalPath = finalPath; // can be null during search
        }
    }

    public List<Step> solveWithAStarAnimated(Cell start, Cell end) {
        Map<Cell, Integer> gScore = new HashMap<>();
        Map<Cell, Integer> fScore = new HashMap<>();
        Map<Cell, Cell> cameFrom = new HashMap<>();
        PriorityQueue<CellDistance> openSetPQ = new PriorityQueue<>(Comparator.comparingInt(cd -> cd.distance));
        Set<Cell> openSet = new HashSet<>();
        Set<Cell> closedSet = new HashSet<>();

        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                gScore.put(maze.grid[r][c], Integer.MAX_VALUE);
                fScore.put(maze.grid[r][c], Integer.MAX_VALUE);
            }
        }

        gScore.put(start, 0);
        fScore.put(start, heuristic(start, end));
        openSetPQ.add(new CellDistance(start, fScore.get(start)));
        openSet.add(start);

        List<Step> steps = new ArrayList<>();

        while (!openSetPQ.isEmpty()) {
            Cell current = openSetPQ.poll().cell;
            openSet.remove(current);

            // Record current step (before processing neighbors)
            steps.add(new Step(openSet, closedSet, current, null));

            if (current == end) {
                // reconstruct path
                List<Cell> path = reconstructPath(cameFrom, start, end);
                // final step with full path
                steps.add(new Step(openSet, closedSet, current, path));
                break;
            }

            closedSet.add(current);

            for (Direction dir : Direction.values()) {
                if (!current.walls.get(dir)) {
                    Cell neighbor = getNeighbor(current, dir);
                    if (neighbor == null || closedSet.contains(neighbor)) continue;

                    int tentativeG = gScore.get(current) + 1;
                    if (tentativeG < gScore.get(neighbor)) {
                        cameFrom.put(neighbor, current);
                        gScore.put(neighbor, tentativeG);
                        fScore.put(neighbor, tentativeG + heuristic(neighbor, end));

                        if (!openSet.contains(neighbor)) {
                            openSetPQ.add(new CellDistance(neighbor, fScore.get(neighbor)));
                            openSet.add(neighbor);
                        }
                    }
                }
            }
        }

        return steps;
    }

    private int heuristic(Cell a, Cell b) {
        return Math.abs(a.row - b.row) + Math.abs(a.col - b.col);
    }

    private Cell getNeighbor(Cell cell, Direction dir) {
        int r = cell.row, c = cell.col;
        switch (dir) {
            case TOP:    r--; break;
            case RIGHT:  c++; break;
            case BOTTOM: r++; break;
            case LEFT:   c--; break;
        }
        if (r >= 0 && r < maze.rows && c >= 0 && c < maze.cols) {
            return maze.grid[r][c];
        }
        return null;
    }

    private List<Cell> reconstructPath(Map<Cell, Cell> cameFrom, Cell start, Cell end) {
        List<Cell> path = new ArrayList<>();
        Cell current = end;

        while (current != null && current != start) {
            path.add(0, current);
            current = cameFrom.get(current);
        }

        if (current == start) {
            path.add(0, start);
        }

        return path;
    }

    private static class CellDistance {
        Cell cell;
        int distance;

        CellDistance(Cell cell, int distance) {
            this.cell = cell;
            this.distance = distance;
        }
    }
}
