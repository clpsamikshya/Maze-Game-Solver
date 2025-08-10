package com.example.maze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "MazeGame.db";
    public static final int DB_VERSION = 1;
    public static final int TOTAL_LEVELS = 8;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table for levels
        db.execSQL("CREATE TABLE levels (level_number INTEGER PRIMARY KEY, is_completed INTEGER DEFAULT 0)");

        // Insert levels 1 to 8: only level 1 unlocked
        for (int i = 1; i <= TOTAL_LEVELS; i++) {
            ContentValues values = new ContentValues();
            values.put("level_number", i);
            values.put("is_completed", i == 1 ? 1 : 0); // Only level 1 unlocked initially
            db.insert("levels", null, values);
        }

        // Create table for storing maze and path
        db.execSQL("CREATE TABLE maze_table (id INTEGER PRIMARY KEY AUTOINCREMENT, level INTEGER, maze TEXT, path TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS levels");
        db.execSQL("DROP TABLE IF EXISTS maze_table");
        onCreate(db);
    }

    public int getCurrentLevel() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(level_number) FROM levels", null);
        int level = 1;
        if (cursor.moveToFirst()) {
            level = cursor.getInt(0);
        }
        cursor.close();
        return level;
    }

    public void completeLevel(int level) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Mark current level as completed
        ContentValues update = new ContentValues();
        update.put("is_completed", 1);
        db.update("levels", update, "level_number = ?", new String[]{String.valueOf(level)});

        // Unlock next level if within TOTAL_LEVELS and not already added
        if (level < TOTAL_LEVELS) {
            Cursor cursor = db.rawQuery("SELECT level_number FROM levels WHERE level_number = ?", new String[]{String.valueOf(level + 1)});
            if (!cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put("level_number", level + 1);
                values.put("is_completed", 0);
                db.insert("levels", null, values);
            }
            cursor.close();
        }
    }

    public boolean isLevelCompleted(int level) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT level_number FROM levels WHERE level_number = ? AND is_completed = 1", new String[]{String.valueOf(level)});
        boolean completed = cursor.moveToFirst();
        cursor.close();
        return completed;
    }

    public int getMaxUnlockedLevel() {
        SQLiteDatabase db = this.getReadableDatabase();
        int maxCompletedLevel = 1;

        Cursor cursor = db.rawQuery("SELECT MAX(level_number) FROM levels WHERE is_completed = 1", null);
        if (cursor.moveToFirst()) {
            maxCompletedLevel = cursor.getInt(0);
        }
        cursor.close();

        // Unlock next if it exists
        Cursor nextCursor = db.rawQuery("SELECT level_number FROM levels WHERE level_number = ?", new String[]{String.valueOf(maxCompletedLevel + 1)});
        boolean nextExists = nextCursor.moveToFirst();
        nextCursor.close();

        return nextExists ? maxCompletedLevel + 1 : maxCompletedLevel;
    }

    public long insertMaze(int level, String maze, String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("level", level);
        values.put("maze", maze);
        values.put("path", path);
        return db.insert("maze_table", null, values);
    }

    // Reset all progress except unlock level 1
    public void resetProgress() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE levels SET is_completed = 0");
        db.execSQL("UPDATE levels SET is_completed = 1 WHERE level_number = 1");
        db.close();
    }
}
