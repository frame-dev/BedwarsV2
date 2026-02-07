# BedWars Plugin - Quick Start Guide

## 🚀 Getting Started

### Prerequisites
- Minecraft Server (Spigot/Paper 1.20.4 or higher)
- Java 17 or higher
- Maven 3.6+ (for building from source)

### Installation

#### Option 1: Build from Source
```bash
# Clone or navigate to the project directory
cd bedwars

# Build the plugin
mvn clean package

# The compiled plugin will be in target/bedwars-1.0-SNAPSHOT.jar
```

#### Option 2: Use Pre-built JAR
1. Download the latest release JAR
2. Place it in your server's `plugins/` folder

### First Time Setup

1. **Start your server** to generate the plugin files:
   ```
   plugins/BedWars/
   ├── config.yml      # Main configuration
   ├── arenas.yml      # Arena definitions
   ├── messages.yml    # Configurable messages
   ├── shop.yml        # Shop items
   ├── upgrades.yml    # Team upgrades
   └── bedwars.db      # SQLite database (auto-created)
   ```

2. **Stop the server** and configure your first arena

   **Note**: The plugin now includes multiple configuration files:
    - `config.yml` - Main settings
    - `arenas.yml` - Arenas, teams, and generators
   - `messages.yml` - All plugin messages (100+ configurable)
   - `shop.yml` - Shop items and prices (fully customizable)
   - `upgrades.yml` - Team upgrades (add new upgrades without code!)

3. **Edit arenas.yml** to add an arena (or use `/bw setup` commands):
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
             z: 0
   ```

4. **Start your server** again

### In-Game Setup with Commands (Recommended)

The easiest way to set up an arena is using in-game commands:

#### Step 1: Create New Arena
```bash
/bw setup create myarena
# Creates a new arena with default settings
```

#### Step 2: Set Lobby Spawn
This is where players wait before the game starts.
```bash
# Stand in the area where you want players to spawn
/bw setup setlobby
```
**Tip**: Choose a central location safe from PvP

#### Step 3: Set Spectator Spawn
This is where players who die can watch.
```bash
# Stand high above the arena for good visibility
/bw setup setspectator
```
**Tip**: Place this on a high platform for overview

#### Step 4: Configure Teams
For each team color you want (minimum 2):

```bash
# Team 1: RED
# Stand at the RED team spawn location
/bw setup setspawn red

# Stand at the RED team bed location
/bw setup setbed red

# Team 2: BLUE
# Stand at the BLUE team spawn location
/bw setup setspawn blue

# Stand at the BLUE team bed location
/bw setup setbed blue
```

**Available Teams**: RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY

#### Step 5: Add Resource Generators (Optional)
```bash
# Stand at generator location and add it
/bw setup addgenerator iron1    # Iron generator
/bw setup addgenerator gold1    # Gold generator
/bw setup addgenerator diamond1 # Diamond generator
/bw setup addgenerator emerald1 # Emerald generator
```

#### Step 6: Set Player Limits
```bash
/bw setup setminplayers 2
/bw setup setmaxplayers 8
```

#### Step 7: View Setup Progress
```bash
/bw setup info
# Shows all configured settings
```

#### Step 8: Save Arena
```bash
/bw setup save
# Saves arena configuration to arenas.yml
```

### Complete Setup Example

Here's a full example of setting up a 2v2 arena:

```bash
# 1. Create arena
/bw setup create tutorial

# 2. Set lobby (safe central location)
# Stand at (0, 100, 0)
/bw setup setlobby

# 3. Set spectator (high platform)
# Stand at (0, 150, 0)
/bw setup setspectator

# 4. Red Team Setup
# Stand at red base (50, 64, 50)
/bw setup setspawn red
# Stand at red bed (55, 64, 55)
/bw setup setbed red

# 5. Blue Team Setup
# Stand at blue base (-50, 64, -50)
/bw setup setspawn blue
# Stand at blue bed (-55, 64, -55)
/bw setup setbed blue

# 6. Add generators
# Iron generator (center)
# Stand at (0, 65, 0)
/bw setup addgenerator iron1

# Gold generator (center, higher)
# Stand at (0, 70, 0)
/bw setup addgenerator gold1

# 7. Set player limits
/bw setup setminplayers 2
/bw setup setmaxplayers 4

# 8. Save everything
/bw setup save
```

### Testing Your Arena

1. **Start the game**:
   ```
   /bw join tutorial
   ```

2. **Test with minimum players** (2 if that's your setting):
   - Game should start with 30-second countdown
   - Players should be placed on different teams
   - Colored armor should apply automatically

3. **Verify all features**:
   - ✓ Players spawn at correct locations
   - ✓ Colored armor matches team
   - ✓ Resource generators drop items
   - ✓ Shop works (interact with villagers)
   - ✓ Can break enemy bed
   - ✓ Respawn works (if bed alive)
   - ✓ Game ends when team eliminated
   - ✓ Statistics are saved

4. **If something is wrong**:
   ```
   /bw setup info  # Check current settings
   /bw setup cancel  # Start over
   ```

## 🎮 Playing Your First Game

### Joining a Game

```bash
# List all available arenas
/bw list

# Join a specific arena
/bw join tutorial

# You'll spawn in the lobby and wait for other players
```

### Game Phases

#### 1. **Lobby Phase** (Everyone waiting)
- Players spawn in lobby area
- Waiting for minimum players
- Chat and prepare
- No combat or building

#### 2. **Starting Phase** (30 second countdown)
```
Game starting in 30 seconds...
Game starting in 10 seconds...
5... 4... 3... 2... 1...
GAME STARTED!
```
- Countdown appears in chat
- Can't cancel if players stay
- Get ready to spawn

#### 3. **Active Game** (The real game!)
- **You spawn** at your team's base with colored armor
- **Get resources**: Collect iron from generators (1 per second)
- **Build defenses**: Use wool and blocks to protect your bed
- **Collect better items**: Trade iron for wood/stone tools at shop
- **Upgrade team**: Buy team upgrades (Sharpness, Protection, etc.)
- **Explore map**: Get gold and diamonds from center generators
- **Break enemy beds**: Destroy other team's beds with pickaxe
- **Eliminate enemies**: Final kill when their bed is destroyed
- **Win**: Last team standing wins the game!

#### 4. **End Game** (Winner announced)
- Winner team gets message: `TEAM RED WINS!`
- Stats are saved to database
- Players teleported to lobby
- Arena resets for next game

### Game Mechanics Overview

#### Resources & Economy

| Resource | Spawn Rate | Uses |
|----------|-----------|------|
| **Iron** | Every 1 second | Tools, armor, cheap items |
| **Gold** | Every 8 seconds | Better gear |
| **Diamond** | Every 30 seconds | Powerful upgrades |
| **Emerald** | Every 60 seconds | Luxury items |

#### Beds & Respawning

- **Bed Alive**: You respawn after 5 seconds when killed
- **Bed Destroyed**: You have NO respawn (final death next time)
- **Protect Your Bed**: Use blocks and weapons to defend it
- **Break Beds**: Use a pickaxe on enemy beds to destroy them

#### Shop Items

Press **[SHIFT + RIGHT CLICK]** on the shop keeper to buy:
- Blocks (wool, obsidian for defending)
- Weapons (swords for fighting)
- Armor (iron, diamond for survival)
- Tools (pickaxes for mining)
- Food (steak for healing)
- Potions (speed, jump, invisibility)
- Special (TNT, ender pearls)

#### Upgrades

Get team upgrades from the upgrade shop:
- **Sharpened Swords** (8 diamonds) - Sharpness I on all swords
- **Reinforced Armor** (2-16 diamonds) - Protection on armor
- **Maniac Miner** (2-4 diamonds) - Haste for faster mining
- **Heal Pool** (3 diamonds) - Regeneration for whole team
- **Dragon Buff** (5 emeralds) - Base protection

### Strategy Tips

#### Early Game (First 3 minutes)
1. Collect iron from your base generator
2. Buy a pickaxe (10 iron)
3. Build walls around your bed
4. Team stay together
5. Watch for rushes from enemy teams

#### Mid Game (3-10 minutes)
1. Expand territory
2. Contest gold and diamond generators
3. Buy better armor and weapons
4. Get first team upgrades (Protection is priority!)
5. Scout enemy bases

#### Late Game (10+ minutes)
1. Secure diamonds and emeralds
2. Get all team upgrades
3. Focus fire on weak teams
4. Bed rush! Attack when enemies scattered
5. Final fight for victory

### Winning the Game

To win, you must be the **last team standing**. This means:
- ✓ All enemy beds destroyed AND all players dead, OR
- ✓ All other teams eliminated

**Keys to Victory:**
1. **Protect your bed** - This is your lifeline
2. **Get upgrades** - Armor upgrades = huge power boost
3. **Control resources** - Diamonds and gold matter
4. **Team coordination** - Never fight alone
5. **Deny enemies** - Kill them before they get good gear

### Leaving a Game

```bash
# Leave current game
/bw leave

# You'll be teleported back to lobby
```

### Viewing Statistics

```bash
# View your stats
/bw stats

# View another player's stats
/bw stats PlayerName

# View leaderboards
/bw top wins    # Most wins
/bw top kills   # Most kills
/bw top beds    # Most beds broken
```

| Command | Description | Permission |
|---------|-------------|------------|
| `/bedwars join <arena>` | Join a game | `bedwars.join` (default: true) |
| `/bedwars leave` | Leave current game | `bedwars.leave` (default: true) |
| `/bedwars spectate <arena>` | Spectate a game | `bedwars.spectate` (default: true) |
| `/bedwars stats` | View your statistics | `bedwars.stats` (default: true) |
| `/bedwars list` | List all arenas | `bedwars.list` (default: true) |
| `/bw` | Short alias for /bedwars | Same as above |

## 🛠️ Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/bedwars setup create <name>` | Create new arena | `bedwars.setup` (default: op) |
| `/bedwars setup setlobby <arena>` | Set lobby spawn | `bedwars.setup` (default: op) |
| `/bedwars setup setspectator <arena>` | Set spectator spawn | `bedwars.setup` (default: op) |
| `/bedwars setup setspawn <arena> <team>` | Set team spawn | `bedwars.setup` (default: op) |
| `/bedwars setup setbed <arena> <team>` | Set bed location | `bedwars.setup` (default: op) |
| `/bedwars setup addgenerator <name>` | Add generator | `bedwars.setup` (default: op) |

## 🎯 Game Features

### Resource Generators
- **Iron**: Spawns every 1 second at team base
- **Gold**: Spawns every 8 seconds at team base
- **Diamond**: Spawns at map locations (configurable)
- **Emerald**: Spawns at map locations (configurable)

### Shop Categories
1. **Blocks** - Building materials (wool, terracotta, obsidian)
2. **Weapons** - Swords and special weapons
3. **Armor** - Chainmail, iron, diamond armor
4. **Tools** - Pickaxes, axes, shears
5. **Food** - Apples, steak, golden apples
6. **Potions** - Speed, jump, invisibility
7. **Special** - TNT, ender pearls, fire charges

### Team Upgrades
- **Sharpened Swords** - All team members get Sharpness I
- **Reinforced Armor** - Protection I-IV for all armor
- **Maniac Miner** - Permanent Haste I-II
- **Heal Pool** - Regeneration field at base
- **Dragon Buff** - Protective dragons at base

## 📊 Game Flow

1. **Lobby Phase** (WAITING)
   - Players join and are auto-balanced into teams
   - Chat and wait for more players
   - No PvP or block breaking

2. **Countdown Phase** (STARTING)
   - Minimum players reached
   - 30-second countdown
   - Can be cancelled if players leave

3. **Active Game** (RUNNING)
   - Players spawn at team bases with colored armor
   - Resource generators active
   - Gather resources and buy items
   - Destroy enemy beds
   - Eliminate other teams

4. **End Game** (ENDING)
   - Winner announced
   - Statistics saved
   - Players teleported to lobby
   - Game resets automatically

## 🎨 Customization

### Modify Game Settings
Edit `config.yml`:
```yaml
game:
  countdown-time: 30        # Seconds before game starts
  respawn-time: 5          # Seconds to respawn
  diamond-upgrade-time: 720  # When diamond generators upgrade
  emerald-upgrade-time: 1440 # When emerald generators upgrade
```

### Add More Teams
Supported colors: RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY

```yaml
teams:
  green:
      spawn: world,10.0,64.0,10.0,0.0,0.0
      bed: world,15.0,64.0,10.0,0.0,0.0
  yellow:
      spawn: world,-10.0,64.0,-10.0,0.0,0.0
      bed: world,-15.0,64.0,-10.0,0.0,0.0
```

### Customize Messages
Edit messages in `messages.yml`:
```yaml
messages:
  game-starting: "&aThe game is starting!"
  bed-destroyed: "&c&lBED DESTROYED!"
  victory: "&6&l{TEAM} TEAM WINS!"
```

## 🐛 Troubleshooting

### Plugin Not Loading
- Check server console for errors
- Verify Java 17+ is installed
- Ensure Spigot/Paper 1.20.4+

### Arena Not Working
- Verify all locations are set in arenas.yml
- Check world names match exactly
- Ensure minimum 2 teams configured

### Shop Not Opening
- Verify villagers are spawned at team bases
- Farmer villager = Item shop
- Librarian villager = Upgrades shop
- Check game is in RUNNING state

### Resources Not Generating
- Verify generators are defined in arenas.yml
- Check game state is RUNNING
- Resource drops may be picked up instantly

## 📝 Tips for Best Experience

1. **Build a Proper Map**
   - Separate team islands
   - Diamond/emerald generators in between
   - Protected lobby area
   - Clear bed locations

2. **Place Shop NPCs**
   - Put one Farmer villager at each team base
   - Put one Librarian villager at each team base
   - Make them immobile (Name them, set AI to false)

3. **Balance Teams**
   - Even number of team spawns
   - Equal distance from center
   - Similar island sizes

4. **Test Thoroughly**
   - Test with 2 players (minimum)
   - Test with full lobby (maximum)
   - Test all shop purchases
   - Test bed breaking
   - Test win conditions

## 🔗 Useful Links

- [Spigot API Documentation](https://hub.spigotmc.org/javadocs/spigot/)
- [Paper Documentation](https://docs.papermc.io/)
- [Maven Documentation](https://maven.apache.org/)

## 💡 Next Steps

1. Set up your first arena
2. Test with friends
3. Customize messages and settings
4. Add more arenas for variety
5. Monitor statistics in `stats.yml`

## ❓ Support

For issues or questions:
1. Check the [README.md](../README.md) for detailed documentation
2. Review [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) for technical details
3. Check server console for error messages
4. Verify configuration syntax in config.yml and arenas.yml

---

**Happy Gaming! 🎮**
