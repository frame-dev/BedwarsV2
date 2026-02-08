package ch.framedev.bedwars.game;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.stats.PlayerStats;
import ch.framedev.bedwars.team.Team;
import ch.framedev.bedwars.team.TeamColor;
import ch.framedev.bedwars.generators.ResourceGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Represents a single BedWars game instance
 */
public class Game {

    private final BedWarsPlugin plugin;
    private final Arena arena;
    private final Map<UUID, GamePlayer> players;
    private final Map<UUID, GamePlayer> participants;
    private final Set<UUID> spectators;
    private final Map<TeamColor, Team> teams;
    private final List<ResourceGenerator> generators;
    private final WorldResetManager worldResetManager;
    private final Map<TeamColor, List<UUID>> dragonBuffs;
    private GameState state;
    private int countdown;
    private BukkitRunnable countdownTask;
    private BukkitRunnable gameTask;
    private BukkitRunnable healPoolTask;

    public Game(BedWarsPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.players = new HashMap<>();
        this.participants = new HashMap<>();
        this.spectators = new HashSet<>();
        this.teams = new HashMap<>();
        this.generators = new ArrayList<>();
        this.worldResetManager = new WorldResetManager();
        this.dragonBuffs = new HashMap<>();
        this.state = GameState.WAITING;

        initializeTeams();
        initializeGenerators();
        plugin.getDebugLogger().debug("Game initialized for arena: " + arena.getName()
                + ", teams=" + teams.size() + ", generators=" + generators.size());
    }

    private void initializeTeams() {
        for (TeamColor color : TeamColor.values()) {
            if (arena.getTeamSpawn(color) != null) {
                teams.put(color, new Team(color, arena.getTeamSpawn(color), arena.getBedLocation(color)));
            }
        }
        plugin.getDebugLogger().debug("Teams initialized for arena: " + arena.getName()
                + ", count=" + teams.size());
    }

    private void initializeGenerators() {
        FileConfiguration config = plugin.getConfig();

        boolean ironEnabled = config.getBoolean("generators.iron.enabled", true);
        boolean goldEnabled = config.getBoolean("generators.gold.enabled", true);
        boolean diamondEnabled = config.getBoolean("generators.diamond.enabled", true);
        boolean emeraldEnabled = config.getBoolean("generators.emerald.enabled", true);

        int ironTier1Delay = secondsToTicks(config.getDouble("generators.iron.base-delay", 1));
        int ironTier2Delay = secondsToTicks(config.getDouble("generators.iron.upgraded-delay", 1));
        int ironSpawnAmount = config.getInt("generators.iron.spawn-amount", 1);
        int ironMaxStack = config.getInt("generators.iron.max-stack", 64);

        int goldTier1Delay = secondsToTicks(config.getDouble("generators.gold.base-delay", 8));
        int goldTier2Delay = secondsToTicks(config.getDouble("generators.gold.upgraded-delay", 8));
        int goldSpawnAmount = config.getInt("generators.gold.spawn-amount", 1);
        int goldMaxStack = config.getInt("generators.gold.max-stack", 32);

        int diamondTier1Delay = secondsToTicks(config.getDouble("generators.diamond.tier1-delay", 30));
        int diamondTier2Delay = secondsToTicks(config.getDouble("generators.diamond.tier2-delay", 20));
        int diamondSpawnAmount = config.getInt("generators.diamond.spawn-amount", 1);
        int diamondMaxStack = config.getInt("generators.diamond.max-stack", 16);

        int emeraldTier1Delay = secondsToTicks(config.getDouble("generators.emerald.tier1-delay", 60));
        int emeraldTier2Delay = secondsToTicks(config.getDouble("generators.emerald.tier2-delay", 40));
        int emeraldSpawnAmount = config.getInt("generators.emerald.spawn-amount", 1);
        int emeraldMaxStack = config.getInt("generators.emerald.max-stack", 8);

        // Add team generators
        for (Team team : teams.values()) {
            if (ironEnabled) {
                generators.add(new ResourceGenerator(team.getSpawnLocation(), ResourceGenerator.ResourceType.IRON, 1,
                        ironTier1Delay, ironTier2Delay, ironSpawnAmount, ironMaxStack));
            }
            if (goldEnabled) {
                generators.add(new ResourceGenerator(team.getSpawnLocation().clone().add(2, 0, 0),
                        ResourceGenerator.ResourceType.GOLD, 1, goldTier1Delay, goldTier2Delay, goldSpawnAmount,
                        goldMaxStack));
            }
        }

        // Add arena-configured generators by name prefix
        for (Map.Entry<String, org.bukkit.Location> entry : arena.getGenerators().entrySet()) {
            ResourceGenerator.ResourceType type = parseGeneratorType(entry.getKey());
            if (type == null) {
                continue;
            }

            if (type == ResourceGenerator.ResourceType.DIAMOND && !diamondEnabled) {
                continue;
            }
            if (type == ResourceGenerator.ResourceType.EMERALD && !emeraldEnabled) {
                continue;
            }
            if (type == ResourceGenerator.ResourceType.IRON && !ironEnabled) {
                continue;
            }
            if (type == ResourceGenerator.ResourceType.GOLD && !goldEnabled) {
                continue;
            }

            int tier1Delay;
            int tier2Delay;
            int spawnAmount;
            int maxStack;

            switch (type) {
                case IRON -> {
                    tier1Delay = ironTier1Delay;
                    tier2Delay = ironTier2Delay;
                    spawnAmount = ironSpawnAmount;
                    maxStack = ironMaxStack;
                }
                case GOLD -> {
                    tier1Delay = goldTier1Delay;
                    tier2Delay = goldTier2Delay;
                    spawnAmount = goldSpawnAmount;
                    maxStack = goldMaxStack;
                }
                case DIAMOND -> {
                    tier1Delay = diamondTier1Delay;
                    tier2Delay = diamondTier2Delay;
                    spawnAmount = diamondSpawnAmount;
                    maxStack = diamondMaxStack;
                }
                case EMERALD -> {
                    tier1Delay = emeraldTier1Delay;
                    tier2Delay = emeraldTier2Delay;
                    spawnAmount = emeraldSpawnAmount;
                    maxStack = emeraldMaxStack;
                }
                default -> {
                    continue;
                }
            }

            generators.add(new ResourceGenerator(entry.getValue(), type, 1, tier1Delay, tier2Delay, spawnAmount,
                    maxStack));
        }

        plugin.getDebugLogger().debug("Generators initialized for arena: " + arena.getName()
                + ", count=" + generators.size());
    }

    public void addPlayer(Player player) {
        plugin.getDebugLogger().debug("Add player requested: " + player.getName()
                + ", arena=" + arena.getName() + ", state=" + state);
        if (state == GameState.RUNNING || state == GameState.ENDING) {
            plugin.getMessageManager().sendMessage(player, "command.game-already-running");
            return;
        }

        if (players.size() >= arena.getMaxPlayers()) {
            plugin.getMessageManager().sendMessage(player, "command.game-full");
            return;
        }

        GamePlayer gamePlayer = new GamePlayer(player);
        players.put(player.getUniqueId(), gamePlayer);
        participants.put(player.getUniqueId(), gamePlayer);

        // Assign to team with least players
        Team team = getSmallestTeam();
        if (team != null) {
            team.addPlayer(gamePlayer);
            gamePlayer.setTeam(team);
            plugin.getDebugLogger().debug("Assigned team: " + player.getName() + " -> " + team.getColor().name());
        }

        plugin.getGameManager().addPlayerToGame(player, this);

        // Teleport to lobby
        player.teleport(arena.getLobbySpawn());

        broadcast("game.player-joined", player.getName(), players.size(), arena.getMaxPlayers());

        if (players.size() >= arena.getMinPlayers() && state == GameState.WAITING) {
            startCountdown();
        }
    }

    public void removePlayer(Player player) {
        GamePlayer gamePlayer = players.remove(player.getUniqueId());
        plugin.getDebugLogger().debug("Remove player: " + player.getName()
                + ", arena=" + arena.getName() + ", state=" + state);
        if (gamePlayer != null && gamePlayer.getTeam() != null) {
            gamePlayer.getTeam().removePlayer(gamePlayer);
        }

        participants.remove(player.getUniqueId());

        plugin.getGameManager().removePlayerFromGame(player);

        broadcast("game.player-left", player.getName());

        boolean sendOnLeave = plugin.getConfig().getBoolean("bungeecord.send-to-lobby-on-leave", false);
        if (sendOnLeave && (plugin.getBungeeManager().isEnabled()
                || (plugin.getCloudNetManager() != null && plugin.getCloudNetManager().isEnabled()))) {
            sendToLobbyDelayed(player);
        }

        if (state == GameState.RUNNING) {
            checkWinCondition();
        } else if (players.size() < arena.getMinPlayers() && state == GameState.STARTING) {
            cancelCountdown();
        }
    }

    private void startCountdown() {
        state = GameState.STARTING;
        countdown = plugin.getConfig().getInt("game.countdown-time", 30);
        List<Integer> broadcastIntervals = plugin.getConfig().getIntegerList("game.countdown-broadcast-intervals");

        plugin.getDebugLogger().debug("Countdown started: arena=" + arena.getName()
                + ", seconds=" + countdown);

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown <= 0) {
                    startGame();
                    cancel();
                } else if (shouldBroadcastCountdown(countdown, broadcastIntervals)) {
                    broadcast("game.countdown", countdown);
                }
                countdown--;
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        state = GameState.WAITING;
        broadcast("game.countdown-cancelled");
        plugin.getDebugLogger().debug("Countdown cancelled: arena=" + arena.getName());
    }

    /**
     * Force start the game, bypassing countdown and minimum player requirements
     */
    public void forceStart() {
        if (state == GameState.STARTING && countdownTask != null) {
            // Cancel countdown and start immediately
            countdownTask.cancel();
            countdownTask = null;
        }
        startGame();
    }

    private void startGame() {
        state = GameState.RUNNING;
        broadcast("game.game-started");
        plugin.getDebugLogger().debug("Game started: arena=" + arena.getName()
                + ", players=" + players.size());

        // Teleport players to their team spawns
        for (GamePlayer gamePlayer : players.values()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && gamePlayer.getTeam() != null) {
                player.teleport(gamePlayer.getTeam().getSpawnLocation());
                player.getInventory().clear();
                gamePlayer.giveTeamArmor();
                giveStartingItem(player);
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }
        }

        // Start resource generators
        for (ResourceGenerator generator : generators) {
            generator.start(plugin);
        }
        plugin.getDebugLogger().debug("Resource generators started: count=" + generators.size());

        // Start game timer
        startGameTimer();

        // Start heal pool task
        startHealPoolTask();
    }

    private void startGameTimer() {
        int diamondUpgradeTime = plugin.getConfig().getInt("game.diamond-upgrade-time", 720);
        int emeraldUpgradeTime = plugin.getConfig().getInt("game.emerald-upgrade-time", 1440);
        boolean[] upgraded = new boolean[] { false, false };

        plugin.getDebugLogger().debug("Game timer started: diamondUpgrade=" + diamondUpgradeTime
                + "s, emeraldUpgrade=" + emeraldUpgradeTime + "s");

        gameTask = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                elapsed++;

                // Diamond generators upgrade at 12 minutes
                if (!upgraded[0] && diamondUpgradeTime > 0 && elapsed >= diamondUpgradeTime) {
                    upgraded[0] = true;
                    upgradeGenerators(ResourceGenerator.ResourceType.DIAMOND);
                    broadcast("game.diamond-upgrade");
                    plugin.getDebugLogger().debug("Diamond generators upgraded: arena=" + arena.getName());
                }

                // Emerald generators upgrade at 24 minutes
                if (!upgraded[1] && emeraldUpgradeTime > 0 && elapsed >= emeraldUpgradeTime) {
                    upgraded[1] = true;
                    upgradeGenerators(ResourceGenerator.ResourceType.EMERALD);
                    broadcast("game.emerald-upgrade");
                    plugin.getDebugLogger().debug("Emerald generators upgraded: arena=" + arena.getName());
                }
            }
        };
        gameTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void handlePlayerDeath(Player player) {
        GamePlayer gamePlayer = players.get(player.getUniqueId());
        if (gamePlayer == null)
            return;

        plugin.getDebugLogger().debug("Handle player death: " + player.getName()
                + ", bedAlive=" + (gamePlayer.getTeam() != null && gamePlayer.getTeam().isBedAlive()));

        gamePlayer.addDeath();

        Team team = gamePlayer.getTeam();
        if (team != null && team.isBedAlive()) {
            // Respawn player
            new BukkitRunnable() {
                int respawnTime = plugin.getConfig().getInt("game.respawn-time", 5);

                @Override
                public void run() {
                    if (respawnTime <= 0) {
                        player.spigot().respawn();
                        player.teleport(team.getSpawnLocation());
                        gamePlayer.giveTeamArmor();
                        giveStartingItem(player);
                        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                        plugin.getMessageManager().sendMessage(player, "game.respawned");
                        plugin.getDebugLogger().debug("Player respawned: " + player.getName());
                        cancel();
                    } else {
                        plugin.getMessageManager().sendMessage(player, "game.respawning", respawnTime);
                        respawnTime--;
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
        } else {
            // Bed is destroyed - make player a spectator
            gamePlayer.setEliminated(true);

            // Remove from active players
            Team playerTeam = gamePlayer.getTeam();
            if (playerTeam != null) {
                playerTeam.removePlayer(gamePlayer);
            }
            // Add as spectator
            spectators.add(player.getUniqueId());
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(arena.getSpectatorSpawn());

            // Clear inventory and make invisible to active players
            player.getInventory().clear();
            for (GamePlayer gp : players.values()) {
                Player activePlayer = Bukkit.getPlayer(gp.getUuid());
                if (activePlayer != null) {
                    activePlayer.hidePlayer(plugin, player);
                }
            }

            broadcast("game.player-eliminated", player.getName());
            plugin.getMessageManager().sendMessage(player, "game.now-spectating");

            plugin.getDebugLogger().debug("Player eliminated: " + player.getName());

            checkWinCondition();
        }
    }

    private void checkWinCondition() {
        List<Team> aliveTeams = new ArrayList<>();
        for (Team team : teams.values()) {
            if (team.hasAlivePlayers()) {
                aliveTeams.add(team);
            }
        }

        if (aliveTeams.size() == 1) {
            Team winningTeam = aliveTeams.get(0);
            endGame(winningTeam);
        } else if (aliveTeams.isEmpty()) {
            endGame(null);
        }
    }

    public void endGame(Team winningTeam) {
        state = GameState.ENDING;
        plugin.getDebugLogger().debug("Game ending: arena=" + arena.getName()
                + ", winner=" + (winningTeam != null ? winningTeam.getColor().name() : "none"));

        if (gameTask != null) {
            gameTask.cancel();
        }

        // Stop generators
        for (ResourceGenerator generator : generators) {
            generator.stop();
        }

        stopHealPoolTask();
        clearDragonBuffs();

        // Update player statistics
        for (GamePlayer gamePlayer : participants.values()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null) {
                PlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());

                if (winningTeam != null && gamePlayer.getTeam() == winningTeam) {
                    stats.addWin();
                    if (plugin.getAchievementsManager() != null) {
                        plugin.getAchievementsManager().recordWin(player.getUniqueId());
                    }
                } else {
                    stats.addLoss();
                }

                if (plugin.getAchievementsManager() != null) {
                    plugin.getAchievementsManager().recordGamePlayed(player.getUniqueId());
                }

                stats.setKills(stats.getKills() + gamePlayer.getKills());
                stats.setDeaths(stats.getDeaths() + gamePlayer.getDeaths());
                stats.setFinalKills(stats.getFinalKills() + gamePlayer.getFinalKills());
                stats.setBedsBroken(stats.getBedsBroken() + gamePlayer.getBedsBroken());

                // Save stats to database
                plugin.getStatsManager().savePlayerStats(player.getUniqueId(), player.getName());
            }
        }

        if (winningTeam != null) {
            broadcast("game.winner-border");
            broadcast("game.winner-announcement",
                    winningTeam.getColor().getChatColor() + winningTeam.getColor().name());
            broadcast("game.winner-border");
        } else {
            broadcast("game.no-winner");
        }

        // Teleport players back to lobby after delay
        new BukkitRunnable() {
            @Override
            public void run() {
                for (GamePlayer gamePlayer : players.values()) {
                    Player player = Bukkit.getPlayer(gamePlayer.getUuid());
                    if (player != null) {
                        boolean sendOnEnd = plugin.getConfig().getBoolean("bungeecord.send-to-lobby-on-end", true);
                        if (sendOnEnd && (plugin.getBungeeManager().isEnabled()
                                || (plugin.getCloudNetManager() != null && plugin.getCloudNetManager().isEnabled()))) {
                            sendToLobbyDelayed(player);
                        } else {
                            player.teleport(arena.getLobbySpawn());
                            player.getInventory().clear();
                        }
                    }
                }

                // Reset game
                resetGame();
            }
        }.runTaskLater(plugin, 100L);
    }

    /**
     * Send player to lobby server with delay
     */
    private void sendToLobbyDelayed(Player player) {
        int delay = plugin.getConfig().getInt("bungeecord.lobby-send-delay", 3);

        plugin.getDebugLogger().debug("Sending player to lobby (delayed): " + player.getName()
                + ", delay=" + delay + "s");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    if (plugin.getCloudNetManager() != null && plugin.getCloudNetManager().isEnabled()) {
                        plugin.getCloudNetManager().connectToLobby(player);
                    } else {
                        plugin.getMessageManager().sendMessage(player, "game.sending-to-lobby");
                        plugin.getBungeeManager().sendPlayerToLobby(player);
                    }
                }
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    private void resetGame() {
        // Reset the world to original state
        if (plugin.getConfig().getBoolean("world.reset-on-end", true)) {
            int blocksReset = worldResetManager.getBlockChangeCount();
            if (blocksReset > 0) {
                worldResetManager.resetWorld();
                broadcast("game.world-reset", blocksReset);
                plugin.getDebugLogger().debug("World reset completed: blocks=" + blocksReset);
            }
        }

        stopHealPoolTask();
        clearDragonBuffs();
        players.clear();
        participants.clear();
        state = GameState.WAITING;

        for (Team team : teams.values()) {
            team.reset();
        }

        plugin.getDebugLogger().debug("Game reset to WAITING: arena=" + arena.getName());
    }

    private Team getSmallestTeam() {
        Team smallest = null;
        int minSize = Integer.MAX_VALUE;

        for (Team team : teams.values()) {
            if (team.getPlayers().size() < minSize) {
                smallest = team;
                minSize = team.getPlayers().size();
            }
        }

        return smallest;
    }

    public void broadcast(String messageKey, Object... args) {
        String message = plugin.getMessageManager().getMessage(messageKey, args);
        for (GamePlayer gamePlayer : players.values()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    public GameState getState() {
        return state;
    }

    public Arena getArena() {
        return arena;
    }

    public Map<UUID, GamePlayer> getPlayers() {
        return players;
    }

    public Map<TeamColor, Team> getTeams() {
        return teams;
    }

    public GamePlayer getGamePlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public GamePlayer getGamePlayer(UUID uuid) {
        return players.get(uuid);
    }

    public WorldResetManager getWorldResetManager() {
        return worldResetManager;
    }

    public void addSpectator(Player player) {
        if (state != GameState.RUNNING && state != GameState.ENDING) {
            plugin.getMessageManager().sendMessage(player, "spectator.only-during-game");
            return;
        }

        spectators.add(player.getUniqueId());
        plugin.getGameManager().addPlayerToGame(player, this);

        plugin.getDebugLogger().debug("Spectator added: " + player.getName()
                + ", arena=" + arena.getName());

        // Set spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(arena.getSpectatorSpawn());
        plugin.getMessageManager().sendMessage(player, "spectator.now-spectating");

        // Make them invisible to players (they can still see)
        for (GamePlayer gp : players.values()) {
            Player gamePlayer = Bukkit.getPlayer(gp.getUuid());
            if (gamePlayer != null) {
                gamePlayer.hidePlayer(plugin, player);
            }
        }
    }

    public void removeSpectator(Player player) {
        if (spectators.remove(player.getUniqueId())) {
            plugin.getGameManager().removePlayerFromGame(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(arena.getLobbySpawn());
            plugin.getMessageManager().sendMessage(player, "spectator.stopped-spectating");

            plugin.getDebugLogger().debug("Spectator removed: " + player.getName()
                    + ", arena=" + arena.getName());

            // Make them visible again
            for (GamePlayer gp : players.values()) {
                Player gamePlayer = Bukkit.getPlayer(gp.getUuid());
                if (gamePlayer != null) {
                    gamePlayer.showPlayer(plugin, player);
                }
            }
        }
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public void applySpecialUpgrade(Team team, String upgradeId) {
        if (team == null || upgradeId == null) {
            return;
        }

        plugin.getDebugLogger().debug("Apply special upgrade: " + upgradeId
                + " for team=" + team.getColor().name());

        String id = upgradeId.toLowerCase(Locale.ROOT);
        if (id.equals("heal-pool")) {
            startHealPoolTask();
            return;
        }

        if (id.equals("dragon-buff")) {
            spawnDragonBuff(team);
        }
    }

    private void startHealPoolTask() {
        if (healPoolTask != null) {
            return;
        }

        double radius = plugin.getConfig().getDouble("upgrades.heal-pool.radius", 6.0);
        int amplifier = plugin.getConfig().getInt("upgrades.heal-pool.amplifier", 0);
        int durationTicks = plugin.getConfig().getInt("upgrades.heal-pool.duration-ticks", 60);

        healPoolTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Team team : teams.values()) {
                    if (!team.getUpgrades().hasHealPool()) {
                        continue;
                    }

                    Location base = getTeamBaseLocation(team);
                    if (base == null) {
                        continue;
                    }

                    for (GamePlayer gp : team.getPlayers()) {
                        Player player = Bukkit.getPlayer(gp.getUuid());
                        if (player == null) {
                            continue;
                        }

                        if (player.getLocation().getWorld() != base.getWorld()) {
                            continue;
                        }

                        if (player.getLocation().distance(base) <= radius) {
                            player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.REGENERATION,
                                    durationTicks,
                                    amplifier,
                                    false,
                                    false));
                        }
                    }
                }
            }
        };

        healPoolTask.runTaskTimer(plugin, 0L, 20L);
        plugin.getDebugLogger().debug("Heal pool task started: arena=" + arena.getName());
    }

    private void stopHealPoolTask() {
        if (healPoolTask != null) {
            healPoolTask.cancel();
            healPoolTask = null;
            plugin.getDebugLogger().debug("Heal pool task stopped: arena=" + arena.getName());
        }
    }

    private void spawnDragonBuff(Team team) {
        if (team == null || team.getUpgrades() == null) {
            return;
        }

        if (dragonBuffs.containsKey(team.getColor())) {
            return;
        }

        Location base = getTeamBaseLocation(team);
        if (base == null) {
            return;
        }

        World world = base.getWorld();
        if (world == null) {
            return;
        }

        int count = plugin.getConfig().getInt("upgrades.dragon-buff.count", 2);
        List<UUID> ids = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Location spawn = base.clone().add(i == 0 ? 5 : -5, 12, 0);
            Entity entity = world.spawnEntity(spawn, EntityType.ENDER_DRAGON);
            if (entity instanceof EnderDragon dragon) {
                dragon.setCustomName(team.getColor().getChatColor() + team.getColor().name() + " Dragon");
                dragon.setCustomNameVisible(true);
                dragon.setPersistent(true);
                dragon.setRemoveWhenFarAway(false);
                ids.add(dragon.getUniqueId());
            }
        }

        if (!ids.isEmpty()) {
            dragonBuffs.put(team.getColor(), ids);
            plugin.getDebugLogger().debug("Dragon buff spawned: team=" + team.getColor().name()
                    + ", count=" + ids.size());
        }
    }

    private void clearDragonBuffs() {
        for (List<UUID> ids : dragonBuffs.values()) {
            for (UUID id : ids) {
                Entity entity = Bukkit.getEntity(id);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        dragonBuffs.clear();
        plugin.getDebugLogger().debug("Dragon buffs cleared: arena=" + arena.getName());
    }

    private Location getTeamBaseLocation(Team team) {
        if (team.getBedLocation() != null) {
            return team.getBedLocation();
        }
        return team.getSpawnLocation();
    }

    private boolean shouldBroadcastCountdown(int value, List<Integer> intervals) {
        if (intervals != null && !intervals.isEmpty()) {
            return intervals.contains(value);
        }
        return value <= 10 || value % 10 == 0;
    }

    private void upgradeGenerators(ResourceGenerator.ResourceType type) {
        for (ResourceGenerator generator : generators) {
            if (generator.getType() == type) {
                generator.upgrade();
            }
        }
        plugin.getDebugLogger().debug("Generators upgraded: type=" + type + ", arena=" + arena.getName());
    }

    private ResourceGenerator.ResourceType parseGeneratorType(String name) {
        if (name == null) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.startsWith("iron")) {
            return ResourceGenerator.ResourceType.IRON;
        }
        if (lower.startsWith("gold")) {
            return ResourceGenerator.ResourceType.GOLD;
        }
        if (lower.startsWith("diamond")) {
            return ResourceGenerator.ResourceType.DIAMOND;
        }
        if (lower.startsWith("emerald")) {
            return ResourceGenerator.ResourceType.EMERALD;
        }
        return null;
    }

    private int secondsToTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0));
    }

    private void giveStartingItem(Player player) {
        String itemName = plugin.getConfig().getString("game.starting-item", "WOODEN_SWORD");
        String normalized = itemName == null ? "WOODEN_SWORD" : itemName;
        Material material = Material.matchMaterial(normalized);
        if (material == null && "WOOD_SWORD".equalsIgnoreCase(normalized)) {
            material = Material.WOODEN_SWORD;
        }
        if (material == null || material == Material.AIR) {
            return;
        }

        if (!player.getInventory().contains(material)) {
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(material, 1));
        }
    }
}
