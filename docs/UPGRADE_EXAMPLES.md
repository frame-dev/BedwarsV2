# BedWars Dynamic Upgrade System

## Overview
The upgrade system is now **fully dynamic**. You can add new upgrades to `upgrades.yml` without touching any code!

## Adding New Upgrades

### Example 1: Speed Boost (Potion Effect)
Add this to `upgrades.yml` configuration file:

```yaml
  speed:
    enabled: true
    display-name: "&eSpeed Boost"
    description:
      - "&7All team members get"
      - "&7permanent Speed II!"
    icon: SUGAR
    max-level: 2
    effect-type: POTION_EFFECT
    potion-type: SPEED
    duration: 2147483647  # Integer.MAX_VALUE = permanent
    amplifier-per-level: true  # Level 1 = Speed I, Level 2 = Speed II
    cost:
      level-1:
        material: EMERALD
        amount: 1
      level-2:
        material: EMERALD
        amount: 2
```

### Example 2: Fire Aspect on Weapons (Enchantment)
```yaml
  fire-aspect:
    enabled: true
    display-name: "&6Blazing Weapons"
    description:
      - "&7Your weapons set enemies"
      - "&7on fire!"
    icon: BLAZE_POWDER
    max-level: 2
    effect-type: ENCHANTMENT
    target: WEAPON  # Applies to swords and axes
    enchantment: FIRE_ASPECT
    cost:
      level-1:
        material: GOLD_INGOT
        amount: 10
      level-2:
        material: GOLD_INGOT
        amount: 20
```

### Example 3: Feather Falling on Armor (Enchantment)
```yaml
  feather-falling:
    enabled: true
    display-name: "&bSoft Landing"
    description:
      - "&7Reduces fall damage"
      - "&7for all team members!"
    icon: FEATHER
    max-level: 4
    effect-type: ENCHANTMENT
    target: ARMOR  # Applies to all armor pieces
    enchantment: PROTECTION_FALL
    cost:
      level-1:
        material: IRON_INGOT
        amount: 4
      level-2:
        material: IRON_INGOT
        amount: 8
      level-3:
        material: GOLD_INGOT
        amount: 4
      level-4:
        material: GOLD_INGOT
        amount: 8
```

### Example 4: Regeneration Effect
```yaml
  regeneration:
    enabled: true
    display-name: "&dHealthy Team"
    description:
      - "&7All team members"
      - "&7slowly regenerate health!"
    icon: GOLDEN_APPLE
    max-level: 1
    effect-type: POTION_EFFECT
    potion-type: REGENERATION
    duration: 2147483647
    amplifier-per-level: false  # Always Regeneration I
    cost:
      level-1:
        material: DIAMOND
        amount: 5
```

### Example 5: Custom Special Upgrade (requires custom logic)
```yaml
  trap-detector:
    enabled: true
    display-name: "&cTrap Detector"
    description:
      - "&7Alerts team members when"
      - "&7enemies place traps nearby!"
    icon: REDSTONE
    max-level: 1
    effect-type: SPECIAL  # Requires custom code logic
    cost:
      level-1:
        material: EMERALD
        amount: 3
```

## Effect Types

### 1. ENCHANTMENT
Adds enchantments to items automatically.

**Required fields:**
- `target`: "WEAPON" (swords/axes) or "ARMOR" (all armor pieces)
- `enchantment`: Minecraft enchantment name (e.g., DAMAGE_ALL, PROTECTION_ENVIRONMENTAL)

**How it works:**
- Enchantments are applied when players respawn (to starting armor/sword)
- Enchantments are applied when players pick up items (auto-enchant)
- Enchantments are applied when players craft items
- Level increases with upgrade level (Protection I → II → III → IV)

**Available enchantments for WEAPON:**
- DAMAGE_ALL (Sharpness)
- DAMAGE_ARTHROPODS (Bane of Arthropods)
- DAMAGE_UNDEAD (Smite)
- FIRE_ASPECT
- KNOCKBACK
- LOOT_BONUS_MOBS (Looting)

**Available enchantments for ARMOR:**
- PROTECTION_ENVIRONMENTAL (Protection)
- PROTECTION_FIRE (Fire Protection)
- PROTECTION_EXPLOSIONS (Blast Protection)
- PROTECTION_PROJECTILE (Projectile Protection)
- PROTECTION_FALL (Feather Falling)
- THORNS

### 2. POTION_EFFECT
Gives permanent potion effects to all team members.

**Required fields:**
- `potion-type`: Minecraft potion effect name (e.g., FAST_DIGGING, SPEED)
- `duration`: Duration in ticks (20 ticks = 1 second, 2147483647 = permanent)
- `amplifier-per-level`: true = effect strength increases per level, false = fixed strength

**How it works:**
- Effects are applied when players respawn
- Effects persist until player dies or leaves
- `amplifier-per-level: true` means Level 1 = Effect I, Level 2 = Effect II, etc.
- `amplifier-per-level: false` means always Effect I regardless of level

**Available potion effects:**
- SPEED
- SLOW
- FAST_DIGGING (Haste)
- SLOW_DIGGING (Mining Fatigue)
- INCREASE_DAMAGE (Strength)
- HEAL
- HARM
- JUMP
- CONFUSION (Nausea)
- REGENERATION
- DAMAGE_RESISTANCE (Resistance)
- FIRE_RESISTANCE
- WATER_BREATHING
- INVISIBILITY
- BLINDNESS
- NIGHT_VISION
- HUNGER
- WEAKNESS
- POISON
- WITHER
- HEALTH_BOOST
- ABSORPTION
- SATURATION
- GLOWING
- LEVITATION
- LUCK
- UNLUCK

### 3. SPECIAL
Custom effects that require code implementation.

**Built-in SPECIAL upgrades:**
- Heal Pool (regeneration field at base)
- Dragon Buff (spawns ender dragons at base)

**Custom SPECIAL upgrades** still require code changes. To add one:
1. Add the upgrade to `upgrades.yml` with `effect-type: SPECIAL`
2. Implement custom logic in your code (e.g., in Game.java or custom listeners)
3. Check if team has the upgrade: `team.getUpgrades().hasUpgrade("upgrade-id")`

## Configuration Properties

### Required for all upgrades:
- `enabled`: true/false - whether upgrade is active
- `display-name`: Name shown in GUI (supports & color codes)
- `description`: List of lore lines (supports & color codes)
- `icon`: Material type for GUI display
- `max-level`: Maximum upgrade level
- `effect-type`: ENCHANTMENT, POTION_EFFECT, or SPECIAL
- `cost`: Cost per level (level-1, level-2, etc.)
  - `material`: Resource material
  - `amount`: Resource amount

### Optional (depends on effect-type):
- `target`: "WEAPON" or "ARMOR" (for ENCHANTMENT)
- `enchantment`: Enchantment name (for ENCHANTMENT)
- `potion-type`: PotionEffectType name (for POTION_EFFECT)
- `duration`: Duration in ticks (for POTION_EFFECT)
- `amplifier-per-level`: true/false (for POTION_EFFECT)

## Tips

1. **Balance costs carefully**: Diamonds and emeralds should be rare
2. **Multi-level upgrades**: Use increasing costs (2, 4, 8, 16)
3. **Test effects**: Some enchantments may not work on certain items
4. **Color codes**: Use & codes (&a=green, &c=red, &e=yellow, &6=gold, &7=gray)
5. **Order matters**: Upgrades appear in GUI in the order they're defined

## Troubleshooting

**Upgrade doesn't appear in GUI:**
- Check `enabled: true`
- Check YAML syntax (proper indentation)
- Restart server or use `/bedwars reload`

**Upgrade appears but doesn't work:**
- For ENCHANTMENT: Check target and enchantment name spelling
- For POTION_EFFECT: Check potion-type name spelling
- For SPECIAL: Custom code implementation needed

**Invalid material error:**
- Check Material names at https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
- Use UPPERCASE with underscores (e.g., GOLDEN_APPLE, not golden_apple)

**Cost doesn't work:**
- Ensure cost section uses "level-1", "level-2", etc. (with quotes)
- Each level needs both material and amount
