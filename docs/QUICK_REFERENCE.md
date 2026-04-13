# BedWars Quick Reference

## Installation

```bash
mvn clean package
cp target/bedwars-1.2-SNAPSHOT.jar server/plugins/
```

## Basic Arena Config Example

```yaml
arenas:
  myarena:
    lobby-spawn: world,0.0,100.0,0.0,0.0,0.0
    min-players: 2
    max-players: 8
    teams:
      red:
        spawn: world,50.0,64.0,0.0,0.0,0.0
        bed: world,55.0,64.0,0.0,0.0,0.0
```

## Player Commands

| Command | Description |
| --- | --- |
| `/bw join <arena>` | Join a game |
| `/bw leave` | Leave current game |
| `/bw spectate <arena>` | Spectate a game |
| `/bw stats [player]` | View statistics |
| `/bw list` | List arenas |

## Setup and Admin Commands

| Command | Description |
| --- | --- |
| `/bw setup create <name>` | Create arena |
| `/bw setup setlobby` | Set lobby spawn |
| `/bw setup setspectator` | Set spectator spawn |
| `/bw setup setspawn <team>` | Set team spawn |
| `/bw setup setbed <team>` | Set team bed |
| `/bw setup addgenerator <id>` | Add generator |
| `/bw setup save` | Save arena |
| `/bw setup info` | Show setup progress |
| `/bw start` | Force start match |
| `/bw stop` | Force stop match |
| `/bw reload` | Reload plugin configuration |

## Shop Categories

1. Blocks
2. Weapons
3. Armor
4. Tools
5. Food
6. Potions
7. Special

## Team Upgrades

1. Sharpened Swords
2. Reinforced Armor
3. Maniac Miner
4. Heal Pool
5. Dragon Buff

## Resource Timing

- Iron: about every 1 second.
- Gold: about every 8 seconds.
- Diamond: base interval around 30 seconds.
- Emerald: base interval around 60 seconds.

## Game Flow

```text
WAITING -> STARTING -> RUNNING -> ENDING -> WAITING
```

## Win Condition

- Destroy enemy beds and eliminate all enemy players.
- Last surviving team wins.

## Statistics Tracked

- Wins and losses
- Kills and deaths
- Final kills
- Beds broken
- Games played

## Team Colors

- Red
- Blue
- Green
- Yellow
- Aqua
- White
- Pink
- Gray

## Requirements

- Spigot or Paper 1.20.4+
- Java 17+
- Maven 3.6+ (for build only)

## Key Paths

```text
src/main/java/
src/main/resources/
docs/
pom.xml
```

## Documentation

- [README.md](../README.md)
- [QUICK_START.md](QUICK_START.md)
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)
- [API.md](API.md)

## Version

- Plugin: 1.2-SNAPSHOT
