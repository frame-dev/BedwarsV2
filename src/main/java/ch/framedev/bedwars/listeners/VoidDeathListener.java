package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Applies the configured BedWars void death level.
 */
public class VoidDeathListener implements Listener {

    private final BedWarsPlugin plugin;
    private final int voidDeathLevel;

    public VoidDeathListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.voidDeathLevel = plugin.getConfig().getInt("game.void-death-level", 0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null || to.getY() > voidDeathLevel) return;

        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null || game.getState() != GameState.RUNNING || game.isSpectator(player)) return;
        if (player.isDead() || player.getHealth() <= 0.0) return;

        player.setFallDistance(0.0f);
        player.setHealth(0.0);
    }
}
