package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player quit events
 */
public class PlayerQuitListener implements Listener {

    private final BedWarsPlugin plugin;

    public PlayerQuitListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);

        plugin.getDebugLogger().debug("Player quit: " + player.getName() + " (" + player.getUniqueId()
            + "), inGame=" + (game != null));

        if (game != null) {
            if (game.isSpectator(player)) {
                game.removeSpectator(player);
            } else {
                game.removePlayer(player);
            }
        }

        // Save player stats when they quit
        plugin.getStatsManager().savePlayerStats(player.getUniqueId(), player.getName());

        // Clear cache to free memory
        plugin.getStatsManager().clearCache(player.getUniqueId());

        if (plugin.getMapVoteManager() != null) {
            plugin.getMapVoteManager().removePlayer(player);
        }
    }
}