package ch.framedev.bedwars.commands;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.arena.ArenaManager;
import ch.framedev.bedwars.arena.ArenaSetupSession;
import ch.framedev.bedwars.game.Arena;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.stats.PlayerStats;
import ch.framedev.bedwars.team.TeamColor;
import ch.framedev.bedwars.utils.MessageManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Map;

/**
 * Improved BedWars command handler with arena setup and spectator support
 */
public class ImprovedBedWarsCommand implements CommandExecutor {

    private final BedWarsPlugin plugin;
    private final ArenaManager arenaManager;

    public ImprovedBedWarsCommand(BedWarsPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "command.only-players");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                if (!player.hasPermission("bedwars.join")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getMessageManager().sendMessage(player, "command.join-usage");
                    return true;
                }
                handleJoin(player, args[1]);
                break;

            case "leave":
                if (!player.hasPermission("bedwars.leave")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                handleLeave(player);
                break;

            case "spectate":
                if (!player.hasPermission("bedwars.spectate")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getMessageManager().sendMessage(player, "command.spectate-usage");
                    return true;
                }
                handleSpectate(player, args[1]);
                break;

            case "stats":
                if (!player.hasPermission("bedwars.stats")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                if (args.length >= 2) {
                    handleStatsOther(player, args[1]);
                } else {
                    handleStats(player);
                }
                break;

            case "leaderboard":
            case "top":
                handleLeaderboard(player, args);
                break;

            case "setup":
                if (!player.hasPermission("bedwars.setup")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                handleSetup(player, args);
                break;

            case "list":
                if (!player.hasPermission("bedwars.list")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                handleList(player);
                break;

            case "start":
                if (!player.hasPermission("bedwars.admin")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                handleForceStart(player);
                break;

            case "stop":
                if (!player.hasPermission("bedwars.admin")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                handleForceStop(player);
                break;

            case "resetworld":
                if (!player.hasPermission("bedwars.admin")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                handleResetWorld(player);
                break;

            case "reload":
                if (!player.hasPermission("bedwars.admin")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                handleReload(player);
                break;

            case "lobby":
                if (!player.hasPermission("bedwars.bungee")) {
                    plugin.getMessageManager().sendMessage(player, "command.no-permission");
                    return true;
                }
                handleLobby(player);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getMessageManager().getMessage("help.header"));
        player.sendMessage(plugin.getMessageManager().getMessage("help.title"));
        player.sendMessage(plugin.getMessageManager().getMessage("help.separator"));
        player.sendMessage(plugin.getMessageManager().getMessage("help.join"));
        player.sendMessage(plugin.getMessageManager().getMessage("help.leave"));
        player.sendMessage(plugin.getMessageManager().getMessage("help.spectate"));
        player.sendMessage(plugin.getMessageManager().getMessage("help.stats"));
        player.sendMessage(plugin.getMessageManager().getMessage("help.leaderboard"));
        player.sendMessage(plugin.getMessageManager().getMessage("help.list"));

        if (player.hasPermission("bedwars.setup")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help.separator"));
            player.sendMessage(plugin.getMessageManager().getMessage("help.setup"));
        }

        if (plugin.getBungeeManager().isEnabled() && player.hasPermission("bedwars.bungee")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help.separator"));
            player.sendMessage(plugin.getMessageManager().getMessage("help.lobby"));
        }

        if (player.hasPermission("bedwars.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help.separator"));
            player.sendMessage(plugin.getMessageManager().getMessage("help.start"));
            player.sendMessage(plugin.getMessageManager().getMessage("help.stop"));
            player.sendMessage(plugin.getMessageManager().getMessage("help.resetworld"));
            player.sendMessage(plugin.getMessageManager().getMessage("help.reload"));
        }

        player.sendMessage(plugin.getMessageManager().getMessage("help.footer"));
    }

    private void handleJoin(Player player, String arenaName) {
        Game currentGame = plugin.getGameManager().getPlayerGame(player);
        if (currentGame != null) {
            if (currentGame.isSpectator(player)) {
                currentGame.removeSpectator(player);
            } else {
                plugin.getMessageManager().sendMessage(player, "command.already-in-game");
                return;
            }
        }

        Game game = plugin.getGameManager().getGame(arenaName);
        if (game == null) {
            plugin.getMessageManager().sendMessage(player, "command.arena-not-found", arenaName);
            return;
        }

        game.addPlayer(player);
    }

    private void handleLeave(Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            plugin.getMessageManager().sendMessage(player, "command.not-in-game");
            return;
        }

        if (game.isSpectator(player)) {
            game.removeSpectator(player);
        } else {
            game.removePlayer(player);
        }

        plugin.getMessageManager().sendMessage(player, "command.left-game");
    }

    private void handleSpectate(Player player, String arenaName) {
        Game currentGame = plugin.getGameManager().getPlayerGame(player);
        if (currentGame != null) {
            plugin.getMessageManager().sendMessage(player, "command.leave-game-first");
            return;
        }

        Game game = plugin.getGameManager().getGame(arenaName);
        if (game == null) {
            plugin.getMessageManager().sendMessage(player, "command.arena-not-found", arenaName);
            return;
        }

        game.addSpectator(player);
    }

    private void handleStats(Player player) {
        PlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());
        displayStats(player, player.getName(), stats);
    }

    private void handleStatsOther(Player player, String targetName) {
        // Lookup player by name (async to avoid blocking)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

            if (target == null || !target.hasPlayedBefore()) {
                plugin.getMessageManager().sendMessage(player, "command.player-not-found", targetName);
                return;
            }

            PlayerStats stats = plugin.getStatsManager().getPlayerStats(target.getUniqueId());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                displayStats(player, target.getName(), stats);
            });
        });
    }

    private void displayStats(Player viewer, String playerName, PlayerStats stats) {
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.header"));
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.title", playerName));
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.separator"));
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.games-played", stats.getGamesPlayed()));
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.wins", stats.getWins(), stats.getLosses()));
        viewer.sendMessage(
                plugin.getMessageManager().getMessage("stats.win-rate", String.format("%.1f", stats.getWinRate())));
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.kills", stats.getKills(), stats.getDeaths()));
        viewer.sendMessage(
                plugin.getMessageManager().getMessage("stats.kd-ratio", String.format("%.2f", stats.getKDRatio())));
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.final-kills", stats.getFinalKills()));
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.beds-broken", stats.getBedsBroken()));
        viewer.sendMessage(plugin.getMessageManager().getMessage("stats.footer"));
    }

    private void handleList(Player player) {
        player.sendMessage(plugin.getMessageManager().getMessage("arena-list.header"));
        player.sendMessage(plugin.getMessageManager().getMessage("arena-list.title"));
        player.sendMessage(plugin.getMessageManager().getMessage("arena-list.separator"));

        for (Game game : plugin.getGameManager().getGames()) {
            String status = getStatusColor(game) + game.getState().toString();
            int playerCount = game.getPlayers().size();
            int spectatorCount = game.getSpectators().size();

            player.sendMessage(
                    plugin.getMessageManager().getMessage("arena-list.arena-line", game.getArena().getName()));
            player.sendMessage(plugin.getMessageManager().getMessage("arena-list.arena-info",
                    playerCount, game.getArena().getMaxPlayers(), spectatorCount, status));
        }

        player.sendMessage(plugin.getMessageManager().getMessage("arena-list.footer"));
    }

    private ChatColor getStatusColor(Game game) {
        switch (game.getState()) {
            case WAITING:
                return ChatColor.GRAY;
            case STARTING:
                return ChatColor.YELLOW;
            case RUNNING:
                return ChatColor.GREEN;
            case ENDING:
                return ChatColor.RED;
            default:
                return ChatColor.WHITE;
        }
    }

    private void handleLeaderboard(Player player, String[] args) {
        String category = "wins";
        if (args.length > 1) {
            category = args[1].toLowerCase();
        }

        plugin.getMessageManager().sendMessage(player, "command.loading-leaderboard");

        switch (category) {
            case "wins":
            case "win":
                plugin.getStatsManager().getTopWins(10).thenAccept(leaders -> {
                    displayLeaderboard(player, plugin.getMessageManager().getMessage("leaderboard.top-wins"),
                            leaders, "wins", ChatColor.GREEN);
                });
                break;

            case "kills":
            case "kill":
                plugin.getStatsManager().getTopKills(10).thenAccept(leaders -> {
                    displayLeaderboard(player, plugin.getMessageManager().getMessage("leaderboard.top-kills"),
                            leaders, "kills", ChatColor.RED);
                });
                break;

            case "beds":
            case "bed":
                plugin.getStatsManager().getTopBedsBroken(10).thenAccept(leaders -> {
                    displayLeaderboard(player, plugin.getMessageManager().getMessage("leaderboard.top-beds"),
                            leaders, "beds", ChatColor.AQUA);
                });
                break;

            default:
                plugin.getMessageManager().sendMessage(player, "command.leaderboard-usage");
                break;
        }
    }

    private void displayLeaderboard(Player player, String title, Map<String, Integer> leaders, String stat,
            ChatColor color) {
        player.sendMessage(plugin.getMessageManager().getMessage("leaderboard.header"));
        player.sendMessage(plugin.getMessageManager().getMessage("leaderboard.title", title));
        player.sendMessage(plugin.getMessageManager().getMessage("leaderboard.separator"));

        int rank = 1;
        for (Map.Entry<String, Integer> entry : leaders.entrySet()) {
            String medal = getMedal(rank);
            player.sendMessage(plugin.getMessageManager().getMessage("leaderboard.entry",
                    medal, rank, entry.getKey(), color, entry.getValue(), stat));
            rank++;
        }

        player.sendMessage(plugin.getMessageManager().getMessage("leaderboard.footer"));
    }

    private String getMedal(int rank) {
        switch (rank) {
            case 1:
                return "🥇";
            case 2:
                return "🥈";
            case 3:
                return "🥉";
            default:
                return " ";
        }
    }

    private void handleSetup(Player player, String[] args) {
        if (args.length < 2) {
            sendSetupHelp(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create":
                handleSetupCreate(player, args);
                break;
            case "delete":
                handleSetupDelete(player, args);
                break;
            case "setlobby":
                handleSetupSetLobby(player);
                break;
            case "setspectator":
                handleSetupSetSpectator(player);
                break;
            case "setspawn":
                handleSetupSetSpawn(player, args);
                break;
            case "setbed":
                handleSetupSetBed(player, args);
                break;
            case "addgenerator":
                handleSetupAddGenerator(player, args);
                break;
            case "setminplayers":
                handleSetupSetMinPlayers(player, args);
                break;
            case "setmaxplayers":
                handleSetupSetMaxPlayers(player, args);
                break;
            case "info":
                handleSetupInfo(player);
                break;
            case "save":
                handleSetupSave(player);
                break;
            case "cancel":
                handleSetupCancel(player);
                break;
            case "list":
                handleSetupList(player);
                break;
            default:
                sendSetupHelp(player);
                break;
        }
    }

    private void sendSetupHelp(Player player) {
        MessageManager mm = plugin.getMessageManager();
        mm.sendMessage(player, "setup-help.header");
        mm.sendMessage(player, "setup-help.title");
        mm.sendMessage(player, "setup-help.separator");
        mm.sendMessage(player, "setup-help.create");
        mm.sendMessage(player, "setup-help.delete");
        mm.sendMessage(player, "setup-help.setlobby");
        mm.sendMessage(player, "setup-help.setspectator");
        mm.sendMessage(player, "setup-help.setspawn");
        mm.sendMessage(player, "setup-help.setbed");
        mm.sendMessage(player, "setup-help.addgenerator");
        mm.sendMessage(player, "setup-help.setminplayers");
        mm.sendMessage(player, "setup-help.setmaxplayers");
        mm.sendMessage(player, "setup-help.info");
        mm.sendMessage(player, "setup-help.save");
        mm.sendMessage(player, "setup-help.cancel");
        mm.sendMessage(player, "setup-help.list");
        mm.sendMessage(player, "setup-help.footer");
    }

    private void handleSetupCreate(Player player, String[] args) {
        MessageManager mm = plugin.getMessageManager();
        if (args.length < 3) {
            mm.sendMessage(player, "command.setup-create-usage");
            return;
        }

        String arenaName = args[2];
        if (arenaManager.arenaExists(arenaName)) {
            mm.sendMessage(player, "command.arena-already-exists", arenaName);
            return;
        }

        ArenaSetupSession session = arenaManager.getOrCreateSession(player.getUniqueId());
        session.setArenaName(arenaName);
        mm.sendMessage(player, "command.setup-started", arenaName);
        mm.sendMessage(player, "command.setup-use-commands");
    }

    private void handleSetupDelete(Player player, String[] args) {
        MessageManager mm = plugin.getMessageManager();
        if (args.length < 3) {
            mm.sendMessage(player, "command.setup-delete-usage");
            return;
        }

        String arenaName = args[2];
        if (arenaManager.deleteArena(arenaName)) {
            mm.sendMessage(player, "command.arena-deleted", arenaName);
        } else {
            mm.sendMessage(player, "command.arena-delete-failed");
        }
    }

    private void handleSetupSetLobby(Player player) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        session.setLobbySpawn(player.getLocation());
        mm.sendMessage(player, "command.lobby-spawn-set");
    }

    private void handleSetupSetSpectator(Player player) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        session.setSpectatorSpawn(player.getLocation());
        mm.sendMessage(player, "command.spectator-spawn-set");
    }

    private void handleSetupSetSpawn(Player player, String[] args) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        if (args.length < 3) {
            mm.sendMessage(player, "command.setup-setspawn-usage");
            String teams = String.join(", ",
                    java.util.Arrays.stream(TeamColor.values()).map(Enum::name).toArray(String[]::new));
            mm.sendMessage(player, "command.available-teams", teams);
            return;
        }

        try {
            TeamColor color = TeamColor.valueOf(args[2].toUpperCase());
            session.setTeamSpawn(color, player.getLocation());
            mm.sendMessage(player, "command.team-spawn-set", color.getChatColor() + color.name());
        } catch (IllegalArgumentException e) {
            mm.sendMessage(player, "command.invalid-team-color");
        }
    }

    private void handleSetupSetBed(Player player, String[] args) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        if (args.length < 3) {
            mm.sendMessage(player, "command.setup-setbed-usage");
            return;
        }

        try {
            TeamColor color = TeamColor.valueOf(args[2].toUpperCase());
            session.setBedLocation(color, player.getLocation());
            mm.sendMessage(player, "command.team-bed-set", color.getChatColor() + color.name());
        } catch (IllegalArgumentException e) {
            mm.sendMessage(player, "command.invalid-team-color");
        }
    }

    private void handleSetupAddGenerator(Player player, String[] args) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        if (args.length < 3) {
            mm.sendMessage(player, "command.setup-addgenerator-usage");
            return;
        }

        String genName = args[2];
        session.addGenerator(genName, player.getLocation());
        mm.sendMessage(player, "command.generator-added", genName);
    }

    private void handleSetupSetMinPlayers(Player player, String[] args) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        if (args.length < 3) {
            mm.sendMessage(player, "command.setup-setminplayers-usage");
            return;
        }

        try {
            int count = Integer.parseInt(args[2]);
            session.setMinPlayers(count);
            mm.sendMessage(player, "command.min-players-set", String.valueOf(count));
        } catch (NumberFormatException e) {
            mm.sendMessage(player, "command.invalid-number");
        }
    }

    private void handleSetupSetMaxPlayers(Player player, String[] args) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        if (args.length < 3) {
            mm.sendMessage(player, "command.setup-setmaxplayers-usage");
            return;
        }

        try {
            int count = Integer.parseInt(args[2]);
            session.setMaxPlayers(count);
            mm.sendMessage(player, "command.max-players-set", String.valueOf(count));
        } catch (NumberFormatException e) {
            mm.sendMessage(player, "command.invalid-number");
        }
    }

    private void handleSetupInfo(Player player) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        mm.sendMessage(player, "command.setup-info-header");
        mm.sendMessage(player, "command.setup-info-title");
        mm.sendMessage(player, "command.setup-info-separator");
        player.sendMessage(ChatColor.YELLOW + session.getProgress());
        mm.sendMessage(player, "command.setup-info-footer");
    }

    private void handleSetupSave(Player player) {
        MessageManager mm = plugin.getMessageManager();
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) {
            mm.sendMessage(player, "command.no-setup-session");
            return;
        }

        if (!session.isComplete()) {
            mm.sendMessage(player, "command.setup-incomplete");
            mm.sendMessage(player, "command.setup-incomplete-hint");
            return;
        }

        try {
            arenaManager.saveArena(session);
            mm.sendMessage(player, "command.arena-saved", session.getArenaName());

            // Load the arena into the game manager
            Arena arena = arenaManager.loadArena(session.getArenaName());
            if (arena != null) {
                plugin.getGameManager().createGame(session.getArenaName(), arena);
                mm.sendMessage(player, "command.arena-loaded");
            }

            arenaManager.removeSession(player.getUniqueId());
        } catch (IOException e) {
            mm.sendMessage(player, "command.arena-save-failed", e.getMessage());
        }
    }

    private void handleSetupCancel(Player player) {
        MessageManager mm = plugin.getMessageManager();
        arenaManager.removeSession(player.getUniqueId());
        mm.sendMessage(player, "command.setup-cancelled");
    }

    private void handleSetupList(Player player) {
        MessageManager mm = plugin.getMessageManager();
        mm.sendMessage(player, "configured-arenas.header");
        mm.sendMessage(player, "configured-arenas.title");
        mm.sendMessage(player, "configured-arenas.separator");

        for (String arenaName : arenaManager.getArenaNames()) {
            mm.sendMessage(player, "configured-arenas.arena-line", arenaName);
        }

        mm.sendMessage(player, "configured-arenas.footer");
    }

    private void handleForceStart(Player player) {
        MessageManager mm = plugin.getMessageManager();
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            mm.sendMessage(player, "command.must-be-in-game");
            return;
        }

        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
            mm.sendMessage(player, "command.game-already-running");
            return;
        }

        mm.sendMessage(player, "command.force-starting");
        game.forceStart();
    }

    private void handleForceStop(Player player) {
        MessageManager mm = plugin.getMessageManager();
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            mm.sendMessage(player, "command.must-be-in-game");
            return;
        }

        game.endGame(null);
        mm.sendMessage(player, "command.game-stopped");
    }

    private void handleResetWorld(Player player) {
        MessageManager mm = plugin.getMessageManager();
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            mm.sendMessage(player, "command.must-be-in-game");
            return;
        }

        int blockCount = game.getWorldResetManager().getBlockChangeCount();
        game.getWorldResetManager().resetWorld();
        mm.sendMessage(player, "command.world-reset", String.valueOf(blockCount));
    }

    private void handleReload(Player player) {
        MessageManager mm = plugin.getMessageManager();
        plugin.reloadConfig();
        mm.reload();
        mm.sendMessage(player, "command.config-reloaded");
    }

    /**
     * Send player to lobby server
     */
    private void handleLobby(Player player) {
        MessageManager mm = plugin.getMessageManager();
        if (!plugin.getBungeeManager().isEnabled()) {
            mm.sendMessage(player, "command.bungee-not-enabled");
            return;
        }

        // Leave game if in one
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            game.removePlayer(player);
        }

        mm.sendMessage(player, "command.sending-to-lobby");
        plugin.getBungeeManager().sendPlayerToLobby(player);
    }
}
