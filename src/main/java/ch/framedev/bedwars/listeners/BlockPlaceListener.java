package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Handles block place events
 */
public class BlockPlaceListener implements Listener {

    private final BedWarsPlugin plugin;

    public BlockPlaceListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game != null && game.getState() != GameState.RUNNING) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "block.cannot-place-yet");
            return;
        }

        if (game != null) {
            if (!plugin.getConfig().getBoolean("world.allow-block-placing", true)) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "block.place-disabled");
                return;
            }

            // Track player-placed blocks for cleanup
            game.getWorldResetManager().recordPlacedBlock(event.getBlock());
        }
    }
}