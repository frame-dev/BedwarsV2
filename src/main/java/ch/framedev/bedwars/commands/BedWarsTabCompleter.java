package ch.framedev.bedwars.commands;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.arena.ArenaManager;
import ch.framedev.bedwars.shop.ShopType;
import ch.framedev.bedwars.team.TeamColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tab completion for BedWars commands.
 * <p>
 * De-duplication / fixes:
 * - Central filter helper (startsWithIgnoreCase)
 * - Central arena name completion helper
 * - Central online player completion helper
 * - Central static lists (commands/setup/party/vote/leaderboard)
 * - Permission-aware suggestions
 */
public class BedWarsTabCompleter implements TabCompleter {

    private static final List<String> BASE_COMMANDS = List.of(
            "join", "leave", "stats", "leaderboard", "top", "list", "spectate", "party",
            "queue", "vote", "cosmetics", "achievements", "team"
    );

    private static final List<String> ADMIN_COMMANDS = List.of("resetworld", "start", "stop", "reload");
    private static final List<String> SETUP_COMMANDS = List.of(
            "create", "delete", "setlobby", "setspectator",
            "setspawn", "setbed", "setshop", "addgenerator",
            "setminplayers", "setmaxplayers", "info", "save", "cancel", "list"
    );

    private static final List<String> PARTY_COMMANDS = List.of(
            "create", "invite", "accept", "deny", "leave", "kick", "promote", "disband", "list", "chat"
    );

    private static final List<String> VOTE_COMMANDS = List.of("open", "start", "end", "force");
    private static final List<String> LEADERBOARD_CATEGORIES = List.of("wins", "kills", "beds");

    private final BedWarsPlugin plugin;
    private final ArenaManager arenaManager;

    public BedWarsTabCompleter(BedWarsPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String @NonNull [] args) {
        if (args.length == 0) return Collections.emptyList();

        // /bedwars <sub>
        if (args.length == 1) {
            List<String> cmds = new ArrayList<>(BASE_COMMANDS);

            if (sender.hasPermission("bedwars.setup")) cmds.add("setup");
            if (sender.hasPermission("bedwars.admin")) cmds.addAll(ADMIN_COMMANDS);
            if (sender.hasPermission("bedwars.bungee")) cmds.add("lobby");

            return filter(cmds, args[0]);
        }

        // /bedwars <sub> <arg2>
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            return switch (sub) {
                case "join", "spectate" -> completeArenas(args[1]);
                case "leaderboard", "top" -> filter(LEADERBOARD_CATEGORIES, args[1]);
                case "setup" -> sender.hasPermission("bedwars.setup") ? filter(SETUP_COMMANDS, args[1]) : Collections.emptyList();
                case "party" -> sender.hasPermission("bedwars.party") ? filter(PARTY_COMMANDS, args[1]) : Collections.emptyList();
                case "vote" -> filter(VOTE_COMMANDS, args[1]);
                case "team" -> filterTeamColors(args[1]);
                default -> Collections.emptyList();
            };
        }

        // /bedwars <sub> <arg2> <arg3>
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String sub2 = args[1].toLowerCase(Locale.ROOT);

            if ("setup".equals(sub)) {
                if (!sender.hasPermission("bedwars.setup")) return Collections.emptyList();

                return switch (sub2) {
                    // team color
                    case "setspawn", "setbed", "setshop" -> filterTeamColors(args[2]);

                    // arena name
                    case "delete", "info" -> completeArenas(args[2]);

                    // these take no arena argument in your command implementation
                    // (kept empty intentionally to avoid misleading completions)
                    case "setlobby", "setspectator", "addgenerator", "setminplayers", "setmaxplayers", "save", "cancel", "list", "create" ->
                            Collections.emptyList();

                    default -> Collections.emptyList();
                };
            }

            if ("party".equals(sub)) {
                if (!sender.hasPermission("bedwars.party")) return Collections.emptyList();

                return switch (sub2) {
                    case "invite", "kick", "promote", "accept", "deny" -> completeOnlinePlayers(args[2]);
                    default -> Collections.emptyList();
                };
            }

            if ("vote".equals(sub) && "force".equals(sub2)) {
                return completeArenas(args[2]);
            }

            return Collections.emptyList();
        }

        // /bedwars setup setshop <team> <type>
        if (args.length == 4) {
            if ("setup".equalsIgnoreCase(args[0]) && "setshop".equalsIgnoreCase(args[1])) {
                if (!sender.hasPermission("bedwars.setup")) return Collections.emptyList();
                return Arrays.stream(ShopType.values())
                        .map(t -> t.getConfigKey().toLowerCase(Locale.ROOT))
                        .filter(s -> startsWithIgnoreCase(s, args[3]))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private List<String> completeArenas(String prefix) {
        // arenaManager.getArenaNames() already returns a Set; filter it consistently
        return arenaManager.getArenaNames().stream()
                .filter(name -> startsWithIgnoreCase(name, prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private List<String> completeOnlinePlayers(String prefix) {
        return plugin.getServer().getOnlinePlayers().stream()
                .map(HumanEntity::getName)
                .filter(name -> startsWithIgnoreCase(name, prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private List<String> filterTeamColors(String prefix) {
        return Arrays.stream(TeamColor.values())
                .map(c -> c.name().toLowerCase(Locale.ROOT))
                .filter(c -> startsWithIgnoreCase(c, prefix))
                .collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String prefix) {
        if (options == null || options.isEmpty()) return Collections.emptyList();
        return options.stream()
                .filter(o -> startsWithIgnoreCase(o, prefix))
                .collect(Collectors.toList());
    }

    private boolean startsWithIgnoreCase(String value, String prefix) {
        if (prefix == null || prefix.isEmpty()) return true;
        if (value == null) return false;
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}