package ch.framedev.bedwars.arena;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Arena;
import ch.framedev.bedwars.shop.ShopType;
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
 * Manages arena creation, modification, and persistence.
 * <p>
 * De-duplication:
 * - Save teams uses ONE loop (union of colors in session)
 * - Load teams uses ONE loop (colors in config)
 * - Central helpers for team paths + read/set location
 * - Legacy shop path handled once (shop -> item fallback)
 */
public class ArenaManager {

    private static final String ARENAS_ROOT = "arenas";

    private final BedWarsPlugin plugin;
    private final Map<UUID, ArenaSetupSession> setupSessions = new HashMap<>();

    private final File arenasFile;
    private FileConfiguration arenasConfig;

    public ArenaManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
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

    /* --------------------------------------------------------------------- */
    /* Sessions                                                               */
    /* --------------------------------------------------------------------- */

    public ArenaSetupSession getOrCreateSession(UUID playerUUID) {
        return setupSessions.computeIfAbsent(playerUUID, ArenaSetupSession::new);
    }

    public ArenaSetupSession getSession(UUID playerUUID) {
        return setupSessions.get(playerUUID);
    }

    public void removeSession(UUID playerUUID) {
        setupSessions.remove(playerUUID);
    }

    /* --------------------------------------------------------------------- */
    /* CRUD                                                                   */
    /* --------------------------------------------------------------------- */

    public boolean arenaExists(String name) {
        return arenasConfig.contains(pathArena(name));
    }

    public void saveArena(ArenaSetupSession session) throws IOException {
        String base = pathArena(session.getArenaName());
        plugin.getDebugLogger().debug("Saving arena: " + session.getArenaName());

        setLocation(base + ".lobby-spawn", session.getLobbySpawn());
        setLocation(base + ".spectator-spawn", session.getSpectatorSpawn());
        arenasConfig.set(base + ".min-players", session.getMinPlayers());
        arenasConfig.set(base + ".max-players", session.getMaxPlayers());

        saveTeams(base, session);
        saveGenerators(base, session);

        arenasConfig.save(arenasFile);
        plugin.getDebugLogger().debug("Arena saved: " + session.getArenaName());
    }

    public Arena loadArena(String name) {
        if (!arenaExists(name)) return null;

        ConfigurationSection section = arenasConfig.getConfigurationSection(pathArena(name));
        if (section == null) return null;

        try {
            Location lobbySpawn = LocationUtils.fromString(section.getString("lobby-spawn"));
            Location spectatorSpawn = LocationUtils.fromString(section.getString("spectator-spawn"));
            int minPlayers = section.getInt("min-players", 2);
            int maxPlayers = section.getInt("max-players", 8);

            Arena arena = new Arena(name, lobbySpawn, spectatorSpawn, minPlayers, maxPlayers);

            loadTeams(section, arena);
            loadGenerators(section, arena);

            return arena;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load arena " + name + ": " + e.getMessage());
            plugin.getDebugLogger().debug("Arena load failed: " + name);
            return null;
        }
    }

    public boolean deleteArena(String name) {
        if (!arenaExists(name)) return false;

        arenasConfig.set(pathArena(name), null);
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
        ConfigurationSection root = arenasConfig.getConfigurationSection(ARENAS_ROOT);
        if (root == null) return Collections.emptySet();
        return Collections.unmodifiableSet(root.getKeys(false));
    }

    /* --------------------------------------------------------------------- */
    /* Save helpers                                                           */
    /* --------------------------------------------------------------------- */

    private void saveTeams(String base, ArenaSetupSession session) {
        // Build a union of all colors referenced by the session to avoid multiple loops
        Set<TeamColor> colors = new HashSet<>();
        colors.addAll(session.getTeamSpawns().keySet());
        colors.addAll(session.getBedLocations().keySet());
        colors.addAll(session.getShopLocations().keySet());

        for (TeamColor color : colors) {
            String teamBase = teamBasePath(base, color);

            // spawn / bed
            setLocation(teamBase + ".spawn", session.getTeamSpawns().get(color));
            setLocation(teamBase + ".bed", session.getBedLocations().get(color));

            // shops
            String shopBase = teamBase + ".shop";
            arenasConfig.set(shopBase, null); // wipe per team to avoid stale keys

            Map<ShopType, Location> shops = session.getShopLocations().get(color);
            if (shops != null) {
                for (Map.Entry<ShopType, Location> shop : shops.entrySet()) {
                    setLocation(shopBase + "." + shop.getKey().getConfigKey(), shop.getValue());
                }
            }
        }
    }

    private void saveGenerators(String base, ArenaSetupSession session) {
        arenasConfig.set(base + ".generators", null);
        for (Map.Entry<String, Location> e : session.getGeneratorLocations().entrySet()) {
            setLocation(base + ".generators." + e.getKey(), e.getValue());
        }
    }

    private void setLocation(String path, Location location) {
        arenasConfig.set(path, location == null ? null : LocationUtils.toString(location));
    }

    /* --------------------------------------------------------------------- */
    /* Load helpers                                                           */
    /* --------------------------------------------------------------------- */

    private void loadTeams(ConfigurationSection arenaSection, Arena arena) {
        ConfigurationSection teams = arenaSection.getConfigurationSection("teams");
        if (teams == null) return;

        for (String colorKey : teams.getKeys(false)) {
            TeamColor color = parseTeamColor(colorKey);
            if (color == null) continue;

            String teamBase = "teams." + colorKey;

            // spawn/bed
            setIfPresent(teams, teamBase + ".spawn", loc -> arena.setTeamSpawn(color, loc));
            setIfPresent(teams, teamBase + ".bed", loc -> arena.setBedLocation(color, loc));

            // shops (new format)
            Location shopItem = readLocation(teams, teamBase + ".shop.item");
            Location shopUpgrade = readLocation(teams, teamBase + ".shop.upgrade");

            // legacy format fallback: teams.<color>.shop
            if (shopItem == null) {
                shopItem = readLocation(teams, teamBase + ".shop");
            }

            if (shopItem != null) arena.setShopLocation(color, ShopType.ITEM, shopItem);
            if (shopUpgrade != null) arena.setShopLocation(color, ShopType.UPGRADE, shopUpgrade);
        }
    }

    private void loadGenerators(ConfigurationSection arenaSection, Arena arena) {
        ConfigurationSection generators = arenaSection.getConfigurationSection("generators");
        if (generators == null) return;

        for (String genName : generators.getKeys(false)) {
            Location loc = LocationUtils.fromString(generators.getString(genName));
            if (loc != null) arena.addGenerator(genName, loc);
        }
    }

    private void setIfPresent(ConfigurationSection section, String path, java.util.function.Consumer<Location> setter) {
        Location loc = readLocation(section, path);
        if (loc != null) setter.accept(loc);
    }

    private Location readLocation(ConfigurationSection section, String path) {
        if (section == null) return null;
        String raw = section.getString(path);
        if (raw == null || raw.isEmpty()) return null;
        return LocationUtils.fromString(raw);
    }

    private TeamColor parseTeamColor(String colorName) {
        try {
            return TeamColor.valueOf(colorName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid team color: " + colorName);
            return null;
        }
    }

    /* --------------------------------------------------------------------- */
    /* Path helpers                                                           */
    /* --------------------------------------------------------------------- */

    private String pathArena(String name) {
        return ARENAS_ROOT + "." + name;
    }

    private String teamBasePath(String arenaBase, TeamColor color) {
        return arenaBase + ".teams." + color.name().toLowerCase(Locale.ROOT);
    }
}