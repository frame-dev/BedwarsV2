package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles player join events
 */
public class PlayerJoinListener implements Listener {

    private final BedWarsPlugin plugin;

    public PlayerJoinListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getDebugLogger().debug("Player join: " + event.getPlayer().getName()
                + " (" + event.getPlayer().getUniqueId() + ")");
        // Load player stats asynchronously when they join
        plugin.getStatsManager().loadPlayerStats(event.getPlayer().getUniqueId());
    }
}