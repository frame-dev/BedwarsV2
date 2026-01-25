# BedWars Game Mechanics Guide

## Game Overview

BedWars is a competitive team-based minigame where teams battle to destroy each other's beds while surviving attacks. The last team with an intact bed wins!

---

## Game Flow

### Game States

```
WAITING (30+ secs)
    ↓
STARTING (30 second countdown)
    ↓
RUNNING (Game in progress)
    ↓
ENDING (5 second finale)
    ↓
RESET (Cleanup & next game)
```

### Game Timing

- **Lobby Wait**: Players join and wait for minimum players (default: 2 minimum)
- **Countdown**: 30 seconds before game starts
- **Game Duration**: Typically 10-30 minutes (depends on playstyle)
- **Cleanup**: 5 seconds after game ends before reset

---

## Core Mechanics

### Teams & Spawning

- **Team Sizes**: 1v1 to 4v4 (8 teams max)
- **Colors**: Red, Blue, Green, Yellow, Aqua, White, Pink, Gray
- **Spawn Protection**: 5 seconds of invulnerability on respawn
- **Armor Color**: Automatically matched to team color (leather armor)

### Beds & Respawning

**Bed Active:**
- Players respawn in 5 seconds after death
- Bed is protected and cannot be broken
- Team continues to accumulate resources

**Bed Destroyed:**
- Team cannot respawn
- Final elimination on next death (no respawn)
- Last player(s) of team must survive or be eliminated

**Win Condition:**
- All other teams eliminated (beds destroyed OR all players dead)
- Last team standing wins

### Damage & Combat

- **Fall Damage**: Enabled (watch your step!)
- **Drowning**: Enabled
- **Suffocation**: Enabled
- **Fire Damage**: Enabled
- **Void Damage**: Enabled (fall off = instant death)
- **PvP Combat**: Enabled with other teams only
- **Team Damage**: DISABLED (no friendly fire)

---

## Resource System

### Resource Generators

Resources automatically spawn at configured locations and are critical for progression.

#### Iron Generators
- **Drop Rate**: 1 iron ingot every 1 second
- **Purpose**: Basic economy, tools, armor
- **Placement**: Usually near team spawn

#### Gold Generators
- **Drop Rate**: 1 gold ingot every 8 seconds
- **Purpose**: Better armor, weapons
- **Placement**: Usually in center/contested areas

#### Diamond Generators
- **Drop Rate**: 1 diamond every 30 seconds (base)
- **Upgrade Available**: Every 12 minutes, can upgrade to 20 second interval
- **Purpose**: Powerful upgrades and gear
- **Placement**: Usually high-value map positions

#### Emerald Generators
- **Drop Rate**: 1 emerald every 60 seconds (base)
- **Upgrade Available**: Every 24 minutes, can upgrade to 40 second interval
- **Purpose**: Luxury upgrades and special items
- **Placement**: Rare, high-risk locations

### Resource Economy

Resources serve as currency:
- **Iron**: Common, used for basic items
- **Gold**: Medium, used for better gear
- **Diamonds**: Rare, used for powerful upgrades
- **Emeralds**: Rarest, used for premium items

---

## Shop System

### Item Shop

The Item Shop allows teams to purchase equipment. Access it by interacting with the shop keeper (clay block at spawn).

#### Categories

**1. Blocks**
- Wool (1 iron)
- Terracotta (2 iron)
- End Stone (3 iron)
- Obsidian (4 diamonds)

**2. Weapons**
- Stone Sword (10 iron)
- Iron Sword (5 gold)
- Diamond Sword (3 diamonds)
- Knockback Stick (5 iron)

**3. Armor**
- Chainmail Helmet (40 iron)
- Iron Armor Set (5 gold each piece)
- Diamond Armor Set (3 diamonds each piece)

**4. Tools**
- Wooden Pickaxe (Free)
- Iron Pickaxe (10 iron)
- Diamond Pickaxe (1 diamond)
- Iron Axe (8 iron)
- Shears (10 iron)

**5. Food**
- Apple (2 iron)
- Steak (3 iron)
- Golden Apple (2 diamonds)

**6. Potions**
- Speed Potion (3 gold)
- Jump Boost Potion (2 gold)
- Invisibility Potion (1 diamond)

**7. Special Items**
- TNT (8 iron)
- Ender Pearl (4 gold)
- Fire Charge (2 iron)
- Ladders (1 iron per 8)

### Upgrade Shop

Team upgrades are permanent for the entire game and benefit all team members.

#### Available Upgrades

**1. Sharpened Swords**
- Cost: 8 diamonds
- Effect: Sharpness I on all swords
- Benefit: +1.25 damage per hit

**2. Reinforced Armor**
- Tiers: Protection I (2 diam) → IV (16 diam)
- Effect: Reduces all damage by 4% per tier
- Benefit: Much harder to kill

**3. Maniac Miner**
- Tiers: Haste I (2 diam) → II (4 diam)
- Effect: Faster block breaking
- Benefit: Mine resources and terrain faster

**4. Heal Pool**
- Cost: 3 diamonds
- Effect: Regeneration aura (1 HP/sec nearby)
- Benefit: Passive healing for team

**5. Dragon Buff**
- Cost: 5 emeralds
- Effect: Base protection enhancement
- Benefit: Team becomes harder to kill

---

## Strategy Tips

### Early Game (First 5 minutes)
1. **Secure resources**: Focus on collecting iron from generators
2. **Build protection**: Use blocks to create walls around your base
3. **Team unity**: Stay together for protection
4. **Watch enemies**: Monitor other teams for early aggression

### Mid Game (5-15 minutes)
1. **Upgrade gear**: Purchase better weapons and armor
2. **Get upgrades**: Buy team upgrades (Reinforced Armor is priority)
3. **Expand**: Build out from your base, take neutral territory
4. **Contest diamonds**: Fight for gold and diamond generators
5. **Scout**: Use ender pearls to explore enemy positions

### Late Game (15+ minutes)
1. **Target weak teams**: Focus fire on teams with fewer players
2. **Bed rush**: If enemy bed accessible, attempt bed break
3. **Hold diamonds**: Deny diamonds to other teams
4. **Final push**: Go for victory with coordinated attacks

### General Strategy
- **Never fight at full strength**: Avoid 1v4 scenarios
- **Protect generators**: Don't let enemies farm your resources
- **Use terrain**: High ground is advantage
- **Team upgrades first**: Better than individual items
- **Watch knockback**: Knockback stick is powerful near edges

---

## Victory Conditions

### Bed Destruction Victory
- Destroy enemy bed
- Eliminate all players on that team
- Last team standing wins

### Time-Based Victory (Rare)
- Some servers implement time limits
- Team with most resources/players wins if time expires
- Usually doesn't happen in normal games

---

## Common Mistakes to Avoid

❌ **Spreading out** - Stay together for safety
❌ **Ignoring upgrades** - Team upgrades = huge power boost
❌ **Fighting at void level** - One hit = death
❌ **Leaving bed undefended** - Protect your bed at all costs
❌ **No armor** - Always purchase armor quickly
❌ **Wasteful spending** - Buy strategically, not randomly
❌ **Ignoring resource generators** - They're your lifeline

---

## Common Questions

**Q: Can I break my own bed?**
A: No, your bed is protected from your own team.

**Q: What happens if my bed is destroyed?**
A: You cannot respawn. Next death = elimination.

**Q: Can I heal during the game?**
A: Yes, using food or potions. Golden apples heal 4 hearts.

**Q: What's the best strategy?**
A: Secure resources → Get upgrades → Contest generators → Win

**Q: How do I respawn?**
A: You respawn at your team spawn automatically after 5 seconds (if bed alive).

**Q: Can I play solo?**
A: Technically yes, but it's much harder. Teamwork = victory.
