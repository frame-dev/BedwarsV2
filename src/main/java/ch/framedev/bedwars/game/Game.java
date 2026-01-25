package ch.framedev.bedwars.game;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.stats.PlayerStats;
import ch.framedev.bedwars.team.Team;
import ch.framedev.bedwars.team.TeamColor;
import ch.framedev.bedwars.generators.ResourceGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Represents a single BedWars game instance
 */
public class Game {

    private final BedWarsPlugin plugin;
    private final Arena arena;
    private final Map<UUID, GamePlayer> players;
    private final Set<UUID> spectators;
    private final Map<TeamColor, Team> teams;
    private final List<ResourceGenerator> generators;
    private final WorldResetManager worldResetManager;
    private GameState state;
    private int countdown;
    private BukkitRunnable countdownTask;
    private BukkitRunnable gameTask;

    public Game(BedWarsPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.players = new HashMap<>();
        this.spectators = new HashSet<>();
        this.teams = new HashMap<>();
        this.generators = new ArrayList<>();
        this.worldResetManager = new WorldResetManager();
        this.state = GameState.WAITING;

        initializeTeams();
        initializeGenerators();
    }

    private void initializeTeams() {
        for (TeamColor color : TeamColor.values()) {
            if (arena.getTeamSpawn(color) != null) {
                teams.put(color, new Team(color, arena.getTeamSpawn(color), arena.getBedLocation(color)));
            }
        }
    }

    private void initializeGenerators() {
        // Add team generators
        for (Team team : teams.values()) {
            generators.add(new ResourceGenerator(team.getSpawnLocation(), ResourceGenerator.ResourceType.IRON, 1));
            generators.add(new ResourceGenerator(team.getSpawnLocation().clone().add(2, 0, 0),
                    ResourceGenerator.ResourceType.GOLD, 8));
        }

        // Add diamond and emerald generators (would be configured in arena)
        // This is simplified - in real implementation, these would come from arena
        // config
    }

    public void addPlayer(Player player) {
        GamePlayer gamePlayer = new GamePlayer(player);
        players.put(player.getUniqueId(), gamePlayer);

        // Assign to team with least players
        Team team = getSmallestTeam();
        if (team != null) {
            team.addPlayer(gamePlayer);
            gamePlayer.setTeam(team);
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
        if (gamePlayer != null && gamePlayer.getTeam() != null) {
            gamePlayer.getTeam().removePlayer(gamePlayer);
        }

        plugin.getGameManager().removePlayerFromGame(player);

        broadcast("game.player-left", player.getName());

        // Send to lobby server if BungeeCord is enabled
        if (plugin.getBungeeManager().isEnabled() &&
                plugin.getConfig().getBoolean("bungeecord.send-to-lobby-on-leave", false)) {
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
        countdown = 30;

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown <= 0) {
                    startGame();
                    cancel();
                } else if (countdown <= 10 || countdown % 10 == 0) {
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

        // Teleport players to their team spawns
        for (GamePlayer gamePlayer : players.values()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && gamePlayer.getTeam() != null) {
                player.teleport(gamePlayer.getTeam().getSpawnLocation());
                player.getInventory().clear();
                gamePlayer.giveTeamArmor();
            }
        }

        // Start resource generators
        for (ResourceGenerator generator : generators) {
            generator.start(plugin);
        }

        // Start game timer
        startGameTimer();
    }

    private void startGameTimer() {
        gameTask = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                elapsed++;

                // Diamond generators upgrade at 12 minutes
                if (elapsed == 720) {
                    broadcast("game.diamond-upgrade");
                }

                // Emerald generators upgrade at 24 minutes
                if (elapsed == 1440) {
                    broadcast("game.emerald-upgrade");
                }
            }
        };
        gameTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void handlePlayerDeath(Player player) {
        GamePlayer gamePlayer = players.get(player.getUniqueId());
        if (gamePlayer == null)
            return;

        gamePlayer.addDeath();

        Team team = gamePlayer.getTeam();
        if (team != null && team.isBedAlive()) {
            // Respawn player
            new BukkitRunnable() {
                int respawnTime = 5;

                @Override
                public void run() {
                    if (respawnTime <= 0) {
                        player.spigot().respawn();
                        player.teleport(team.getSpawnLocation());
                        gamePlayer.giveTeamArmor();
                        plugin.getMessageManager().sendMessage(player, "game.respawned");
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
            players.remove(player.getUniqueId());

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
            endGame(aliveTeams.get(0));
        } else if (aliveTeams.isEmpty()) {
            endGame(null);
        }
    }

    public void endGame(Team winningTeam) {
        state = GameState.ENDING;

        if (gameTask != null) {
            gameTask.cancel();
        }

        // Stop generators
        for (ResourceGenerator generator : generators) {
            generator.stop();
        }

        // Update player statistics
        for (GamePlayer gamePlayer : players.values()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null) {
                PlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());

                if (winningTeam != null && gamePlayer.getTeam() == winningTeam) {
                    stats.addWin();
                } else {
                    stats.addLoss();
                }

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
                        // Send to BungeeCord lobby if enabled
                        if (plugin.getBungeeManager().isEnabled() &&
                                plugin.getConfig().getBoolean("bungeecord.send-to-lobby-on-end", true)) {
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

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    plugin.getMessageManager().sendMessage(player, "game.sending-to-lobby");
                    plugin.getBungeeManager().sendPlayerToLobby(player);
                }
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    private void resetGame() {
        // Reset the world to original state
        int blocksReset = worldResetManager.getBlockChangeCount();
        if (blocksReset > 0) {
            worldResetManager.resetWorld();
            broadcast("game.world-reset", blocksReset);
        }

        players.clear();
        state = GameState.WAITING;

        for (Team team : teams.values()) {
            team.reset();
        }
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
}
