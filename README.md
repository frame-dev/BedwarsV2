# BedWars Plugin

A comprehensive BedWars plugin for Minecraft (Spigot/Paper 1.20.4+) with all essential features.

## Features

### Core Gameplay

- **Multiple Game States**: Waiting, Starting, Running, Ending
- **Team System**: Support for up to 8 teams (Red, Blue, Green, Yellow, Aqua, White, Pink, Gray)
- **Bed Mechanics**: Teams can respawn while their bed is alive
- **Resource Generators**: Automatic iron, gold, diamond, and emerald generators
- **Respawn System**: 5-second respawn timer when bed is alive

### Shop System

- **Item Shop**: Buy blocks, weapons, armor, tools, food, potions, and special items
- **Multiple Categories**:
  - Blocks (wool, terracotta, end stone, obsidian)
  - Weapons (swords, knockback stick)
  - Armor (chainmail, iron, diamond)
  - Tools (pickaxes, axes, shears)
  - Food (apples, steak, golden apples)
  - Potions (speed, jump, invisibility)
  - Special items (TNT, ender pearls, fire charges, ladders)

### Upgrade System

- **Team Upgrades**:
  - Sharpened Swords (Sharpness I)
  - Reinforced Armor (Protection I-IV)
  - Maniac Miner (Haste I-II)
  - Heal Pool (Regeneration field near base)
  - Dragon Buff (Protective dragons at base)

### Player Features

- **Statistics Tracking**:
  - Kills and deaths
  - Final kills
  - Beds broken
  - Team wins/losses
  - Ordered leaderboards for wins, kills, and beds broken
- **Party System**: Invite friends, manage party leadership, and auto-join arenas together
- **Map Voting Queue**: Queue up, vote via GUI, and join the winning map automatically
- **Join Signs**: Clickable arena signs with live state and player count
- **Cosmetics System**: Kill effects and bed destroy effects with a selection GUI
- **Achievements System**: Progress-based achievements with a GUI
- **Colored Armor**: Leather armor automatically colored to team color
- **Starting Items**: Configurable starting item (default wooden sword)

### Game Management

- **Arena System**: Multiple arena support with configuration
- **Countdown System**: 30-second countdown before game start
- **Win Conditions**: Last team standing wins
- **Automatic Cleanup**: Inventories cleared, visibility restored, and players removed from the game when it ends
- **Safe Explosions**: TNT and other explosions do not destroy BedWars arena blocks

## Commands

| Command | Description | Permission |
| ------- | ----------- | ---------- |
| `/bedwars join <arena>` | Join a BedWars game | bedwars.join |
| `/bedwars leave` | Leave your current game | bedwars.leave |
| `/bedwars spectate <arena>` | Spectate a game | bedwars.spectate |
| `/bedwars stats` | View your statistics | bedwars.stats |
| `/bedwars list` | List all available arenas | bedwars.list |
| `/bedwars setup` | Arena setup commands | bedwars.setup |
| `/bedwars party` | Party commands | bedwars.party |
| `/bedwars queue` | Join map voting queue | bedwars.queue |
| `/bedwars vote` | Open map voting GUI | bedwars.vote |
| `/bedwars vote <start\|end\|force> [arena]` | Admin map voting controls | bedwars.vote.admin |
| `/bedwars cosmetics` | Cosmetics menu | bedwars.cosmetics |
| `/bedwars achievements` | Achievements menu | bedwars.achievements |
| `/bedwars team [color]` | Open team selector or choose a team | bedwars.team |

## Join Signs

Players can join arenas by right-clicking a BedWars join sign. Sign joins remember the player's click location and return them there after the game ends or when they leave, instead of keeping them in the arena lobby.

Create a join sign by placing a sign with:

```text
[BedWars]
arenaName
```

`[BW]` also works as the first line. After creation, the sign updates to:

```text
[BedWars]
arenaName
WAITING
online/maxplayers
```

The third line automatically shows `WAITING`, `STARTING`, `RUNNING`, `ENDING`, or `FULL`. Join signs are persisted in `plugins/BedWars/joinsigns.yml` and refreshed when players join, leave, countdowns start/cancel, games start, games end, and arenas reset.

| Permission | Description | Default |
| ---------- | ----------- | ------- |
| `bedwars.join` | Right-click join signs and use `/bedwars join` | Everyone |
| `bedwars.sign.create` | Create BedWars join signs | Operators |

## Setup Commands

| Command | Description |
| ------- | ----------- |
| `/bedwars setup create <name>` | Create a new arena |
| `/bedwars setup setlobby` | Set lobby spawn point |
| `/bedwars setup setspectator` | Set spectator spawn point |
| `/bedwars setup setspawn <team>` | Set team spawn point |
| `/bedwars setup setbed <team>` | Register the complete bed you are looking at (within 5 blocks) |
| `/bedwars setup setshop <team> <item\|upgrade>` | Set team shop villager location |
| `/bedwars setup addgenerator <name>` | Add a generator location |
| `/bedwars setup setminplayers <count>` | Set minimum players |
| `/bedwars setup setmaxplayers <count>` | Set maximum players |
| `/bedwars setup info` | Show setup progress |
| `/bedwars setup save` | Save the arena |

## Installation

1. Build the plugin using Maven:

```bash
mvn clean package
```

1. Copy the generated JAR file from `target/bedwars-1.2-SNAPSHOT.jar` (or your current snapshot version) to your server's `plugins` folder.
1. Start or restart your server.
1. Configure arenas in `plugins/BedWars/arenas.yml`.

See full documentation in [docs](docs/).

## Configuration Files

The plugin uses multiple YAML configuration files for customization.

### config.yml

- Game settings (countdown time, respawn time, generator upgrades)
- Starting item and block/combat toggles
- BungeeCord integration settings
- Network mode selection (local, bungee, cloudnet)
- Player limits and team colors
- Debug logging toggles (enable/verbose and generator spawn tracing)
- Party settings (max size, invite expiry)
- Map voting settings (queue size, vote duration, GUI title)
- CloudNet settings (service mapping, lobby service, map voting)

### arenas.yml

- Arena definitions (lobby spawn, team spawns, beds, generators)

### joinsigns.yml

- Automatically generated join sign registry
- Stores sign locations and arena names
- Used to refresh sign state and `online/maxplayers` display after reloads

### messages.yml

- 100+ configurable messages organized by category
- Support for color codes (`&a`, `&c`, `&6`, etc.)
- Placeholders for dynamic values (`{0}`, `{1}`, etc.)
- Categories: game, death, shop, commands, stats, leaderboard, and more

### shop.yml

- 7 shop categories with 30+ items
- Fully customizable items (material, cost, amount, display name)
- Potions and knockback stick are applied by display name
- Add or remove items without touching code
- Categories: Blocks, Weapons, Armor, Tools, Food, Potions, Special

### upgrades.yml

- Fully dynamic upgrade system
- Add new upgrades without code changes
- Three effect types: ENCHANTMENT, POTION_EFFECT, SPECIAL
- Configurable costs, levels, and effects
- See [docs/UPGRADE_EXAMPLES.md](docs/UPGRADE_EXAMPLES.md) for examples

### cosmetics.yml

- Cosmetic effect definitions for kill and bed destroy effects
- GUI layout and cosmetics access permissions

### achievements.yml

- Achievement definitions, GUI layout, progress text, locked/unlocked text, icon, target, and tracked stat type

Statistics and achievements are stored in `plugins/BedWars/bedwars.db`. Player statistics are cached while online and saved on quit, game end, and plugin shutdown. Achievement progress is merged safely with database progress, so fast joins, reloads, or early in-game events do not overwrite newly earned progress.

Optional runtime settings in `config.yml`:

```yaml
network:
  mode: "local"

debug:
  enabled: false
  verbose-logging: false
  show-generator-spawns: false

party:
  max-size: 4
  invite-expire-seconds: 60

map-voting:
  enabled: true
  queue-min-players: 2
  vote-duration-seconds: 20
  allow-revote: true
  gui-title: "&aMap Voting"
  gui-size: 27
  gui-item: "MAP"
  gui-slot-start: 10
  gui-slot-step: 1
  gui-lore:
    - "&7Votes: &e{votes}"
    - "&7Queued: &e{queued}"
    - "&7Click to vote"

cloudnet:
  enabled: false
  one-arena-per-server: true
  use-for-map-voting: true
  lobby-service: "Lobby-1"
  arena-service-prefix: ""
  game-services:
    - "bw-1"
    - "bw-2"
  arena-service-map:
    example: "bw-1"

upgrades:
  heal-pool:
    radius: 6.0
    amplifier: 0
    duration-ticks: 60
  dragon-buff:
    count: 2
```

Example arena configuration in `arenas.yml`:

```yaml
arenas:
  example:
    lobby-spawn: world,0.0,100.0,0.0,0.0,0.0
    min-players: 2
    max-players: 8
    teams:
      red:
        spawn: world,50.0,64.0,0.0,0.0,0.0
        bed: world,55.0,64.0,0.0,0.0,0.0
      blue:
        spawn: world,-50.0,64.0,0.0,0.0,0.0
        bed: world,-55.0,64.0,0.0,0.0,0.0
```

## Requirements

- Spigot or Paper 1.20.4+
- Java 17+
- Maven 3.6+ (for building)

## Project Structure

```text
src/main/java/ch/framedev/
├── BedWarsPlugin.java          # Main plugin class
├── bedwars/
│   ├── commands/               # Command handlers
│   ├── game/                   # Game logic and management
│   ├── generators/             # Resource generators
│   ├── listeners/              # Event listeners
│   ├── player/                 # Player data management
│   ├── signs/                  # Join sign persistence and display updates
│   ├── shop/                   # Shop system
│   ├── stats/                  # Player stats and leaderboards
│   ├── achievements/           # Achievement definitions, progress, and GUI
│   ├── team/                   # Team management
│   └── upgrades/               # Upgrade system
```

## Features in Detail

### Resource Generators

- **Iron**: Spawns every 1 second at team base
- **Gold**: Spawns every 8 seconds at team base
- **Diamond**: Spawns every 30s (Tier 1) or 20s (Tier 2) at map points
- **Emerald**: Spawns every 60s (Tier 1) or 40s (Tier 2) at map points

### Game Flow

1. Players join lobby.
2. Players can join with `/bw join <arena>` or by right-clicking a join sign.
3. Join signs update with the current state and online/max player count.
4. Minimum players reached and countdown starts.
5. Game starts and players are teleported to team spawns.
6. Resource generators activate.
7. Players gather resources, buy items, and destroy beds.
8. Last team standing wins.
9. Players are removed from the game, made visible again, and cleaned up.
10. Sign-joined players return to the place where they clicked the sign; command-joined players use the configured lobby behavior.
11. Game resets for next round.

## Development

1. Clone the repository.
2. Open in your IDE (IntelliJ IDEA recommended).
3. Make changes.
4. Build with `mvn clean package`.
5. Test on your development server.

Documentation:

- [Quick Start Guide](docs/QUICK_START.md)
- [Full Setup Guide](docs/FULL_SETUP.md)
- [Configuration Examples](docs/UPGRADE_EXAMPLES.md)
- [Database Setup](docs/DATABASE_GUIDE.md)
- [BungeeCord Integration](docs/BUNGEECORD_SETUP.md)
- More in [docs](docs/)

## Recent Updates

- Added persistent join signs with live game state and online/max player counts.
- Sign-joined players now return to their original click location after leaving or after the game ends.
- Game-end cleanup now removes players from the active game mapping, so `/bw leave` is not needed after a match.
- Fixed arena team loading from `arenas.yml` so saved spawns, beds, and shops load correctly.
- Added configured void-death handling and improved BedWars respawn flow after deaths.
- Hardened the shop GUI to use one shared shop manager, avoid duplicate categories, preserve configured prices, and prevent purchased items from disappearing when inventories are full.
- Improved world reset behavior for destroyed beds by recording and restoring both bed halves.
- Hardened explosion behavior so TNT and other explosions do not modify BedWars arena blocks.
- Fixed shop reliability by making category and upgrade slot mapping deterministic.
- Strengthened listener safety (notably chat routing) to avoid async access to mutable game state.
- Improved end-game stat consistency for disconnecting and offline participants.
- Hardened statistics saves so quit-time cache clearing cannot drop pending stat updates or overwrite names as `Unknown`.
- Improved achievement progress loading, saving, GUI ordering, and display clamping.
- Improved runtime performance by reducing unchanged scoreboard updates, indexing join signs and shop categories, lowering generator scan work, and tuning SQLite for concurrent plugin activity.

## License

This project is provided as-is for educational purposes.

## Support

For issues or feature requests, create an issue in the project repository.

## Credits

Developed by FrameDev.
