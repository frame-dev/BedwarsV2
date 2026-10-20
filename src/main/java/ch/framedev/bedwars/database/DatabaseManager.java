package ch.framedev.bedwars.database;

import ch.framedev.BedWarsPlugin;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SQLite database connections and operations
 */
public class DatabaseManager {

    private final BedWarsPlugin plugin;
    private final Map<Long, Connection> connections;
    private final String dbPath;

    public DatabaseManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.connections = new ConcurrentHashMap<>();
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
            Connection connection = getConnection();
            plugin.getLogger().info("Successfully connected to SQLite database!");
            plugin.getDebugLogger().debug("SQLite connection established");
            createTables(connection);
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }

    /**
     * Create necessary database tables if they don't exist
     */
    private void createTables(Connection connection) {
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

        String createPartyInvitesTable = """
            CREATE TABLE IF NOT EXISTS party_invites (
                target_uuid TEXT PRIMARY KEY,
                party_id TEXT NOT NULL,
                inviter_uuid TEXT NOT NULL,
                inviter_name TEXT NOT NULL,
                expires_at INTEGER NOT NULL
            )
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
            stmt.execute(createPartyInvitesTable);
            stmt.execute(createCosmeticsTable);
            stmt.execute(createAchievementsTable);
            plugin.getLogger().info("Database tables created successfully!");
            plugin.getDebugLogger().debug("Database schema ensured");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
        }
    }

    private Connection createConnection() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder: " + dataFolder.getAbsolutePath());
        }

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        configureConnection(connection);
        return connection;
    }

    private void configureConnection(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA busy_timeout=5000");
            stmt.execute("PRAGMA foreign_keys=ON");
        }
    }

    /**
     * Disconnect from the database
     */
    public void disconnect() {
        for (Connection connection : connections.values()) {
            try {
                if (connection == null || connection.isClosed()) {
                    continue;
                }
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
            }
        }
        connections.clear();
        plugin.getLogger().info("Database connections closed.");
        plugin.getDebugLogger().debug("SQLite connections closed");
    }

    /**
     * Get the database connection
     */
    public synchronized Connection getConnection() throws SQLException {
        long threadId = Thread.currentThread().getId();
        Connection connection = connections.get(threadId);
        if (connection == null || connection.isClosed()) {
            connection = createConnection();
            connections.put(threadId, connection);
        }
        return connection;
    }

    /**
     * Check if the database connection is valid
     */
    public boolean isConnected() {
        return connections.values().stream().anyMatch(connection -> {
            try {
                return connection != null && !connection.isClosed();
            } catch (SQLException e) {
                return false;
            }
        });
    }

    /**
     * Execute a query and return the result set
     */
    public ResultSet executeQuery(String query, Object... params) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(query);
        stmt.closeOnCompletion();
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
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            plugin.getDebugLogger().verbose("DB update: " + query + " | params=" + params.length);
            return stmt.executeUpdate();
        }
    }
}
