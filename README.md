🌀 Maze Game & Solver

An Android maze game where players can solve procedurally generated mazes,
track progress, and compare their path with system-generated solutions using A* and Greedy Best-First Search.

🎮 Features

1. Maze Generation
     i. DFS (Iterative) – uses a stack to explore and backtrack.
     ii. Prim’s Algorithm – generates more randomized mazes.

2. Maze Solving
    i. A* – Guarantees shortest path (Manhattan/Euclidean heuristic).
    ii. Greedy Best-First Search – Faster but not always optimal.

3. Gameplay
    i. Different colors distinguish user path vs solver path
    ii. Move counter & sound effects on movement

4. Levels & Difficulty
   i. Increasing maze complexity with level progression
   ii. Levels saved using SQLite (DBHelper)

5. User Management
   i. Google Sign-In integration
   ii. User data storage & progress tracking

🛠️ Tech Stack

1. Language: Java
2. IDE: Android Studio
3. Database: SQLite (via DBHelper)
4. UI: XML + Custom Canvas Drawing
   

