package ch.framedev.bedwars.stats;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.database.DatabaseManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Manages player statistics using SQLite database
 */
public class StatsManager {

    private final BedWarsPlugin plugin;
    private final DatabaseManager database;
    private final ConcurrentMap<UUID, PlayerStats> statsCache;
    private final Executor executor;
    private final Map<UUID, CompletableFuture<Void>> pendingSaves = new HashMap<>();

    public StatsManager(BedWarsPlugin plugin, DatabaseManager database) {
        this(plugin, database, ForkJoinPool.commonPool());
    }

    StatsManager(BedWarsPlugin plugin, DatabaseManager database, Executor executor) {
        this.plugin = plugin;
        this.database = database;
        this.executor = executor;
        this.statsCache = new ConcurrentHashMap<>();
        plugin.getLogger().info("StatsManager initialized with database connection");
        plugin.getDebugLogger().debug("StatsManager ready");
    }

    /**
     * Load player stats from database (async)
     */
    public synchronized CompletableFuture<PlayerStats> loadPlayerStats(UUID uuid) {
        // A quick reconnect must not reload the database before the quit save finishes.
        CompletableFuture<Void> previous = pendingSaves.getOrDefault(uuid, CompletableFuture.completedFuture(null));
        return previous.handle((ignored, failure) -> null).thenApplyAsync(ignored -> {
            // Check cache first
            PlayerStats cachedStats = statsCache.get(uuid);
            if (cachedStats != null) return cachedStats;

            plugin.getDebugLogger().debug("Loading stats: " + uuid);

            String query = "SELECT * FROM player_stats WHERE uuid = ?";
            try (ResultSet rs = database.executeQuery(query, uuid.toString())) {
                if (rs.next()) {
                    PlayerStats stats = new PlayerStats(uuid);
                    stats.setWins(rs.getInt("wins"));
                    stats.setLosses(rs.getInt("losses"));
                    stats.setKills(rs.getInt("kills"));
                    stats.setDeaths(rs.getInt("deaths"));
                    stats.setFinalKills(rs.getInt("final_kills"));
                    stats.setBedsBroken(rs.getInt("beds_broken"));
                    stats.setGamesPlayed(rs.getInt("games_played"));

                    PlayerStats existing = statsCache.putIfAbsent(uuid, stats);
                    return existing != null ? existing : stats;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load stats for " + uuid + ": " + e.getMessage());
            }

            // Create new stats if player doesn't exist
            PlayerStats newStats = new PlayerStats(uuid);
            PlayerStats existing = statsCache.putIfAbsent(uuid, newStats);
            if (existing != null) return existing;
            plugin.getDebugLogger().debug("Created new stats: " + uuid);
            return newStats;
        }, executor);
    }

    /**
     * Save player stats to database (async)
     */
    public synchronized CompletableFuture<Void> savePlayerStats(UUID uuid, String playerName) {
        PlayerStats stats = statsCache.get(uuid);
        if (stats == null) return CompletableFuture.completedFuture(null);

        String safePlayerName = normalizePlayerName(playerName, uuid);
        int wins = stats.getWins();
        int losses = stats.getLosses();
        int kills = stats.getKills();
        int deaths = stats.getDeaths();
        int finalKills = stats.getFinalKills();
        int bedsBroken = stats.getBedsBroken();
        int gamesPlayed = stats.getGamesPlayed();
        long now = System.currentTimeMillis();

        CompletableFuture<Void> previous = pendingSaves.getOrDefault(uuid, CompletableFuture.completedFuture(null));
        CompletableFuture<Void> save = previous.handle((ignored, failure) -> null).thenRunAsync(() -> {
            plugin.getDebugLogger().debug("Saving stats: " + uuid + " (" + safePlayerName + ")");

            String query = """
                    INSERT INTO player_stats (uuid, player_name, wins, losses, kills, deaths,
                        final_kills, beds_broken, games_played, last_played, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        player_name = excluded.player_name,
                        wins = excluded.wins,
                        losses = excluded.losses,
                        kills = excluded.kills,
                        deaths = excluded.deaths,
                        final_kills = excluded.final_kills,
                        beds_broken = excluded.beds_broken,
                        games_played = excluded.games_played,
                        last_played = excluded.last_played
                    """;

            try {
                database.executeUpdate(query,
                        uuid.toString(),
                        safePlayerName,
                        wins,
                        losses,
                        kills,
                        deaths,
                        finalKills,
                        bedsBroken,
                        gamesPlayed,
                        now,
                        now);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save stats for " + uuid + ": " + e.getMessage());
            }
        }, executor);
        pendingSaves.put(uuid, save);
        save.whenComplete((ignored, failure) -> {
            synchronized (StatsManager.this) {
                pendingSaves.remove(uuid, save);
            }
        });
        return save;
    }

    /**
     * Save all cached stats to database
     */
    public void saveAllStats() {
        for (Map.Entry<UUID, PlayerStats> entry : statsCache.entrySet()) {
            savePlayerStats(entry.getKey(), resolveKnownPlayerName(entry.getKey())).join(); // Join to ensure completion
        }
        CompletableFuture<Void> pending;
        synchronized (this) {
            // Quit-time cache eviction must not hide unfinished writes from shutdown.
            pending = CompletableFuture.allOf(pendingSaves.values().toArray(new CompletableFuture<?>[0]));
        }
        pending.join();
        plugin.getLogger().info("Saved " + statsCache.size() + " player statistics to database.");
        plugin.getDebugLogger().debug("Saved all stats: count=" + statsCache.size());
    }

    /**
     * Get player stats from cache (synchronous)
     */
    public PlayerStats getPlayerStats(UUID uuid) {
        PlayerStats stats = statsCache.get(uuid);
        return stats != null ? stats : loadPlayerStats(uuid).join();
    }

    /**
     * Get top players by wins
     */
    public CompletableFuture<Map<String, Integer>> getTopWins(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> topPlayers = new LinkedHashMap<>();
            String query = "SELECT player_name, wins FROM player_stats ORDER BY wins DESC LIMIT ?";
            int safeLimit = Math.max(1, limit);

            plugin.getDebugLogger().debug("Leaderboard query: wins limit=" + safeLimit);

            try (ResultSet rs = database.executeQuery(query, safeLimit)) {
                while (rs.next()) {
                    topPlayers.put(rs.getString("player_name"), rs.getInt("wins"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get top wins: " + e.getMessage());
            }

            return topPlayers;
        });
    }

    /**
     * Get top players by kills
     */
    public CompletableFuture<Map<String, Integer>> getTopKills(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> topPlayers = new LinkedHashMap<>();
            String query = "SELECT player_name, kills FROM player_stats ORDER BY kills DESC LIMIT ?";
            int safeLimit = Math.max(1, limit);

            plugin.getDebugLogger().debug("Leaderboard query: kills limit=" + safeLimit);

            try (ResultSet rs = database.executeQuery(query, safeLimit)) {
                while (rs.next()) {
                    topPlayers.put(rs.getString("player_name"), rs.getInt("kills"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get top kills: " + e.getMessage());
            }

            return topPlayers;
        });
    }

    /**
     * Get top players by beds broken
     */
    public CompletableFuture<Map<String, Integer>> getTopBedsBroken(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> topPlayers = new LinkedHashMap<>();
            String query = "SELECT player_name, beds_broken FROM player_stats ORDER BY beds_broken DESC LIMIT ?";
            int safeLimit = Math.max(1, limit);

            plugin.getDebugLogger().debug("Leaderboard query: beds limit=" + safeLimit);

            try (ResultSet rs = database.executeQuery(query, safeLimit)) {
                while (rs.next()) {
                    topPlayers.put(rs.getString("player_name"), rs.getInt("beds_broken"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get top beds broken: " + e.getMessage());
            }

            return topPlayers;
        });
    }

    /**
     * Clear stats cache for a player
     */
    public void clearCache(UUID uuid) {
        statsCache.remove(uuid);
        plugin.getDebugLogger().debug("Stats cache cleared: " + uuid);
    }

    /**
     * Get total number of players in database
     */
    public CompletableFuture<Integer> getTotalPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT COUNT(*) as total FROM player_stats";
            plugin.getDebugLogger().debug("Total players query");
            try (ResultSet rs = database.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get total players: " + e.getMessage());
            }
            return 0;
        });
    }

    private String resolveKnownPlayerName(UUID uuid) {
        Player onlinePlayer = plugin.getServer().getPlayer(uuid);
        if (onlinePlayer != null) return onlinePlayer.getName();

        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
        return offlinePlayer.getName();
    }

    private String normalizePlayerName(String playerName, UUID uuid) {
        if (playerName != null && !playerName.isBlank() && !"Unknown".equalsIgnoreCase(playerName)) {
            return playerName;
        }

        String resolvedName = resolveKnownPlayerName(uuid);
        return resolvedName == null || resolvedName.isBlank() ? "Unknown" : resolvedName;
    }
}
