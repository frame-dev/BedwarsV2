package ch.framedev.bedwars.game;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.generators.ResourceGenerator;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.shop.ShopType;
import ch.framedev.bedwars.stats.PlayerStats;
import ch.framedev.bedwars.team.Team;
import ch.framedev.bedwars.team.TeamColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Represents a single BedWars game instance
 * <p>
 * Notes:
 * - Keeps your existing structure but fixes common bugs/leaks:
 *   - prevents double countdown / double game start
 *   - cancels all tasks on end/reset (countdown, timer, heal pool, respawns)
 *   - clamps health safely
 *   - teleports/cleans up spectators as well
 *   - restores visibility after hidePlayer usage
 *   - safer villager spawning
 *   - caches common config values (countdown/respawn/upgrade times)
 */
public class Game {

    private final BedWarsPlugin plugin;
    private final Arena arena;

    /** All players who joined this game instance (includes eliminated/spectators). */
    private final Map<UUID, GamePlayer> players;

    /** Players who participated for stats (joiners). */
    private final Map<UUID, GamePlayer> participants;

    /** UUIDs of spectators (subset of players usually). */
    private final Set<UUID> spectators;

    private final Map<TeamColor, Team> teams;
    private final List<ResourceGenerator> generators;
    private final Map<TeamColor, Map<ShopType, UUID>> shopVillagers;
    private final WorldResetManager worldResetManager;

    private final Map<TeamColor, List<UUID>> dragonBuffs;

    private GameState state;
    private int countdown;
    private int gameElapsedSeconds;

    private BukkitRunnable countdownTask;
    private BukkitRunnable gameTask;
    private BukkitRunnable healPoolTask;

    /** Per-player respawn countdown tasks (so we can cancel on end/reset). */
    private final Map<UUID, BukkitRunnable> respawnTasks = new HashMap<>();

    /** Scoreboard (optional) */
    private GameScoreboard gameScoreboard;

    // Cached config values (set at countdown/game start)
    private int cfgCountdownSeconds;
    private int cfgRespawnSeconds;
    private int cfgDiamondUpgradeSeconds;
    private int cfgEmeraldUpgradeSeconds;

    public Game(BedWarsPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;

        this.players = new HashMap<>();
        this.participants = new HashMap<>();
        this.spectators = new HashSet<>();

        this.teams = new HashMap<>();
        this.generators = new ArrayList<>();
        this.shopVillagers = new HashMap<>();
        this.worldResetManager = new WorldResetManager();
        this.dragonBuffs = new HashMap<>();

        this.state = GameState.WAITING;

        initializeTeams();
        initializeGenerators();

        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            this.gameScoreboard = new GameScoreboard(plugin, this);
        }

        plugin.getDebugLogger().debug("Game initialized for arena: " + arena.getName()
                + ", teams=" + teams.size() + ", generators=" + generators.size());
    }

    /* --------------------------------------------------------------------- */
    /* Initialization                                                        */
    /* --------------------------------------------------------------------- */

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

        boolean onlyIronGold = config.getBoolean("generators.only-iron-gold", false);
        if (onlyIronGold) {
            diamondEnabled = false;
            emeraldEnabled = false;
        }

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

        // Add arena-configured generators by name prefix
        for (Map.Entry<String, Location> entry : arena.getGenerators().entrySet()) {
            ResourceGenerator.ResourceType type = parseGeneratorType(entry.getKey());
            if (type == null) continue;

            if (type == ResourceGenerator.ResourceType.DIAMOND && !diamondEnabled) continue;
            if (type == ResourceGenerator.ResourceType.EMERALD && !emeraldEnabled) continue;
            if (type == ResourceGenerator.ResourceType.IRON && !ironEnabled) continue;
            if (type == ResourceGenerator.ResourceType.GOLD && !goldEnabled) continue;

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

            generators.add(new ResourceGenerator(entry.getValue(), type, 1, tier1Delay, tier2Delay, spawnAmount, maxStack));
        }

        plugin.getDebugLogger().debug("Generators initialized for arena: " + arena.getName()
                + ", count=" + generators.size());
    }

    /* --------------------------------------------------------------------- */
    /* Join/Leave                                                            */
    /* --------------------------------------------------------------------- */

    public void addPlayer(Player player) {
        if (player == null) return;

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

        // Teleport to lobby and give lobby items
        player.teleport(arena.getLobbySpawn());
        giveLobbyItems(player);

        broadcast("game.player-joined", player.getName(), players.size(), arena.getMaxPlayers());

        if (gameScoreboard != null) {
            gameScoreboard.show(player);
            gameScoreboard.startUpdateTask();
        }

        // Start countdown if enough players
        if (players.size() >= arena.getMinPlayers() && state == GameState.WAITING) {
            startCountdown();
        }
    }

    public void removePlayer(Player player) {
        if (player == null) return;

        plugin.getDebugLogger().debug("Remove player: " + player.getName()
                + ", arena=" + arena.getName() + ", state=" + state);

        // Cancel respawn timer if any
        BukkitRunnable respawn = respawnTasks.remove(player.getUniqueId());
        if (respawn != null) respawn.cancel();

        GamePlayer gamePlayer = players.remove(player.getUniqueId());
        if (gamePlayer != null && gamePlayer.getTeam() != null) {
            gamePlayer.getTeam().removePlayer(gamePlayer);
        }

        spectators.remove(player.getUniqueId());
        participants.remove(player.getUniqueId());

        plugin.getGameManager().removePlayerFromGame(player);

        if (gameScoreboard != null) {
            gameScoreboard.hide(player);
        }

        broadcast("game.player-left", player.getName());

        // restore visibility (important if they were hidden as spectator/eliminated)
        restoreVisibility(player);

        boolean sendOnLeave = plugin.getConfig().getBoolean("bungeecord.send-to-lobby-on-leave", false);
        if (sendOnLeave && (plugin.getBungeeManager().isEnabled()
                || (plugin.getCloudNetManager() != null && plugin.getCloudNetManager().isEnabled()))) {
            sendToLobbyDelayed(player);
        } else {
            player.teleport(arena.getLobbySpawn());
        }

        if (state == GameState.RUNNING) {
            checkWinCondition();
        } else if (players.size() < arena.getMinPlayers() && state == GameState.STARTING) {
            cancelCountdown();
        }
    }

    /* --------------------------------------------------------------------- */
    /* Countdown / Start                                                     */
    /* --------------------------------------------------------------------- */

    private void startCountdown() {
        if (state != GameState.WAITING) return;
        if (countdownTask != null) return;

        state = GameState.STARTING;
        cfgCountdownSeconds = plugin.getConfig().getInt("game.countdown-time", 30);
        countdown = cfgCountdownSeconds;

        List<Integer> broadcastIntervals = plugin.getConfig().getIntegerList("game.countdown-broadcast-intervals");

        plugin.getDebugLogger().debug("Countdown started: arena=" + arena.getName()
                + ", seconds=" + countdown);

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (players.size() < arena.getMinPlayers()) {
                    cancelCountdown();
                    return;
                }

                if (countdown <= 0) {
                    cancel();
                    countdownTask = null;
                    startGame();
                    return;
                }

                if (shouldBroadcastCountdown(countdown, broadcastIntervals)) {
                    String color = getCountdownColorCode(countdown);
                    broadcast("game.countdown", countdown, color);
                    sendCountdownVisuals(countdown, color);
                }

                countdown--;
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void sendCountdownVisuals(int seconds, String colorCode) {
        String title = plugin.getMessageManager().getMessage("game.countdown-title", seconds, colorCode);
        String subtitle = plugin.getMessageManager().getMessage("game.countdown-subtitle", seconds, colorCode);
        String actionBar = plugin.getMessageManager().getMessage("game.countdown-actionbar", seconds, colorCode);

        for (GamePlayer gp : players.values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p == null) continue;

            p.sendTitle(title, subtitle, 5, 20, 5);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));
        }
    }

    private String getCountdownColorCode(int seconds) {
        if (seconds <= 5) return ChatColor.RED.toString();
        if (seconds <= 10) return ChatColor.YELLOW.toString();
        return ChatColor.GREEN.toString();
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (state == GameState.STARTING) {
            state = GameState.WAITING;
            broadcast("game.countdown-cancelled");
            plugin.getDebugLogger().debug("Countdown cancelled: arena=" + arena.getName());
        }
    }

    /** Force start the game (bypass countdown/min players). */
    public void forceStart() {
        if (state == GameState.RUNNING || state == GameState.ENDING) return;

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        startGame();
    }

    private void startGame() {
        if (state == GameState.RUNNING) return;

        state = GameState.RUNNING;

        cfgRespawnSeconds = plugin.getConfig().getInt("game.respawn-time", 5);
        cfgDiamondUpgradeSeconds = plugin.getConfig().getInt("game.diamond-upgrade-time", 720);
        cfgEmeraldUpgradeSeconds = plugin.getConfig().getInt("game.emerald-upgrade-time", 1440);

        broadcast("game.game-started");

        plugin.getDebugLogger().debug("Game started: arena=" + arena.getName()
                + ", players=" + players.size());

        // Teleport players to their team spawns
        for (GamePlayer gp : players.values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p == null || !p.isOnline()) continue;

            Team team = gp.getTeam();
            if (team == null) continue;

            p.teleport(team.getSpawnLocation());
            p.getInventory().clear();
            gp.giveTeamArmor();
            giveStartingItem(p);

            p.setGameMode(GameMode.SURVIVAL);
            setToMaxHealth(p);
        }

        // Start resource generators
        for (ResourceGenerator generator : generators) {
            generator.start(plugin);
        }

        spawnShopVillagers();
        startGameTimer();
        startHealPoolTask();

        if (gameScoreboard != null) {
            gameScoreboard.startUpdateTask();
        }
    }

    private void startGameTimer() {
        if (gameTask != null) return;

        final boolean[] upgraded = new boolean[]{false, false};

        gameTask = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (state != GameState.RUNNING) {
                    cancel();
                    gameTask = null;
                    return;
                }

                elapsed++;
                gameElapsedSeconds = elapsed;

                if (!upgraded[0] && cfgDiamondUpgradeSeconds > 0 && elapsed >= cfgDiamondUpgradeSeconds) {
                    upgraded[0] = true;
                    upgradeGenerators(ResourceGenerator.ResourceType.DIAMOND);
                    broadcast("game.diamond-upgrade");
                }

                if (!upgraded[1] && cfgEmeraldUpgradeSeconds > 0 && elapsed >= cfgEmeraldUpgradeSeconds) {
                    upgraded[1] = true;
                    upgradeGenerators(ResourceGenerator.ResourceType.EMERALD);
                    broadcast("game.emerald-upgrade");
                }
            }
        };
        gameTask.runTaskTimer(plugin, 0L, 20L);
    }

    /* --------------------------------------------------------------------- */
    /* Death / Win / End                                                     */
    /* --------------------------------------------------------------------- */

    public void handlePlayerDeath(Player player) {
        if (player == null) return;
        if (state != GameState.RUNNING) return;

        GamePlayer gp = players.get(player.getUniqueId());
        if (gp == null) return;

        gp.addDeath();

        Team team = gp.getTeam();
        boolean bedAlive = team != null && team.isBedAlive();

        // Cancel any previous respawn timer for this player
        BukkitRunnable existing = respawnTasks.remove(player.getUniqueId());
        if (existing != null) existing.cancel();

        if (bedAlive) {
            BukkitRunnable task = new BukkitRunnable() {
                int remaining = cfgRespawnSeconds;

                @Override
                public void run() {
                    if (state != GameState.RUNNING) {
                        cancel();
                        respawnTasks.remove(player.getUniqueId());
                        return;
                    }
                    if (!player.isOnline()) {
                        cancel();
                        respawnTasks.remove(player.getUniqueId());
                        return;
                    }

                    if (remaining <= 0) {
                        player.spigot().respawn();
                        player.teleport(team.getSpawnLocation());
                        gp.giveTeamArmor();
                        giveStartingItem(player);
                        setToMaxHealth(player);

                        plugin.getMessageManager().sendMessage(player, "game.respawned");

                        cancel();
                        respawnTasks.remove(player.getUniqueId());
                        return;
                    }

                    plugin.getMessageManager().sendMessage(player, "game.respawning", remaining);
                    remaining--;
                }
            };

            respawnTasks.put(player.getUniqueId(), task);
            task.runTaskTimer(plugin, 0L, 20L);
            return;
        }

        // Eliminated
        gp.setEliminated(true);

        if (team != null) {
            team.removePlayer(gp);
        }

        spectators.add(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(arena.getSpectatorSpawn());
        player.getInventory().clear();

        // Hide eliminated player from alive players
        for (GamePlayer other : players.values()) {
            Player alive = Bukkit.getPlayer(other.getUuid());
            if (alive != null) {
                alive.hidePlayer(plugin, player);
            }
        }

        broadcast("game.player-eliminated", player.getName());
        plugin.getMessageManager().sendMessage(player, "game.now-spectating");

        checkWinCondition();
    }

    private void checkWinCondition() {
        List<Team> aliveTeams = new ArrayList<>();
        for (Team t : teams.values()) {
            if (t.hasAlivePlayers()) aliveTeams.add(t);
        }

        if (aliveTeams.size() == 1) {
            endGame(aliveTeams.get(0));
        } else if (aliveTeams.isEmpty()) {
            endGame(null);
        }
    }

    public void endGame(Team winningTeam) {
        if (state == GameState.ENDING) return;

        state = GameState.ENDING;

        plugin.getDebugLogger().debug("Game ending: arena=" + arena.getName()
                + ", winner=" + (winningTeam != null ? winningTeam.getColor().name() : "none"));

        // Stop tasks
        if (gameScoreboard != null) gameScoreboard.stopUpdateTask();

        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (gameTask != null) { gameTask.cancel(); gameTask = null; }

        stopHealPoolTask();

        for (BukkitRunnable r : respawnTasks.values()) r.cancel();
        respawnTasks.clear();

        // Stop generators
        for (ResourceGenerator generator : generators) generator.stop();

        clearShopVillagers();
        clearDragonBuffs();

        // Update player stats
        for (GamePlayer gp : participants.values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p == null) continue;

            PlayerStats stats = plugin.getStatsManager().getPlayerStats(p.getUniqueId());

            if (winningTeam != null && gp.getTeam() == winningTeam) {
                stats.addWin();
                if (plugin.getAchievementsManager() != null) {
                    plugin.getAchievementsManager().recordWin(p.getUniqueId());
                }
            } else {
                stats.addLoss();
            }

            if (plugin.getAchievementsManager() != null) {
                plugin.getAchievementsManager().recordGamePlayed(p.getUniqueId());
            }

            stats.setKills(stats.getKills() + gp.getKills());
            stats.setDeaths(stats.getDeaths() + gp.getDeaths());
            stats.setFinalKills(stats.getFinalKills() + gp.getFinalKills());
            stats.setBedsBroken(stats.getBedsBroken() + gp.getBedsBroken());

            plugin.getStatsManager().savePlayerStats(p.getUniqueId(), p.getName());
        }

        if (winningTeam != null) {
            broadcast("game.winner-border");
            broadcast("game.winner-announcement", winningTeam.getColor().getChatColor() + winningTeam.getColor().name());
            broadcast("game.winner-border");
        } else {
            broadcast("game.no-winner");
        }

        // Teleport players back to lobby after delay, then reset game
        new BukkitRunnable() {
            @Override
            public void run() {
                // Teleport all current game players + spectators
                for (UUID id : new HashSet<>(players.keySet())) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) cleanupAndSendToLobby(p);
                }
                for (UUID id : new HashSet<>(spectators)) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) cleanupAndSendToLobby(p);
                }

                resetGame();
            }
        }.runTaskLater(plugin, 100L);
    }

    private void cleanupAndSendToLobby(Player player) {
        if (player == null) return;

        restoreVisibility(player);

        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);

        if (gameScoreboard != null) gameScoreboard.hide(player);

        boolean sendOnEnd = plugin.getConfig().getBoolean("bungeecord.send-to-lobby-on-end", true);
        if (sendOnEnd && (plugin.getBungeeManager().isEnabled()
                || (plugin.getCloudNetManager() != null && plugin.getCloudNetManager().isEnabled()))) {
            sendToLobbyDelayed(player);
        } else {
            player.teleport(arena.getLobbySpawn());
        }
    }

    /**
     * Restore visibility between this player and all online players.
     * (Important if you used hidePlayer when eliminating/spectating.)
     */
    private void restoreVisibility(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(plugin, player);
            player.showPlayer(plugin, other);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Lobby / Teams                                                         */
    /* --------------------------------------------------------------------- */

    private void resetGame() {
        if (plugin.getConfig().getBoolean("world.reset-on-end", true)) {
            int blocksReset = worldResetManager.getBlockChangeCount();
            if (blocksReset > 0) {
                worldResetManager.resetWorld();
                broadcast("game.world-reset", blocksReset);
                plugin.getDebugLogger().debug("World reset completed: blocks=" + blocksReset);
            }
        }

        clearShopVillagers();
        stopHealPoolTask();
        clearDragonBuffs();

        // Hide scoreboard from any remaining online players in this game
        if (gameScoreboard != null) {
            gameScoreboard.stopUpdateTask();
        }

        spectators.clear();
        players.clear();
        participants.clear();

        gameElapsedSeconds = 0;
        countdown = 0;

        for (Team team : teams.values()) {
            team.reset();
        }

        state = GameState.WAITING;
        plugin.getDebugLogger().debug("Game reset to WAITING: arena=" + arena.getName());
    }

    private void spawnShopVillagers() {
        clearShopVillagers();

        for (Team team : teams.values()) {
            for (ShopType type : ShopType.values()) {
                Location shopLocation = arena.getShopLocation(team.getColor(), type);
                if (shopLocation == null || shopLocation.getWorld() == null) continue;

                Villager villager = shopLocation.getWorld().spawn(shopLocation, Villager.class);
                villager.setProfession(type.getProfession());
                villager.setAI(false);
                villager.setInvulnerable(true);
                villager.setCollidable(false);
                villager.setSilent(true);
                villager.setCustomName(team.getColor().getChatColor() + type.getDisplayName());
                villager.setCustomNameVisible(true);

                shopVillagers.computeIfAbsent(team.getColor(), k -> new HashMap<>())
                        .put(type, villager.getUniqueId());
            }
        }
    }

    private void clearShopVillagers() {
        for (Map<ShopType, UUID> byType : shopVillagers.values()) {
            for (UUID id : byType.values()) {
                Entity entity = Bukkit.getEntity(id);
                if (entity != null) entity.remove();
            }
        }
        shopVillagers.clear();
    }

    private Team getSmallestTeam() {
        Team smallest = null;
        int minSize = Integer.MAX_VALUE;

        for (Team t : teams.values()) {
            int size = t.getPlayers().size();
            if (size < minSize) {
                smallest = t;
                minSize = size;
            }
        }
        return smallest;
    }

    private void giveLobbyItems(Player player) {
        player.getInventory().clear();

        ItemStack selector = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = selector.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Team Selector");
            selector.setItemMeta(meta);
        }
        player.getInventory().setItem(0, selector);
    }

    public boolean changeTeam(Player player, TeamColor newTeamColor) {
        if (state != GameState.WAITING && state != GameState.STARTING) {
            plugin.getMessageManager().sendMessage(player, "team.change-only-in-lobby");
            return false;
        }

        GamePlayer gamePlayer = players.get(player.getUniqueId());
        if (gamePlayer == null) {
            plugin.getMessageManager().sendMessage(player, "command.not-in-game");
            return false;
        }

        if (gamePlayer.getTeam() != null && gamePlayer.getTeam().getColor() == newTeamColor) {
            plugin.getMessageManager().sendMessage(player, "team.already-on-team",
                    newTeamColor.getChatColor() + newTeamColor.name());
            return false;
        }

        Team newTeam = teams.get(newTeamColor);
        if (newTeam == null) {
            plugin.getMessageManager().sendMessage(player, "command.invalid-team-color");
            return false;
        }

        int maxTeamSize = arena.getMaxPlayers() / Math.max(1, teams.size());
        if (newTeam.getPlayers().size() >= maxTeamSize) {
            plugin.getMessageManager().sendMessage(player, "team.team-full",
                    newTeamColor.getChatColor() + newTeamColor.name());
            return false;
        }

        Team oldTeam = gamePlayer.getTeam();
        if (oldTeam != null) oldTeam.removePlayer(gamePlayer);

        newTeam.addPlayer(gamePlayer);
        gamePlayer.setTeam(newTeam);

        plugin.getDebugLogger().debug("Team changed: " + player.getName() + " -> " + newTeamColor.name());

        plugin.getMessageManager().sendMessage(player, "team.changed",
                newTeamColor.getChatColor() + newTeamColor.name());

        return true;
    }

    /* --------------------------------------------------------------------- */
    /* Messaging / Getters                                                   */
    /* --------------------------------------------------------------------- */

    public void broadcast(String messageKey, Object... args) {
        String message = plugin.getMessageManager().getMessage(messageKey, args);

        for (GamePlayer gp : players.values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p != null) p.sendMessage(message);
        }
    }

    public GameState getState() {
        return state;
    }

    public int getCountdown() {
        return countdown;
    }

    public int getGameElapsedSeconds() {
        return gameElapsedSeconds;
    }

    public int getDiamondUpgradeTime() {
        return cfgDiamondUpgradeSeconds > 0 ? cfgDiamondUpgradeSeconds : plugin.getConfig().getInt("game.diamond-upgrade-time", 720);
    }

    public int getEmeraldUpgradeTime() {
        return cfgEmeraldUpgradeSeconds > 0 ? cfgEmeraldUpgradeSeconds : plugin.getConfig().getInt("game.emerald-upgrade-time", 1440);
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

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    /* --------------------------------------------------------------------- */
    /* Spectators                                                            */
    /* --------------------------------------------------------------------- */

    public void addSpectator(Player player) {
        if (state != GameState.RUNNING && state != GameState.ENDING) {
            plugin.getMessageManager().sendMessage(player, "spectator.only-during-game");
            return;
        }

        spectators.add(player.getUniqueId());
        plugin.getGameManager().addPlayerToGame(player, this);

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(arena.getSpectatorSpawn());

        if (gameScoreboard != null) {
            gameScoreboard.show(player);
            gameScoreboard.startUpdateTask();
        }

        plugin.getMessageManager().sendMessage(player, "spectator.now-spectating");

        for (GamePlayer gp : players.values()) {
            Player gamePlayer = Bukkit.getPlayer(gp.getUuid());
            if (gamePlayer != null) {
                gamePlayer.hidePlayer(plugin, player);
            }
        }
    }

    public void removeSpectator(Player player) {
        if (!spectators.remove(player.getUniqueId())) return;

        plugin.getGameManager().removePlayerFromGame(player);

        if (gameScoreboard != null) {
            gameScoreboard.hide(player);
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(arena.getLobbySpawn());

        restoreVisibility(player);

        plugin.getMessageManager().sendMessage(player, "spectator.stopped-spectating");
    }

    /* --------------------------------------------------------------------- */
    /* Upgrades: Heal Pool / Dragon Buff                                     */
    /* --------------------------------------------------------------------- */

    public void applySpecialUpgrade(Team team, String upgradeId) {
        if (team == null || upgradeId == null) return;

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
        if (healPoolTask != null) return;

        double radius = plugin.getConfig().getDouble("upgrades.heal-pool.radius", 6.0);
        int amplifier = plugin.getConfig().getInt("upgrades.heal-pool.amplifier", 0);
        int durationTicks = plugin.getConfig().getInt("upgrades.heal-pool.duration-ticks", 60);

        healPoolTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.RUNNING && state != GameState.ENDING) return;

                for (Team team : teams.values()) {
                    if (!team.getUpgrades().hasHealPool()) continue;

                    Location base = getTeamBaseLocation(team);
                    if (base == null || base.getWorld() == null) continue;

                    for (GamePlayer gp : team.getPlayers()) {
                        Player p = Bukkit.getPlayer(gp.getUuid());
                        if (p == null) continue;
                        if (p.getWorld() != base.getWorld()) continue;

                        if (p.getLocation().distance(base) <= radius) {
                            p.addPotionEffect(new PotionEffect(
                                    PotionEffectType.REGENERATION,
                                    durationTicks,
                                    amplifier,
                                    false,
                                    false
                            ));
                        }
                    }
                }
            }
        };

        healPoolTask.runTaskTimer(plugin, 0L, 20L);
        plugin.getDebugLogger().debug("Heal pool task started: arena=" + arena.getName());
    }

    private void stopHealPoolTask() {
        if (healPoolTask == null) return;
        healPoolTask.cancel();
        healPoolTask = null;
    }

    private void spawnDragonBuff(Team team) {
        if (team == null || team.getUpgrades() == null) return;
        if (dragonBuffs.containsKey(team.getColor())) return;

        Location base = getTeamBaseLocation(team);
        if (base == null || base.getWorld() == null) return;

        World world = base.getWorld();

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
        }
    }

    private void clearDragonBuffs() {
        for (List<UUID> ids : dragonBuffs.values()) {
            for (UUID id : ids) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
        }
        dragonBuffs.clear();
    }

    private Location getTeamBaseLocation(Team team) {
        if (team.getBedLocation() != null) return team.getBedLocation();
        return team.getSpawnLocation();
    }

    /* --------------------------------------------------------------------- */
    /* Utils                                                                  */
    /* --------------------------------------------------------------------- */

    private boolean shouldBroadcastCountdown(int value, List<Integer> intervals) {
        if (intervals != null && !intervals.isEmpty()) {
            return intervals.contains(value);
        }
        return value <= 10 || value % 10 == 0;
    }

    private void upgradeGenerators(ResourceGenerator.ResourceType type) {
        for (ResourceGenerator generator : generators) {
            if (generator.getType() == type) generator.upgrade();
        }
    }

    private ResourceGenerator.ResourceType parseGeneratorType(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase(Locale.ROOT);

        if (lower.startsWith("iron")) return ResourceGenerator.ResourceType.IRON;
        if (lower.startsWith("gold")) return ResourceGenerator.ResourceType.GOLD;
        if (lower.startsWith("diamond")) return ResourceGenerator.ResourceType.DIAMOND;
        if (lower.startsWith("emerald")) return ResourceGenerator.ResourceType.EMERALD;

        return null;
    }

    private int secondsToTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0));
    }

    private void setToMaxHealth(Player p) {
        if (p == null) return;
        if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH) == null) return;
        double max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        if (max <= 0) return;
        p.setHealth(max);
    }

    private void giveStartingItem(Player player) {
        String itemName = plugin.getConfig().getString("game.starting-item", "WOODEN_SWORD");
        String normalized = (itemName == null ? "WOODEN_SWORD" : itemName);

        Material material = Material.matchMaterial(normalized);
        if (material == null && "WOOD_SWORD".equalsIgnoreCase(normalized)) {
            material = Material.WOODEN_SWORD;
        }
        if (material == null || material == Material.AIR) return;

        if (!player.getInventory().contains(material)) {
            player.getInventory().addItem(new ItemStack(material, 1));
        }
    }

    /**
     * Send player to lobby server with delay.
     */
    private void sendToLobbyDelayed(Player player) {
        int delay = plugin.getConfig().getInt("bungeecord.lobby-send-delay", 3);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                if (plugin.getCloudNetManager() != null && plugin.getCloudNetManager().isEnabled()) {
                    plugin.getCloudNetManager().connectToLobby(player);
                } else {
                    plugin.getMessageManager().sendMessage(player, "game.sending-to-lobby");
                    plugin.getBungeeManager().sendPlayerToLobby(player);
                }
            }
        }.runTaskLater(plugin, delay * 20L);
    }
}