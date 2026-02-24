package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Prevents spectators from leaving the arena.
 * <p>
 * Improvements:
 * - Only runs when the player actually changes block (cheap)
 * - Teleports spectators back if they leave the arena world (safe default)
 * - Optional radius boundary check around spectator spawn (configurable)
 * - Uses LOW priority + ignoreCancelled to play nice with other plugins
 */
public class SpectatorListener implements Listener {

    private final BedWarsPlugin plugin;

    // Optional boundary check around spectator spawn
    private final boolean enforceRadius;
    private final double maxRadius;

    public SpectatorListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.enforceRadius = plugin.getConfig().getBoolean("spectator.boundary.enabled", false);
        this.maxRadius = plugin.getConfig().getDouble("spectator.boundary.radius", 150.0);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Fast exit: only spectators
        if (player.getGameMode() != GameMode.SPECTATOR) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Only handle real movement between blocks (prevents running every tiny move)
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()
                && from.getWorld() == to.getWorld()) {
            return;
        }

        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null || !game.isSpectator(player)) return;

        Location specSpawn = game.getArena().getSpectatorSpawn();
        if (specSpawn == null || specSpawn.getWorld() == null) return;

        // Hard rule: spectator must stay in the arena world
        if (to.getWorld() == null || !to.getWorld().equals(specSpawn.getWorld())) {
            plugin.getDebugLogger().debug("Spectator left arena world, teleport back: " + player.getName());
            player.teleport(specSpawn);
            return;
        }

        // Optional radius boundary around spectator spawn (prevents flying far away)
        if (enforceRadius) {
            // distanceSquared is cheaper
            double maxSq = maxRadius * maxRadius;
            if (to.distanceSquared(specSpawn) > maxSq) {
                plugin.getDebugLogger().debug("Spectator left boundary, teleport back: " + player.getName());
                player.teleport(specSpawn);
            }
        }

        // Verbose logging (optional)
        if (plugin.getDebugLogger().isVerbose()) {
            plugin.getDebugLogger().verbose("Spectator move: " + player.getName() + " -> "
                    + to.getWorld().getName() + ":" + to.getBlockX() + "," + to.getBlockY() + "," + to.getBlockZ());
        }
    }
}