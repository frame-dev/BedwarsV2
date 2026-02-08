package ch.framedev.bedwars.arena;

import ch.framedev.bedwars.team.TeamColor;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an arena setup session for a player
 */
public class ArenaSetupSession {

    private final UUID playerUUID;
    private String arenaName;
    private Location lobbySpawn;
    private Location spectatorSpawn;
    private int minPlayers = 2;
    private int maxPlayers = 8;
    private Map<TeamColor, Location> teamSpawns;
    private Map<TeamColor, Location> bedLocations;
    private Map<TeamColor, Location> shopLocations;
    private Map<String, Location> generatorLocations;

    public ArenaSetupSession(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.teamSpawns = new HashMap<>();
        this.bedLocations = new HashMap<>();
        this.shopLocations = new HashMap<>();
        this.generatorLocations = new HashMap<>();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getArenaName() {
        return arenaName;
    }

    public void setArenaName(String arenaName) {
        this.arenaName = arenaName;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Map<TeamColor, Location> getTeamSpawns() {
        return teamSpawns;
    }

    public void setTeamSpawn(TeamColor color, Location location) {
        teamSpawns.put(color, location);
    }

    public Map<TeamColor, Location> getBedLocations() {
        return bedLocations;
    }

    public void setBedLocation(TeamColor color, Location location) {
        bedLocations.put(color, location);
    }

    public Map<TeamColor, Location> getShopLocations() {
        return shopLocations;
    }

    public void setShopLocation(TeamColor color, Location location) {
        shopLocations.put(color, location);
    }

    public Map<String, Location> getGeneratorLocations() {
        return generatorLocations;
    }

    public void addGenerator(String name, Location location) {
        generatorLocations.put(name, location);
    }

    public boolean isComplete() {
        if (arenaName == null || lobbySpawn == null || spectatorSpawn == null) {
            return false;
        }

        // Must have at least 2 teams configured
        if (teamSpawns.size() < 2 || bedLocations.size() < 2) {
            return false;
        }

        // All teams must have both spawn and bed
        for (TeamColor color : teamSpawns.keySet()) {
            if (!bedLocations.containsKey(color)) {
                return false;
            }
        }

        return true;
    }

    public String getProgress() {
        StringBuilder progress = new StringBuilder();
        progress.append("Arena Setup Progress:\n");
        progress.append("Name: ").append(arenaName != null ? arenaName : "NOT SET").append("\n");
        progress.append("Lobby Spawn: ").append(lobbySpawn != null ? "SET" : "NOT SET").append("\n");
        progress.append("Spectator Spawn: ").append(spectatorSpawn != null ? "SET" : "NOT SET").append("\n");
        progress.append("Min Players: ").append(minPlayers).append("\n");
        progress.append("Max Players: ").append(maxPlayers).append("\n");
        progress.append("Team Spawns: ").append(teamSpawns.size()).append("\n");
        progress.append("Bed Locations: ").append(bedLocations.size()).append("\n");
        progress.append("Shop Locations: ").append(shopLocations.size()).append("\n");
        progress.append("Generators: ").append(generatorLocations.size()).append("\n");
        return progress.toString();
    }
}
