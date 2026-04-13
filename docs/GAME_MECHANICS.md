# BedWars Game Mechanics Guide

## Overview

BedWars is a team-based game mode where each team protects its bed while trying to destroy enemy beds. The final surviving team wins.

## Game Flow

### States

```text
WAITING -> STARTING -> RUNNING -> ENDING -> RESET
```

### Typical Timing

- Lobby wait: until minimum players join.
- Starting countdown: 30 seconds.
- Active match: variable based on player decisions.
- End phase: short winner display, then cleanup.

## Teams and Respawns

### Team Basics

- Supported team modes range from solo up to larger team configurations.
- Team colors identify players and their leather armor.
- Team damage is disabled.

### Bed Rules

- If a team bed is intact, eliminated players on that team can respawn.
- If a team bed is destroyed, that team loses respawn capability.
- Once all players of a no-bed team are eliminated, that team is out.

### Win Condition

- Last remaining team wins the match.

## Combat and Survival

- Player-versus-player combat is enabled across teams.
- Void falls are lethal and often decide fights near bridges.
- Environmental damage sources (for example, fall and fire) remain relevant unless configured otherwise.

## Resource Economy

### Resource Types

- Iron: basic purchases.
- Gold: stronger gear and utility.
- Diamond: team upgrades and high-value options.
- Emerald: premium and late-game purchases.

### Generator Roles

- Team-side generators provide early progression.
- Mid-map generators drive contest and map control.
- Holding central resources usually determines late-game momentum.

## Shop System

### Item Shop

The item shop is used for blocks, weapons, armor, tools, consumables, and special utilities.

Common purchase priorities:

- Early blocks to protect the bed.
- Basic weapon and armor upgrades.
- Utility items for pressure and mobility.

### Upgrade Shop

Team upgrades apply to all teammates and are usually stronger long-term value than isolated purchases.

Common priorities:

1. Reinforced armor levels.
2. Sharpened swords.
3. Utility upgrades based on map and opponent behavior.

## Match Strategy

### Early Game

- Secure initial resources quickly.
- Build first-layer bed defense.
- Scout adjacent teams and bridge routes.

### Mid Game

- Contest neutral generators.
- Track enemy bed status and upgrade progression.
- Rotate between defense and pressure.

### Late Game

- Deny critical resources from remaining teams.
- Coordinate pushes on exposed beds.
- Avoid unnecessary isolated fights.

## Common Mistakes

- Ignoring team upgrades too long.
- Taking repeated fights on unsafe bridges.
- Overcommitting to offense with no bed defense.
- Spending resources without a plan.

## Quick FAQ

### Can a team respawn after bed loss?

No. After bed destruction, future deaths are final eliminations.

### Is friendly fire enabled?

No. Teammates cannot damage each other.

### What usually decides winners?

Consistent resource control, upgrade timing, and coordinated bed pressure.
