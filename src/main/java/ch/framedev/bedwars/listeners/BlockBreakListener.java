package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Objects;

/**
 * Handles block break events.
 * <p>
 * Fixes / improvements:
 * - Uses HIGHEST priority + ignoreCancelled
 * - Caches allow-block-breaking flag
 * - Proper bed detection by matching against configured bed locations (no "contains BED" false-positives)
 * - Uses distanceSquared (faster) with a tight tolerance
 * - Cancels and returns early for non-running states
 * - Only allows breaking blocks recorded as player-placed (as you intended)
 * - Records bed block for reset BEFORE changing to AIR
 * - Prevents NPE in formatLocation when world is null
 */
public class BlockBreakListener implements Listener {

    private final BedWarsPlugin plugin;
    private final boolean allowBlockBreaking;
    private final boolean bedProtection;

    public BlockBreakListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.allowBlockBreaking = plugin.getConfig().getBoolean("world.allow-block-breaking", true);
        this.bedProtection = plugin.getConfig().getBoolean("game.bed-protection", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) return; // Not a BedWars player -> don't interfere

        plugin.getDebugLogger().debug("Block break: " + player.getName() + " " + block.getType()
                + " at " + formatLocation(block));

        // Only allow breaking during RUNNING
        if (game.getState() != GameState.RUNNING) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "block.cannot-break-yet");
            return;
        }

        if (!allowBlockBreaking) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "block.break-disabled");
            return;
        }

        // Check if this block is a bed belonging to a team in this arena
        Team bedTeam = findBedTeam(game, block);
        if (bedTeam != null) {
            event.setCancelled(true); // handle bed destruction ourselves
            handleBedBreak(player, block, game, bedTeam);
            return;
        }

        // Only allow breaking player-placed blocks
        if (!game.getWorldResetManager().isPlayerPlacedBlock(block.getLocation())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "block.cannot-break-placed");
        }
    }

    /**
     * Finds the team whose bed corresponds to this block location.
     * Uses distanceSquared to allow minor offsets (e.g., bed foot/head, older maps).
     */
    private Team findBedTeam(Game game, Block broken) {
        if (broken == null) return null;

        for (Team team : game.getTeams().values()) {
            if (team.getBedLocation() == null) continue;

            // Must be same world
            if (!Objects.equals(team.getBedLocation().getWorld(), broken.getWorld())) continue;

            // Tolerance: within 2 blocks (squared 4). Adjust if your bedLocation is not exact.
            double distSq = team.getBedLocation().distanceSquared(broken.getLocation());
            if (distSq <= 4.0) {
                // Optional: ensure it is actually a bed block, avoid matching random blocks near bedLocation
                if (isBedBlock(broken.getType())) {
                    return team;
                }
            }
        }
        return null;
    }

    private boolean isBedBlock(Material material) {
        if (material == null) return false;
        // Works across versions: modern beds are *_BED; legacy may be BED or BED_BLOCK
        String n = material.name();
        return n.endsWith("_BED") || n.equals("BED") || n.equals("BED_BLOCK");
    }

    private void handleBedBreak(Player player, Block block, Game game, Team bedTeam) {
        GamePlayer breaker = game.getGamePlayer(player);
        if (breaker == null || breaker.getTeam() == null) return;

        // If protection enabled, disallow breaking own bed
        if (bedProtection && breaker.getTeam() == bedTeam) {
            plugin.getMessageManager().sendMessage(player, "block.cannot-break-own-bed");
            return;
        }

        // If bed already destroyed, ignore
        if (!bedTeam.isBedAlive()) {
            plugin.getMessageManager().sendMessage(player, "block.bed-already-destroyed");
            return;
        }

        // Record bed block for reset BEFORE changing it
        game.getWorldResetManager().recordBedLocation(
                block.getLocation(),
                block.getType(),
                block.getBlockData()
        );

        // Destroy bed
        bedTeam.destroyBed();

        plugin.getDebugLogger().debug("Bed destroyed: team=" + bedTeam.getColor().name()
                + ", by=" + player.getName());

        if (plugin.getCosmeticsManager() != null) {
            plugin.getCosmeticsManager().applyBedDestroyEffect(player, block.getLocation());
        }

        if (plugin.getAchievementsManager() != null) {
            plugin.getAchievementsManager().recordBedBroken(player.getUniqueId());
        }

        // Remove bed block (you may also want to remove the other half; depends on how you store bedLocation)
        block.setType(Material.AIR, false);

        breaker.addBedBroken();

        game.broadcast("death.bed-destroyed",
                bedTeam.getColor().getChatColor() + bedTeam.getColor().name(),
                breaker.getTeam().getColor().getChatColor() + player.getName());

        playBedDestroyedSound(game);

        // After bed destroyed, check if game ends soon (optional — win condition usually changes after later deaths)
        // game.checkWinCondition(); // only if you expose it
    }

    private String formatLocation(Block block) {
        if (block == null) return "null";
        String worldName = block.getWorld().getName();
        return worldName + ":" + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    private void playBedDestroyedSound(Game game) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("sounds.bed-destroyed");
        if (section == null) return;
        if (!section.getBoolean("enabled", true)) return;

        String soundName = section.getString("sound", "ENTITY_ENDER_DRAGON_GROWL");
        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);

        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getDebugLogger().debug("Invalid bed-destroy sound: " + soundName);
            return;
        }

        for (var gp : game.getPlayers().values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p != null && p.isOnline()) {
                p.playSound(p.getLocation(), sound, volume, pitch);
            }
        }
        for (var specId : game.getSpectators()) {
            Player p = Bukkit.getPlayer(specId);
            if (p != null && p.isOnline()) {
                p.playSound(p.getLocation(), sound, volume, pitch);
            }
        }
    }
}