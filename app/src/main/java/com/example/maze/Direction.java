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
}
