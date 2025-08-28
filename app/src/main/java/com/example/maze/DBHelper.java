package com.example.maze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "MazeGame.db";
    public static final int DB_VERSION = 3;
    public static final int TOTAL_LEVELS = 8;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Levels table (linked to user)
        db.execSQL("CREATE TABLE levels (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "level_number INTEGER, " +
                "is_completed INTEGER DEFAULT 0, " +
                "FOREIGN KEY(user_id) REFERENCES users(id))");

        // Maze table
        db.execSQL("CREATE TABLE maze_table (id INTEGER PRIMARY KEY AUTOINCREMENT, level INTEGER, maze TEXT, path TEXT)");

        // Users table
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "email TEXT UNIQUE, " +
                "password TEXT, " +   // For normal accounts; null for Google sign-in
                "google_id TEXT, " +  // For Google accounts; null for normal accounts
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS levels");
        db.execSQL("DROP TABLE IF EXISTS maze_table");
        db.execSQL("DROP TABLE IF EXISTS custom_mazes");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    /* ------------------ USER ACCOUNT METHODS ------------------ */
    public long registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        long userId = db.insert("users", null, values);
        if (userId != -1) {
            insertDefaultLevelsForUser((int) userId);
        }
        return userId;
    }

    public long registerGoogleUser(String name, String email, String googleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("google_id", googleId);
        long userId = db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (userId != -1) {
            insertDefaultLevelsForUser((int) userId);
        }
        return userId;
    }

    private void insertDefaultLevelsForUser(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 1; i <= TOTAL_LEVELS; i++) {
            ContentValues levelValues = new ContentValues();
            levelValues.put("user_id", userId);
            levelValues.put("level_number", i);
            levelValues.put("is_completed", i == 1 ? 1 : 0);
            db.insert("levels", null, levelValues);
        }
    }

    public boolean validateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ? AND password = ?", new String[]{email, password});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT name, email FROM users", null);
    }

    // ✅ Added method to get userId
    public int getUserIdByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ?", new String[]{email});
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }
        cursor.close();
        return userId;
    }

    /* ------------------ LEVEL & MAZE METHODS ------------------ */
    public int getCurrentLevel(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(level_number) FROM levels WHERE user_id = ?", new String[]{String.valueOf(userId)});
        int level = 1;
        if (cursor.moveToFirst()) {
            level = cursor.getInt(0);
        }
        cursor.close();
        return level;
    }

    public void saveCustomMaze(String mazeData, int rows, int cols) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maze_data", mazeData);
        values.put("rows", rows);
        values.put("cols", cols);
        db.insert("custom_mazes", null, values);
    }

    public void completeLevel(int userId, int level) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues update = new ContentValues();
        update.put("is_completed", 1);
        db.update("levels", update, "user_id = ? AND level_number = ?", new String[]{String.valueOf(userId), String.valueOf(level)});

        if (level < TOTAL_LEVELS) {
            Cursor cursor = db.rawQuery("SELECT level_number FROM levels WHERE user_id = ? AND level_number = ?", new String[]{String.valueOf(userId), String.valueOf(level + 1)});
            if (!cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put("user_id", userId);
                values.put("level_number", level + 1);
                values.put("is_completed", 0);
                db.insert("levels", null, values);
            }
            cursor.close();
        }
    }

    public boolean isLevelCompleted(int userId, int level) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT level_number FROM levels WHERE user_id = ? AND level_number = ? AND is_completed = 1", new String[]{String.valueOf(userId), String.valueOf(level)});
        boolean completed = cursor.moveToFirst();
        cursor.close();
        return completed;
    }

    public int getMaxUnlockedLevel(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int maxCompletedLevel = 1;

        Cursor cursor = db.rawQuery("SELECT MAX(level_number) FROM levels WHERE user_id = ? AND is_completed = 1", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            maxCompletedLevel = cursor.getInt(0);
        }
        cursor.close();

        Cursor nextCursor = db.rawQuery("SELECT level_number FROM levels WHERE user_id = ? AND level_number = ?", new String[]{String.valueOf(userId), String.valueOf(maxCompletedLevel + 1)});
        boolean nextExists = nextCursor.moveToFirst();
        nextCursor.close();

        return nextExists ? maxCompletedLevel + 1 : maxCompletedLevel;
    }

    public int getMaxUnlockedLevel() {
        int defaultUserId = 1; // Adjust if needed
        return getMaxUnlockedLevel(defaultUserId);
    }

    public long insertMaze(int level, String maze, String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("level", level);
        values.put("maze", maze);
        values.put("path", path);
        return db.insert("maze_table", null, values);
    }

    public void resetProgressForUser(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Reset all levels for this user
        db.execSQL("UPDATE levels SET is_completed = 0 WHERE user_id = " + userId);
        // Unlock level 1 for this user
        db.execSQL("UPDATE levels SET is_completed = 1 WHERE user_id = " + userId + " AND level_number = 1");
    }

    // Old global version kept for compatibility
    public void resetProgress() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE levels SET is_completed = 0");
        db.execSQL("UPDATE levels SET is_completed = 1 WHERE level_number = 1");
        db.close();
    }

    public String getUserFullName(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
        String name = "User";
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        return name;
    }

    // Delete a user by email
    public boolean deleteUser(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("users", "email=?", new String[]{email});
        return rows > 0;
    }

    // Update user information
    public boolean updateUser(String oldEmail, String newName, String newEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        values.put("email", newEmail);
        int rows = db.update("users", values, "email=?", new String[]{oldEmail});
        return rows > 0;
    }
    // Get user ID by Google ID
    public long getUserIdByGoogleId(String googleId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE google_id = ?",
                new String[]{googleId}
        );
        long userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getLong(0);
        }
        cursor.close();
        return userId;
    }

    // Check if Google user exists by email
    public boolean googleUserExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE email = ? AND google_id IS NOT NULL",
                new String[]{email}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // Get user ID by Google ID

}
