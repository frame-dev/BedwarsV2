package ch.framedev.bedwars.game;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.arena.ArenaManager;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages all BedWars games
 */
public class GameManager {

    private final BedWarsPlugin plugin;
    private final ArenaManager arenaManager;
    private final Map<String, Game> games;
    private final Map<UUID, Game> playerGames;

    public GameManager(BedWarsPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.games = new HashMap<>();
        this.playerGames = new HashMap<>();

        loadArenas();
        plugin.getLogger().info("GameManager initialized with " + games.size() + " games loaded");
    }

    private void loadArenas() {
        // Load arenas from ArenaManager
        for (String arenaName : arenaManager.getArenaNames()) {
            Arena arena = arenaManager.loadArena(arenaName);
            if (arena != null) {
                Game game = new Game(plugin, arena);
                games.put(arenaName, game);
                plugin.getLogger().info("Loaded arena: " + arenaName);
            }
        }
    }

    public Game getGame(String name) {
        return games.get(name);
    }

    public Game getPlayerGame(Player player) {
        return playerGames.get(player.getUniqueId());
    }

    public void addPlayerToGame(Player player, Game game) {
        playerGames.put(player.getUniqueId(), game);
    }

    public void removePlayerFromGame(Player player) {
        playerGames.remove(player.getUniqueId());
    }

    public Collection<Game> getGames() {
        return games.values();
    }

    public void createGame(String name, Arena arena) {
        if (!games.containsKey(name)) {
            Game game = new Game(plugin, arena);
            games.put(name, game);
        }
    }

    public void stopAllGames() {
        for (Game game : games.values()) {
            if (game.getState() != GameState.WAITING) {
                game.endGame(null);
            }
        }
    }
}
