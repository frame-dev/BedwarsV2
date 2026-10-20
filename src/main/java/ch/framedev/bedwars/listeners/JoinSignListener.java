package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.signs.JoinSignManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Locale;

/**
 * Handles BedWars join signs.
 *
 * Create one by writing:
 * [BedWars]
 * arenaName
 */
public class JoinSignListener implements Listener {

    private final BedWarsPlugin plugin;
    private final JoinSignManager signManager;

    public JoinSignListener(BedWarsPlugin plugin, JoinSignManager signManager) {
        this.plugin = plugin;
        this.signManager = signManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        String header = normalize(event.getLine(0));
        if (!isJoinSignHeader(header)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("bedwars.sign.create")) {
            event.setLine(0, ChatColor.RED + "[Denied]");
            plugin.getMessageManager().sendMessage(player, "sign.no-permission");
            return;
        }

        String arenaName = ChatColor.stripColor(event.getLine(1));
        if (arenaName == null || arenaName.isBlank()) {
            event.setLine(0, ChatColor.RED + "[BedWars]");
            event.setLine(1, ChatColor.RED + "Missing arena");
            plugin.getMessageManager().sendMessage(player, "sign.missing-arena");
            return;
        }

        Game game = plugin.getGameManager().getGame(arenaName.trim());
        if (game == null) {
            event.setLine(0, ChatColor.RED + "[BedWars]");
            event.setLine(1, ChatColor.RED + arenaName.trim());
            plugin.getMessageManager().sendMessage(player, "sign.invalid-arena", arenaName.trim());
            return;
        }

        writeSignLines(event, game);
        signManager.registerSign(event.getBlock(), game);
        plugin.getMessageManager().sendMessage(player, "sign.created", game.getArena().getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Sign sign)) return;
        if (!isJoinSignHeader(normalize(sign.getLine(0)))) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("bedwars.join")) {
            plugin.getMessageManager().sendMessage(player, "command.no-permission");
            return;
        }

        String arenaName = signManager.getArenaName(sign);
        if (arenaName == null || arenaName.isBlank()) {
            plugin.getMessageManager().sendMessage(player, "sign.missing-arena");
            return;
        }

        Game game = plugin.getGameManager().getGame(arenaName.trim());
        if (game == null) {
            plugin.getMessageManager().sendMessage(player, "sign.invalid-arena", arenaName.trim());
            return;
        }

        Location returnLocation = player.getLocation().clone();
        Game currentGame = plugin.getGameManager().getPlayerGame(player);
        if (currentGame != null) {
            if (currentGame.isSpectator(player)) {
                currentGame.removeSpectator(player);
            } else {
                plugin.getMessageManager().sendMessage(player, "command.already-in-game");
                return;
            }
        }

        game.addPlayer(player, returnLocation);
        signManager.registerSign(block, game);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign sign)) return;
        if (!signManager.isTrackedSign(block) && !isJoinSignHeader(normalize(sign.getLine(0)))) return;

        signManager.unregisterSign(block);
    }

    private void writeSignLines(SignChangeEvent event, Game game) {
        event.setLine(0, ChatColor.GOLD + JoinSignManager.SIGN_HEADER);
        event.setLine(1, ChatColor.DARK_BLUE + game.getArena().getName());
        event.setLine(2, signManager.getStateLine(game));
        event.setLine(3, ChatColor.DARK_GRAY + playerCount(game));
    }

    private String playerCount(Game game) {
        return game.getPlayers().size() + "/" + game.getArena().getMaxPlayers();
    }

    private boolean isJoinSignHeader(String value) {
        return "[bedwars]".equals(value) || "[bw]".equals(value);
    }

    private String normalize(String value) {
        String stripped = ChatColor.stripColor(value == null ? "" : value);
        return stripped == null ? "" : stripped.trim().toLowerCase(Locale.ROOT);
    }
}
