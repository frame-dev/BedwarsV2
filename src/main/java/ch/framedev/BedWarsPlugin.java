package ch.framedev;

import ch.framedev.bedwars.arena.ArenaManager;
import ch.framedev.bedwars.bungee.BungeeManager;
import ch.framedev.bedwars.commands.ImprovedBedWarsCommand;
import ch.framedev.bedwars.commands.BedWarsTabCompleter;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.game.GameManager;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.stats.StatsManager;
import ch.framedev.bedwars.utils.MessageManager;
import ch.framedev.bedwars.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main BedWars Plugin Class
 */
public class BedWarsPlugin extends JavaPlugin {

    private static BedWarsPlugin instance;
    private GameManager gameManager;
    private StatsManager statsManager;
    private DatabaseManager databaseManager;
    private ArenaManager arenaManager;
    private BungeeManager bungeeManager;
    private MessageManager messageManager;
    private UpgradeManager upgradeManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize message manager
        messageManager = new MessageManager(this);

        // Initialize upgrade manager
        upgradeManager = new UpgradeManager(this);
        GamePlayer.setUpgradeManager(upgradeManager);

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // Initialize BungeeCord support
        bungeeManager = new BungeeManager(this);

        // Initialize managers
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this, arenaManager);
        statsManager = new StatsManager(this, databaseManager);

        // Register commands with tab completion
        ImprovedBedWarsCommand commandExecutor = new ImprovedBedWarsCommand(this, arenaManager);
        BedWarsTabCompleter tabCompleter = new BedWarsTabCompleter(this, arenaManager);
        getCommand("bedwars").setExecutor(commandExecutor);
        getCommand("bedwars").setTabCompleter(tabCompleter);

        // Register listeners
        registerListeners();

        getLogger().info("BedWars plugin has been enabled!");
        getLogger().info("Loaded " + arenaManager.getArenaNames().size() + " arenas");
    }

    @Override
    public void onDisable() {
        // Stop all games
        if (gameManager != null) {
            gameManager.stopAllGames();
        }

        // Save statistics
        if (statsManager != null) {
            statsManager.saveAllStats();
        }

        // Disconnect database
        // Disable BungeeCord
        if (bungeeManager != null) {
            bungeeManager.disable();
        }

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("BedWars plugin has been disabled!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemPickupListener(this, upgradeManager), this);
    }

    public static BedWarsPlugin getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public BungeeManager getBungeeManager() {
        return bungeeManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
