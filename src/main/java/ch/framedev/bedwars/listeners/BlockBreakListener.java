package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles block break events
 */
public class BlockBreakListener implements Listener {

    private final BedWarsPlugin plugin;

    public BlockBreakListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Game game = plugin.getGameManager().getPlayerGame(player);

        plugin.getDebugLogger().debug("Block break: " + player.getName() + " " + block.getType()
            + " at " + formatLocation(block));

        if (game != null && game.getState() == GameState.RUNNING) {
            if (!plugin.getConfig().getBoolean("world.allow-block-breaking", true)) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "block.break-disabled");
                return;
            }

            // Check if it's a bed
            if (block.getType().name().contains("BED")) {
                event.setCancelled(true);
                handleBedBreak(player, block, game);
                return;
            }

            // Only allow breaking player-placed blocks
            if (!game.getWorldResetManager().isPlayerPlacedBlock(block.getLocation())) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "block.cannot-break-placed");
            }
        } else if (game != null) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "block.cannot-break-yet");
        }
    }

    private void handleBedBreak(Player player, Block block, Game game) {
        // Find which team's bed was broken
        for (var teamEntry : game.getTeams().entrySet()) {
            var team = teamEntry.getValue();
            if (team.getBedLocation() != null &&
                    team.getBedLocation().distance(block.getLocation()) < 3) {

                var breakerPlayer = game.getGamePlayer(player);
                if (breakerPlayer == null) {
                    return;
                }
                boolean bedProtection = plugin.getConfig().getBoolean("game.bed-protection", true);
                if (bedProtection && breakerPlayer.getTeam() == team) {
                    plugin.getMessageManager().sendMessage(player, "block.cannot-break-own-bed");
                    return;
                }

                team.destroyBed();

                plugin.getDebugLogger().debug("Bed destroyed: team=" + team.getColor().name()
                    + ", by=" + player.getName());

                if (plugin.getCosmeticsManager() != null) {
                    plugin.getCosmeticsManager().applyBedDestroyEffect(player, block.getLocation());
                }

                if (plugin.getAchievementsManager() != null) {
                    plugin.getAchievementsManager().recordBedBroken(player.getUniqueId());
                }

                // Record bed for reset
                game.getWorldResetManager().recordBedLocation(
                        block.getLocation(),
                        block.getType(),
                        block.getBlockData());

                block.setType(Material.AIR);

                game.broadcast("death.bed-destroyed",
                        team.getColor().getChatColor() + team.getColor().name(),
                        breakerPlayer.getTeam().getColor().getChatColor() + player.getName());

                playBedDestroyedSound(game);

                if (breakerPlayer != null) {
                    breakerPlayer.addBedBroken();
                }

                return;
            }
        }
    }

    private String formatLocation(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    private void playBedDestroyedSound(Game game) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("sounds.bed-destroyed");
        if (section == null) {
            return;
        }

        if (!section.getBoolean("enabled", true)) {
            return;
        }

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

        for (var gamePlayer : game.getPlayers().values()) {
            Player onlinePlayer = org.bukkit.Bukkit.getPlayer(gamePlayer.getUuid());
            if (onlinePlayer != null) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), sound, volume, pitch);
            }
        }
    }
}
