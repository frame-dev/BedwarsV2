package ch.framedev.bedwars.game;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

/**
 * Improved BedWars scoreboard:
 * - Stable lines using unique entries
 * - Team-based prefix/suffix (supports longer lines & no flicker)
 * - Per-player teams to avoid collisions
 * - Safe color splitting
 */
public class GameScoreboard {

    private static final String OBJECTIVE_NAME = "bw_main";
    private static final int MAX_LINES = 15;

    // Unique entries for each line (must be unique strings)
    private static final String[] ENTRIES = new String[MAX_LINES];
    static {
        // Use distinct color codes as entries (ChatColor has >= 16)
        for (int i = 0; i < MAX_LINES; i++) {
            ENTRIES[i] = ChatColor.values()[i].toString() + ChatColor.RESET;
        }
    }

    private final BedWarsPlugin plugin;
    private final Game game;

    private BukkitRunnable updateTask;

    // player -> (lineIndex -> team)
    private final Map<UUID, Map<Integer, org.bukkit.scoreboard.Team>> lineTeams = new HashMap<>();

    public GameScoreboard(BedWarsPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    public void show(Player player) {
        if (player == null) return;

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective(
                OBJECTIVE_NAME,
                "dummy",
                color(plugin.getConfig().getString("scoreboard.title", "&6&lBEDWARS"))
        );
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Pre-register all lines (stable ordering)
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = ENTRIES[i];
            obj.getScore(entry).setScore(MAX_LINES - i);
        }

        player.setScoreboard(sb);
        ensureTeams(player, sb);
        update(player);
    }

    public void hide(Player player) {
        if (player == null) return;
        lineTeams.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void update(Player player) {
        if (player == null) return;

        Scoreboard sb = player.getScoreboard();
        if (sb == null) return;

        Objective obj = sb.getObjective(DisplaySlot.SIDEBAR);
        if (obj == null || !OBJECTIVE_NAME.equals(obj.getName())) return;

        obj.setDisplayName(color(plugin.getConfig().getString("scoreboard.title", "&6&lBEDWARS")));

        ensureTeams(player, sb);

        List<String> lines = buildLines(player);
        if (lines.size() > MAX_LINES) {
            // Hard cap; better to trim than overflow scoreboard
            lines = lines.subList(0, MAX_LINES);
        }

        // Write lines
        for (int i = 0; i < MAX_LINES; i++) {
            org.bukkit.scoreboard.Team team = getLineTeam(player.getUniqueId(), i, sb);

            if (i < lines.size()) {
                String raw = lines.get(i);
                if (raw == null) raw = "";

                // Split into prefix/suffix safely
                SplitText split = splitLegacy(raw, 64); // total target; we still split for compatibility
                team.setPrefix(split.prefix);
                team.setSuffix(split.suffix);
            } else {
                // Clear unused lines
                team.setPrefix("");
                team.setSuffix("");
            }
        }
    }

    private void ensureTeams(Player player, Scoreboard sb) {
        UUID uuid = player.getUniqueId();
        lineTeams.computeIfAbsent(uuid, k -> new HashMap<>());

        for (int i = 0; i < MAX_LINES; i++) {
            getLineTeam(uuid, i, sb); // ensure it exists
        }
    }

    private org.bukkit.scoreboard.Team getLineTeam(UUID uuid, int lineIndex, Scoreboard sb) {
        Map<Integer, org.bukkit.scoreboard.Team> teams = lineTeams.computeIfAbsent(uuid, k -> new HashMap<>());
        org.bukkit.scoreboard.Team team = teams.get(lineIndex);
        if (team != null) return team;

        // Per-player unique team names; max 16 chars in older versions, but modern allows more.
        // Keep it short: "bw" + 6 chars uuid + lineIndex
        String id = uuid.toString().replace("-", "");
        String teamName = ("bw" + id.substring(0, 6) + lineIndex);
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        team = sb.getTeam(teamName);
        if (team == null) team = sb.registerNewTeam(teamName);

        String entry = ENTRIES[lineIndex];
        if (!team.hasEntry(entry)) team.addEntry(entry);

        teams.put(lineIndex, team);
        return team;
    }

    private List<String> buildLines(Player player) {
        List<String> lines = new ArrayList<>();
        GameState state = game.getState();
        String arenaName = game.getArena().getName();

        lines.add(ChatColor.GRAY + arenaName);
        lines.add("");

        if (state == GameState.WAITING) {
            lines.add(ChatColor.YELLOW + "Players: " + ChatColor.WHITE
                    + game.getPlayers().size() + "/" + game.getArena().getMaxPlayers());
            lines.add(ChatColor.GRAY + "Waiting for players...");
            return lines;
        }

        if (state == GameState.STARTING) {
            lines.add(ChatColor.YELLOW + "Starting in: " + ChatColor.WHITE + game.getCountdown() + "s");
            lines.add(ChatColor.YELLOW + "Players: " + ChatColor.WHITE
                    + game.getPlayers().size() + "/" + game.getArena().getMaxPlayers());
            return lines;
        }

        if (state == GameState.ENDING) {
            lines.add(ChatColor.GRAY + "Game ended");
            return lines;
        }

        // RUNNING
        int elapsed = game.getGameElapsedSeconds();
        lines.add(ChatColor.AQUA + "Time: " + ChatColor.WHITE + formatTime(elapsed));
        lines.add("");

        GamePlayer gp = game.getGamePlayer(player);

        if (gp != null && gp.getTeam() != null) {
            lines.add(ChatColor.GRAY + "Your team: " + gp.getTeam().getColor().getChatColor()
                    + gp.getTeam().getColor().name());
            lines.add("");
        }

        lines.add(ChatColor.DARK_GRAY + "Teams:");
        // stable ordering by team color/name (prevents lines jumping)
        List<Team> teams = new ArrayList<>(game.getTeams().values());
        teams.sort(Comparator.comparing(t -> t.getColor().name()));

        for (Team team : teams) {
            String bed = team.isBedAlive() ? (ChatColor.GREEN + "✓") : (ChatColor.RED + "✗");
            long alive = team.getPlayers().stream().filter(p -> !p.isEliminated()).count();
            lines.add(" " + team.getColor().getChatColor() + team.getColor().name()
                    + ChatColor.GRAY + ": " + bed + ChatColor.GRAY + " (" + alive + ")");
        }

        lines.add("");

        if (gp != null) {
            lines.add(ChatColor.GRAY + "Kills: " + ChatColor.WHITE + gp.getKills());
            lines.add(ChatColor.GRAY + "Final Kills: " + ChatColor.WHITE + gp.getFinalKills());
            lines.add(ChatColor.GRAY + "Beds: " + ChatColor.WHITE + gp.getBedsBroken());
            lines.add("");
        }

        int diamondTime = game.getDiamondUpgradeTime();
        int emeraldTime = game.getEmeraldUpgradeTime();

        if (diamondTime > 0) {
            int remaining = Math.max(0, diamondTime - elapsed);
            lines.add(ChatColor.AQUA + "Diamond II: " + ChatColor.WHITE + formatTime(remaining));
        }
        if (emeraldTime > 0) {
            int remaining = Math.max(0, emeraldTime - elapsed);
            lines.add(ChatColor.GREEN + "Emerald II: " + ChatColor.WHITE + formatTime(remaining));
        }

        return lines;
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /**
     * Split a legacy scoreboard line into prefix/suffix without breaking color codes.
     * Prefix max 32 chars, suffix max 32 chars on older servers.
     * We target a "total" length but still enforce safe boundaries.
     */
    private SplitText splitLegacy(String input, int totalMax) {
        String text = color(input);
        if (text.length() <= 32) {
            return new SplitText(text, "");
        }

        // Hard cut prefix at 32, but avoid ending with '§'
        int cut = 32;
        if (cut > text.length()) cut = text.length();

        String prefix = text.substring(0, cut);
        if (prefix.endsWith("§")) {
            prefix = prefix.substring(0, prefix.length() - 1);
            cut--;
        }

        String remainder = text.substring(cut);
        // Preserve colors into suffix
        String lastColors = ChatColor.getLastColors(prefix);
        String suffix = lastColors + remainder;

        if (suffix.length() > 32) {
            suffix = suffix.substring(0, 32);
            if (suffix.endsWith("§")) suffix = suffix.substring(0, suffix.length() - 1);
        }

        return new SplitText(prefix, suffix);
    }

    private static final class SplitText {
        final String prefix;
        final String suffix;

        SplitText(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }

    public void startUpdateTask() {
        if (updateTask != null) return;

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                GameState state = game.getState();
                if (state != GameState.WAITING && state != GameState.STARTING
                        && state != GameState.RUNNING && state != GameState.ENDING) {
                    cancel();
                    updateTask = null;
                    return;
                }

                // Players
                for (GamePlayer gp : game.getPlayers().values()) {
                    Player p = Bukkit.getPlayer(gp.getUuid());
                    if (p != null && p.isOnline()) update(p);
                }
                // Spectators
                for (UUID specId : game.getSpectators()) {
                    Player p = Bukkit.getPlayer(specId);
                    if (p != null && p.isOnline()) update(p);
                }
            }
        };

        // 1 Hz is fine for time; if you want smoother, do 10 ticks, but avoid heavy work
        updateTask.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }
}