package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Objects;

/**
 * Handles block place events in BedWars.
 */
public class BlockPlaceListener implements Listener {

    private static final String META_BW_TNT = "bw_tnt";

    private final BedWarsPlugin plugin;
    // Cache config flags (refresh on reload by recreating listener or add a reload hook)
    private final boolean allowBlockPlacing;

    public BlockPlaceListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.allowBlockPlacing = plugin.getConfig().getBoolean("world.allow-block-placing", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);

        // If not in a game, don't interfere (vanilla behavior)
        if (game == null) return;

        // Don’t allow placing before running
        if (game.getState() != GameState.RUNNING) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "block.cannot-place-yet");
            return;
        }

        // Optional global rule (per your config)
        if (!allowBlockPlacing) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "block.place-disabled");
            return;
        }

        // Special TNT behavior: instantly prime TNT instead of placing a block
        if (event.getBlockPlaced().getType() == Material.TNT) {
            primeTnt(event, game);
            return;
        }

        // Track player-placed blocks for cleanup/reset
        game.getWorldResetManager().recordPlacedBlock(event.getBlockPlaced());

        // Debug (do it after checks to avoid spam from cancelled events)
        plugin.getDebugLogger().debug("Block place: " + player.getName() + " " + event.getBlockPlaced().getType()
                + " at " + formatLocation(event.getBlockPlaced().getLocation()));
    }

    private void primeTnt(BlockPlaceEvent event, Game game) {
        Location loc = event.getBlockPlaced().getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Remove the placed block and spawn primed TNT
        event.getBlockPlaced().setType(Material.AIR);

        TNTPrimed tnt = world.spawn(loc.add(0.5, 0.0, 0.5), TNTPrimed.class);
        tnt.setSource(event.getPlayer());
        // Optional: customize fuse
        // tnt.setFuseTicks(40);

        // Mark it so we can identify it in explode event
        tnt.setMetadata(META_BW_TNT, new FixedMetadataValue(plugin, game.getArena().getName()));

        plugin.getDebugLogger().debug("Primed TNT by " + event.getPlayer().getName()
                + " at " + formatLocation(loc));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();

        // Only modify explosions of TNT we spawned/marked
        if (!(entity instanceof TNTPrimed)) return;
        if (!entity.hasMetadata(META_BW_TNT)) return;

        // BedWars-style: TNT does no block damage
        event.blockList().clear();

        // Optional: also control entity damage via EntityDamageEvent elsewhere if needed
    }

    private String formatLocation(Location loc) {
        World w = loc.getWorld();
        String worldName = (w == null ? "null" : w.getName());
        return worldName + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}