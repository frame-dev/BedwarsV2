package ch.framedev.bedwars.signs;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.utils.LocationUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Stores and refreshes BedWars join signs.
 */
public class JoinSignManager {

    public static final String SIGN_HEADER = "[BedWars]";

    private final BedWarsPlugin plugin;
    private final File signsFile;
    private final Map<String, JoinSign> signs;
    private final Map<String, Set<String>> signsByArena;

    public JoinSignManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.signsFile = new File(plugin.getDataFolder(), "joinsigns.yml");
        this.signs = new HashMap<>();
        this.signsByArena = new HashMap<>();
    }

    public void loadSigns() {
        signs.clear();
        signsByArena.clear();
        if (!signsFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(signsFile);
        ConfigurationSection section = config.getConfigurationSection("signs");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection signSection = section.getConfigurationSection(id);
            if (signSection == null) continue;

            String arenaName = signSection.getString("arena");
            Location location = LocationUtils.deserialize(signSection.getConfigurationSection("location"));
            if (arenaName == null || arenaName.isBlank() || location == null) continue;

            addSign(location, arenaName);
        }

        updateAllSigns();
    }

    public void saveSigns() {
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (JoinSign joinSign : signs.values()) {
            String path = "signs." + index++;
            config.set(path + ".arena", joinSign.arenaName());
            config.set(path + ".location", LocationUtils.serialize(joinSign.location()));
        }

        try {
            File parent = signsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(signsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save join signs: " + e.getMessage());
        }
    }

    public void registerSign(Block block, Game game) {
        if (block == null || game == null) return;

        Location location = block.getLocation();
        addSign(location, game.getArena().getName());
        saveSigns();
        updateSign(location, game);
    }

    public void unregisterSign(Block block) {
        if (block == null) return;

        if (removeSign(block.getLocation()) != null) {
            saveSigns();
        }
    }

    public String getArenaName(Sign sign) {
        if (sign == null) return null;

        JoinSign tracked = signs.get(toKey(sign.getLocation()));
        if (tracked != null) {
            return tracked.arenaName();
        }

        String arenaName = ChatColor.stripColor(sign.getLine(1));
        return arenaName == null ? null : arenaName.trim();
    }

    public boolean isTrackedSign(Block block) {
        return block != null && signs.containsKey(toKey(block.getLocation()));
    }

    public void updateSigns(Game game) {
        if (game == null) return;

        Set<String> signKeys = signsByArena.get(normalizeArena(game.getArena().getName()));
        if (signKeys == null || signKeys.isEmpty()) return;

        for (String signKey : new HashSet<>(signKeys)) {
            JoinSign joinSign = signs.get(signKey);
            if (joinSign != null) {
                updateSign(joinSign.location(), game);
            }
        }
    }

    public void updateAllSigns() {
        Iterator<Map.Entry<String, JoinSign>> iterator = signs.entrySet().iterator();
        boolean changed = false;

        while (iterator.hasNext()) {
            Map.Entry<String, JoinSign> entry = iterator.next();
            JoinSign joinSign = entry.getValue();
            Game game = plugin.getGameManager().getGame(joinSign.arenaName());
            if (game == null) {
                continue;
            }

            Block block = joinSign.location().getBlock();
            if (!(block.getState() instanceof Sign)) {
                iterator.remove();
                removeSignIndex(entry.getKey(), joinSign.arenaName());
                changed = true;
                continue;
            }

            updateSign(joinSign.location(), game);
        }

        if (changed) {
            saveSigns();
        }
    }

    public void updateSign(Location location, Game game) {
        if (location == null || game == null) return;

        Block block = location.getBlock();
        if (!(block.getState() instanceof Sign sign)) return;

        String line0 = ChatColor.GOLD + SIGN_HEADER;
        String line1 = ChatColor.DARK_BLUE + game.getArena().getName();
        String line2 = getStateLine(game);
        String line3 = ChatColor.DARK_GRAY.toString() + game.getPlayers().size() + "/" + game.getArena().getMaxPlayers();

        if (line0.equals(sign.getLine(0))
                && line1.equals(sign.getLine(1))
                && line2.equals(sign.getLine(2))
                && line3.equals(sign.getLine(3))) {
            return;
        }

        sign.setLine(0, line0);
        sign.setLine(1, line1);
        sign.setLine(2, line2);
        sign.setLine(3, line3);
        sign.update(true, false);
    }

    public String getStateLine(Game game) {
        if (game == null) return ChatColor.DARK_GRAY + "UNKNOWN";
        if ((game.getState() == GameState.WAITING || game.getState() == GameState.STARTING)
                && game.getPlayers().size() >= game.getArena().getMaxPlayers()) {
            return ChatColor.GOLD + "FULL";
        }
        return stateLine(game.getState());
    }

    private String stateLine(GameState state) {
        return switch (state) {
            case WAITING -> ChatColor.GREEN + "WAITING";
            case STARTING -> ChatColor.YELLOW + "STARTING";
            case RUNNING -> ChatColor.RED + "RUNNING";
            case ENDING -> ChatColor.DARK_RED + "ENDING";
        };
    }

    private String toKey(Location location) {
        if (location == null || location.getWorld() == null) return "unknown";
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private void addSign(Location location, String arenaName) {
        String key = toKey(location);
        JoinSign oldSign = signs.put(key, new JoinSign(location, arenaName));
        if (oldSign != null) {
            removeSignIndex(key, oldSign.arenaName());
        }
        signsByArena.computeIfAbsent(normalizeArena(arenaName), ignored -> new HashSet<>()).add(key);
    }

    private JoinSign removeSign(Location location) {
        String key = toKey(location);
        JoinSign removed = signs.remove(key);
        if (removed != null) {
            removeSignIndex(key, removed.arenaName());
        }
        return removed;
    }

    private void removeSignIndex(String signKey, String arenaName) {
        Set<String> arenaSigns = signsByArena.get(normalizeArena(arenaName));
        if (arenaSigns == null) return;
        arenaSigns.remove(signKey);
        if (arenaSigns.isEmpty()) {
            signsByArena.remove(normalizeArena(arenaName));
        }
    }

    private String normalizeArena(String arenaName) {
        return arenaName == null ? "" : arenaName.toLowerCase(Locale.ROOT);
    }

    private record JoinSign(Location location, String arenaName) {
    }
}
