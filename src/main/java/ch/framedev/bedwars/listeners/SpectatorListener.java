package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Prevents spectators from leaving the arena
 */
public class SpectatorListener implements Listener {

    private final BedWarsPlugin plugin;

    public SpectatorListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }

        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null && game.isSpectator(player)) {
            if (event.getTo() != null
                    && (event.getFrom().getBlockX() != event.getTo().getBlockX()
                            || event.getFrom().getBlockY() != event.getTo().getBlockY()
                            || event.getFrom().getBlockZ() != event.getTo().getBlockZ())) {
                plugin.getDebugLogger().verbose("Spectator move: " + player.getName() + " to "
                        + event.getTo().getWorld().getName() + ":" + event.getTo().getX() + ","
                        + event.getTo().getY() + "," + event.getTo().getZ());
            }
            // Could add boundary checking here if needed
            // For now, spectators can move freely
        }
    }
}
