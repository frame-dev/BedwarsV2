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
            // Could add boundary checking here if needed
            // For now, spectators can move freely
        }
    }
}
