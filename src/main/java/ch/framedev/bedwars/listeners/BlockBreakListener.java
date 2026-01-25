package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import org.bukkit.Material;
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

        if (game != null && game.getState() == GameState.RUNNING) {
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
                if (breakerPlayer != null && breakerPlayer.getTeam() == team) {
                    plugin.getMessageManager().sendMessage(player, "block.cannot-break-own-bed");
                    return;
                }

                team.destroyBed();

                // Record bed for reset
                game.getWorldResetManager().recordBedLocation(
                        block.getLocation(),
                        block.getType(),
                        block.getBlockData());

                block.setType(Material.AIR);

                game.broadcast("death.bed-destroyed",
                        team.getColor().getChatColor() + team.getColor().name(),
                        breakerPlayer.getTeam().getColor().getChatColor() + player.getName());

                if (breakerPlayer != null) {
                    breakerPlayer.addBedBroken();
                }

                return;
            }
        }
    }
}
