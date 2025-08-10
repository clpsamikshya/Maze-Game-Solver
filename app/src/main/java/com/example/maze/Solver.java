package com.example.maze;

import java.util.*;

public class Solver {
    private Maze maze;

    public Solver(Maze maze) {
        this.maze = maze;
    }

    // Dijkstra's algorithm for shortest path
    public List<Cell> solveWithDijkstra(Cell start, Cell end) {
        Map<Cell, Integer> dist = new HashMap<>();
        Map<Cell, Cell> prev = new HashMap<>();
        PriorityQueue<CellDistance> pq = new PriorityQueue<>(Comparator.comparingInt(cd -> cd.distance));
        Set<Cell> visited = new HashSet<>();

        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                dist.put(maze.grid[r][c], Integer.MAX_VALUE);
            }
        }
        dist.put(start, 0);
        pq.add(new CellDistance(start, 0));

        while (!pq.isEmpty()) {
            CellDistance cd = pq.poll();
            Cell current = cd.cell;

            if (visited.contains(current)) continue;
            visited.add(current);

            if (current == end) break;

            for (Direction dir : Direction.values()) {
                if (!current.walls.get(dir)) {
                    Cell neighbor = getNeighbor(current, dir);
                    if (neighbor != null && !visited.contains(neighbor)) {
                        int alt = dist.get(current) + 1;
                        if (alt < dist.get(neighbor)) {
                            dist.put(neighbor, alt);
                            prev.put(neighbor, current);
                            pq.add(new CellDistance(neighbor, alt));
                        }
                    }
                }
            }
        }

        return reconstructPath(prev, start, end);
    }

    // A* algorithm for shortest path
    public List<Cell> solveWithAStar(Cell start, Cell end) {
        Map<Cell, Integer> gScore = new HashMap<>();
        Map<Cell, Integer> fScore = new HashMap<>();
        Map<Cell, Cell> cameFrom = new HashMap<>();
        PriorityQueue<CellDistance> openSet = new PriorityQueue<>(Comparator.comparingInt(cd -> cd.distance));
        Set<Cell> closedSet = new HashSet<>();

        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                gScore.put(maze.grid[r][c], Integer.MAX_VALUE);
                fScore.put(maze.grid[r][c], Integer.MAX_VALUE);
            }
        }

        gScore.put(start, 0);
        fScore.put(start, heuristic(start, end));
        openSet.add(new CellDistance(start, fScore.get(start)));

        while (!openSet.isEmpty()) {
            Cell current = openSet.poll().cell;

            if (current == end) break;

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

                        boolean inOpen = openSet.stream().anyMatch(cd -> cd.cell == neighbor);
                        if (!inOpen) {
                            openSet.add(new CellDistance(neighbor, fScore.get(neighbor)));
                        }
                    }
                }
            }
        }

        return reconstructPath(cameFrom, start, end);
    }

    private int heuristic(Cell a, Cell b) {
        return Math.abs(a.row - b.row) + Math.abs(a.col - b.col); // Manhattan distance
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
