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
- **Colored Armor**: Leather armor automatically colored to team color
- **Starting Items**: Configurable starting item (default wooden sword)

### Game Management
- **Arena System**: Multiple arena support with configuration
- **Countdown System**: 30-second countdown before game start
- **Win Conditions**: Last team standing wins
- **Automatic Cleanup**: Players teleported back to lobby, inventories cleared

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/bedwars join <arena>` | Join a BedWars game | bedwars.join |
| `/bedwars leave` | Leave your current game | bedwars.leave |
| `/bedwars spectate <arena>` | Spectate a game | bedwars.spectate |
| `/bedwars stats` | View your statistics | bedwars.stats |
| `/bedwars list` | List all available arenas | bedwars.list |
| `/bedwars setup` | Arena setup commands | bedwars.setup |

## Setup Commands

| Command | Description |
|---------|-------------|
| `/bedwars setup create <name>` | Create a new arena |
| `/bedwars setup setlobby <arena>` | Set lobby spawn point |
| `/bedwars setup setspectator <arena>` | Set spectator spawn point |
| `/bedwars setup setspawn <arena> <team>` | Set team spawn point |
| `/bedwars setup setbed <arena> <team>` | Set bed location |
| `/bedwars setup addgenerator <name>` | Add a generator location |

## Installation

1. Build the plugin using Maven:
   ```bash
   mvn clean package
   ```

2. Copy the generated JAR file from `target/bedwars-1.0-SNAPSHOT.jar` to your server's `plugins` folder

3. Start or restart your server

4. Configure arenas in `plugins/BedWars/config.yml`

📚 **[Full documentation available in /docs folder](docs/)**

## Configuration Files

The plugin uses multiple YAML configuration files for easy customization:

### config.yml
- Game settings (countdown time, respawn time, generator upgrades)
- Starting item and block/combat toggles
- BungeeCord integration settings
- Arena definitions (lobby spawn, team spawns, beds, generators)
- Player limits and team colors

### messages.yml
- **100+ configurable messages** organized by category
- Support for color codes (`&a`, `&c`, `&6`, etc.)
- Placeholders for dynamic values (`{0}`, `{1}`, etc.)
- Categories: game, death, shop, commands, stats, leaderboard, and more

### shop.yml
- **7 shop categories** with 30+ items
- Fully customizable items (material, cost, amount, display name)
- Potions and knockback stick are applied by display name
- Add/remove items without touching code
- Categories: Blocks, Weapons, Armor, Tools, Food, Potions, Special

### upgrades.yml
- **Fully dynamic upgrade system**
- Add new upgrades without code changes
- Three effect types: ENCHANTMENT, POTION_EFFECT, SPECIAL
- Configurable costs, levels, and effects
- See [docs/UPGRADE_EXAMPLES.md](docs/UPGRADE_EXAMPLES.md) for examples

Optional runtime settings in config.yml:
```yaml
upgrades:
  heal-pool:
    radius: 6.0
    amplifier: 0
    duration-ticks: 60
  dragon-buff:
    count: 2
```

Example arena configuration in `config.yml`:
```yaml
arenas:
  example:
    lobby-spawn:
      world: world
      x: 0
      y: 100
      z: 0
    min-players: 2
    max-players: 8
    teams:
      red:
        spawn: {...}
        bed: {...}
      blue:
        spawn: {...}
        bed: {...}
```

## Requirements

- Spigot or Paper 1.20.4+
- Java 17+
- Maven 3.6+ (for building)

## Project Structure

```
src/main/java/ch/framedev/
├── BedWarsPlugin.java          # Main plugin class
├── bedwars/
│   ├── commands/               # Command handlers
│   ├── game/                   # Game logic and management
│   ├── generators/             # Resource generators
│   ├── listeners/              # Event listeners
│   ├── player/                 # Player data management
│   ├── shop/                   # Shop system
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
1. Players join lobby
2. Minimum players reached → countdown starts
3. Game starts → players teleported to team spawns
4. Resource generators activated
5. Players gather resources, buy items, destroy beds
6. Last team standing wins
7. Players teleported back to lobby
8. Game resets for next round

## Development

To extend or modify the plugin:

1. Clone the repository
2. Open in your favorite IDE (IntelliJ IDEA recommended)
3. Make changes
4. Build with `mvn clean package`
5. Test on your development server

📚 **Documentation**: See [docs/](docs/) folder for:
- [Quick Start Guide](docs/QUICK_START.md)
- [Configuration Examples](docs/UPGRADE_EXAMPLES.md)
- [Database Setup](docs/DATABASE_GUIDE.md)
- [BungeeCord Integration](docs/BUNGEECORD_SETUP.md)
- And more...

## Recent Updates

The following files were updated to align gameplay, upgrades, and shop behavior with the docs:

- [src/main/java/ch/framedev/BedWarsPlugin.java](src/main/java/ch/framedev/BedWarsPlugin.java)
- [src/main/java/ch/framedev/bedwars/commands/ImprovedBedWarsCommand.java](src/main/java/ch/framedev/bedwars/commands/ImprovedBedWarsCommand.java)
- [src/main/java/ch/framedev/bedwars/game/Game.java](src/main/java/ch/framedev/bedwars/game/Game.java)
- [src/main/java/ch/framedev/bedwars/generators/ResourceGenerator.java](src/main/java/ch/framedev/bedwars/generators/ResourceGenerator.java)
- [src/main/java/ch/framedev/bedwars/listeners/BlockBreakListener.java](src/main/java/ch/framedev/bedwars/listeners/BlockBreakListener.java)
- [src/main/java/ch/framedev/bedwars/listeners/BlockPlaceListener.java](src/main/java/ch/framedev/bedwars/listeners/BlockPlaceListener.java)
- [src/main/java/ch/framedev/bedwars/listeners/EntityDamageListener.java](src/main/java/ch/framedev/bedwars/listeners/EntityDamageListener.java)
- [src/main/java/ch/framedev/bedwars/listeners/InventoryClickListener.java](src/main/java/ch/framedev/bedwars/listeners/InventoryClickListener.java)
- [src/main/java/ch/framedev/bedwars/listeners/ItemPickupListener.java](src/main/java/ch/framedev/bedwars/listeners/ItemPickupListener.java)
- [src/main/java/ch/framedev/bedwars/listeners/PlayerInteractListener.java](src/main/java/ch/framedev/bedwars/listeners/PlayerInteractListener.java)
- [src/main/java/ch/framedev/bedwars/listeners/PlayerQuitListener.java](src/main/java/ch/framedev/bedwars/listeners/PlayerQuitListener.java)
- [src/main/java/ch/framedev/bedwars/manager/UpgradeManager.java](src/main/java/ch/framedev/bedwars/manager/UpgradeManager.java)
- [src/main/java/ch/framedev/bedwars/shop/ShopGUI.java](src/main/java/ch/framedev/bedwars/shop/ShopGUI.java)
- [src/main/java/ch/framedev/bedwars/shop/ShopManager.java](src/main/java/ch/framedev/bedwars/shop/ShopManager.java)
- [src/main/java/ch/framedev/bedwars/stats/PlayerStats.java](src/main/java/ch/framedev/bedwars/stats/PlayerStats.java)
- [src/main/java/ch/framedev/bedwars/upgrades/UpgradeShopGUI.java](src/main/java/ch/framedev/bedwars/upgrades/UpgradeShopGUI.java)
- [src/main/resources/messages.yml](src/main/resources/messages.yml)
- [src/main/resources/plugin.yml](src/main/resources/plugin.yml)

## License

This project is provided as-is for educational purposes.

## Support

For issues or feature requests, please create an issue on the project repository.

## Credits

Developed by FrameDev
