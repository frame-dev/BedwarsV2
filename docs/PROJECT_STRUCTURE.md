# BedWars Project Structure

## Repository Layout

```text
bedwars/
├── pom.xml
├── README.md
├── docs/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
└── target/
```

## Java Package Structure

```text
src/main/java/ch/framedev/
├── BedWarsPlugin.java
└── bedwars/
    ├── achievements/
    ├── arena/
    ├── bungee/
    ├── cloudnet/
    ├── commands/
    ├── cosmetics/
    ├── database/
    ├── game/
    ├── generators/
    ├── listeners/
    ├── manager/
    ├── party/
    ├── player/
    ├── shop/
    ├── stats/
    ├── team/
    ├── upgrades/
    ├── utils/
    └── voting/
```

## Core Modules

| Module | Purpose |
| --- | --- |
| `BedWarsPlugin` | Main plugin entry point and lifecycle wiring |
| `game` | Match lifecycle, state transitions, and win logic |
| `arena` | Arena registration and setup support |
| `team` | Team metadata and team-level state |
| `player` | Player game wrapper and runtime state |
| `listeners` | Bukkit event handling and gameplay rules |
| `shop` | Item shop UI and purchase flow |
| `upgrades` | Team upgrade definitions and application |
| `stats` | Statistics tracking and persistence integration |
| `database` | Database-related access and storage helpers |
| `commands` | Command routing and tab completion |
| `utils` | Shared helper utilities |

## Runtime Resources

```text
src/main/resources/
├── plugin.yml
├── config.yml
├── arenas.yml
├── messages.yml
├── shop.yml
├── upgrades.yml
├── achievements.yml
└── cosmetics.yml
```

## Architecture Snapshot

```text
BedWarsPlugin
├── managers (arena/game/stats/other services)
├── commands and completers
└── listeners

Game instance
├── teams
├── players
├── generators
└── world reset and end flow
```

## Build and Output

```bash
mvn clean package
```

Primary artifact:

```text
target/bedwars-1.2-SNAPSHOT.jar
```

## Quality Notes

- Event listeners enforce most gameplay constraints.
- Configuration files drive behavior and messages.
- Match logic is isolated under the `game` package.
- Shops, upgrades, and stats are modular for easier extension.
