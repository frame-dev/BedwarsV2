package ch.framedev;

import ch.framedev.bedwars.commands.BedWarsCommand;
import ch.framedev.bedwars.game.GameManager;
import ch.framedev.bedwars.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main BedWars Plugin Class
 */
public class BedWarsPlugin extends JavaPlugin {
    
    private static BedWarsPlugin instance;
    private GameManager gameManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        gameManager = new GameManager(this);
        
        // Register commands
        getCommand("bedwars").setExecutor(new BedWarsCommand(this));
        
        // Register listeners
        registerListeners();
        
        getLogger().info("BedWars plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Stop all games
        if (gameManager != null) {
            gameManager.stopAllGames();
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
    }
    
    public static BedWarsPlugin getInstance() {
        return instance;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
}
