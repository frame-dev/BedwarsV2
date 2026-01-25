# BedWars SQLite Database Guide

## Overview

The BedWars plugin now uses an SQLite database to store player statistics persistently. This provides better performance, reliability, and data integrity compared to YAML files.

## Database Location

The database file is automatically created at:
```
plugins/BedWars/bedwars.db
```

## Database Schema

### Table: `player_stats`

| Column | Type | Description |
|--------|------|-------------|
| `uuid` | TEXT (PRIMARY KEY) | Player's unique identifier |
| `player_name` | TEXT | Player's last known name |
| `wins` | INTEGER | Total games won |
| `losses` | INTEGER | Total games lost |
| `kills` | INTEGER | Total kills |
| `deaths` | INTEGER | Total deaths |
| `final_kills` | INTEGER | Final kills (kills when player has no bed) |
| `beds_broken` | INTEGER | Total beds broken |
| `games_played` | INTEGER | Total games participated in |
| `last_played` | INTEGER | Timestamp of last game (milliseconds) |
| `created_at` | INTEGER | Timestamp of first game (milliseconds) |

### Indexes

- `idx_wins` - Index on wins (descending) for leaderboards
- `idx_kills` - Index on kills (descending) for leaderboards
- `idx_beds_broken` - Index on beds_broken (descending) for leaderboards

## Features

### Automatic Operations

1. **Player Join**: Stats are automatically loaded from database when a player joins the server
2. **Player Quit**: Stats are automatically saved to database when a player leaves
3. **Game End**: Stats are updated and saved when a game ends
4. **Plugin Disable**: All cached stats are saved before shutdown

### Statistics Tracking

The plugin tracks the following statistics:
- **Wins/Losses**: Updated at the end of each game
- **Games Played**: Automatically incremented with wins/losses
- **Kills/Deaths**: Tracked during gameplay
- **Final Kills**: Kills made against players without beds
- **Beds Broken**: Tracked when a team's bed is destroyed
- **Win Rate**: Calculated as (wins / games_played) * 100
- **K/D Ratio**: Calculated as kills / deaths (shows 0 if no deaths)

### Caching System

- Stats are cached in memory for fast access during gameplay
- Cache is automatically updated when stats change
- Cache is cleared when players quit to free memory
- Cache is repopulated when players rejoin

### Async Operations

All database operations are performed asynchronously to prevent server lag:
- Loading stats doesn't block player joins
- Saving stats doesn't block player quits
- Queries run on separate threads

## API Usage

### For Developers

```java
// Get the stats manager
StatsManager statsManager = plugin.getStatsManager();

// Load player stats (async)
CompletableFuture<PlayerStats> statsFuture = statsManager.loadPlayerStats(playerUUID);
statsFuture.thenAccept(stats -> {
    // Use the stats
});

// Get stats from cache (sync)
PlayerStats stats = statsManager.getPlayerStats(playerUUID);

// Save stats (async)
statsManager.savePlayerStats(playerUUID, playerName);

// Get leaderboards (async)
CompletableFuture<Map<String, Integer>> topWins = statsManager.getTopWins(10);
topWins.thenAccept(leaders -> {
    // Display leaderboard
});
```

### Available Leaderboard Methods

- `getTopWins(limit)` - Top players by wins
- `getTopKills(limit)` - Top players by kills
- `getTopBedsBroken(limit)` - Top players by beds broken
- `getTotalPlayers()` - Total unique players in database

## Migration from YAML

If you were using the old YAML-based stats system:

1. **Backup**: Make a backup of your `stats.yml` file
2. **Auto-Migration**: Old stats are not automatically migrated
3. **Fresh Start**: Players will start with fresh statistics
4. **Manual Migration**: If needed, you can manually transfer stats using SQL INSERT statements

## Database Maintenance

### Backup

To backup your statistics:
```bash
cp plugins/BedWars/bedwars.db plugins/BedWars/bedwars.db.backup
```

### Reset Stats

To reset all statistics:
```bash
rm plugins/BedWars/bedwars.db
```
The database will be recreated on next server start.

### Query Database

You can query the database using any SQLite client:
```bash
sqlite3 plugins/BedWars/bedwars.db "SELECT * FROM player_stats ORDER BY wins DESC LIMIT 10;"
```

## Troubleshooting

### Database Connection Issues

If you see connection errors in console:
1. Check that the plugin folder exists and is writable
2. Ensure SQLite JDBC driver is included in the plugin JAR
3. Check for file permission issues

### Stats Not Saving

If stats aren't being saved:
1. Check console for SQL errors
2. Verify the database file is not corrupted
3. Ensure proper plugin shutdown (use `/stop` command)

### Performance Issues

If you experience lag:
1. Stats operations are async - check if other plugins are causing issues
2. Database file size should not be an issue (very small)
3. Indexes are optimized for common queries

## Technical Details

### Connection Pooling

The plugin uses a single persistent connection that:
- Reconnects automatically if closed
- Is properly closed on plugin disable
- Uses transactions for batch operations

### Data Integrity

- UUID is the primary key (prevents duplicates)
- All stats fields have DEFAULT 0 (prevents null values)
- Timestamps stored as UNIX milliseconds (long integers)

### Thread Safety

- All database operations are thread-safe
- Prepared statements prevent SQL injection
- Cache operations are synchronized

## Future Enhancements

Potential future additions:
- Leaderboard GUIs
- Stat reset commands
- Player comparison features
- Advanced analytics (average kills per game, etc.)
- Export/import functionality
- Web-based statistics viewer
