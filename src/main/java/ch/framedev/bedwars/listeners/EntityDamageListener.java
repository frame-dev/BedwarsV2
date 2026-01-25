package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Handles entity damage events
 */
public class EntityDamageListener implements Listener {

    private final BedWarsPlugin plugin;

    public EntityDamageListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Game game = plugin.getGameManager().getPlayerGame(player);

            if (game != null && game.getState() != GameState.RUNNING) {
                event.setCancelled(true);
            }
        }
    }
}
