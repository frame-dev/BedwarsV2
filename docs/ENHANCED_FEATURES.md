# BedWars Enhanced Features Guide

## Overview

This document covers the enhanced features added to the BedWars plugin:
- **Improved Commands** with tab completion
- **Better Arena Setup** with guided workflow
- **Spectator Mode** for watching games

---

## Table of Contents

1. [Commands](#commands)
2. [Arena Setup](#arena-setup)
3. [Spectator Mode](#spectator-mode)
4. [Permissions](#permissions)
5. [Configuration](#configuration)

---

## Commands

### Player Commands

| Command | Description | Usage |
|---------|-------------|-------|
| `/bw join <arena>` | Join a game | `/bw join arena1` |
| `/bw leave` | Leave current game | `/bw leave` |
| `/bw spectate <arena>` | Spectate a game | `/bw spectate arena1` |
| `/bw stats [player]` | View statistics | `/bw stats` or `/bw stats PlayerName` |
| `/bw top <category>` | View leaderboards | `/bw top wins` |
| `/bw list` | List all arenas | `/bw list` |

**Leaderboard Categories:**
- `wins` - Top players by wins
- `kills` - Top players by kills
- `beds` - Top players by beds broken

### Setup Commands

**Requires Permission:** `bedwars.setup`

| Command | Description | Usage |
|---------|-------------|-------|
| `/bw setup create <name>` | Create new arena | `/bw setup create arena1` |
| `/bw setup delete <arena>` | Delete arena | `/bw setup delete arena1` |
| `/bw setup setlobby` | Set lobby spawn | Stand at location and run command |
| `/bw setup setspectator` | Set spectator spawn | Stand at location and run command |
| `/bw setup setspawn <team>` | Set team spawn | `/bw setup setspawn red` |
| `/bw setup setbed <team>` | Set bed location | `/bw setup setbed red` |
| `/bw setup addgenerator <name>` | Add resource generator | `/bw setup addgenerator diamond1` |
| `/bw setup setminplayers <count>` | Set minimum players | `/bw setup setminplayers 2` |
| `/bw setup setmaxplayers <count>` | Set maximum players | `/bw setup setmaxplayers 8` |
| `/bw setup info` | Show setup progress | `/bw setup info` |
| `/bw setup save` | Save arena | `/bw setup save` |
| `/bw setup cancel` | Cancel setup | `/bw setup cancel` |
| `/bw setup list` | List all arenas | `/bw setup list` |

**Available Team Colors:**
- `RED`, `BLUE`, `GREEN`, `YELLOW`, `AQUA`, `WHITE`, `PINK`, `GRAY`

### Admin Commands

**Requires Permission:** `bedwars.admin`

| Command | Description | Usage |
|---------|-------------|-------|
| `/bw start` | Force start game | `/bw start` |
| `/bw stop` | Force stop game | `/bw stop` |
| `/bw resetworld` | Reset world blocks | `/bw resetworld` |
| `/bw reload` | Reload configuration | `/bw reload` |

---

## Arena Setup

### Complete Setup Workflow

#### 1. Create Arena
```
/bw setup create MyArena
```
This starts a new arena setup session.

#### 2. Set Lobby Spawn
Stand where you want players to spawn when joining:
```
/bw setup setlobby
```

#### 3. Set Spectator Spawn
Stand where you want spectators to spawn:
```
/bw setup setspectator
```
**Tip:** Place this high above the arena for best view

#### 4. Configure Teams
For each team you want (minimum 2):

**Set Team Spawn:**
```
/bw setup setspawn red
/bw setup setspawn blue
/bw setup setspawn green
/bw setup setspawn yellow
```

**Set Bed Locations:**
```
/bw setup setbed red
/bw setup setbed blue
/bw setup setbed green
/bw setup setbed yellow
```

#### 5. Add Resource Generators (Optional)
```
/bw setup addgenerator diamond1
/bw setup addgenerator diamond2
/bw setup addgenerator emerald1
```
**Note:** Team generators (Iron/Gold) are added automatically

#### 6. Set Player Limits
```
/bw setup setminplayers 2
/bw setup setmaxplayers 8
```

#### 7. Check Progress
```
/bw setup info
```
This shows what's been configured and what's missing.

#### 8. Save Arena
```
/bw setup save
```
**Requirements to save:**
- Arena name set
- Lobby spawn set
- Spectator spawn set
- At least 2 teams with both spawn and bed
- Min/max players configured

#### Cancel Setup
If you make a mistake:
```
/bw setup cancel
```
This discards all unsaved changes.

### Arena File Location

Arenas are saved in:
```
plugins/BedWars/arenas.yml
```

### Example Arena Structure
```yaml
arenas:
  example:
    lobby-spawn: world,0.0,64.0,0.0,0.0,0.0
    spectator-spawn: world,0.0,80.0,0.0,0.0,0.0
    min-players: 2
    max-players: 8
    teams:
      red:
        spawn: world,10.0,64.0,10.0,0.0,0.0
        bed: world,15.0,64.0,15.0,0.0,0.0
      blue:
        spawn: world,-10.0,64.0,-10.0,0.0,0.0
        bed: world,-15.0,64.0,-15.0,0.0,0.0
    generators:
      diamond1: world,0.0,64.0,20.0,0.0,0.0
```

---

## Spectator Mode

### How to Spectate

1. **Join as Spectator:**
   ```
   /bw spectate arena1
   ```
   **Note:** You can only spectate games that are running or ending

2. **Spectator Features:**
   - ✅ Fly through walls (Ghost mode)
   - ✅ See all players
   - ✅ Invisible to all players
   - ✅ Can't interact with the game
   - ✅ Can't be damaged

3. **Leave Spectating:**
   ```
   /bw leave
   ```

### Spectator Restrictions

- Cannot join a game while spectating
- Cannot spectate if already in a game
- Can only spectate running or ending games
- Cannot interact with any game elements

### Best Practices

**For Server Admins:**
- Place spectator spawn high above the arena
- Ensure spectator spawn has clear view of entire arena
- Test spectator view before finalizing arena

**For Spectators:**
- Use `/bw list` to see active games
- Respect players and don't spoil strategies in chat
- Use `/bw leave` when done watching

---

## BungeeCord Support

The plugin includes full BungeeCord integration for cross-server functionality.

### Setup BungeeCord

**In config.yml:**
```yaml
bungeecord:
  enabled: true
  lobby-server: lobby  # Where to send players after game
```

### BungeeCord Commands

```bash
# Send player to another server
/bw send <player> <server>

# Get player count on a server
/bw playercount <server>

# Get all servers
/bw serverlist

# Get current server
/bw currentserver
```

### BungeeCord Features

- ✅ Send winning players to lobby server
- ✅ Send losers back to lobby
- ✅ Cross-server stats sync
- ✅ Global leaderboards (with database sync)
- ✅ Player communication across servers

### Requirements

- BungeeCord or Waterfall proxy
- All servers in proxy network
- Shared database (MySQL recommended for cross-server)
- `BungeeCord` channel registered

---

## Database System

### SQLite (Default)

Included by default, no setup needed:
```
plugins/BedWars/bedwars.db
```

**Pros:**
- ✓ No external server needed
- ✓ Auto-created
- ✓ Perfect for single-server setups

**Cons:**
- ✗ Can't share with other servers
- ✗ Limited concurrent access

### MySQL

For multi-server networks:

**Setup:**
1. Create database: `CREATE DATABASE bedwars;`
2. Edit config.yml:
```yaml
database:
  type: mysql
  host: localhost
  port: 3306
  database: bedwars
  username: bedwars_user
  password: your_password
  pool-size: 10
```

**Pros:**
- ✓ Share stats across servers
- ✓ Global leaderboards
- ✓ Better performance on large servers
- ✓ Remote database support

**Cons:**
- ✗ Requires MySQL server
- ✗ Network dependency

### Statistics Stored

For each player:
- Games played
- Wins and losses
- Kills and deaths
- Final kills
- Beds broken
- Win rate
- K/D ratio

---

## Database Maintenance

### Backup Statistics

```bash
# Backup SQLite
cp plugins/BedWars/bedwars.db bedwars.db.backup

# Backup MySQL
mysqldump -u bedwars_user -p bedwars > bedwars.sql
```

### Clean Old Data

To remove stats older than 90 days (MySQL):
```sql
DELETE FROM player_stats WHERE last_played < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

### Reset All Statistics

```bash
# SQLite: Delete the database file
rm plugins/BedWars/bedwars.db

# MySQL: DROP database and recreate
DROP DATABASE bedwars;
CREATE DATABASE bedwars;
```

Restart server to recreate fresh database.

---

## Permissions

### Permission Nodes

| Permission | Description | Default |
|------------|-------------|---------|
| `bedwars.join` | Join games | Everyone |
| `bedwars.spectate` | Spectate games | Everyone |
| `bedwars.stats` | View statistics | Everyone |
| `bedwars.leaderboard` | View leaderboards | Everyone |
| `bedwars.setup` | Arena setup commands | Operators |
| `bedwars.admin` | Administrative commands | Operators |

### Permission Setup Example

**Using LuckPerms:**
```
/lp group builder permission set bedwars.setup true
/lp group admin permission set bedwars.admin true
```

**Using PermissionsEx:**
```
/pex group builder add bedwars.setup
/pex group admin add bedwars.admin
```

---

## Configuration

### Tab Completion

The plugin includes smart tab completion for:
- Arena names
- Team colors
- Leaderboard categories
- All command arguments

**How to Use:**
1. Type command: `/bw join `
2. Press TAB
3. See available arenas
4. Press TAB again to cycle through options

### Command Aliases

You can use these short forms:
- `/bedwars` → `/bw`
- `/bedwars leaderboard` → `/bw top`
- Team colors can be lowercase: `red`, `blue`, etc.

### UI Improvements

All commands now feature:
- ✨ Colored output with icons
- 📊 Formatted boxes for better readability
- 🏆 Medals for leaderboard top 3
- ✓ Success checkmarks
- ✗ Error indicators

### Example Command Outputs

**Stats Display:**
```
╔══════════════════════════════╗
║  YourName's Statistics       ║
╠══════════════════════════════╣
 Games Played: 42
 Wins: 25 | Losses: 17
 Win Rate: 59.5%
 Kills: 156 | Deaths: 98
 K/D Ratio: 1.59
 Final Kills: 45
 Beds Broken: 32
╚══════════════════════════════╝
```

**Leaderboard Display:**
```
╔══════════════════════════════╗
║  Top 10 Wins                 ║
╠══════════════════════════════╣
 🥇 #1 PlayerOne - 150 wins
 🥈 #2 PlayerTwo - 142 wins
 🥉 #3 PlayerThree - 138 wins
  #4 PlayerFour - 127 wins
  ...
╚══════════════════════════════╝
```

---

## Troubleshooting

### Common Issues

**"Arena not found!"**
- Check arena name with `/bw list`
- Arena names are case-sensitive
- Ensure arena is loaded (check console on startup)

**"Arena setup incomplete!"**
- Run `/bw setup info` to see missing items
- Must have at least 2 teams fully configured
- Both spawn and bed required for each team

**"You can only spectate during an active game!"**
- Game must be in RUNNING or ENDING state
- Use `/bw list` to check game status
- Wait for game to start before spectating

**Players can't see spectators**
- This is intentional - spectators are invisible
- Spectators can see everyone
- Feature prevents spectators from interfering

### Getting Help

1. Check console for error messages
2. Verify permissions are set correctly
3. Ensure all setup steps completed
4. Test with `/bw setup info`

---

## Advanced Features

### Multiple Arenas

You can run multiple arenas simultaneously:
```
/bw setup create arena1
/bw setup create arena2
/bw setup create arena3
```

Each arena operates independently with its own:
- Player limit
- Team configuration
- Generators
- World reset system

### Custom Generators

Add generators with descriptive names:
```
/bw setup addgenerator diamond_middle
/bw setup addgenerator diamond_side1
/bw setup addgenerator emerald_center
```

Generators are saved with their exact locations.

### Arena Management

**Delete Unused Arenas:**
```
/bw setup delete old_arena
```

**List All Configured Arenas:**
```
/bw setup list
```

**Reload After Manual Edits:**
```
/bw reload
```

---

## Tips & Best Practices

### For Builders

1. **Test Arena Flow**
   - Walk from spawn to spawn
   - Check sight lines
   - Verify bed accessibility

2. **Spectator Positioning**
   - High altitude (Y=100+)
   - Center of arena
   - Clear view of all teams

3. **Generator Placement**
   - Diamond: Neutral areas (2-4 generators)
   - Emerald: High-risk center (1-2 generators)
   - Team generators auto-added near spawns

### For Admins

1. **Before Opening**
   - Test join/leave commands
   - Verify spectator mode works
   - Check world reset functionality
   - Test with minimum players

2. **Permissions**
   - Give `bedwars.setup` to trusted builders only
   - Keep `bedwars.admin` restricted
   - Everyone should have basic play permissions

3. **Performance**
   - Monitor server TPS during games
   - Check database file size regularly
   - Use `/bw resetworld` if lag occurs

### For Players

1. **Before Joining**
   - Check `/bw list` for available games
   - See player counts and status
   - Choose games that are starting soon

2. **During Games**
   - Break enemy beds for final kills
   - Protect your own bed
   - Collect resources from generators

3. **After Games**
   - Check stats with `/bw stats`
   - Compare with leaderboards `/bw top`
   - Spectate other games to learn strategies

---

## Summary

The enhanced BedWars plugin now provides:

✅ **36 Java classes** with full functionality
✅ **Comprehensive command system** with tab completion
✅ **Guided arena setup** workflow
✅ **Spectator mode** for watching games
✅ **Beautiful UI** with formatted output
✅ **SQLite database** for persistent stats
✅ **World reset** to restore blocks
✅ **Multiple arenas** support
✅ **Full documentation** and help system

Enjoy your enhanced BedWars experience! 🎮
