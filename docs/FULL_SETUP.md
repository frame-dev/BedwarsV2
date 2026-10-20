# BedWars full setup guide

This guide describes the current `1.2-SNAPSHOT` implementation. It takes you from
the source project to a playable two-team arena named `skyislands`, with shops,
resource generators, join signs, and automatic round cleanup.

## 1. Requirements

- A Spigot/Paper server and a Minecraft world containing your arena.
- A Java runtime compatible with your server. The plugin compiles for Java 17;
  your server version may require a newer runtime.
- Maven to build this project from source.
- An operator account, or the setup permissions listed below.
- At least two player accounts for a normal match test.

The project README lists Spigot/Paper 1.20.4+ as its server baseline. The build
uses the Spigot 1.18.2 API. A successful build is not a live compatibility test
for every server version.

## 2. Build and install

From the project directory, run:

```powershell
mvn clean verify
```

Copy this file into your server's `plugins` directory:

```text
target/bedwars-1.2-SNAPSHOT.jar
```

Use that file rather than `original-bedwars-1.2-SNAPSHOT.jar`. Keep only one
BedWars plugin JAR installed. Start the server and check its console for:

```text
BedWars plugin has been enabled!
```

The server must be able to resolve the SQLite library declared in `plugin.yml`.
Check the startup log if library downloading or plugin loading fails.

The plugin stores its files under:

```text
plugins/BedWars/
```

| File | Purpose |
| --- | --- |
| `config.yml` | Gameplay, generators, network mode, voting, and debug settings |
| `arenas.yml` | Saved arena positions and player limits |
| `shop.yml` | Item categories, quantities, and prices |
| `upgrades.yml` | Team upgrade effects, levels, and prices |
| `messages.yml` | Messages and help text |
| `cosmetics.yml` | Cosmetic definitions and menus |
| `achievements.yml` | Achievement definitions and menus |
| `joinsigns.yml` | Registered signs; saved when signs are registered |
| `bedwars.db` | SQLite statistics, parties, cosmetics, and achievements |

Back up the arena world and plugin folder before changing an existing setup.

## 3. Configure a local server

For the first arena, use local mode. Stop the server before editing configuration
files, then merge these settings into the existing `config.yml` sections. Do not
add duplicate `game`, `world`, or `network` sections.

```yaml
network:
  mode: "local"

game:
  countdown-time: 30
  respawn-time: 5
  starting-item: "WOODEN_SWORD"
  void-death-level: 0
  bed-protection: true
  allow-pvp: true
  allow-fall-damage: true
  allow-projectile-damage: true
  diamond-upgrade-time: 720
  emerald-upgrade-time: 1440

world:
  reset-on-end: true
  allow-block-breaking: true
  allow-block-placing: true

map-voting:
  enabled: true
  queue-min-players: 2
  vote-duration-seconds: 20
```

Set the void level below the playable islands. A participating player at or below
that Y level dies; do not put an island, spawn, or lobby there.

`starting-item: "AIR"` disables the starting item. Other valid material names,
such as `STONE_SWORD`, replace the default starting item.

Arena player limits are set separately in the setup commands below. The
`game.min-players` and `game.max-players` entries do not replace those arena limits.

Restart the server after editing settings. Some listeners and managers cache
their configuration, so a restart applies changes consistently.

## 4. Prepare the map

Build these features before registering their positions:

1. A safe lobby with a solid floor outside the fighting area. Players who join
   by command return to this arena lobby after a round.
2. A safe spectator position overlooking the arena.
3. A red base and a blue base, each with a spawn platform and a complete bed.
4. Two shop positions at each base: one item shop and one upgrade shop.
5. Iron and gold generator positions at each base.
6. Diamond and emerald generator positions elsewhere on the map.

Place beds on solid support with both head and foot present. Use a distinct bed
for each team. A bed's physical block color and orientation come from the bed
you place; setting a team does not recolor or rotate it.

The setup commands record locations. They do not build islands or place beds.
Shop villagers and resource generation appear when the match starts.

Use a world dedicated to BedWars where possible: the current explosion protection
checks worlds used by active arenas, rather than a bounded region around an arena.

## 5. Create the arena

Run setup commands as a player. All commands in this guide use `/bw`, an alias
for `/bedwars`. Keep the same administrator account throughout the setup session.

```text
/bw setup create skyislands
```

An arena with that name must not already exist. A setup session is temporary
until saved; finish it before restarting the server.

### Set the lobby and spectator locations

Stand at the lobby spawn and run:

```text
/bw setup setlobby
```

Move to the spectator position and run:

```text
/bw setup setspectator
```

Both locations are required to save the arena.

### Register the red team

Stand at the red team's spawn:

```text
/bw setup setspawn red
```

Look directly at either half of the red team's complete bed from within five
blocks, then run:

```text
/bw setup setbed red
```

The command stores the exact foot block. You do not need to stand on the bed.
Looking at air, another block, or an incomplete bed is rejected.

Stand where the red item-shop villager should stand, then run:

```text
/bw setup setshop red item
```

Move to the red upgrade-shop position, then run:

```text
/bw setup setshop red upgrade
```

Leave enough floor space for each villager and for players to interact with it.
You do not need to spawn the villagers yourself.

### Register the blue team

Repeat the same process at the blue base, moving before each position command
and looking at the blue bed for `setbed`:

```text
/bw setup setspawn blue
/bw setup setbed blue
/bw setup setshop blue item
/bw setup setshop blue upgrade
```

Additional supported team names are `green`, `yellow`, `aqua`, `white`, `pink`,
and `gray`. Each additional team needs a spawn and a distinct bed; add both
shops and base generators for that team as well.

### Add generators

Stand at the intended item-drop point for each generator before running its
command. Place the point just above a solid floor.

At the red base:

```text
/bw setup addgenerator iron_red
/bw setup addgenerator gold_red
```

At the blue base:

```text
/bw setup addgenerator iron_blue
/bw setup addgenerator gold_blue
```

At the corresponding diamond and emerald map positions:

```text
/bw setup addgenerator diamond_mid_1
/bw setup addgenerator diamond_mid_2
/bw setup addgenerator emerald_mid_1
/bw setup addgenerator emerald_mid_2
```

The name prefix determines the resource: `iron`, `gold`, `diamond`, or `emerald`.
Names such as `red_iron` do not match. Names must be unique within the arena;
reusing one replaces its recorded location in the setup session.

The actual resource types are fixed to iron ingots, gold ingots, diamonds, and
emeralds. The `material` entries in the generator configuration do not change
those types in this implementation.

Each resource must be enabled under `generators` in `config.yml`. Setting
`generators.only-iron-gold: true` disables diamond and emerald generation.

### Set limits, review, and save

For a two-team arena with up to four players per team:

```text
/bw setup setminplayers 2
/bw setup setmaxplayers 8
/bw setup info
/bw setup save
```

Use positive limits, with minimum no greater than maximum. Choose a maximum
divisible by the number of teams for even team capacities.

The save check requires a name, lobby, spectator location, at least two teams,
and a bed for every configured spawn. It does not require shops or generators,
so confirm those counts yourself in `/bw setup info`.

Saving writes `arenas.yml`, loads the new arena, and ends your setup session.
Check that it appears in:

```text
/bw list
```

Use `/bw setup cancel` to discard an unfinished session.

## 6. Create a join sign

Place a sign in the public lobby. Enter these first two lines, leaving the other
lines blank:

```text
[BedWars]
skyislands
```

`[BW]` also works. The creator needs `bedwars.sign.create`; operators have it by
default. The sign should show the arena name, status, and player count.

Right-clicking the sign joins the arena and records the player's current
location. When they leave or the round finishes, sign-joined players return to
their own recorded click location, rather than to the sign's block coordinates.

Ordinary `/bw join skyislands` players return to the lobby set with
`/bw setup setlobby`. This is a per-arena lobby, not a separate global spawn.

## 7. Test a complete round

1. Join with two accounts using the sign or `/bw join skyislands`.
2. Choose opposing teams using the nether star, `/bw team red`, or `/bw team blue`.
3. Wait for the countdown. Check both team spawns and starting equipment.
4. Check iron, gold, diamond, and emerald spawns.
5. Right-click the shop villagers and buy an item and a team upgrade.
6. Die while your bed is intact. Check the respawn countdown and team spawn.
7. Break either half of the opposing team's bed. Both halves should disappear
   without dropping a bed item or XP.
8. Eliminate the opposing player. Confirm the winner announcement.
9. After the five-second winner display, check that all remaining participants
   and spectators leave the match and reach their expected return locations.
10. Confirm both bed halves return with their original color and direction, and
    tracked player-placed blocks are removed when `world.reset-on-end` is true.
11. Join another round and check that the arena works again.

Administrative test commands:

```text
/bw start
/bw stop
```

These act on your current game and require `bedwars.admin`. Force-starting
bypasses the normal minimum/countdown, so use two opposing players for the full
win-condition test.

More regression scenarios are in [TESTING.md](TESTING.md). Automated tests use
mocked Bukkit services; live server checks are still required.

## 8. Customize gameplay

Stop the server, edit the relevant file, and restart after changes.

| Change | Where to edit |
| --- | --- |
| Countdown, respawn, void level, starting item | `config.yml` → `game` |
| Resource delays, amounts, and nearby item caps | `config.yml` → `generators` |
| Item prices and quantities | `shop.yml` → `categories` |
| Team upgrade prices, levels, and effects | `upgrades.yml` → `upgrades` |
| Messages | `messages.yml` |
| Cosmetics and achievement targets | `cosmetics.yml`, `achievements.yml` |

For example, a shop entry uses:

```yaml
- item: WHITE_WOOL
  amount: 16
  cost-item: IRON_INGOT
  cost-amount: 4
```

White wool purchases are converted to the buyer's team color. Keep positive
amounts and valid material names. Keep shop category names distinct from other
plugin menu titles. See [UPGRADE_EXAMPLES.md](UPGRADE_EXAMPLES.md) for upgrade
definitions.

The current item menu displays up to 45 items per category. The upgrade menu
displays seven upgrade entries; configure menus within those display limits.

## 9. Permissions and player commands

| Permission | Purpose | Default |
| --- | --- | --- |
| `bedwars.setup` | Create and configure arenas | Operator |
| `bedwars.admin` | Start, stop, and administrative controls | Operator |
| `bedwars.sign.create` | Register join signs | Operator |
| `bedwars.vote.admin` | Start/end votes and force an arena | Operator |
| `bedwars.join`, `bedwars.leave` | Join by command/sign and leave | Everyone |
| `bedwars.team`, `bedwars.spectate` | Team selector and spectating | Everyone |
| `bedwars.stats`, `bedwars.list` | Statistics and arena listing | Everyone |
| `bedwars.queue`, `bedwars.vote` | Map queue and voting | Everyone |
| `bedwars.party` | Party commands | Everyone |
| `bedwars.cosmetics`, `bedwars.achievements` | Player menus | Everyone |

Common player commands:

```text
/bw join skyislands
/bw leave
/bw team
/bw spectate skyislands
/bw stats
/bw queue
/bw vote
/bw party invite PlayerName
/bw party accept PlayerName
/bw cosmetics
/bw achievements
```

Local voting selects eligible empty waiting arenas that can fit the queued
players. Test direct joining first, then test `/bw queue` with enough accounts.

### Party workflow

The leader invites a player with `/bw party invite PlayerName`. That player
accepts with `/bw party accept LeaderName`. The leader then uses
`/bw join skyislands` to join all online members together, provided the arena has
enough room and no member is in a different match. A normal party member cannot
start this group join. Join signs and map-voting queues currently join players
individually; use the leader's `/bw join` command for a group.

`/bw party promote PlayerName` transfers leadership; `/bw party kick PlayerName`
removes another member. `/bw party leave` leaves the party and transfers leadership
when necessary. `/bw party disband` is for the leader. A leader cannot kick
themselves. Invites expire and are invalidated when the party is disbanded or
the inviting leader changes.

Match leaving is separate from party membership: a member's `/bw leave` removes
only that member from the match. A leader's `/bw leave` also removes online
members who are in that same match. Membership is saved in this server's SQLite
database; this is not a shared cross-server party database.

### Default achievements and cosmetics

The achievements menu starts at slot 10 (Bukkit slots are zero-based) in this
configuration order:

| Achievement | Requirement |
| --- | --- |
| First Blood | 1 kill |
| Killer I | 10 kills |
| Final Blow | 1 final kill |
| Bed Breaker | 1 enemy bed broken |
| Bed Breaker II | 10 enemy beds broken |
| First Victory | 1 win |
| Veteran | 25 games played |

Final kills also count as kills. Progress is retained across reconnects and
restarts, and unlocked achievements stop incrementing at their target.

The default cosmetics menu places kill effects at slots 10–20: none, firework,
heart, smoke, explosion, flame, soul, portal, enchant, music, totem. Bed effects
occupy slots 28–37: none, lightning, firework, explosion, flame, soul, portal,
enchant, music, totem. Each effect has a default `permission` in `cosmetics.yml`;
selecting and playing it both require that permission. Disabled effects
cannot play. Fireworks are tagged so their damage is cancelled; lightning uses
the visual-only lightning effect and explosion cosmetics use particles.

To let everyone test enabled effects without granting individual effect
permissions, add or change this section in `plugins/BedWars/cosmetics.yml`:

```yaml
testing:
  unlock-all: true
```

Restart the server, then use `/bw cosmetics`. This bypass applies to selection,
menu lock labels, and playing effects. It defaults to `false`; set it back to
`false` and restart to restore permission checks. Individual `enabled: false`
effects and the global cosmetics enable switch are still respected. The menu
command still requires `bedwars.cosmetics`, which is granted to everyone by default.

Default kill permissions are `bedwars.cosmetics.kill.firework`,
`bedwars.cosmetics.kill.heart`, `bedwars.cosmetics.kill.smoke`, and
`bedwars.cosmetics.kill.explosion`. Bed permissions are
`bedwars.cosmetics.bed.lightning`, `bedwars.cosmetics.bed.firework`, and
`bedwars.cosmetics.bed.explosion`. These default to operators; grant them to
players or groups through your permissions plugin. The `.kill.none` and
`.bed.none` options default to everyone. Testing unlock-all bypasses all of these
effect permissions. Set any effect's `permission` to `""` to make it public
without testing mode. Existing server configuration files are not overwritten
by a plugin update: copy the permission fields from the bundled `cosmetics.yml`
into your server's file when upgrading an older installation.

The flame, soul, portal, enchant, music, and totem effects each have permissions
`bedwars.cosmetics.kill.<id>` and `bedwars.cosmetics.bed.<id>`, also defaulting to
operators. They use particles only, so they do not ignite blocks, create portals,
consume totems, or cause damage. Music shows note particles; it does not play audio.

Missing bundled effect entries are added to existing `cosmetics.yml` files on
startup. Existing effect settings, permissions, and the testing flag are preserved.
To hide an effect permanently, set its `enabled` field to `false` rather than
deleting its entry. Restart after updating the plugin, then open `/bw cosmetics`.

To create your own particle-based cosmetics, see
[CUSTOM_COSMETICS.md](CUSTOM_COSMETICS.md). It includes copyable kill and bed
effects, colored dust, permissions, testing mode, and menu placement.

## 10. Optional network mode

Finish the local setup before enabling a network integration. Choose one
`network.mode`; a nonempty mode takes precedence over the legacy `enabled` flags.

### BungeeCord

Merge these values into `config.yml` on the BedWars server:

```yaml
network:
  mode: "bungee"

bungeecord:
  enabled: true
  lobby-server: "lobby"
  send-to-lobby-on-end: true
  send-to-lobby-on-leave: false
  lobby-send-delay: 3
```

The proxy must already have a reachable server named `lobby`. This plugin sends
connection messages; it does not create or configure the proxy or lobby server.

At round cleanup, command-joined players teleport to the local arena lobby
before the optional delayed proxy transfer. Sign-joined players return to their
click locations and do not receive the automatic end-of-round lobby transfer.

See [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md) for additional integration details.

### CloudNet

CloudNet mode requires the supported Bridge classes to be available on the
server. Merge settings such as:

```yaml
network:
  mode: "cloudnet"

cloudnet:
  enabled: true
  one-arena-per-server: true
  use-for-map-voting: true
  lobby-service: "Lobby-1"
  game-services:
    - "bw-1"
    - "bw-2"
  arena-service-map:
    skyislands: "bw-1"
```

The named services must already exist in CloudNet. The integration uses
reflection against the Bridge API, so verify actual connections in your
environment. CloudNet voting selects configured services rather than local
empty arenas. Local join signs still join the local registered arena.

## 11. Persistence, maintenance, and existing arenas

The current database implementation uses SQLite in `bedwars.db`. Changing
`database.type` to `mysql` does not enable a MySQL implementation, even though
the default configuration contains MySQL fields.

For backups, stop the server and copy the plugin folder and arena world together.
Preserve any SQLite `bedwars.db-wal` and `bedwars.db-shm` files present in the
folder. Do not delete the database when updating the plugin JAR.

There is no setup command to load an existing arena into an editable session.
To adjust a saved arena, stop the server, back up `arenas.yml`, edit the relevant
positions, and restart. Setup currently writes position coordinates without
view direction:

```text
worldName,x,y,z
```

The loader also accepts the following format when you need to set a spawn's
view direction manually:

```text
worldName,x,y,z,yaw,pitch
```

Bed coordinates must identify an actual head or foot block. Older versions
saved the administrator's position for `setbed`; those coordinates may need
correction. A complete replacement arena can instead be registered with the
setup commands under a new name.

## 12. Troubleshooting

| Symptom | Check |
| --- | --- |
| Plugin does not enable | Console errors, server/Java compatibility, SQLite library loading |
| Arena will not save | Lobby, spectator spawn, two team spawns, and matching bed entries |
| Bed setup is rejected | Look at a complete supported bed within five blocks |
| Enemy bed cannot be broken | Exact bed coordinates, correct team, and `world.allow-block-breaking` |
| Generators do not spawn | Correct name prefix, enabled resource, solid drop point, match running |
| No shop villagers | Both shop locations registered for each team; match running |
| Countdown never starts | Arena minimum, current player count, and arena state |
| No map-vote choices | Empty waiting arenas whose player limits fit the queue |
| Arena blocks/beds stay changed | `world.reset-on-end: true`; inspect errors during cleanup |
| Players return to an unexpected lobby | Check the arena's lobby position and whether they used a sign |
| Proxy transfer fails | Selected network mode, service/server names, Bridge availability, console logs |

For detailed diagnostics, temporarily enable:

```yaml
debug:
  enabled: true
  verbose-logging: false
  show-generator-spawns: false
```

Restart, reproduce the problem, and inspect the server console or
`logs/latest.log`. Turn debug logging off again after diagnosis.
