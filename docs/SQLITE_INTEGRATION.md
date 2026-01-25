# SQLite Database Integration - Summary

## What Was Added

### 1. SQLite JDBC Dependency
- Added `org.xerial:sqlite-jdbc:3.45.0.0` to [pom.xml](../pom.xml)
- Dependency is automatically included in the final JAR via Maven Shade plugin

### 2. DatabaseManager Class
- **Location**: [DatabaseManager.java](../src/main/java/ch/framedev/bedwars/database/DatabaseManager.java)
- **Purpose**: Handles SQLite connection, table creation, and query execution
- **Features**:
  - Automatic database creation at `plugins/BedWars/bedwars.db`
  - Creates `player_stats` table with indexes for leaderboards
  - Connection pooling with auto-reconnect
  - Prepared statements to prevent SQL injection
  - Helper methods for queries and updates

### 3. Updated StatsManager
- **Location**: [StatsManager.java](../src/main/java/ch/framedev/bedwars/stats/StatsManager.java)
- **Changes**: Complete rewrite from YAML to SQLite
- **Features**:
  - **Async Operations**: All database I/O is non-blocking using CompletableFuture
  - **Caching**: In-memory cache for fast gameplay access
  - **Auto-save**: Stats saved when players quit or games end
  - **Leaderboards**: Methods to query top players (wins, kills, beds broken)

### 4. Updated PlayerStats
- **Location**: [PlayerStats.java](../src/main/java/ch/framedev/bedwars/stats/PlayerStats.java)
- **Changes**:
  - Added `gamesPlayed` field
  - Added setter methods for database loading
  - Updated `addWin()` and `addLoss()` to increment games played

### 5. Updated BedWarsPlugin
- **Location**: [BedWarsPlugin.java](../src/main/java/ch/framedev/BedWarsPlugin.java)
- **Changes**:
  - Initialize DatabaseManager on plugin enable
  - Pass DatabaseManager to StatsManager
  - Disconnect database on plugin disable
  - Added getter for DatabaseManager

### 6. Updated Listeners

#### PlayerJoinListener
- **Location**: [PlayerJoinListener.java](../src/main/java/ch/framedev/bedwars/listeners/PlayerJoinListener.java)
- **Change**: Load player stats asynchronously when they join

#### PlayerQuitListener
- **Location**: [PlayerQuitListener.java](../src/main/java/ch/framedev/bedwars/listeners/PlayerQuitListener.java)
- **Changes**:
  - Save player stats to database when they quit
  - Clear stats cache to free memory

### 7. Updated Game Logic
- **Location**: [Game.java](../src/main/java/ch/framedev/bedwars/game/Game.java)
- **Changes**: When a game ends:
  - Update win/loss stats for all players
  - Save updated stats to database immediately

### 8. Updated Commands
- **Location**: [src/main/java/ch/framedev/bedwars/commands/BedWarsCommand.java](src/main/java/ch/framedev/bedwars/commands/BedWarsCommand.java)
- **Changes**:
  - `/bedwars stats` now shows real statistics from database
  - Added `/bedwars leaderboard <wins|kills|beds>` command
  - Added `/bedwars top` as alias for leaderboard

### 9. Database Documentation
- **Location**: [DATABASE_GUIDE.md](DATABASE_GUIDE.md)
- **Contents**: Complete guide covering:
  - Database schema and structure
  - Feature explanations
  - API usage examples
  - Migration notes from YAML
  - Maintenance and troubleshooting
  - Technical details

## Database Schema

```sql
CREATE TABLE player_stats (
    uuid TEXT PRIMARY KEY,           -- Player UUID
    player_name TEXT,                -- Player name
    wins INTEGER DEFAULT 0,          -- Total wins
    losses INTEGER DEFAULT 0,        -- Total losses
    kills INTEGER DEFAULT 0,         -- Total kills
    deaths INTEGER DEFAULT 0,        -- Total deaths
    final_kills INTEGER DEFAULT 0,   -- Final kills
    beds_broken INTEGER DEFAULT 0,   -- Beds broken
    games_played INTEGER DEFAULT 0,  -- Total games
    last_played INTEGER,             -- Last play timestamp
    created_at INTEGER               -- First play timestamp
);

-- Indexes for leaderboards
CREATE INDEX idx_wins ON player_stats(wins DESC);
CREATE INDEX idx_kills ON player_stats(kills DESC);
CREATE INDEX idx_beds_broken ON player_stats(beds_broken DESC);
```

## How It Works

### Lifecycle

1. **Server Start**:
   - DatabaseManager connects to SQLite
   - Creates tables if they don't exist
   - Indexes created for fast queries

2. **Player Join**:
   - Stats loaded from database asynchronously
   - Cached in memory for fast access
   - If player is new, empty stats created

3. **During Game**:
   - Stats updated in memory cache
   - No database writes during gameplay (performance)

4. **Game End**:
   - Win/loss stats updated for all players
   - Stats saved to database immediately
   - Games played counter incremented

5. **Player Quit**:
   - Stats saved to database
   - Cache cleared to free memory

6. **Server Stop**:
   - All cached stats saved to database
   - Database connection closed gracefully

### Performance Optimizations

- **Async I/O**: All database operations use CompletableFuture
- **Caching**: Stats cached in memory during gameplay
- **Batch Operations**: Multiple stats can be saved efficiently
- **Indexes**: Database indexes speed up leaderboard queries
- **Prepared Statements**: Compiled queries for better performance

## New Commands

### `/bedwars stats`
Shows comprehensive player statistics:
- Games played
- Wins and losses with win rate
- Kills and deaths with K/D ratio
- Final kills
- Beds broken

### `/bedwars leaderboard <category>`
Shows top 10 players in a category:
- `wins` - Players with most wins
- `kills` - Players with most kills
- `beds` - Players with most beds broken

Aliases: `/bedwars top`

## Migration Notes

### From YAML to SQLite

The old YAML-based stats system (`stats.yml`) is **not automatically migrated**. Players will start with fresh statistics.

If you need to migrate old stats:
1. Backup your `stats.yml` file
2. Write a custom migration script to read YAML and insert into database
3. Or manually accept that players start fresh

### Advantages of SQLite

- **Performance**: Much faster than YAML for large player bases
- **Reliability**: ACID transactions prevent data corruption
- **Scalability**: Can handle millions of player records
- **Queries**: Easy to generate leaderboards and analytics
- **Standard**: SQLite is widely supported and documented

## Testing

The plugin compiles successfully with all changes:
```
[INFO] BUILD SUCCESS
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

The final JAR includes SQLite JDBC driver automatically via Maven Shade.

## Files Modified

- [pom.xml](pom.xml) - Added SQLite dependency
- [src/main/java/ch/framedev/bedwars/database/DatabaseManager.java](src/main/java/ch/framedev/bedwars/database/DatabaseManager.java) - New
- [src/main/java/ch/framedev/bedwars/stats/StatsManager.java](src/main/java/ch/framedev/bedwars/stats/StatsManager.java) - Complete rewrite
- [src/main/java/ch/framedev/bedwars/stats/PlayerStats.java](src/main/java/ch/framedev/bedwars/stats/PlayerStats.java) - Added fields/setters
- [src/main/java/ch/framedev/BedWarsPlugin.java](src/main/java/ch/framedev/BedWarsPlugin.java) - Database initialization
- [src/main/java/ch/framedev/bedwars/listeners/PlayerJoinListener.java](src/main/java/ch/framedev/bedwars/listeners/PlayerJoinListener.java) - Load stats
- [src/main/java/ch/framedev/bedwars/listeners/PlayerQuitListener.java](src/main/java/ch/framedev/bedwars/listeners/PlayerQuitListener.java) - Save stats
- [src/main/java/ch/framedev/bedwars/game/Game.java](src/main/java/ch/framedev/bedwars/game/Game.java) - Update stats on game end
- [src/main/java/ch/framedev/bedwars/commands/BedWarsCommand.java](src/main/java/ch/framedev/bedwars/commands/BedWarsCommand.java) - Stats & leaderboard commands
- [DATABASE_GUIDE.md](DATABASE_GUIDE.md) - New documentation

## Next Steps

To use the plugin:

1. **Build**: `mvn clean package`
2. **Install**: Copy `target/bedwars-1.0-SNAPSHOT.jar` to your server's `plugins/` folder
3. **Start**: Start your Spigot 1.20.4 server
4. **Verify**: Check console for "Successfully connected to SQLite database!"
5. **Play**: Join games and your stats will be automatically tracked

The database file will be created at: `plugins/BedWars/bedwars.db`

## Troubleshooting

If you encounter issues:

1. **Check Console**: Look for database connection errors
2. **Permissions**: Ensure plugin folder is writable
3. **SQLite**: Verify `sqlite-jdbc` is in the JAR (use `jar tf bedwars-1.0-SNAPSHOT.jar | grep sqlite`)
4. **Backup**: Database file can be backed up by copying `bedwars.db`

For more details, see [DATABASE_GUIDE.md](DATABASE_GUIDE.md).
