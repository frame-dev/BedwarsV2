package ch.framedev.bedwars.arena;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Arena;
import ch.framedev.bedwars.team.TeamColor;
import ch.framedev.bedwars.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages arena creation, modification, and persistence
 */
public class ArenaManager {

    private final BedWarsPlugin plugin;
    private final Map<UUID, ArenaSetupSession> setupSessions;
    private final File arenasFile;
    private FileConfiguration arenasConfig;

    public ArenaManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.setupSessions = new HashMap<>();
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenasFile();
        plugin.getLogger().info("ArenaManager initialized with " + getArenaNames().size() + " arenas loaded");
    }

    private void loadArenasFile() {
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
            plugin.getDebugLogger().debug("Created default arenas.yml");
        }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        plugin.getDebugLogger().debug("Loaded arenas.yml from " + arenasFile.getAbsolutePath());
    }

    public ArenaSetupSession getOrCreateSession(UUID playerUUID) {
        return setupSessions.computeIfAbsent(playerUUID, ArenaSetupSession::new);
    }

    public ArenaSetupSession getSession(UUID playerUUID) {
        return setupSessions.get(playerUUID);
    }

    public void removeSession(UUID playerUUID) {
        setupSessions.remove(playerUUID);
    }

    public boolean arenaExists(String name) {
        return arenasConfig.contains("arenas." + name);
    }

    public void saveArena(ArenaSetupSession session) throws IOException {
        String basePath = "arenas." + session.getArenaName();
        plugin.getDebugLogger().debug("Saving arena: " + session.getArenaName());

        // Basic settings
        arenasConfig.set(basePath + ".lobby-spawn", LocationUtils.toString(session.getLobbySpawn()));
        arenasConfig.set(basePath + ".spectator-spawn", LocationUtils.toString(session.getSpectatorSpawn()));
        arenasConfig.set(basePath + ".min-players", session.getMinPlayers());
        arenasConfig.set(basePath + ".max-players", session.getMaxPlayers());

        // Team data
        for (Map.Entry<TeamColor, Location> entry : session.getTeamSpawns().entrySet()) {
            String colorName = entry.getKey().name().toLowerCase();
            arenasConfig.set(basePath + ".teams." + colorName + ".spawn",
                    LocationUtils.toString(entry.getValue()));
        }

        for (Map.Entry<TeamColor, Location> entry : session.getBedLocations().entrySet()) {
            String colorName = entry.getKey().name().toLowerCase();
            arenasConfig.set(basePath + ".teams." + colorName + ".bed",
                    LocationUtils.toString(entry.getValue()));
        }

        for (Map.Entry<TeamColor, Location> entry : session.getShopLocations().entrySet()) {
            String colorName = entry.getKey().name().toLowerCase();
            arenasConfig.set(basePath + ".teams." + colorName + ".shop",
                    LocationUtils.toString(entry.getValue()));
        }

        // Generator locations
        for (Map.Entry<String, Location> entry : session.getGeneratorLocations().entrySet()) {
            arenasConfig.set(basePath + ".generators." + entry.getKey(),
                    LocationUtils.toString(entry.getValue()));
        }

        arenasConfig.save(arenasFile);
        plugin.getDebugLogger().debug("Arena saved: " + session.getArenaName());
    }

    public Arena loadArena(String name) {
        if (!arenaExists(name)) {
            return null;
        }

        ConfigurationSection section = arenasConfig.getConfigurationSection("arenas." + name);

        try {
            Location lobbySpawn = LocationUtils.fromString(section.getString("lobby-spawn"));
            Location spectatorSpawn = LocationUtils.fromString(section.getString("spectator-spawn"));
            int minPlayers = section.getInt("min-players", 2);
            int maxPlayers = section.getInt("max-players", 8);

            Arena arena = new Arena(name, lobbySpawn, spectatorSpawn, minPlayers, maxPlayers);

            // Load teams
            if (section.contains("teams")) {
                ConfigurationSection teams = section.getConfigurationSection("teams");
                for (String colorName : teams.getKeys(false)) {
                    try {
                        TeamColor color = TeamColor.valueOf(colorName.toUpperCase());
                        Location spawn = LocationUtils.fromString(teams.getString(colorName + ".spawn"));
                        Location bed = LocationUtils.fromString(teams.getString(colorName + ".bed"));
                        Location shop = LocationUtils.fromString(teams.getString(colorName + ".shop"));

                        if (spawn != null)
                            arena.setTeamSpawn(color, spawn);
                        if (bed != null)
                            arena.setBedLocation(color, bed);
                        if (shop != null)
                            arena.setShopLocation(color, shop);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid team color: " + colorName);
                    }
                }
            }

            // Load generators
            if (section.contains("generators")) {
                ConfigurationSection generators = section.getConfigurationSection("generators");
                for (String genName : generators.getKeys(false)) {
                    Location loc = LocationUtils.fromString(generators.getString(genName));
                    if (loc != null) {
                        arena.addGenerator(genName, loc);
                    }
                }
            }

            return arena;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load arena " + name + ": " + e.getMessage());
            plugin.getDebugLogger().debug("Arena load failed: " + name);
            return null;
        }
    }

    public boolean deleteArena(String name) {
        if (!arenaExists(name)) {
            return false;
        }

        arenasConfig.set("arenas." + name, null);
        try {
            arenasConfig.save(arenasFile);
            plugin.getDebugLogger().debug("Arena deleted: " + name);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to delete arena: " + e.getMessage());
            plugin.getDebugLogger().debug("Arena delete failed: " + name);
            return false;
        }
    }

    public Set<String> getArenaNames() {
        if (!arenasConfig.contains("arenas")) {
            return Collections.emptySet();
        }
        return arenasConfig.getConfigurationSection("arenas").getKeys(false);
    }
}
