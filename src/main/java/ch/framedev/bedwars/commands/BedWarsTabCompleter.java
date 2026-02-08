package ch.framedev.bedwars.commands;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.arena.ArenaManager;
import ch.framedev.bedwars.team.TeamColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completion for BedWars commands
 */
public class BedWarsTabCompleter implements TabCompleter {

    private final BedWarsPlugin plugin;
    private final ArenaManager arenaManager;

    public BedWarsTabCompleter(BedWarsPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Main commands
            List<String> commands = new ArrayList<>(
                    Arrays.asList("join", "leave", "stats", "leaderboard", "top", "list", "spectate", "party",
                            "queue", "vote", "cosmetics", "achievements"));
            if (sender.hasPermission("bedwars.setup")) {
                commands.add("setup");
            }
            if (sender.hasPermission("bedwars.admin")) {
                commands.addAll(Arrays.asList("resetworld", "start", "stop", "reload"));
            }
            if (sender.hasPermission("bedwars.bungee")) {
                commands.add("lobby");
            }

            return commands.stream()
                    .filter(c -> c.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join":
                case "spectate":
                    // Arena names
                    return new ArrayList<>(arenaManager.getArenaNames()).stream()
                            .filter(a -> a.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "leaderboard":
                case "top":
                    return Arrays.asList("wins", "kills", "beds").stream()
                            .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "setup":
                    List<String> setupCommands = Arrays.asList(
                            "create", "delete", "setlobby", "setspectator",
                            "setspawn", "setbed", "setshop", "addgenerator", "setminplayers",
                            "setmaxplayers", "info", "save", "cancel", "list");
                    return setupCommands.stream()
                            .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "party":
                    List<String> partyCommands = Arrays.asList(
                            "create", "invite", "accept", "deny", "leave", "kick",
                            "promote", "disband", "list", "chat");
                    return partyCommands.stream()
                            .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "vote":
                    return Arrays.asList("open", "start", "end", "force").stream()
                            .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "setup":
                    switch (args[1].toLowerCase()) {
                        case "setspawn":
                        case "setbed":
                        case "setshop":
                            return Arrays.stream(TeamColor.values())
                                    .map(c -> c.name().toLowerCase())
                                    .filter(c -> c.startsWith(args[2].toLowerCase()))
                                    .collect(Collectors.toList());
                        case "delete":
                        case "setlobby":
                        case "setspectator":
                        case "addgenerator":
                        case "setminplayers":
                        case "setmaxplayers":
                        case "info":
                            // Arena names
                            return new ArrayList<>(arenaManager.getArenaNames()).stream()
                                    .filter(a -> a.toLowerCase().startsWith(args[2].toLowerCase()))
                                    .collect(Collectors.toList());
                    }
                    break;
                case "party":
                    switch (args[1].toLowerCase()) {
                        case "invite":
                        case "kick":
                        case "promote":
                        case "accept":
                        case "deny":
                            return plugin.getServer().getOnlinePlayers().stream()
                                    .map(p -> p.getName())
                                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                                    .collect(Collectors.toList());
                    }
                    break;
                case "vote":
                    if ("force".equalsIgnoreCase(args[1])) {
                        return new ArrayList<>(arenaManager.getArenaNames()).stream()
                                .filter(a -> a.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;
            }
        }

        if (args.length == 4) {
            if ("setup".equalsIgnoreCase(args[0])) {
                // No 4th argument suggestions needed for setup commands.
            }
        }

        return completions;
    }
}
