package com.example.maze;

public enum Direction {
    TOP, RIGHT, BOTTOM, LEFT;

    public Direction opposite() {
        switch (this) {
            case TOP: return BOTTOM;
            case RIGHT: return LEFT;
            case BOTTOM: return TOP;
            case LEFT: return RIGHT;
            default: throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public int rowOffset() {
        switch (this) {
            case TOP: return -1;
            case BOTTOM: return 1;
            default: return 0;
        }
    }

    public int colOffset() {
        switch (this) {
            case LEFT: return -1;
            case RIGHT: return 1;
            default: return 0;
        }
    }
}
