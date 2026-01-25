package ch.framedev.bedwars.game;

import ch.framedev.bedwars.team.TeamColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a BedWars arena/map
 */
public class Arena {

    private final String name;
    private final Location lobbySpawn;
    private final Location spectatorSpawn;
    private final Map<TeamColor, Location> teamSpawns;
    private final Map<TeamColor, Location> bedLocations;
    private final Map<String, Location> generators;
    private final int minPlayers;
    private final int maxPlayers;

    public Arena(String name, Location lobbySpawn, Location spectatorSpawn, int minPlayers, int maxPlayers) {
        this.name = name;
        this.lobbySpawn = lobbySpawn;
        this.spectatorSpawn = spectatorSpawn;
        this.teamSpawns = new HashMap<>();
        this.bedLocations = new HashMap<>();
        this.generators = new HashMap<>();
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    public void setTeamSpawn(TeamColor color, Location location) {
        teamSpawns.put(color, location);
    }

    public void setBedLocation(TeamColor color, Location location) {
        bedLocations.put(color, location);
    }

    public void addGenerator(String name, Location location) {
        generators.put(name, location);
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public Map<String, Location> getGenerators() {
        return generators;
    }

    public static Arena fromConfig(ConfigurationSection section) {
        try {
            String name = section.getName();
            Location lobbySpawn = (Location) section.get("lobby-spawn");
            Location spectatorSpawn = (Location) section.get("spectator-spawn");
            if (spectatorSpawn == null)
                spectatorSpawn = lobbySpawn;
            int minPlayers = section.getInt("min-players", 2);
            int maxPlayers = section.getInt("max-players", 8);

            Arena arena = new Arena(name, lobbySpawn, spectatorSpawn, minPlayers, maxPlayers);

            // Load team spawns and bed locations
            if (section.contains("teams")) {
                ConfigurationSection teams = section.getConfigurationSection("teams");
                for (String colorName : teams.getKeys(false)) {
                    TeamColor color = TeamColor.valueOf(colorName.toUpperCase());
                    Location spawn = (Location) teams.get(colorName + ".spawn");
                    Location bed = (Location) teams.get(colorName + ".bed");

                    if (spawn != null)
                        arena.setTeamSpawn(color, spawn);
                    if (bed != null)
                        arena.setBedLocation(color, bed);
                }
            }

            return arena;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public Location getTeamSpawn(TeamColor color) {
        return teamSpawns.get(color);
    }

    public Location getBedLocation(TeamColor color) {
        return bedLocations.get(color);
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
