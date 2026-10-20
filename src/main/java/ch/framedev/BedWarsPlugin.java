package ch.framedev;

import ch.framedev.bedwars.arena.ArenaManager;
import ch.framedev.bedwars.bungee.BungeeManager;
import ch.framedev.bedwars.commands.ImprovedBedWarsCommand;
import ch.framedev.bedwars.commands.BedWarsTabCompleter;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.game.GameManager;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.party.PartyManager;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.signs.JoinSignManager;
import ch.framedev.bedwars.shop.ShopManager;
import ch.framedev.bedwars.stats.StatsManager;
import ch.framedev.bedwars.utils.DebugLogger;
import ch.framedev.bedwars.utils.MessageManager;
import ch.framedev.bedwars.voting.MapVoteManager;
import ch.framedev.bedwars.cosmetics.CosmeticsManager;
import ch.framedev.bedwars.achievements.AchievementsManager;
import ch.framedev.bedwars.cloudnet.CloudNetManager;
import ch.framedev.bedwars.team.TeamSelectionGUI;
import ch.framedev.bedwars.listeners.*;
import org.bukkit.command.PluginCommand;
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
    private DebugLogger debugLogger;
    private PartyManager partyManager;
    private MapVoteManager mapVoteManager;
    private CosmeticsManager cosmeticsManager;
    private AchievementsManager achievementsManager;
    private CloudNetManager cloudNetManager;
    private TeamSelectionGUI teamSelectionGUI;
    private JoinSignManager joinSignManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize debug logger early
        debugLogger = new DebugLogger(this);
        if (debugLogger.isEnabled()) {
            debugLogger.debug("Debug logging enabled");
        }

        // Initialize message manager
        messageManager = new MessageManager(this);

        // Initialize upgrade manager
        upgradeManager = new UpgradeManager(this);
        GamePlayer.setUpgradeManager(upgradeManager);

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // Initialize party manager
        partyManager = new PartyManager(this, databaseManager);
        partyManager.loadParties();

        // Initialize BungeeCord support
        bungeeManager = new BungeeManager(this);
        bungeeManager.enable();

        // Initialize CloudNet support
        cloudNetManager = new CloudNetManager(this);

        // Initialize managers
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this, arenaManager);
        statsManager = new StatsManager(this, databaseManager);
        shopManager = new ShopManager(this);
        joinSignManager = new JoinSignManager(this);
        joinSignManager.loadSigns();

        // Initialize map voting manager
        mapVoteManager = new MapVoteManager(this);

        // Initialize cosmetics manager
        cosmeticsManager = new CosmeticsManager(this, databaseManager);

        // Initialize achievements manager
        achievementsManager = new AchievementsManager(this, databaseManager);

        // Initialize team selection GUI
        teamSelectionGUI = new TeamSelectionGUI();

        // Register commands with tab completion
        ImprovedBedWarsCommand commandExecutor = new ImprovedBedWarsCommand(this, arenaManager);
        BedWarsTabCompleter tabCompleter = new BedWarsTabCompleter(this, arenaManager);
        PluginCommand bedwarsCommand = getCommand("bedwars");
        if (bedwarsCommand == null) {
            getLogger().severe("Command 'bedwars' is not defined in plugin.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        bedwarsCommand.setExecutor(commandExecutor);
        bedwarsCommand.setTabCompleter(tabCompleter);

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

        if (partyManager != null) {
            partyManager.shutdown();
        }

        if (cosmeticsManager != null) {
            cosmeticsManager.shutdown();
        }

        if (achievementsManager != null) {
            achievementsManager.shutdown();
        }

        if (joinSignManager != null) {
            joinSignManager.saveSigns();
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
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinSignListener(this, joinSignManager), this);
        getServer().getPluginManager().registerEvents(new VoidDeathListener(this), this);
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

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public DebugLogger getDebugLogger() {
        return debugLogger;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public MapVoteManager getMapVoteManager() {
        return mapVoteManager;
    }

    public CosmeticsManager getCosmeticsManager() {
        return cosmeticsManager;
    }

    public AchievementsManager getAchievementsManager() {
        return achievementsManager;
    }

    public CloudNetManager getCloudNetManager() {
        return cloudNetManager;
    }

    public TeamSelectionGUI getTeamSelectionGUI() {
        return teamSelectionGUI;
    }

    public JoinSignManager getJoinSignManager() {
        return joinSignManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}
