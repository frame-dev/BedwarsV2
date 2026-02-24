package ch.framedev.bedwars.team;

import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.upgrades.TeamUpgrades;
import org.bukkit.Location;

import java.util.*;

/**
 * Represents a team in BedWars.
 *
 * Improvements:
 * - Uses a Set to avoid duplicate GamePlayers
 * - Exposes an unmodifiable view of members
 * - Adds small helper methods used often by game logic
 */
public class Team {

    private final TeamColor color;
    private final Location spawnLocation;
    private final Location bedLocation;

    // Prevent duplicates and keep insertion order (nice for UI/scoreboards)
    private final Set<GamePlayer> players;

    private final TeamUpgrades upgrades;
    private boolean bedAlive;

    public Team(TeamColor color, Location spawnLocation, Location bedLocation) {
        this.color = Objects.requireNonNull(color, "color");
        this.spawnLocation = Objects.requireNonNull(spawnLocation, "spawnLocation");
        this.bedLocation = bedLocation; // can be null depending on arena config

        this.players = new LinkedHashSet<>();
        this.upgrades = new TeamUpgrades();
        this.bedAlive = true;
    }

    /**
     * Adds a player to the team. Idempotent.
     */
    public void addPlayer(GamePlayer player) {
        if (player == null) return;
        players.add(player);
    }

    /**
     * Removes a player from the team. Idempotent.
     */
    public void removePlayer(GamePlayer player) {
        if (player == null) return;
        players.remove(player);
    }

    public boolean isMember(GamePlayer player) {
        return player != null && players.contains(player);
    }

    public int size() {
        return players.size();
    }

    public void destroyBed() {
        bedAlive = false;
    }

    public boolean hasAlivePlayers() {
        for (GamePlayer player : players) {
            if (player != null && !player.isEliminated()) {
                return true;
            }
        }
        return false;
    }

    public int aliveCount() {
        int count = 0;
        for (GamePlayer player : players) {
            if (player != null && !player.isEliminated()) {
                count++;
            }
        }
        return count;
    }

    public void reset() {
        players.clear();
        bedAlive = true;
        upgrades.reset();
    }

    public TeamColor getColor() {
        return color;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public Location getBedLocation() {
        return bedLocation;
    }

    /**
     * Unmodifiable view to prevent outside code from mutating the team list.
     */
    public Set<GamePlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public TeamUpgrades getUpgrades() {
        return upgrades;
    }

    public boolean isBedAlive() {
        return bedAlive;
    }
}