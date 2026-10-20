# Create your own cosmetics

You can add kill and bed-destruction cosmetics in `plugins/BedWars/cosmetics.yml`
without changing Java. Each cosmetic has a unique ID within its category, menu
details, an optional permission, and an effect.

Stop the server before editing the file. Back it up, add the entries below to
the existing sections, and restart. Do not create duplicate `kill-effects` or
`bed-effects` sections.

## Create a custom kill effect

Add `golden_magic` inside your existing `kill-effects` section:

```yaml
kill-effects:
  # Keep your other kill effects here too.
  golden_magic:
    enabled: true
    display-name: "&6Golden Magic"
    icon: GOLD_NUGGET
    lore:
      - "&7Golden dust mixed with enchanting glyphs."
    permission: "bedwars.cosmetics.kill.golden_magic"
    effect: CUSTOM
    particles:
      - type: REDSTONE
        color: "#FFD700"
        size: 1.5
        count: 40
        height: 1.0
        spread-x: 0.5
        spread-y: 0.8
        spread-z: 0.5
        speed: 0
      - type: ENCHANTMENT_TABLE
        count: 30
        height: 0.5
        spread-x: 0.7
        spread-y: 0.7
        spread-z: 0.7
        speed: 0.2
```

The ID `golden_magic` is saved as the player's selection. Keep IDs stable when
editing an existing cosmetic so saved selections continue to work.

## Create a custom bed effect

Add `soul_burst` inside the existing `bed-effects` section:

```yaml
bed-effects:
  # Keep your other bed effects here too.
  soul_burst:
    enabled: true
    display-name: "&bSoul Burst"
    icon: SOUL_LANTERN
    lore:
      - "&7Blue flames and clouds around the destroyed bed."
    permission: "bedwars.cosmetics.bed.soul_burst"
    effect: CUSTOM
    particles:
      - type: SOUL_FIRE_FLAME
        count: 45
        height: 0.8
        spread-x: 0.8
        spread-y: 0.8
        spread-z: 0.8
        speed: 0.03
      - type: CLOUD
        count: 20
        height: 0.5
        spread-x: 0.6
        spread-y: 0.3
        spread-z: 0.6
        speed: 0.02
```

Particles are visual only. Flame particles do not set fire to blocks or players.
Layers are emitted together as one burst when the kill or bed break happens.

## Available settings

| Setting | Meaning | Default / range |
| --- | --- | --- |
| `enabled` | Show and use this cosmetic | `true` |
| `display-name` | Menu name, supports `&` colors | Cosmetic ID |
| `icon` | Bukkit material name for the menu icon | `PAPER` |
| `lore` | Menu description lines | Empty list |
| `permission` | Required for selection and playback | `""` allows everyone |
| `effect` | `CUSTOM` for your particle layers, or a built-in effect key | `NONE` |
| `particles` | List of particle layers for `CUSTOM` | First 8 layers used |
| Layer `type` | Bukkit particle enum name | Required |
| Layer `count` | Number of particles | `30`, clamped to 1–250 |
| Layer `height` | Vertical offset from the event position | `0.5`, clamped to -4–4 |
| Layer `spread-x/y/z` | Particle spread on each axis | `0.5` each, clamped to 0–4 |
| Layer `speed` | Particle speed/extra parameter | `0`, clamped to 0–1 |
| Layer `color` | Hex RGB for dust particles | `"#FFFFFF"` |
| Layer `size` | Dust particle size | `1`, clamped to 0.1–4 |

Use particle names supported by your running server. Examples supported by the
project's API include `FLAME`, `SOUL_FIRE_FLAME`, `HEART`, `CLOUD`, `PORTAL`,
`ENCHANTMENT_TABLE`, `NOTE`, `TOTEM`, `END_ROD`, and `REDSTONE`.

`REDSTONE` supports custom color and size. Other particles requiring extra data,
such as block or item particles, are not supported by this custom format.
Invalid layers are skipped with a console warning. A custom cosmetic with no
valid layers is disabled rather than throwing an error during gameplay.

## Use an existing effect with a different name or icon

Instead of `CUSTOM`, use any built-in effect key:

```text
NONE, FIREWORK, LIGHTNING, HEART, SMOKE, EXPLOSION,
FLAME, SOUL, PORTAL, ENCHANT, MUSIC, TOTEM
```

For example, `effect: HEART` can be used for a custom bed cosmetic too. A
`particles` list is only used when `effect: CUSTOM` is selected.

## Permissions and testing

Grant your custom permission through your permissions plugin. Custom nodes do
not need a Java change or an entry in `plugin.yml` to be checked. Use
`permission: ""` to make a cosmetic public.

To test without effect permissions, set this at the root of `cosmetics.yml`:

```yaml
testing:
  unlock-all: true
```

This bypasses custom cosmetic permissions as well as built-in ones, for both
selection and playback. Disabled effects remain disabled. Use `/bw cosmetics`
after restarting, select the new cosmetic, and trigger a kill or enemy bed break.
Set unlock-all back to `false` and restart when finished testing.

## Menu placement and upgrades

New entries appear in configuration order. The default menu starts kill effects
at slot 10 and bed effects at slot 28, using a step of 1. A menu contains up to
54 slots, numbered 0–53. Bed effects skip slots already occupied by kill effects.
There is currently no pagination: keep the total number of displayed effects
within the available slots, or adjust `gui.size`, category starts, and step.

New custom IDs survive plugin updates. Missing bundled effects are added on
startup without overwriting existing entries. Disable unwanted bundled entries
with `enabled: false` rather than deleting them.
