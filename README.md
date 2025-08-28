🌀 Maze Game & Solver

An Android maze game where players can solve procedurally generated mazes,
track progress, and compare their path with system-generated solutions using A* and Greedy Best-First Search.

🎮 Features

1. Maze Generation
     -DFS (Iterative) – uses a stack to explore and backtrack.
     -Prim’s Algorithm – generates more randomized mazes.

2. Maze Solving
    -A* – Guarantees shortest path (Manhattan/Euclidean heuristic).
    -Greedy Best-First Search – Faster but not always optimal.

3. Gameplay
    -Different colors distinguish user path vs solver path
    -Move counter & sound effects on movement

4. Levels & Difficulty
   -Increasing maze complexity with level progression
   -Levels saved using SQLite (DBHelper)

5. User Management
   -Google Sign-In integration
   -User data storage & progress tracking

🛠️ Tech Stack

1. Language: Java
2. IDE: Android Studio
3. Database: SQLite (via DBHelper)
4. UI: XML + Custom Canvas Drawing
   
📂 Project Structure
├── MainActivity.java        # Gameplay screen (MazeActivity)
├── LevelSelectActivity.java # Level selection & navigation
├── Maze.java                # Maze generation (DFS, Prim’s)
├── Solver.java              # Maze solving (A*, Greedy)
├── DBHelper.java            # SQLite database helper
├── User Management          # Google Sign-In + user data
└── res/                     # Layouts, Drawables, Sounds
