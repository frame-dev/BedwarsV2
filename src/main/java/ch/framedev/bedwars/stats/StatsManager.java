package ch.framedev.bedwars.stats;

import ch.framedev.bedwars.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages player statistics using SQLite database
 */
public class StatsManager {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Map<UUID, PlayerStats> statsCache;

    public StatsManager(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.statsCache = new HashMap<>();
    }

    /**
     * Load player stats from database (async)
     */
    public CompletableFuture<PlayerStats> loadPlayerStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            if (statsCache.containsKey(uuid)) {
                return statsCache.get(uuid);
            }

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

                    statsCache.put(uuid, stats);
                    return stats;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load stats for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Create new stats if player doesn't exist
            PlayerStats newStats = new PlayerStats(uuid);
            statsCache.put(uuid, newStats);
            return newStats;
        });
    }

    /**
     * Save player stats to database (async)
     */
    public CompletableFuture<Void> savePlayerStats(UUID uuid, String playerName) {
        return CompletableFuture.runAsync(() -> {
            PlayerStats stats = statsCache.get(uuid);
            if (stats == null)
                return;

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
                long now = System.currentTimeMillis();
                database.executeUpdate(query,
                        uuid.toString(),
                        playerName,
                        stats.getWins(),
                        stats.getLosses(),
                        stats.getKills(),
                        stats.getDeaths(),
                        stats.getFinalKills(),
                        stats.getBedsBroken(),
                        stats.getGamesPlayed(),
                        now,
                        now);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save stats for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Save all cached stats to database
     */
    public void saveAllStats() {
        for (Map.Entry<UUID, PlayerStats> entry : statsCache.entrySet()) {
            savePlayerStats(entry.getKey(), "Unknown").join(); // Join to ensure completion
        }
        plugin.getLogger().info("Saved " + statsCache.size() + " player statistics to database.");
    }

    /**
     * Get player stats from cache (synchronous)
     */
    public PlayerStats getPlayerStats(UUID uuid) {
        return statsCache.computeIfAbsent(uuid, k -> {
            loadPlayerStats(uuid).join(); // Load synchronously if not in cache
            return statsCache.get(uuid);
        });
    }

    /**
     * Get top players by wins
     */
    public CompletableFuture<Map<String, Integer>> getTopWins(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> topPlayers = new HashMap<>();
            String query = "SELECT player_name, wins FROM player_stats ORDER BY wins DESC LIMIT ?";

            try (ResultSet rs = database.executeQuery(query, limit)) {
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
            Map<String, Integer> topPlayers = new HashMap<>();
            String query = "SELECT player_name, kills FROM player_stats ORDER BY kills DESC LIMIT ?";

            try (ResultSet rs = database.executeQuery(query, limit)) {
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
            Map<String, Integer> topPlayers = new HashMap<>();
            String query = "SELECT player_name, beds_broken FROM player_stats ORDER BY beds_broken DESC LIMIT ?";

            try (ResultSet rs = database.executeQuery(query, limit)) {
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
    }

    /**
     * Get total number of players in database
     */
    public CompletableFuture<Integer> getTotalPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT COUNT(*) as total FROM player_stats";
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
}
