package ch.framedev.bedwars.team;

import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.upgrades.TeamUpgrades;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a team in BedWars
 */
public class Team {

    private final TeamColor color;
    private final Location spawnLocation;
    private final Location bedLocation;
    private final List<GamePlayer> players;
    private final TeamUpgrades upgrades;
    private boolean bedAlive;

    public Team(TeamColor color, Location spawnLocation, Location bedLocation) {
        this.color = color;
        this.spawnLocation = spawnLocation;
        this.bedLocation = bedLocation;
        this.players = new ArrayList<>();
        this.upgrades = new TeamUpgrades();
        this.bedAlive = true;
    }

    public void addPlayer(GamePlayer player) {
        players.add(player);
    }

    public void removePlayer(GamePlayer player) {
        players.remove(player);
    }

    public void destroyBed() {
        bedAlive = false;
    }

    public boolean hasAlivePlayers() {
        for (GamePlayer player : players) {
            if (!player.isEliminated()) {
                return true;
            }
        }
        return false;
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

    public List<GamePlayer> getPlayers() {
        return players;
    }

    public TeamUpgrades getUpgrades() {
        return upgrades;
    }

    public boolean isBedAlive() {
        return bedAlive;
    }
}
