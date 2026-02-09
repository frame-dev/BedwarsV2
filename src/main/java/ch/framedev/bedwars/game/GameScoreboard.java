package ch.framedev.bedwars.game;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Scoreboard for BedWars games: lobby countdown, game time, teams, bed status, player stats.
 */
public class GameScoreboard {

    private static final String OBJECTIVE_NAME = "bw_main";
    private static final int MAX_LINES = 15;
    private static final String[] LINE_ENTRIES = new String[MAX_LINES];

    static {
        for (int i = 0; i < MAX_LINES; i++) {
            LINE_ENTRIES[i] = ChatColor.values()[i].toString() + ChatColor.RESET;
        }
    }

    private final BedWarsPlugin plugin;
    private final Game game;
    private BukkitRunnable updateTask;

    public GameScoreboard(BedWarsPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @SuppressWarnings("deprecation")
    public void show(Player player) {
        if (player == null) return;
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective(OBJECTIVE_NAME, "dummy",
                ChatColor.GOLD + "" + ChatColor.BOLD + "BEDWARS");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(sb);
        update(player);
    }

    public void update(Player player) {
        if (player == null) return;
        Scoreboard sb = player.getScoreboard();
        Objective obj = sb.getObjective(DisplaySlot.SIDEBAR);
        if (obj == null) return;

        String title = plugin.getConfig().getString("scoreboard.title", "&6&lBEDWARS");
        obj.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));

        List<String> lines = buildLines(player);
        for (int i = 0; i < lines.size(); i++) {
            String entry = getEntry(i);
            int score = MAX_LINES - i;
            String text = lines.get(i);
            if (text.length() > 64) text = text.substring(0, 64);
            org.bukkit.scoreboard.Team scoreboardTeam = sb.getTeam("line" + (i + 1));
            if (scoreboardTeam == null) {
                scoreboardTeam = sb.registerNewTeam("line" + (i + 1));
                scoreboardTeam.addEntry(entry);
            }
            scoreboardTeam.setPrefix(text);
            scoreboardTeam.setSuffix("");
            obj.getScore(entry).setScore(score);
        }
        for (int i = lines.size(); i < MAX_LINES; i++) {
            sb.resetScores(getEntry(i));
        }
    }

    private String getEntry(int index) {
        if (index < 0 || index >= LINE_ENTRIES.length) {
            return ChatColor.values()[Math.min(index, 14)].toString();
        }
        return LINE_ENTRIES[index];
    }

    private List<String> buildLines(Player player) {
        List<String> lines = new ArrayList<>();
        GameState state = game.getState();
        String arenaName = game.getArena().getName();

        lines.add(ChatColor.GRAY + arenaName);
        lines.add("");

        if (state == GameState.WAITING) {
            lines.add(ChatColor.YELLOW + "Players: " + ChatColor.WHITE + game.getPlayers().size() + "/" + game.getArena().getMaxPlayers());
            lines.add(ChatColor.GRAY + "Waiting for players...");
        } else if (state == GameState.STARTING) {
            lines.add(ChatColor.YELLOW + "Starting in: " + ChatColor.WHITE + game.getCountdown() + "s");
            lines.add(ChatColor.YELLOW + "Players: " + ChatColor.WHITE + game.getPlayers().size() + "/" + game.getArena().getMaxPlayers());
        } else if (state == GameState.RUNNING) {
            int elapsed = game.getGameElapsedSeconds();
            int min = elapsed / 60;
            int sec = elapsed % 60;
            lines.add(ChatColor.AQUA + "Time: " + ChatColor.WHITE + String.format("%d:%02d", min, sec));
            lines.add("");

            GamePlayer gp = game.getGamePlayer(player);
            if (gp != null && gp.getTeam() != null) {
                lines.add(ChatColor.GRAY + "Your team: " + gp.getTeam().getColor().getChatColor() + gp.getTeam().getColor().name());
            }
            lines.add(ChatColor.DARK_GRAY + "Teams:");
            for (ch.framedev.bedwars.team.Team team : game.getTeams().values()) {
                String bed = team.isBedAlive() ? (ChatColor.GREEN + "✓") : (ChatColor.RED + "✗");
                long alive = team.getPlayers().stream().filter(p -> !p.isEliminated()).count();
                lines.add(" " + team.getColor().getChatColor() + team.getColor().name() + ": " + bed + ChatColor.GRAY + " (" + alive + ")");
            }
            lines.add("");

            if (gp != null) {
                lines.add(ChatColor.GRAY + "Kills: " + ChatColor.WHITE + gp.getKills());
                lines.add(ChatColor.GRAY + "Final Kills: " + ChatColor.WHITE + gp.getFinalKills());
                lines.add(ChatColor.GRAY + "Beds: " + ChatColor.WHITE + gp.getBedsBroken());
            }
            lines.add("");

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
        } else if (state == GameState.ENDING) {
            lines.add(ChatColor.GRAY + "Game ended");
        }

        return lines;
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    public void hide(Player player) {
        if (player == null) return;
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void startUpdateTask() {
        if (updateTask != null) return;
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING
                        || game.getState() == GameState.RUNNING || game.getState() == GameState.ENDING) {
                    for (GamePlayer gp : game.getPlayers().values()) {
                        Player p = Bukkit.getPlayer(gp.getUuid());
                        if (p != null && p.isOnline()) {
                            update(p);
                        }
                    }
                    for (UUID specId : game.getSpectators()) {
                        Player p = Bukkit.getPlayer(specId);
                        if (p != null && p.isOnline()) {
                            update(p);
                        }
                    }
                } else {
                    cancel();
                    updateTask = null;
                }
            }
        };
        updateTask.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }
}
