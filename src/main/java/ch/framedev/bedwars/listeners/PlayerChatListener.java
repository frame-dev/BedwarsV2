package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.team.Team;
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
 * - Default: team chat during RUNNING
 * - Prefix '!': global chat to all players in the same game (and spectators)
 * - Spectators: spectator chat channel (unless '!' for global)
 */
public class PlayerChatListener implements Listener {

    private final BedWarsPlugin plugin;

    // You can move these into config if you want
    private static final String GLOBAL_PREFIX = "!";
    private static final String TEAM_TAG = ChatColor.GRAY + "[" + ChatColor.AQUA + "TEAM" + ChatColor.GRAY + "] ";
    private static final String ALL_TAG  = ChatColor.GRAY + "[" + ChatColor.YELLOW + "ALL" + ChatColor.GRAY + "] ";
    private static final String SPEC_TAG = ChatColor.GRAY + "[" + ChatColor.DARK_GRAY + "SPEC" + ChatColor.GRAY + "] ";

    public PlayerChatListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player sender = event.getPlayer();
        final String raw = event.getMessage();

        final Game game = plugin.getGameManager().getPlayerGame(sender);
        if (game == null) return;

        // In lobby states, you may want normal chat; here we only route during RUNNING/ENDING
        if (game.getState() != GameState.RUNNING && game.getState() != GameState.ENDING) return;

        // Stop Bukkit from broadcasting to whole server
        event.setCancelled(true);

        // Resolve game player info (keep minimal logic async)
        final GamePlayer gp = game.getGamePlayer(sender);
        final boolean isSpectator = game.isSpectator(sender) || (gp != null && gp.isEliminated());

        boolean global = raw.startsWith(GLOBAL_PREFIX);
        final String msg = global ? raw.substring(GLOBAL_PREFIX.length()).trim() : raw.trim();

        if (msg.isEmpty()) return;

        // Build recipients
        final Set<UUID> recipients = new HashSet<>();

        if (global) {
            // Global: everyone in that game (players + spectators)
            recipients.addAll(game.getPlayers().keySet());
            recipients.addAll(game.getSpectators());
        } else if (isSpectator) {
            // Spectator chat: only spectators
            recipients.addAll(game.getSpectators());
        } else {
            // Team chat: only same team members (alive or not depending on your Team structure)
            Team team = (gp != null ? gp.getTeam() : null);
            if (team != null) {
                for (GamePlayer teammate : team.getPlayers()) {
                    recipients.add(teammate.getUuid());
                }
            } else {
                // Fallback: if no team, treat as global within the game
                recipients.addAll(game.getPlayers().keySet());
                recipients.addAll(game.getSpectators());
                global = true;
            }
        }

        // Format message
        final String formatted = format(game, sender, gp, msg, global, isSpectator);

        // Send on main thread to be safe with other plugins / player state
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID id : recipients) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    p.sendMessage(formatted);
                }
            }
        });
    }

    private String format(Game game, Player sender, GamePlayer gp, String msg, boolean global, boolean spectator) {
        // Example formats:
        // [TEAM] <Red> Name: message
        // [ALL] Name: message
        // [SPEC] Name: message

        String prefix = global ? ALL_TAG : (spectator ? SPEC_TAG : TEAM_TAG);

        String teamPrefix = "";
        if (gp != null && gp.getTeam() != null) {
            teamPrefix = gp.getTeam().getColor().getChatColor() + "<" + gp.getTeam().getColor().name() + "> " + ChatColor.RESET;
        }

        return prefix + teamPrefix + ChatColor.WHITE + sender.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + msg;
    }
}