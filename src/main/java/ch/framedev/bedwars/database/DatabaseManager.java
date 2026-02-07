package ch.framedev.bedwars.database;

import ch.framedev.BedWarsPlugin;

import java.io.File;
import java.sql.*;

/**
 * Manages SQLite database connections and operations
 */
public class DatabaseManager {

    private final BedWarsPlugin plugin;
    private Connection connection;
    private final String dbPath;

    public DatabaseManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + File.separator + "bedwars.db";
        plugin.getLogger().info("DatabaseManager initialized with db path: " + dbPath);
        plugin.getDebugLogger().debug("Database path set: " + dbPath);
    }

    /**
     * Connect to the SQLite database
     */
    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            plugin.getLogger().info("Successfully connected to SQLite database!");
            plugin.getDebugLogger().debug("SQLite connection established");
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create necessary database tables if they don't exist
     */
    private void createTables() {
        String createStatsTable = """
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT,
                    wins INTEGER DEFAULT 0,
                    losses INTEGER DEFAULT 0,
                    kills INTEGER DEFAULT 0,
                    deaths INTEGER DEFAULT 0,
                    final_kills INTEGER DEFAULT 0,
                    beds_broken INTEGER DEFAULT 0,
                    games_played INTEGER DEFAULT 0,
                    last_played INTEGER,
                    created_at INTEGER
                )
                """;

        String createIndexes = """
                CREATE INDEX IF NOT EXISTS idx_wins ON player_stats(wins DESC);
                CREATE INDEX IF NOT EXISTS idx_kills ON player_stats(kills DESC);
                CREATE INDEX IF NOT EXISTS idx_beds_broken ON player_stats(beds_broken DESC);
                """;

        String createPartiesTable = """
            CREATE TABLE IF NOT EXISTS parties (
                id TEXT PRIMARY KEY,
                leader_uuid TEXT NOT NULL,
                created_at INTEGER
            )
            """;

        String createPartyMembersTable = """
            CREATE TABLE IF NOT EXISTS party_members (
                party_id TEXT NOT NULL,
                member_uuid TEXT NOT NULL,
                role TEXT NOT NULL,
                joined_at INTEGER,
                PRIMARY KEY (party_id, member_uuid)
            )
            """;

        String createPartyIndexes = """
            CREATE INDEX IF NOT EXISTS idx_party_member_uuid ON party_members(member_uuid);
            """;

        String createCosmeticsTable = """
            CREATE TABLE IF NOT EXISTS player_cosmetics (
                uuid TEXT PRIMARY KEY,
                kill_effect TEXT,
                bed_effect TEXT,
                updated_at INTEGER
            )
            """;

        String createAchievementsTable = """
                CREATE TABLE IF NOT EXISTS player_achievements (
                    uuid TEXT NOT NULL,
                    achievement_id TEXT NOT NULL,
                    progress INTEGER DEFAULT 0,
                    unlocked_at INTEGER DEFAULT 0,
                    updated_at INTEGER,
                    PRIMARY KEY (uuid, achievement_id)
                )
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createStatsTable);
            stmt.execute(createIndexes);
            stmt.execute(createPartiesTable);
            stmt.execute(createPartyMembersTable);
            stmt.execute(createPartyIndexes);
            stmt.execute(createCosmeticsTable);
            stmt.execute(createAchievementsTable);
            plugin.getLogger().info("Database tables created successfully!");
            plugin.getDebugLogger().debug("Database schema ensured");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Disconnect from the database
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
                plugin.getDebugLogger().debug("SQLite connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the database connection
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check connection status: " + e.getMessage());
        }
        return connection;
    }

    /**
     * Check if the database connection is valid
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Execute a query and return the result set
     */
    public ResultSet executeQuery(String query, Object... params) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        plugin.getDebugLogger().verbose("DB query: " + query + " | params=" + params.length);
        return stmt.executeQuery();
    }

    /**
     * Execute an update query (INSERT, UPDATE, DELETE)
     */
    public int executeUpdate(String query, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            plugin.getDebugLogger().verbose("DB update: " + query + " | params=" + params.length);
            return stmt.executeUpdate();
        }
    }
}
