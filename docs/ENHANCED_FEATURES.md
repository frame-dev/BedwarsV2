# BedWars Enhanced Features Guide

## Overview

This guide summarizes the enhanced gameplay and administration features in the BedWars plugin.

## Contents

1. [Commands](#commands)
2. [Arena Setup Workflow](#arena-setup-workflow)
3. [Spectator Mode](#spectator-mode)
4. [BungeeCord Integration](#bungeecord-integration)
5. [Database Features](#database-features)
6. [Permissions](#permissions)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

## Commands

### Player Commands

| Command | Description | Example |
| --- | --- | --- |
| `/bw join <arena>` | Join a game | `/bw join arena1` |
| `/bw leave` | Leave current game or spectate mode | `/bw leave` |
| `/bw spectate <arena>` | Spectate an active game | `/bw spectate arena1` |
| `/bw stats [player]` | View your stats or another player's stats | `/bw stats` |
| `/bw top <category>` | View leaderboards | `/bw top wins` |
| `/bw list` | List arenas and status | `/bw list` |

### Setup Commands

Requires permission `bedwars.setup`.

| Command | Description | Example |
| --- | --- | --- |
| `/bw setup create <name>` | Start arena setup | `/bw setup create castle` |
| `/bw setup setlobby` | Set waiting lobby spawn | `/bw setup setlobby` |
| `/bw setup setspectator` | Set spectator spawn | `/bw setup setspectator` |
| `/bw setup setspawn <team>` | Set team spawn | `/bw setup setspawn red` |
| `/bw setup setbed <team>` | Set team bed location | `/bw setup setbed red` |
| `/bw setup addgenerator <id>` | Add a resource generator | `/bw setup addgenerator diamond_mid` |
| `/bw setup setminplayers <count>` | Set minimum players | `/bw setup setminplayers 2` |
| `/bw setup setmaxplayers <count>` | Set maximum players | `/bw setup setmaxplayers 8` |
| `/bw setup info` | Show setup progress | `/bw setup info` |
| `/bw setup save` | Save arena configuration | `/bw setup save` |
| `/bw setup cancel` | Discard setup session | `/bw setup cancel` |
| `/bw setup delete <name>` | Delete arena | `/bw setup delete oldmap` |

### Admin Commands

Requires permission `bedwars.admin`.

| Command | Description | Example |
| --- | --- | --- |
| `/bw start` | Force-start game countdown | `/bw start` |
| `/bw stop` | Stop game and cleanup | `/bw stop` |
| `/bw resetworld` | Restore tracked block changes | `/bw resetworld` |
| `/bw reload` | Reload plugin configuration | `/bw reload` |

## Arena Setup Workflow

- Create arena:

```text
/bw setup create MyArena
```

- Set lobby and spectator spawns.
- Configure at least two teams with both spawn and bed locations.
- Add optional diamond and emerald generators.
- Set minimum and maximum players.
- Validate with `/bw setup info` and save.

Example sequence:

```text
/bw setup create MyArena
/bw setup setlobby
/bw setup setspectator
/bw setup setspawn red
/bw setup setbed red
/bw setup setspawn blue
/bw setup setbed blue
/bw setup setminplayers 2
/bw setup setmaxplayers 8
/bw setup save
```

## Spectator Mode

### How It Works

- Players can spectate only active or ending games.
- Spectators are non-interactive and cannot affect gameplay.
- Use `/bw leave` to exit spectator mode.

### Typical Flow

```text
/bw spectate arena1
... watch game ...
/bw leave
```

## BungeeCord Integration

Enable BungeeCord in `config.yml`:

```yaml
bungeecord:
  enabled: true
  lobby-server: "lobby"
  send-to-lobby-on-end: true
  send-to-lobby-on-leave: false
  lobby-send-delay: 3
```

Highlights:

- Send players to lobby after game end.
- Optional redirect when players leave a match.
- Uses standard BungeeCord plugin messaging channels.

## Database Features

The plugin supports persistent player statistics with SQLite by default and optional MySQL for networks.

### SQLite

```text
plugins/BedWars/bedwars.db
```

### MySQL Example

```yaml
database:
  type: mysql
  host: localhost
  port: 3306
  database: bedwars
  username: bedwars_user
  password: change_me
  pool-size: 10
```

Tracked stats include wins, losses, kills, deaths, final kills, beds broken, and games played.

## Permissions

| Permission | Purpose | Default |
| --- | --- | --- |
| `bedwars.join` | Join games | Everyone |
| `bedwars.leave` | Leave games or spectate mode | Everyone |
| `bedwars.spectate` | Spectate active games | Everyone |
| `bedwars.stats` | View stats | Everyone |
| `bedwars.list` | List arenas | Everyone |
| `bedwars.setup` | Arena setup commands | Operators |
| `bedwars.admin` | Administrative commands | Operators |
| `bedwars.bungee` | Lobby transfer command | Everyone |

## Troubleshooting

### Arena Not Found

- Verify arena name with `/bw list`.
- Confirm the arena was saved successfully.
- Check startup logs for arena load errors.

### Setup Incomplete

- Run `/bw setup info` to view missing fields.
- Ensure at least two teams have both spawn and bed configured.
- Confirm player limits are set.

### Spectate Not Allowed

- Spectating is limited to active or ending games.
- Check game state with `/bw list`.

## Best Practices

### For Builders

- Place spectator spawn above center view.
- Keep team island distances balanced.
- Test route timing between generators and islands.

### For Admins

- Limit `bedwars.setup` to trusted staff.
- Keep regular backups of database and configs.
- Test full join -> play -> end -> reset flow after config changes.

## Summary

Enhanced BedWars provides:

- Structured command system with setup workflow.
- Spectator mode for non-interactive viewing.
- BungeeCord-aware lobby routing.
- Persistent statistics with SQLite/MySQL support.
- Clear permission boundaries for staff and players.
