package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * BedWars chat routing:
 * - Players in a BedWars game only talk to players/spectators in that game
 * - Sender names use their team color when available
 */
public class PlayerChatListener implements Listener {

    private final BedWarsPlugin plugin;

    private static final String GAME_TAG = ChatColor.GRAY + "[" + ChatColor.AQUA + "Game" + ChatColor.GRAY + "] ";
    private static final String SPEC_TAG = ChatColor.GRAY + "[" + ChatColor.DARK_GRAY + "SPEC" + ChatColor.GRAY + "] ";

    public PlayerChatListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player sender = event.getPlayer();
        final Game initialGame = plugin.getGameManager().getPlayerGame(sender);
        if (initialGame == null) return;

        final String msg = event.getMessage().trim();
        if (msg.isEmpty()) return;

        event.setCancelled(true);

        // Handle BedWars routing on the main thread to avoid async access to mutable game/team state.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Game game = plugin.getGameManager().getPlayerGame(sender);
            if (game == null) return;

            GamePlayer gp = game.getGamePlayer(sender);
            boolean isSpectator = game.isSpectator(sender) || (gp != null && gp.isEliminated());

            Set<UUID> recipients = new HashSet<>();
            recipients.addAll(game.getPlayers().keySet());
            recipients.addAll(game.getSpectators());

            String formatted = format(sender, gp, msg, isSpectator);
            for (UUID id : recipients) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    p.sendMessage(formatted);
                }
            }
        });
    }

    private String format(Player sender, GamePlayer gp, String msg, boolean spectator) {
        String prefix = spectator ? SPEC_TAG : GAME_TAG;
        ChatColor nameColor = ChatColor.WHITE;

        if (gp != null && gp.getTeam() != null) {
            nameColor = gp.getTeam().getColor().getChatColor();
        }

        return prefix + nameColor + sender.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + msg;
    }
}
