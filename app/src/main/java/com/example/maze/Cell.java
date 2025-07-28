package com.example.maze;

import java.util.EnumMap;

public class Cell {
    public int row, col;
    public boolean visited = false;
    public EnumMap<Direction, Boolean> walls = new EnumMap<>(Direction.class);

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        for (Direction dir : Direction.values()) {
            walls.put(dir, true);
        }
    }
}
