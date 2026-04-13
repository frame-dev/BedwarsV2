# SQLite Integration Summary

## Overview

The BedWars plugin persists player statistics using SQLite for reliable and fast local storage.

## What Was Added

### SQLite Dependency

- Added `org.xerial:sqlite-jdbc` in [pom.xml](../pom.xml).
- Included in shaded build output so no manual dependency install is required.

### Database Layer

- Added [src/main/java/ch/framedev/bedwars/database/DatabaseManager.java](../src/main/java/ch/framedev/bedwars/database/DatabaseManager.java).
- Handles connection lifecycle, schema creation, and query execution.
- Creates database file automatically at `plugins/BedWars/bedwars.db`.

### Stats Layer

- Updated [src/main/java/ch/framedev/bedwars/stats/StatsManager.java](../src/main/java/ch/framedev/bedwars/stats/StatsManager.java) for database-backed stats.
- Updated [src/main/java/ch/framedev/bedwars/stats/PlayerStats.java](../src/main/java/ch/framedev/bedwars/stats/PlayerStats.java) with DB-friendly fields and setters.
- Added async loading/saving and in-memory cache behavior for runtime performance.

### Plugin and Listener Wiring

- Updated [src/main/java/ch/framedev/BedWarsPlugin.java](../src/main/java/ch/framedev/BedWarsPlugin.java) to initialize and close database services.
- Updated [src/main/java/ch/framedev/bedwars/listeners/PlayerJoinListener.java](../src/main/java/ch/framedev/bedwars/listeners/PlayerJoinListener.java) to load stats asynchronously.
- Updated [src/main/java/ch/framedev/bedwars/listeners/PlayerQuitListener.java](../src/main/java/ch/framedev/bedwars/listeners/PlayerQuitListener.java) to save and evict cached stats.
- Updated [src/main/java/ch/framedev/bedwars/game/Game.java](../src/main/java/ch/framedev/bedwars/game/Game.java) to persist match-end stat changes.

### Commands

- Updated [src/main/java/ch/framedev/bedwars/commands/ImprovedBedWarsCommand.java](../src/main/java/ch/framedev/bedwars/commands/ImprovedBedWarsCommand.java) to expose stats and leaderboard output based on DB data.

## Schema

```sql
CREATE TABLE player_stats (
  uuid TEXT PRIMARY KEY,
  player_name TEXT,
  wins INTEGER DEFAULT 0,
  losses INTEGER DEFAULT 0,
  kills INTEGER DEFAULT 0,
  deaths INTEGER DEFAULT 0,
  final_kills INTEGER DEFAULT 0,
  beds_broken INTEGER DEFAULT 0,
  games_played INTEGER DEFAULT 0,
  last_played INTEGER,
  created_at INTEGER
);

CREATE INDEX idx_wins ON player_stats(wins DESC);
CREATE INDEX idx_kills ON player_stats(kills DESC);
CREATE INDEX idx_beds_broken ON player_stats(beds_broken DESC);
```

## Runtime Flow

1. Server starts and initializes database services.
2. Player joins and stats are loaded asynchronously.
3. Gameplay uses cached stats in memory.
4. On game end and player quit, changes are saved to DB.
5. On shutdown, pending writes are flushed and connections are closed.

## Commands Exposed

- `/bedwars stats`
- `/bedwars leaderboard <wins|kills|beds>`
- `/bedwars top` (alias)

## Migration Note

Legacy YAML stats are not automatically migrated. If old data is required, export/import must be done manually.

## Verification and Operations

Build:

```bash
mvn clean package
```

Check plugin startup logs for database initialization success.

Database file location:

```text
plugins/BedWars/bedwars.db
```

## Related Docs

- [DATABASE_GUIDE.md](DATABASE_GUIDE.md)
- [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
