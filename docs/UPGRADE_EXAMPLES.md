# Upgrade Configuration Examples

## Overview

This guide shows valid upgrade definitions for `upgrades.yml`.

## Example: Speed Upgrade (Potion)

```yaml
speed:
  enabled: true
  display-name: "&eSpeed Boost"
  description:
    - "&7All team members gain speed"
  icon: SUGAR
  max-level: 2
  effect-type: POTION_EFFECT
  potion-type: SPEED
  duration: 2147483647
  amplifier-per-level: true
  cost:
    level-1:
      material: EMERALD
      amount: 1
    level-2:
      material: EMERALD
      amount: 2
```

## Example: Fire Aspect Upgrade (Enchantment)

```yaml
fire-aspect:
  enabled: true
  display-name: "&6Blazing Weapons"
  description:
    - "&7Weapons apply fire"
  icon: BLAZE_POWDER
  max-level: 2
  effect-type: ENCHANTMENT
  target: WEAPON
  enchantment: FIRE_ASPECT
  cost:
    level-1:
      material: GOLD_INGOT
      amount: 10
    level-2:
      material: GOLD_INGOT
      amount: 20
```

## Example: Feather Falling Upgrade (Armor Enchantment)

```yaml
feather-falling:
  enabled: true
  display-name: "&bSoft Landing"
  description:
    - "&7Reduces fall damage"
  icon: FEATHER
  max-level: 4
  effect-type: ENCHANTMENT
  target: ARMOR
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

## Effect Types

### ENCHANTMENT

Required keys:

- `target`: `WEAPON` or `ARMOR`
- `enchantment`: valid Bukkit enchantment key

### POTION_EFFECT

Required keys:

- `potion-type`: valid Bukkit potion effect key
- `duration`: ticks
- `amplifier-per-level`: boolean

### SPECIAL

For custom behaviors not covered by built-in enchantment and potion flows. Usually requires corresponding plugin logic.

## Recommended Validation

1. Keep YAML indentation at two spaces.
2. Restart or reload after edits.
3. Test each new upgrade in a staging arena.
4. Increase costs with each level for balance.

## Common Mistakes

- Invalid `material` names.
- Invalid `enchantment` or `potion-type` names.
- Missing `cost` entries for one or more levels.
- Using tabs instead of spaces in YAML.

## Related Docs

- [CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md)
- [ENHANCED_FEATURES.md](ENHANCED_FEATURES.md)
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
