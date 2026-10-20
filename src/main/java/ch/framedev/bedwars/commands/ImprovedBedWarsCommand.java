package ch.framedev.bedwars.commands;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.arena.ArenaManager;
import ch.framedev.bedwars.arena.ArenaSetupSession;
import ch.framedev.bedwars.game.Arena;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.shop.ShopType;
import ch.framedev.bedwars.stats.PlayerStats;
import ch.framedev.bedwars.team.TeamColor;
import ch.framedev.bedwars.utils.MessageManager;
import ch.framedev.bedwars.utils.BedUtils;
import org.bukkit.block.Block;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * BedWars command handler with arena setup, spectator, stats, leaderboard, party, cosmetics, achievements.
 * <p>
 * De-duplication / fixes:
 * - Central permission gate helper (permOrMsg)
 * - Central "must be player" gate
 * - Central "message send" helper (mm())
 * - Central help rendering via list of keys (sendHelpKeys)
 * - Central setup session fetch helper (requireSetupSession)
 * - Central TeamColor parsing helper
 * - Central integer parsing helper
 * - Leaderboard handling via one method (handleLeaderboardCategory)
 */
public class ImprovedBedWarsCommand implements CommandExecutor {

    private final BedWarsPlugin plugin;
    private final ArenaManager arenaManager;

    public ImprovedBedWarsCommand(BedWarsPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;

        plugin.getDebugLogger().debug("Command: /" + label + " " + Arrays.toString(args) + " by " + player.getName());

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "join" -> {
                if (noPermOrMsg(player, "bedwars.join")) return true;

                if (args.length < 2) {
                    if (plugin.getMapVoteManager() != null) {
                        plugin.getMapVoteManager().joinQueue(player);
                        return true;
                    }
                    mm().sendMessage(player, "command.join-usage");
                    return true;
                }
                handleJoin(player, args[1]);
            }

            case "leave" -> {
                if (noPermOrMsg(player, "bedwars.leave")) return true;
                handleLeave(player);
            }

            case "spectate" -> {
                if (noPermOrMsg(player, "bedwars.spectate")) return true;
                if (args.length < 2) {
                    mm().sendMessage(player, "command.spectate-usage");
                    return true;
                }
                handleSpectate(player, args[1]);
            }

            case "stats" -> {
                if (noPermOrMsg(player, "bedwars.stats")) return true;
                if (args.length >= 2) handleStatsOther(player, args[1]);
                else handleStats(player);
            }

            case "leaderboard", "top" -> handleLeaderboard(player, args);

            case "setup" -> {
                if (noPermOrMsg(player, "bedwars.setup")) return true;
                handleSetup(player, args);
            }

            case "list" -> {
                if (noPermOrMsg(player, "bedwars.list")) return true;
                handleList(player);
            }

            case "start" -> {
                if (noPermOrMsg(player, "bedwars.admin")) return true;
                handleForceStart(player);
            }

            case "stop" -> {
                if (noPermOrMsg(player, "bedwars.admin")) return true;
                handleForceStop(player);
            }

            case "resetworld" -> {
                if (noPermOrMsg(player, "bedwars.admin")) return true;
                handleResetWorld(player);
            }

            case "reload" -> {
                if (noPermOrMsg(player, "bedwars.admin")) return true;
                handleReload(player);
            }

            case "lobby" -> {
                if (noPermOrMsg(player, "bedwars.bungee")) return true;
                handleLobby(player);
            }

            case "queue" -> {
                if (noPermOrMsg(player, "bedwars.queue")) return true;
                if (plugin.getMapVoteManager() != null) plugin.getMapVoteManager().joinQueue(player);
            }

            case "vote" -> {
                if (noPermOrMsg(player, "bedwars.vote")) return true;
                handleVote(player, args);
            }

            case "party" -> {
                if (noPermOrMsg(player, "bedwars.party")) return true;
                handleParty(player, args);
            }

            case "cosmetics" -> {
                if (noPermOrMsg(player, "bedwars.cosmetics")) return true;
                if (plugin.getCosmeticsManager() != null) plugin.getCosmeticsManager().openCosmeticsMenu(player);
            }

            case "achievements" -> {
                if (noPermOrMsg(player, "bedwars.achievements")) return true;
                if (plugin.getAchievementsManager() != null) plugin.getAchievementsManager().openMenu(player);
            }

            case "team" -> {
                if (noPermOrMsg(player, "bedwars.team")) return true;
                handleTeam(player, args);
            }

            default -> sendHelp(player);
        }

        return true;
    }

    /* --------------------------------------------------------------------- */
    /* Common helpers                                                         */
    /* --------------------------------------------------------------------- */

    private MessageManager mm() {
        return plugin.getMessageManager();
    }

    private Player asPlayer(CommandSender sender) {
        if (sender instanceof Player p) return p;
        mm().sendMessage(sender, "command.only-players");
        return null;
    }

    private boolean noPermOrMsg(Player player, String perm) {
        if (player.hasPermission(perm)) return false;
        mm().sendMessage(player, "command.no-permission");
        return true;
    }

    private void sendHelpKeys(Player player, List<String> keys) {
        for (String key : keys) {
            player.sendMessage(mm().getMessage(key));
        }
    }

    private TeamColor parseTeamColor(Player player, String raw) {
        try {
            return TeamColor.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            mm().sendMessage(player, "command.invalid-team-color");
            return null;
        }
    }

    private Integer parseIntOrMsg(Player player, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            mm().sendMessage(player, "command.invalid-number");
            return null;
        }
    }

    private ArenaSetupSession requireSetupSession(Player player) {
        ArenaSetupSession session = arenaManager.getSession(player.getUniqueId());
        if (session == null) mm().sendMessage(player, "command.no-setup-session");
        return session;
    }

    /* --------------------------------------------------------------------- */
    /* Help                                                                    */
    /* --------------------------------------------------------------------- */

    private void sendHelp(Player player) {
        List<String> keys = new ArrayList<>(List.of(
                "help.header",
                "help.title",
                "help.separator",
                "help.join",
                "help.leave",
                "help.spectate",
                "help.stats",
                "help.leaderboard",
                "help.list",
                "help.queue",
                "help.vote",
                "help.party",
                "help.cosmetics",
                "help.achievements",
                "help.team"
        ));

        if (player.hasPermission("bedwars.setup")) {
            keys.addAll(List.of("help.separator", "help.setup"));
        }
        if (plugin.getBungeeManager().isEnabled() && player.hasPermission("bedwars.bungee")) {
            keys.addAll(List.of("help.separator", "help.lobby"));
        }
        if (player.hasPermission("bedwars.admin")) {
            keys.addAll(List.of(
                    "help.separator",
                    "help.start",
                    "help.stop",
                    "help.resetworld",
                    "help.reload"
            ));
        }
        if (player.hasPermission("bedwars.vote.admin")) {
            keys.addAll(List.of("help.separator", "help.vote-admin"));
        }

        keys.add("help.footer");
        sendHelpKeys(player, keys);
    }

    /* --------------------------------------------------------------------- */
    /* Join / Leave / Spectate                                                 */
    /* --------------------------------------------------------------------- */

    private void handleJoin(Player player, String arenaName) {
        Game currentGame = plugin.getGameManager().getPlayerGame(player);
        if (currentGame != null) {
            if (currentGame.isSpectator(player)) currentGame.removeSpectator(player);
            else {
                mm().sendMessage(player, "command.already-in-game");
                return;
            }
        }

        plugin.getDebugLogger().debug("Join requested: player=" + player.getName() + " arena=" + arenaName);

        // CloudNet one-arena-per-server shortcut (party support)
        if (plugin.getCloudNetManager() != null
                && plugin.getCloudNetManager().isEnabled()
                && plugin.getCloudNetManager().isOneArenaPerServer()) {

            var partyManager = plugin.getPartyManager();
            var party = partyManager.getParty(player.getUniqueId());

            if (party != null && !partyManager.isLeader(player.getUniqueId())) {
                mm().sendMessage(player, "party.only-leader");
                return;
            }

            if (party != null) {
                for (var memberId : party.getMemberUuids()) {
                    Player member = plugin.getServer().getPlayer(memberId);
                    if (member != null) plugin.getCloudNetManager().connectToArena(member, arenaName);
                }
            } else {
                plugin.getCloudNetManager().connectToArena(player, arenaName);
            }
            return;
        }

        Game game = plugin.getGameManager().getGame(arenaName);
        if (game == null) {
            mm().sendMessage(player, "command.arena-not-found", arenaName);
            return;
        }

        var partyManager = plugin.getPartyManager();
        var party = partyManager.getParty(player.getUniqueId());

        if (party != null && !partyManager.isLeader(player.getUniqueId())) {
            mm().sendMessage(player, "party.only-leader");
            return;
        }

        if (party != null) {
            joinPartyToGame(player, game);
            return;
        }

        game.addPlayer(player);
    }

    private void joinPartyToGame(Player leader, Game game) {
        // party type is unknown here (your Party class), so use var everywhere in calls above.
        // We keep this method signature broad so the file compiles only if Party type is visible.
        var partyManager = plugin.getPartyManager();
        var party = partyManager.getParty(leader.getUniqueId());
        if (party == null) {
            game.addPlayer(leader);
            return;
        }

        List<Player> joiners = new ArrayList<>();
        for (var memberId : party.getMemberUuids()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member == null) {
                String name = plugin.getServer().getOfflinePlayer(memberId).getName();
                mm().sendMessage(leader, "party.member-offline", name == null ? "Unknown" : name);
                return;
            }

            Game memberGame = plugin.getGameManager().getPlayerGame(member);
            if (memberGame != null && !memberGame.equals(game)) {
                mm().sendMessage(leader, "party.member-in-game", member.getName());
                return;
            }

            if (memberGame == null) joiners.add(member);
        }

        if (game.getPlayers().size() + joiners.size() > game.getArena().getMaxPlayers()) {
            mm().sendMessage(leader, "party.party-game-full");
            return;
        }

        for (Player p : joiners) game.addPlayer(p);
    }

    private void handleLeave(Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            if (plugin.getMapVoteManager() != null && plugin.getMapVoteManager().isQueued(player.getUniqueId())) {
                plugin.getMapVoteManager().leaveQueue(player);
                return;
            }
            mm().sendMessage(player, "command.not-in-game");
            return;
        }

        plugin.getDebugLogger().debug("Leave requested: player=" + player.getName() + " arena=" + game.getArena().getName());

        var partyManager = plugin.getPartyManager();
        var party = partyManager != null ? partyManager.getParty(player.getUniqueId()) : null;

        if (party != null) {
            for (var memberId : party.getMemberUuids()) {
                Player member = plugin.getServer().getPlayer(memberId);
                if (member == null) continue;

                Game memberGame = plugin.getGameManager().getPlayerGame(member);
                if (memberGame == null || !memberGame.equals(game)) continue;

                if (memberGame.isSpectator(member)) memberGame.removeSpectator(member);
                else memberGame.removePlayer(member);

                if (!member.getUniqueId().equals(player.getUniqueId())) {
                    mm().sendMessage(member, "command.left-game");
                }
            }
        } else {
            if (game.isSpectator(player)) game.removeSpectator(player);
            else game.removePlayer(player);
        }

        mm().sendMessage(player, "command.left-game");
    }

    private void handleSpectate(Player player, String arenaName) {
        if (plugin.getGameManager().getPlayerGame(player) != null) {
            mm().sendMessage(player, "command.leave-game-first");
            return;
        }

        plugin.getDebugLogger().debug("Spectate requested: player=" + player.getName() + " arena=" + arenaName);

        Game game = plugin.getGameManager().getGame(arenaName);
        if (game == null) {
            mm().sendMessage(player, "command.arena-not-found", arenaName);
            return;
        }

        game.addSpectator(player);
    }

    /* --------------------------------------------------------------------- */
    /* Stats                                                                   */
    /* --------------------------------------------------------------------- */

    private void handleStats(Player player) {
        plugin.getDebugLogger().debug("Stats requested: player=" + player.getName());
        PlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());
        displayStats(player, player.getName(), stats);
    }

    private void handleStatsOther(Player player, String targetName) {
        plugin.getDebugLogger().debug("Stats requested: player=" + player.getName() + " target=" + targetName);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

            if (!target.hasPlayedBefore()) {
                mm().sendMessage(player, "command.player-not-found", targetName);
                return;
            }

            PlayerStats stats = plugin.getStatsManager().getPlayerStats(target.getUniqueId());
            plugin.getServer().getScheduler().runTask(plugin, () -> displayStats(player, target.getName(), stats));
        });
    }

    private void displayStats(Player viewer, String playerName, PlayerStats stats) {
        sendHelpKeys(viewer, List.of("stats.header"));
        viewer.sendMessage(mm().getMessage("stats.title", playerName));
        sendHelpKeys(viewer, List.of("stats.separator"));
        viewer.sendMessage(mm().getMessage("stats.games-played", stats.getGamesPlayed()));
        viewer.sendMessage(mm().getMessage("stats.wins", stats.getWins(), stats.getLosses()));
        viewer.sendMessage(mm().getMessage("stats.win-rate", String.format(Locale.ROOT, "%.1f", stats.getWinRate())));
        viewer.sendMessage(mm().getMessage("stats.kills", stats.getKills(), stats.getDeaths()));
        viewer.sendMessage(mm().getMessage("stats.kd-ratio", String.format(Locale.ROOT, "%.2f", stats.getKDRatio())));
        viewer.sendMessage(mm().getMessage("stats.final-kills", stats.getFinalKills()));
        viewer.sendMessage(mm().getMessage("stats.beds-broken", stats.getBedsBroken()));
        sendHelpKeys(viewer, List.of("stats.footer"));
    }

    /* --------------------------------------------------------------------- */
    /* Arena list                                                             */
    /* --------------------------------------------------------------------- */

    private void handleList(Player player) {
        plugin.getDebugLogger().debug("List arenas requested by " + player.getName());

        sendHelpKeys(player, List.of("arena-list.header", "arena-list.title", "arena-list.separator"));

        for (Game game : plugin.getGameManager().getGames()) {
            String status = getStatusColor(game) + game.getState().toString();
            int playerCount = game.getPlayers().size();
            int spectatorCount = game.getSpectators().size();

            player.sendMessage(mm().getMessage("arena-list.arena-line", game.getArena().getName()));
            player.sendMessage(mm().getMessage("arena-list.arena-info",
                    playerCount, game.getArena().getMaxPlayers(), spectatorCount, status));
        }

        player.sendMessage(mm().getMessage("arena-list.footer"));
    }

    private ChatColor getStatusColor(Game game) {
        return switch (game.getState()) {
            case WAITING -> ChatColor.GRAY;
            case STARTING -> ChatColor.YELLOW;
            case RUNNING -> ChatColor.GREEN;
            case ENDING -> ChatColor.RED;
        };
    }

    /* --------------------------------------------------------------------- */
    /* Leaderboard                                                            */
    /* --------------------------------------------------------------------- */

    private void handleLeaderboard(Player player, String[] args) {
        String category = (args.length > 1) ? args[1].toLowerCase(Locale.ROOT) : "wins";

        plugin.getDebugLogger().debug("Leaderboard requested: player=" + player.getName() + " category=" + category);
        mm().sendMessage(player, "command.loading-leaderboard");

        switch (category) {
            case "wins", "win" -> handleLeaderboardCategory(
                    player,
                    mm().getMessage("leaderboard.top-wins"),
                    "wins",
                    ChatColor.GREEN,
                    () -> plugin.getStatsManager().getTopWins(10)
            );

            case "kills", "kill" -> handleLeaderboardCategory(
                    player,
                    mm().getMessage("leaderboard.top-kills"),
                    "kills",
                    ChatColor.RED,
                    () -> plugin.getStatsManager().getTopKills(10)
            );

            case "beds", "bed" -> handleLeaderboardCategory(
                    player,
                    mm().getMessage("leaderboard.top-beds"),
                    "beds",
                    ChatColor.AQUA,
                    () -> plugin.getStatsManager().getTopBedsBroken(10)
            );

            default -> mm().sendMessage(player, "command.leaderboard-usage");
        }
    }

    private interface LeaderSupplier {
        java.util.concurrent.CompletableFuture<Map<String, Integer>> get();
    }

    private void handleLeaderboardCategory(Player player,
                                           String title,
                                           String statKey,
                                           ChatColor color,
                                           LeaderSupplier supplier) {
        supplier.get().thenAccept(leaders -> displayLeaderboard(player, title, leaders, statKey, color));
    }

    private void displayLeaderboard(Player player, String title, Map<String, Integer> leaders, String stat, ChatColor color) {
        player.sendMessage(mm().getMessage("leaderboard.header"));
        player.sendMessage(mm().getMessage("leaderboard.title", title));
        player.sendMessage(mm().getMessage("leaderboard.separator"));

        int rank = 1;
        for (Map.Entry<String, Integer> entry : leaders.entrySet()) {
            player.sendMessage(mm().getMessage("leaderboard.entry",
                    medal(rank), rank, entry.getKey(), color, entry.getValue(), stat));
            rank++;
        }

        player.sendMessage(mm().getMessage("leaderboard.footer"));
    }

    private String medal(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> " ";
        };
    }

    /* --------------------------------------------------------------------- */
    /* Setup                                                                  */
    /* --------------------------------------------------------------------- */

    private void handleSetup(Player player, String[] args) {
        if (args.length < 2) {
            sendSetupHelp(player);
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        plugin.getDebugLogger().debug("Setup command: player=" + player.getName() + " action=" + sub);

        switch (sub) {
            case "create" -> handleSetupCreate(player, args);
            case "delete" -> handleSetupDelete(player, args);
            case "setlobby" -> withSession(player, s -> {
                s.setLobbySpawn(player.getLocation());
                mm().sendMessage(player, "command.lobby-spawn-set");
            });
            case "setspectator" -> withSession(player, s -> {
                s.setSpectatorSpawn(player.getLocation());
                mm().sendMessage(player, "command.spectator-spawn-set");
            });
            case "setspawn" -> handleSetupSetSpawn(player, args);
            case "setbed" -> handleSetupSetBed(player, args);
            case "setshop" -> handleSetupSetShop(player, args);
            case "addgenerator" -> handleSetupAddGenerator(player, args);
            case "setminplayers" -> handleSetupSetMinPlayers(player, args);
            case "setmaxplayers" -> handleSetupSetMaxPlayers(player, args);
            case "info" -> handleSetupInfo(player);
            case "save" -> handleSetupSave(player);
            case "cancel" -> handleSetupCancel(player);
            case "list" -> handleSetupList(player);
            default -> sendSetupHelp(player);
        }
    }

    private void withSession(Player player, Consumer<ArenaSetupSession> action) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;
        action.accept(session);
        plugin.getDebugLogger().debug("Setup session updated: player=" + player.getName() + " arena=" + session.getArenaName());
    }

    private void sendSetupHelp(Player player) {
        MessageManager mm = mm();
        mm.sendMessage(player, "setup-help.header");
        mm.sendMessage(player, "setup-help.title");
        mm.sendMessage(player, "setup-help.separator");

        for (String key : List.of(
                "setup-help.create",
                "setup-help.delete",
                "setup-help.setlobby",
                "setup-help.setspectator",
                "setup-help.setspawn",
                "setup-help.setbed",
                "setup-help.setshop",
                "setup-help.addgenerator",
                "setup-help.setminplayers",
                "setup-help.setmaxplayers",
                "setup-help.info",
                "setup-help.save",
                "setup-help.cancel",
                "setup-help.list"
        )) {
            mm.sendMessage(player, key);
        }

        mm.sendMessage(player, "setup-help.footer");
    }

    private void handleSetupCreate(Player player, String[] args) {
        if (args.length < 3) {
            mm().sendMessage(player, "command.setup-create-usage");
            return;
        }

        String arenaName = args[2];
        if (arenaManager.arenaExists(arenaName)) {
            mm().sendMessage(player, "command.arena-already-exists", arenaName);
            return;
        }

        ArenaSetupSession session = arenaManager.getOrCreateSession(player.getUniqueId());
        session.setArenaName(arenaName);

        plugin.getDebugLogger().debug("Setup create: player=" + player.getName() + " arena=" + arenaName);
        mm().sendMessage(player, "command.setup-started", arenaName);
        mm().sendMessage(player, "command.setup-use-commands");
    }

    private void handleSetupDelete(Player player, String[] args) {
        if (args.length < 3) {
            mm().sendMessage(player, "command.setup-delete-usage");
            return;
        }

        String arenaName = args[2];
        plugin.getDebugLogger().debug("Setup delete: player=" + player.getName() + " arena=" + arenaName);

        if (arenaManager.deleteArena(arenaName)) mm().sendMessage(player, "command.arena-deleted", arenaName);
        else mm().sendMessage(player, "command.arena-delete-failed");
    }

    private void handleSetupSetSpawn(Player player, String[] args) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;

        if (args.length < 3) {
            mm().sendMessage(player, "command.setup-setspawn-usage");
            String teams = String.join(", ", Arrays.stream(TeamColor.values()).map(Enum::name).toArray(String[]::new));
            mm().sendMessage(player, "command.available-teams", teams);
            return;
        }

        TeamColor color = parseTeamColor(player, args[2]);
        if (color == null) return;

        session.setTeamSpawn(color, player.getLocation());
        mm().sendMessage(player, "command.team-spawn-set", color.getChatColor() + color.name());
    }

    private void handleSetupSetBed(Player player, String[] args) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;

        if (args.length < 3) {
            mm().sendMessage(player, "command.setup-setbed-usage");
            return;
        }

        TeamColor color = parseTeamColor(player, args[2]);
        if (color == null) return;

        Block foot = BedUtils.getFoot(player.getTargetBlockExact(5));
        if (foot == null) {
            mm().sendMessage(player, "command.setup-invalid-bed");
            return;
        }
        session.setBedLocation(color, foot.getLocation());
        mm().sendMessage(player, "command.team-bed-set", color.getChatColor() + color.name());
    }

    private void handleSetupSetShop(Player player, String[] args) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;

        if (args.length < 4) {
            mm().sendMessage(player, "command.setup-setshop-usage");
            mm().sendMessage(player, "command.available-shop-types", "item, upgrade");
            return;
        }

        TeamColor color = parseTeamColor(player, args[2]);
        if (color == null) return;

        ShopType type = ShopType.fromString(args[3]);
        if (type == null) {
            mm().sendMessage(player, "command.invalid-shop-type");
            mm().sendMessage(player, "command.available-shop-types", "item, upgrade");
            return;
        }

        session.setShopLocation(color, type, player.getLocation());
        mm().sendMessage(player, "command.team-shop-set", color.getChatColor() + color.name(), type.getDisplayName());
    }

    private void handleSetupAddGenerator(Player player, String[] args) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;

        if (args.length < 3) {
            mm().sendMessage(player, "command.setup-addgenerator-usage");
            return;
        }

        String genName = args[2];
        session.addGenerator(genName, player.getLocation());
        mm().sendMessage(player, "command.generator-added", genName);
    }

    private void handleSetupSetMinPlayers(Player player, String[] args) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;

        if (args.length < 3) {
            mm().sendMessage(player, "command.setup-setminplayers-usage");
            return;
        }

        Integer count = parseIntOrMsg(player, args[2]);
        if (count == null) return;

        session.setMinPlayers(count);
        mm().sendMessage(player, "command.min-players-set", String.valueOf(count));
    }

    private void handleSetupSetMaxPlayers(Player player, String[] args) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;

        if (args.length < 3) {
            mm().sendMessage(player, "command.setup-setmaxplayers-usage");
            return;
        }

        Integer count = parseIntOrMsg(player, args[2]);
        if (count == null) return;

        session.setMaxPlayers(count);
        mm().sendMessage(player, "command.max-players-set", String.valueOf(count));
    }

    private void handleSetupInfo(Player player) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;

        mm().sendMessage(player, "command.setup-info-header");
        mm().sendMessage(player, "command.setup-info-title");
        mm().sendMessage(player, "command.setup-info-separator");
        player.sendMessage(ChatColor.YELLOW + session.getProgress());
        mm().sendMessage(player, "command.setup-info-footer");
    }

    private void handleSetupSave(Player player) {
        ArenaSetupSession session = requireSetupSession(player);
        if (session == null) return;

        if (!session.isComplete()) {
            mm().sendMessage(player, "command.setup-incomplete");
            mm().sendMessage(player, "command.setup-incomplete-hint");
            return;
        }

        try {
            arenaManager.saveArena(session);
            mm().sendMessage(player, "command.arena-saved", session.getArenaName());

            Arena arena = arenaManager.loadArena(session.getArenaName());
            if (arena != null) {
                plugin.getGameManager().createGame(session.getArenaName(), arena);
                mm().sendMessage(player, "command.arena-loaded");
            }

            arenaManager.removeSession(player.getUniqueId());
        } catch (IOException e) {
            mm().sendMessage(player, "command.arena-save-failed", e.getMessage());
        }
    }

    private void handleSetupCancel(Player player) {
        arenaManager.removeSession(player.getUniqueId());
        mm().sendMessage(player, "command.setup-cancelled");
    }

    private void handleSetupList(Player player) {
        mm().sendMessage(player, "configured-arenas.header");
        mm().sendMessage(player, "configured-arenas.title");
        mm().sendMessage(player, "configured-arenas.separator");

        for (String arenaName : arenaManager.getArenaNames()) {
            mm().sendMessage(player, "configured-arenas.arena-line", arenaName);
        }

        mm().sendMessage(player, "configured-arenas.footer");
    }

    /* --------------------------------------------------------------------- */
    /* Admin actions                                                          */
    /* --------------------------------------------------------------------- */

    private void handleForceStart(Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            mm().sendMessage(player, "command.must-be-in-game");
            return;
        }

        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
            mm().sendMessage(player, "command.game-already-running");
            return;
        }

        mm().sendMessage(player, "command.force-starting");
        game.forceStart();
    }

    private void handleForceStop(Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            mm().sendMessage(player, "command.must-be-in-game");
            return;
        }

        game.endGame(null);
        mm().sendMessage(player, "command.game-stopped");
    }

    private void handleResetWorld(Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            mm().sendMessage(player, "command.must-be-in-game");
            return;
        }

        int blockCount = game.getWorldResetManager().getBlockChangeCount();
        game.getWorldResetManager().resetWorld();
        mm().sendMessage(player, "command.world-reset", String.valueOf(blockCount));
    }

    private void handleReload(Player player) {
        plugin.reloadConfig();
        mm().reload();
        if (plugin.getShopManager() != null) {
            plugin.getShopManager().reload();
        }
        mm().sendMessage(player, "command.config-reloaded");
    }

    private void handleLobby(Player player) {
        if (!plugin.getBungeeManager().isEnabled()) {
            mm().sendMessage(player, "command.bungee-not-enabled");
            return;
        }

        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) game.removePlayer(player);

        mm().sendMessage(player, "command.sending-to-lobby");
        plugin.getBungeeManager().sendPlayerToLobby(player);
    }

    /* --------------------------------------------------------------------- */
    /* Vote                                                                   */
    /* --------------------------------------------------------------------- */

    private void handleVote(Player player, String[] args) {
        if (plugin.getMapVoteManager() == null) return;

        if (args.length > 1) {
            String sub = args[1].toLowerCase(Locale.ROOT);

            switch (sub) {
                case "open" -> {
                    if (!plugin.getMapVoteManager().isQueued(player.getUniqueId())) {
                        mm().sendMessage(player, "map-vote.not-queued");
                        return;
                    }
                    plugin.getMapVoteManager().openVoteGui(player);
                }

                case "start" -> {
                    if (noPermOrMsg(player, "bedwars.vote.admin")) return;
                    plugin.getMapVoteManager().forceStartVoting(player);
                }

                case "end" -> {
                    if (noPermOrMsg(player, "bedwars.vote.admin")) return;
                    plugin.getMapVoteManager().forceEndVoting(player);
                }

                case "force" -> {
                    if (noPermOrMsg(player, "bedwars.vote.admin")) return;
                    if (args.length < 3) {
                        mm().sendMessage(player, "map-vote.force-usage");
                        return;
                    }
                    plugin.getMapVoteManager().forceSelectArena(player, args[2]);
                }

                default -> mm().sendMessage(player, "map-vote.vote-usage");
            }
            return;
        }

        if (!plugin.getMapVoteManager().isQueued(player.getUniqueId())) {
            mm().sendMessage(player, "map-vote.not-queued");
            return;
        }
        plugin.getMapVoteManager().openVoteGui(player);
    }

    /* --------------------------------------------------------------------- */
    /* Team                                                                   */
    /* --------------------------------------------------------------------- */

    private void handleTeam(Player player, String[] args) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            mm().sendMessage(player, "command.not-in-game");
            return;
        }

        if (args.length >= 2) {
            TeamColor color = parseTeamColor(player, args[1]);
            if (color != null) game.changeTeam(player, color);
            return;
        }

        if (plugin.getTeamSelectionGUI() != null) plugin.getTeamSelectionGUI().openTeamSelection(player, game);
        else mm().sendMessage(player, "team.gui-not-available");
    }

    /* --------------------------------------------------------------------- */
    /* Party                                                                  */
    /* --------------------------------------------------------------------- */

    private void handleParty(Player player, String[] args) {
        if (args.length < 2) {
            sendPartyHelp(player);
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "create" -> plugin.getPartyManager().createParty(player);

            case "invite" -> {
                if (noPermOrMsg(player, "bedwars.party.invite")) return;
                if (args.length < 3) {
                    mm().sendMessage(player, "party.invite-usage");
                    return;
                }
                handlePartyInvite(player, args[2]);
            }

            case "accept" -> {
                if (args.length < 3) {
                    mm().sendMessage(player, "party.accept-usage");
                    return;
                }
                plugin.getPartyManager().acceptInvite(player, args[2]);
            }

            case "deny" -> {
                if (args.length < 3) {
                    mm().sendMessage(player, "party.deny-usage");
                    return;
                }
                plugin.getPartyManager().denyInvite(player, args[2]);
            }

            case "leave" -> plugin.getPartyManager().leaveParty(player);

            case "kick" -> {
                if (noPermOrMsg(player, "bedwars.party.kick")) return;
                if (args.length < 3) {
                    mm().sendMessage(player, "party.kick-usage");
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    mm().sendMessage(player, "party.player-not-online");
                    return;
                }
                plugin.getPartyManager().kickMember(player, target);
            }

            case "promote" -> {
                if (noPermOrMsg(player, "bedwars.party.promote")) return;
                if (args.length < 3) {
                    mm().sendMessage(player, "party.promote-usage");
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    mm().sendMessage(player, "party.player-not-online");
                    return;
                }
                plugin.getPartyManager().promoteMember(player, target);
            }

            case "disband" -> plugin.getPartyManager().disbandParty(player);

            case "list" -> plugin.getPartyManager().listParty(player);

            case "chat" -> {
                if (args.length < 3) {
                    mm().sendMessage(player, "party.chat-usage");
                    return;
                }
                String msg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                plugin.getPartyManager().sendPartyChat(player, msg);
            }

            default -> sendPartyHelp(player);
        }
    }

    private void handlePartyInvite(Player player, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target != null) {
            plugin.getPartyManager().invitePlayer(player, target);
            return;
        }

        @SuppressWarnings("deprecation")
        var offline = plugin.getServer().getOfflinePlayer(targetName);

        if (!offline.hasPlayedBefore() && !offline.isOnline()) {
            mm().sendMessage(player, "party.player-not-online");
            return;
        }

        plugin.getPartyManager().invitePlayer(player, offline.getUniqueId(), offline.getName());
    }

    private void sendPartyHelp(Player player) {
        MessageManager mm = mm();
        mm.sendMessage(player, "party-help.header");
        mm.sendMessage(player, "party-help.title");
        mm.sendMessage(player, "party-help.separator");

        for (String key : List.of(
                "party-help.create",
                "party-help.invite",
                "party-help.accept",
                "party-help.deny",
                "party-help.leave",
                "party-help.kick",
                "party-help.promote",
                "party-help.disband",
                "party-help.list",
                "party-help.chat"
        )) {
            mm.sendMessage(player, key);
        }

        mm.sendMessage(player, "party-help.footer");
    }
}
