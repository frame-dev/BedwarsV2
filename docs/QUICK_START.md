# BedWars Quick Start

## Goal

Get one playable BedWars arena running quickly with safe defaults.

## Prerequisites

- Java 17+
- Spigot or Paper 1.20.4+
- Build tooling if compiling from source

## Build and Install

```bash
mvn clean package
cp target/bedwars-1.2-SNAPSHOT.jar <server>/plugins/
```

Start the server once so default config files are generated.

## Minimal Setup Workflow

1. Join your server with operator permissions.
2. Create arena setup session.
3. Set lobby and spectator spawns.
4. Configure at least two teams with spawn and bed locations.
5. Set min and max players.
6. Save the arena.

### Commands

Place each team's complete bed first. Look at either half from within five
blocks when running `/bw setup setbed <team>`; the plugin saves the exact foot
block, including the bed's existing placement. It does not place a bed at your
player position. Re-register bed locations created with older versions if they
point above or beside the actual bed.

```text
/bw setup create myarena
/bw setup setlobby
/bw setup setspectator
/bw setup setspawn red
/bw setup setbed red
/bw setup setspawn blue
/bw setup setbed blue
/bw setup setminplayers 2
/bw setup setmaxplayers 8
/bw setup info
/bw setup save
```

## First Match

1. Have players join the configured arena.
2. Wait for countdown.
3. Verify gameplay loop: join -> start -> running -> ending -> reset.

Useful commands:

```text
/bw join myarena
/bw leave
/bw list
/bw start
/bw stop
```

## Core Config Files

| File | Purpose |
| --- | --- |
| `config.yml` | Global gameplay behavior |
| `arenas.yml` | Arena definitions and spawn/bed data |
| `messages.yml` | Chat and UI messages |
| `shop.yml` | Shop items and prices |
| `upgrades.yml` | Team upgrades |

## Common Checks

### Arena does not save

- Run `/bw setup info` and fill all missing fields.
- Ensure at least two teams have both spawn and bed positions.

### Players cannot join

- Confirm arena exists with `/bw list`.
- Confirm min/max player values are valid.

### Plugin does not start

- Verify Java and server versions.
- Check server logs for configuration errors.

## Recommended Next Steps

1. Tune shop and upgrade values.
2. Customize messages and branding.
3. Add more arenas for rotation.
4. Review advanced docs for BungeeCord and database options.

## Related Documentation

- [INDEX.md](INDEX.md)
- [CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md)
- [GAME_MECHANICS.md](GAME_MECHANICS.md)
- [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
