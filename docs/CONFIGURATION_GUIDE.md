# Configuration Guide

A comprehensive guide to configuring your BedWars plugin.

---

## Table of Contents

1. [config.yml](#configyml)
2. [messages.yml](#messagesyml)
3. [shop.yml](#shopyml)
4. [upgrades.yml](#upgradesyml)
5. [arenas.yml](#arenasyml)

---

## config.yml

Main plugin configuration file. Reload with `/bw reload`.

### Basic Settings

```yaml
# Plugin settings
debug: false
check-for-updates: true

# Database configuration
database:
  type: sqlite
  file: bedwars.db
  # For MySQL:
  # type: mysql
  # host: localhost
  # port: 3306
  # database: bedwars
  # username: root
  # password: password
  # pool-size: 10
```

### Game Configuration

```yaml
game:
  # Time (in seconds) before game starts
  countdown-time: 30
  
  # Respawn time when bed is alive
  respawn-time: 5
  
  # Starting item given to all players
  starting-item: WOOD_SWORD
  
  # Enable/disable specific game mechanics
  allow-block-placement: true
  allow-block-breaking: true
  enable-pvp: true
  
  # Lobby wait time (min players to start instantly)
  min-players-auto-start: 4
```

### World Configuration

```yaml
world:
  # Automatically reset world blocks after game
  auto-reset: true
  
  # World protection radius (in blocks)
  protection-radius: 200
  
  # Allow fire spread
  fire-spread: false
  
  # Allow entity spawning (monsters)
  mob-spawning: false
```

### Arena Configuration

```yaml
arenas:
  example-arena:
    # Arena metadata
    display-name: "Example Arena"
    description: "A 4v4 arena"
    
    # Player limits
    min-players: 2
    max-players: 8
    
    # Lobby spawn (where players wait)
    lobby-spawn:
      world: world
      x: 0
      y: 100
      z: 0
      yaw: 0
      pitch: 0
    
    # Spectator spawn (where watchers spawn)
    spectator-spawn:
      world: world
      x: 50
      y: 150
      z: 50
      yaw: 0
      pitch: 0
    
    # Teams configuration
    teams:
      red:
        # Team spawn (where players respawn)
        spawn:
          world: world
          x: 100
          y: 64
          z: 100
          yaw: 0
          pitch: 0
        
        # Bed location (what to protect)
        bed:
          world: world
          x: 105
          y: 64
          z: 100
        
        # Team color (used for armor)
        color: RED
        
        # Team display name
        display-name: "Red Team"
      
      blue:
        spawn:
          world: world
          x: -100
          y: 64
          z: -100
          yaw: 180
          pitch: 0
        bed:
          world: world
          x: -105
          y: 64
          z: -100
        color: BLUE
        display-name: "Blue Team"
    
    # Resource generators
    generators:
      iron-1:
        type: iron
        location:
          world: world
          x: 0
          y: 65
          z: 0
        drop-rate: 1.0  # Items per second
      
      gold-1:
        type: gold
        location:
          world: world
          x: 50
          y: 70
          z: 50
        drop-rate: 0.125  # 1 every 8 seconds
      
      diamond-1:
        type: diamond
        location:
          world: world
          x: -50
          y: 80
          z: -50
        drop-rate: 0.033  # 1 every 30 seconds
      
      emerald-1:
        type: emerald
        location:
          world: world
          x: 0
          y: 90
          z: -100
        drop-rate: 0.0167  # 1 every 60 seconds
```

### BungeeCord Configuration

```yaml
bungeecord:
  enabled: false
  # Server name to send players back to
  lobby-server: lobby
```

---

## messages.yml

Customize all plugin messages. Supports `&` color codes.

### Message Examples

```yaml
# Command responses
command:
  join-success: "&a✓ You joined game: &b{arena}"
  join-failure: "&c✗ Game is full or doesn't exist"
  already-in-game: "&c✗ You're already in a game"
  not-in-game: "&c✗ You're not in a game"
  left-game: "&aYou left the game"
  force-left-game: "&cYou were removed from the game"

# Game messages
game:
  game-starting: "&6Game starting in &c{countdown} &6seconds..."
  game-started: "&a&lGAME STARTED!"
  game-ended: "&e&l{winner} &eWON!"
  team-eliminated: "&c{team} &7has been eliminated!"
  bed-destroyed: "&c&l⚠ &c{team}'s bed has been destroyed!"
  final-kill: "&e&l★ &e{player} &7got a final kill!"

# Countdown messages
countdown:
  30: "&6Game starting in 30 seconds!"
  10: "&eGame starting in 10 seconds"
  5: "&c5..."
  4: "&c4..."
  3: "&c3..."
  2: "&c2..."
  1: "&c1..."

# Shop messages
shop:
  no-resources: "&cYou don't have enough {resource}!"
  purchase-success: "&a✓ Purchased {item}"
  upgrade-success: "&a✓ Upgraded {upgrade}"
```

---

## shop.yml

Define shop items and their costs.

### Item Definition

```yaml
items:
  # Blocks category
  blocks:
    wool:
      display-name: "&fWool"
      lore:
        - "&7Cost: &61 Iron"
      material: WOOL
      cost:
        iron: 1
      amount: 16
    
    obsidian:
      display-name: "&0Obsidian"
      lore:
        - "&7Cost: &64 Diamonds"
        - "&7Unbreakable"
      material: OBSIDIAN
      cost:
        diamond: 4
      amount: 4
  
  # Weapons category
  weapons:
    iron-sword:
      display-name: "&7Iron Sword"
      lore:
        - "&7Cost: &65 Gold"
      material: IRON_SWORD
      cost:
        gold: 5
      amount: 1
    
    diamond-sword:
      display-name: "&bDiamond Sword"
      lore:
        - "&7Cost: &63 Diamonds"
      material: DIAMOND_SWORD
      cost:
        diamond: 3
      amount: 1
```

---

## upgrades.yml

Define team upgrades available in the upgrade shop.

### Upgrade Definition

```yaml
upgrades:
  sharpened-swords:
    display-name: "&6Sharpened Swords"
    description: "Adds Sharpness I to all swords"
    material: DIAMOND_SWORD
    cost:
      diamond: 8
    effect:
      enchantment: SHARPNESS
      level: 1
    max-tier: 1
  
  reinforced-armor:
    display-name: "&7Reinforced Armor"
    description: "Adds Protection to all armor"
    material: DIAMOND_CHESTPLATE
    tiers:
      1:
        cost:
          diamond: 2
        effect:
          enchantment: PROTECTION_ENVIRONMENTAL
          level: 1
      2:
        cost:
          diamond: 4
        effect:
          enchantment: PROTECTION_ENVIRONMENTAL
          level: 2
      3:
        cost:
          diamond: 8
        effect:
          enchantment: PROTECTION_ENVIRONMENTAL
          level: 3
      4:
        cost:
          diamond: 16
        effect:
          enchantment: PROTECTION_ENVIRONMENTAL
          level: 4
    max-tier: 4
  
  maniac-miner:
    display-name: "&6Maniac Miner"
    description: "Adds Haste to players"
    material: DIAMOND_PICKAXE
    tiers:
      1:
        cost:
          diamond: 2
        effect:
          enchantment: HASTE
          level: 1
      2:
        cost:
          diamond: 4
        effect:
          enchantment: HASTE
          level: 2
    max-tier: 2
```

---

## arenas.yml

Auto-generated file that stores arena configurations created with `/bw setup` commands.

**Do not edit manually** - Use in-game commands instead:
```
/bw setup create <name>
/bw setup setlobby
/bw setup setspawn <team>
/bw setup setbed <team>
/bw setup addgenerator <name>
/bw setup save
```

---

## Color Codes

Use `&` for colors in messages:

| Code | Color |
|------|-------|
| `&0` | Black |
| `&1` | Dark Blue |
| `&2` | Dark Green |
| `&3` | Dark Aqua |
| `&4` | Dark Red |
| `&5` | Dark Purple |
| `&6` | Gold |
| `&7` | Gray |
| `&8` | Dark Gray |
| `&9` | Blue |
| `&a` | Green |
| `&b` | Aqua |
| `&c` | Red |
| `&d` | Light Purple |
| `&e` | Yellow |
| `&f` | White |
| `&l` | Bold |
| `&m` | Strikethrough |
| `&n` | Underline |
| `&o` | Italic |
| `&r` | Reset |

---

## Tips

1. **Reload Configuration**: Use `/bw reload` to reload configs without restart
2. **Comments**: Use `#` for comments in YAML files
3. **Indentation**: YAML requires 2-space indentation (not tabs)
4. **Validation**: Check for YAML syntax errors at www.yamllint.com
5. **Backup**: Always backup your configs before editing
