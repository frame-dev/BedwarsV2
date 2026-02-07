package ch.framedev.bedwars.voting;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.utils.ItemBuilder;
import ch.framedev.bedwars.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages map voting for queued players.
 */
public class MapVoteManager {

    private final BedWarsPlugin plugin;
    private final Set<UUID> queue;
    private final Map<UUID, String> votes;
    private final Map<Integer, String> slotToArena;
    private boolean votingActive;
    private BukkitRunnable voteTask;

    public MapVoteManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.queue = new HashSet<>();
        this.votes = new HashMap<>();
        this.slotToArena = new HashMap<>();
        this.votingActive = false;
    }

    public boolean isQueued(UUID uuid) {
        return queue.contains(uuid);
    }

    public boolean isVotingActive() {
        return votingActive;
    }

    public void joinQueue(Player player) {
        MessageManager mm = plugin.getMessageManager();
        if (!plugin.getConfig().getBoolean("map-voting.enabled", true)) {
            mm.sendMessage(player, "map-vote.disabled");
            return;
        }

        if (plugin.getGameManager().getPlayerGame(player) != null) {
            mm.sendMessage(player, "map-vote.already-in-game");
            return;
        }

        if (!queue.add(player.getUniqueId())) {
            mm.sendMessage(player, "map-vote.already-queued");
            return;
        }

        mm.sendMessage(player, "map-vote.queued");
        plugin.getDebugLogger().debug("Queue join: " + player.getName());

        if (shouldStartVoting()) {
            startVoting();
        } else if (votingActive) {
            openVoteGui(player);
        }
    }

    public void leaveQueue(Player player) {
        if (!queue.remove(player.getUniqueId())) {
            return;
        }
        votes.remove(player.getUniqueId());
        plugin.getMessageManager().sendMessage(player, "map-vote.left-queue");
        plugin.getDebugLogger().debug("Queue leave: " + player.getName());

        if (votingActive && queue.size() < getMinQueueSize()) {
            cancelVoting();
        } else if (votingActive) {
            refreshVoteGui();
        }
    }

    public void forceStartVoting(Player admin) {
        MessageManager mm = plugin.getMessageManager();
        if (votingActive) {
            mm.sendMessage(admin, "map-vote.admin-already-active");
            return;
        }
        if (queue.isEmpty()) {
            mm.sendMessage(admin, "map-vote.admin-no-queue");
            return;
        }
        startVoting();
        mm.sendMessage(admin, "map-vote.admin-started");
    }

    public void forceEndVoting(Player admin) {
        MessageManager mm = plugin.getMessageManager();
        if (!votingActive) {
            mm.sendMessage(admin, "map-vote.admin-not-active");
            return;
        }
        endVoting();
        mm.sendMessage(admin, "map-vote.admin-ended");
    }

    public void forceSelectArena(Player admin, String arenaName) {
        MessageManager mm = plugin.getMessageManager();
        if (queue.isEmpty()) {
            mm.sendMessage(admin, "map-vote.admin-no-queue");
            return;
        }

        Game game = plugin.getGameManager().getGame(arenaName);
        if (game == null) {
            mm.sendMessage(admin, "map-vote.admin-invalid-arena", arenaName);
            return;
        }

        if (game.getState() != GameState.WAITING || game.getPlayers().size() > 0) {
            mm.sendMessage(admin, "map-vote.admin-arena-not-ready", arenaName);
            return;
        }

        int availableSlots = game.getArena().getMaxPlayers() - game.getPlayers().size();
        if (queue.size() > availableSlots) {
            mm.sendMessage(admin, "map-vote.arena-full", arenaName);
            return;
        }

        if (voteTask != null) {
            voteTask.cancel();
            voteTask = null;
        }
        votingActive = false;
        votes.clear();

        finishQueueJoin(game, arenaName, true);
        mm.sendMessage(admin, "map-vote.admin-forced", arenaName);
    }

    public void openVoteGui(Player player) {
        if (!votingActive) {
            return;
        }

        Inventory inventory = buildVoteInventory();
        player.openInventory(inventory);
    }

    public void handleVoteClick(Player player, ItemStack clickedItem, int slot) {
        if (!votingActive || !queue.contains(player.getUniqueId())) {
            return;
        }

        String arenaName = slotToArena.get(slot);
        if (arenaName == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("map-voting.allow-revote", true)) {
            if (votes.containsKey(player.getUniqueId())) {
                plugin.getMessageManager().sendMessage(player, "map-vote.already-voted");
                return;
            }
        }

        votes.put(player.getUniqueId(), arenaName);
        plugin.getMessageManager().sendMessage(player, "map-vote.voted", arenaName);
        plugin.getDebugLogger().debug("Map vote: player=" + player.getName() + " arena=" + arenaName);

        refreshVoteGui();
    }

    public void removePlayer(Player player) {
        queue.remove(player.getUniqueId());
        votes.remove(player.getUniqueId());
        if (votingActive && queue.size() < getMinQueueSize()) {
            cancelVoting();
        }
    }

    private boolean shouldStartVoting() {
        return !votingActive && queue.size() >= getMinQueueSize() && !getEligibleGames().isEmpty();
    }

    private int getMinQueueSize() {
        return plugin.getConfig().getInt("map-voting.queue-min-players", 2);
    }

    private int getVoteDurationSeconds() {
        return plugin.getConfig().getInt("map-voting.vote-duration-seconds", 20);
    }

    private void startVoting() {
        List<Game> eligibleGames = getEligibleGames();
        if (eligibleGames.isEmpty()) {
            broadcastQueue("map-vote.no-available-arenas");
            return;
        }

        votingActive = true;
        votes.clear();
        broadcastQueue("map-vote.start", getVoteDurationSeconds());
        refreshVoteGui();

        voteTask = new BukkitRunnable() {
            int remaining = getVoteDurationSeconds();

            @Override
            public void run() {
                remaining--;
                if (remaining <= 0) {
                    endVoting();
                    cancel();
                }
            }
        };
        voteTask.runTaskTimer(plugin, 20L, 20L);
    }

    private void cancelVoting() {
        if (voteTask != null) {
            voteTask.cancel();
            voteTask = null;
        }
        votingActive = false;
        votes.clear();
        broadcastQueue("map-vote.cancelled");
    }

    private void endVoting() {
        votingActive = false;

        String winner = pickWinner();
        if (winner == null) {
            broadcastQueue("map-vote.no-winner");
            return;
        }

        Game game = plugin.getGameManager().getGame(winner);
        if (game == null || game.getState() != GameState.WAITING) {
            broadcastQueue("map-vote.no-available-arenas");
            return;
        }

        int availableSlots = game.getArena().getMaxPlayers() - game.getPlayers().size();
        if (queue.size() > availableSlots) {
            broadcastQueue("map-vote.arena-full", winner);
            return;
        }

        finishQueueJoin(game, winner, false);
    }

    private String pickWinner() {
        Map<String, Integer> counts = countVotes();
        if (counts.isEmpty()) {
            List<Game> eligible = getEligibleGames();
            if (eligible.isEmpty()) {
                return null;
            }
            return eligible.get((int) (Math.random() * eligible.size())).getArena().getName();
        }

        int top = 0;
        List<String> leaders = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > top) {
                top = entry.getValue();
                leaders.clear();
                leaders.add(entry.getKey());
            } else if (entry.getValue() == top) {
                leaders.add(entry.getKey());
            }
        }

        if (leaders.isEmpty()) {
            return null;
        }

        Collections.shuffle(leaders);
        return leaders.get(0);
    }

    private Map<String, Integer> countVotes() {
        Map<String, Integer> counts = new HashMap<>();
        for (String arena : votes.values()) {
            counts.put(arena, counts.getOrDefault(arena, 0) + 1);
        }
        return counts;
    }

    private List<Game> getEligibleGames() {
        int queueSize = queue.size();
        List<Game> eligible = new ArrayList<>();
        for (Game game : plugin.getGameManager().getGames()) {
            if (game.getState() != GameState.WAITING) {
                continue;
            }

            int minPlayers = game.getArena().getMinPlayers();
            int maxPlayers = game.getArena().getMaxPlayers();
            int currentPlayers = game.getPlayers().size();
            if (currentPlayers > 0) {
                continue;
            }

            if (queueSize >= minPlayers && queueSize <= maxPlayers) {
                eligible.add(game);
            }
        }
        return eligible;
    }

    private Inventory buildVoteInventory() {
        slotToArena.clear();
        int size = plugin.getConfig().getInt("map-voting.gui-size", 27);
        String title = plugin.getConfig().getString("map-voting.gui-title", "Map Voting");
        Inventory inventory = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', title));

        Map<String, Integer> counts = countVotes();
        List<Game> eligible = getEligibleGames();
        int slot = plugin.getConfig().getInt("map-voting.gui-slot-start", 10);
        int step = plugin.getConfig().getInt("map-voting.gui-slot-step", 1);
        if (step < 1) {
            step = 1;
        }

        Material icon = getGuiItemMaterial();
        List<String> baseLore = plugin.getConfig().getStringList("map-voting.gui-lore");
        if (baseLore == null || baseLore.isEmpty()) {
            baseLore = List.of("&7Votes: &e{votes}", "&7Click to vote");
        }

        for (Game game : eligible) {
            if (slot >= size) {
                break;
            }
            String arenaName = game.getArena().getName();
            int votesFor = counts.getOrDefault(arenaName, 0);

                List<String> lore = new ArrayList<>();
                for (String line : baseLore) {
                lore.add(line
                    .replace("{arena}", arenaName)
                    .replace("{votes}", String.valueOf(votesFor))
                    .replace("{queued}", String.valueOf(queue.size())));
                }

                ItemStack item = new ItemBuilder(icon)
                    .setName("&a" + arenaName)
                    .setLore(new ArrayList<>(lore))
                    .build();

            inventory.setItem(slot, item);
            slotToArena.put(slot, arenaName);
            slot += step;
        }

        return inventory;
    }

    private void refreshVoteGui() {
        Inventory inventory = buildVoteInventory();
        for (UUID uuid : queue) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getOpenInventory() != null) {
                player.openInventory(inventory);
            }
        }
    }

    private void broadcastQueue(String key, Object... args) {
        MessageManager mm = plugin.getMessageManager();
        for (UUID uuid : queue) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                mm.sendMessage(player, key, args);
            }
        }
    }

    private void finishQueueJoin(Game game, String arenaName, boolean forced) {
        List<Player> joiners = new ArrayList<>();
        for (UUID uuid : queue) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && plugin.getGameManager().getPlayerGame(player) == null) {
                joiners.add(player);
            }
        }

        if (forced) {
            broadcastQueue("map-vote.admin-winner", arenaName, joiners.size());
        } else {
            broadcastQueue("map-vote.winner", arenaName, joiners.size());
        }

        for (Player player : joiners) {
            game.addPlayer(player);
        }

        queue.clear();
        votes.clear();
    }

    private Material getGuiItemMaterial() {
        String name = plugin.getConfig().getString("map-voting.gui-item", "MAP");
        if (name == null) {
            return Material.MAP;
        }
        Material material = Material.matchMaterial(name.toUpperCase());
        return material == null ? Material.MAP : material;
    }
}
