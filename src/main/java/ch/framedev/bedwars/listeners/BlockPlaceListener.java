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
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.FixedMetadataValue;

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
        tnt.setYield(0.0f);
        tnt.setIsIncendiary(false);
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

        if (!isActiveBedWarsWorld(entity.getWorld())) return;

        // BedWars-style: explosions may happen, but they do not destroy arena blocks.
        event.blockList().clear();
        event.setYield(0.0f);

        if (entity instanceof TNTPrimed tnt) {
            tnt.setIsIncendiary(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isActiveBedWarsWorld(event.getBlock().getWorld())) return;

        event.blockList().clear();
        event.setYield(0.0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTntEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (!(damager instanceof TNTPrimed)) return;
        if (!damager.hasMetadata(META_BW_TNT)) return;

        // Keep TNT purely as a visual/knockback event and prevent destruction of entities/objects.
        event.setCancelled(true);
    }

    private String formatLocation(Location loc) {
        World w = loc.getWorld();
        String worldName = (w == null ? "null" : w.getName());
        return worldName + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private boolean isActiveBedWarsWorld(World world) {
        if (world == null) return false;

        for (Game game : plugin.getGameManager().getGames()) {
            if (game.getState() == GameState.WAITING) continue;
            if (isSameWorld(world, game.getArena().getLobbySpawn())) return true;
            if (isSameWorld(world, game.getArena().getSpectatorSpawn())) return true;
            if (game.getTeams().values().stream()
                    .anyMatch(team -> isSameWorld(world, team.getSpawnLocation())
                            || isSameWorld(world, team.getBedLocation()))) {
                return true;
            }
            if (game.getArena().getGenerators().values().stream()
                    .anyMatch(location -> isSameWorld(world, location))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameWorld(World world, Location location) {
        return location != null && location.getWorld() != null && location.getWorld().equals(world);
    }
}
