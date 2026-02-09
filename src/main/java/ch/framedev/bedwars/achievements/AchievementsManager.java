package ch.framedev.bedwars.achievements;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.utils.ItemBuilder;
import ch.framedev.bedwars.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages achievements and progress.
 */
public class AchievementsManager {

    private final BedWarsPlugin plugin;
    private final DatabaseManager database;
    private final Map<String, AchievementDefinition> definitions;
    private final Map<UUID, Map<String, AchievementProgress>> cache;
    private final Map<UUID, Map<Integer, AchievementDefinition>> menuSlots;
    private FileConfiguration achievementsConfig;

    public AchievementsManager(BedWarsPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.definitions = new HashMap<>();
        this.cache = new ConcurrentHashMap<>();
        this.menuSlots = new ConcurrentHashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        File achievementsFile = new File(plugin.getDataFolder(), "achievements.yml");
        if (!achievementsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("achievements.yml")) {
                if (in != null) {
                    Files.copy(in, achievementsFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create achievements.yml: " + e.getMessage());
            }
        }

        achievementsConfig = YamlConfiguration.loadConfiguration(achievementsFile);
        loadDefinitions();
    }

    public boolean isEnabled() {
        return achievementsConfig.getBoolean("enabled", true);
    }

    public void loadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, AchievementProgress> progress = loadFromDatabase(uuid);
            cache.put(uuid, progress);
        });
    }

    public boolean isAchievementsTitle(String title) {
        String expected = ChatColor.translateAlternateColorCodes('&',
                achievementsConfig.getString("gui.title", "Achievements"));
        return ChatColor.stripColor(title).equalsIgnoreCase(ChatColor.stripColor(expected));
    }

    public void openMenu(Player player) {
        if (!isEnabled()) {
            plugin.getMessageManager().sendMessage(player, "achievements.disabled");
            return;
        }

        Inventory inventory = buildMenu(player);
        player.openInventory(inventory);
        plugin.getMessageManager().sendMessage(player, "achievements.opened");
    }

    public void handleMenuClick(Player player, int slot) {
        Map<Integer, AchievementDefinition> slots = menuSlots.get(player.getUniqueId());
        if (slots == null) {
            return;
        }
        AchievementDefinition definition = slots.get(slot);
        if (definition == null) {
            return;
        }
        if (definition.isEnabled()) {
            String name = ChatColor.translateAlternateColorCodes('&', definition.getDisplayName());
            plugin.getMessageManager().sendMessage(player, "achievements.progress",
                    name,
                    getProgressValue(player.getUniqueId(), definition.getId()),
                    definition.getTarget());
        }
    }

    public void recordKill(UUID uuid) {
        increment(uuid, AchievementType.KILLS, 1);
    }

    public void recordFinalKill(UUID uuid) {
        increment(uuid, AchievementType.FINAL_KILLS, 1);
    }

    public void recordBedBroken(UUID uuid) {
        increment(uuid, AchievementType.BEDS_BROKEN, 1);
    }

    public void recordWin(UUID uuid) {
        increment(uuid, AchievementType.WINS, 1);
    }

    public void recordGamePlayed(UUID uuid) {
        increment(uuid, AchievementType.GAMES_PLAYED, 1);
    }

    public void shutdown() {
        menuSlots.clear();
        cache.clear();
    }

    private void loadDefinitions() {
        definitions.clear();
        ConfigurationSection section = achievementsConfig.getConfigurationSection("achievements");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }

            boolean enabled = entry.getBoolean("enabled", true);
            String displayName = entry.getString("display-name", key);
            String iconName = entry.getString("icon", "PAPER");
            Material icon = Material.matchMaterial(iconName.toUpperCase());
            if (icon == null) {
                icon = Material.PAPER;
            }
            List<String> lore = entry.getStringList("lore");
            AchievementType type;
            try {
                type = AchievementType.valueOf(entry.getString("type", "KILLS").toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid achievement type for " + key + ", defaulting to KILLS");
                type = AchievementType.KILLS;
            }
            int target = entry.getInt("target", 1);

            definitions.put(key, new AchievementDefinition(key, displayName, icon, lore, type, target, enabled));
        }

        plugin.getDebugLogger().debug("Achievements loaded: " + definitions.size());
    }

    private void increment(UUID uuid, AchievementType type, int amount) {
        if (!isEnabled()) {
            return;
        }

        Map<String, AchievementProgress> progressMap = cache.computeIfAbsent(uuid, k -> new HashMap<>());
        for (AchievementDefinition def : definitions.values()) {
            if (!def.isEnabled() || def.getType() != type) {
                continue;
            }

            AchievementProgress progress = progressMap.computeIfAbsent(def.getId(), k -> new AchievementProgress(0, 0));
            if (progress.isUnlocked()) {
                continue;
            }

            int newProgress = progress.getProgress() + amount;
            progress.setProgress(newProgress);

            if (newProgress >= def.getTarget()) {
                progress.setUnlockedAt(System.currentTimeMillis());
                notifyUnlock(uuid, def);
            }

            saveToDatabase(uuid, def.getId(), progress);
        }
    }

    private void notifyUnlock(UUID uuid, AchievementDefinition definition) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            MessageManager mm = plugin.getMessageManager();
            String name = ChatColor.translateAlternateColorCodes('&', definition.getDisplayName());
            mm.sendMessage(player, "achievements.unlocked", name);
        }
    }

    private Inventory buildMenu(Player player) {
        int size = achievementsConfig.getInt("gui.size", 54);
        String title = achievementsConfig.getString("gui.title", "&6Achievements");
        Inventory inventory = Bukkit.createInventory(null, size,
                ChatColor.translateAlternateColorCodes('&', title));

        int start = achievementsConfig.getInt("gui.slot-start", 10);
        int step = achievementsConfig.getInt("gui.slot-step", 1);
        if (step < 1) {
            step = 1;
        }

        String lockedLore = achievementsConfig.getString("gui.locked-lore", "&cLocked");
        String unlockedLore = achievementsConfig.getString("gui.unlocked-lore", "&aUnlocked");
        String progressLore = achievementsConfig.getString("gui.progress-lore", "&eProgress: {progress}/{target}");

        int slot = start;
        Map<Integer, AchievementDefinition> slots = new HashMap<>();
        Map<String, AchievementProgress> progressMap = cache.computeIfAbsent(player.getUniqueId(),
                k -> new HashMap<>());

        for (AchievementDefinition def : definitions.values()) {
            if (!def.isEnabled()) {
                continue;
            }
            if (slot >= size) {
                break;
            }

            AchievementProgress progress = progressMap.getOrDefault(def.getId(), new AchievementProgress(0, 0));

            List<String> lore = new ArrayList<>();
            if (def.getLore() != null && !def.getLore().isEmpty()) {
                lore.addAll(def.getLore());
            }

            lore.add(progressLore
                    .replace("{progress}", String.valueOf(progress.getProgress()))
                    .replace("{target}", String.valueOf(def.getTarget())));

            if (progress.isUnlocked()) {
                lore.add(unlockedLore);
            } else {
                lore.add(lockedLore);
            }

            ItemStack item = new ItemBuilder(def.getIcon())
                    .setName(def.getDisplayName())
                    .setLore(new ArrayList<>(lore))
                    .build();

            inventory.setItem(slot, item);
            slots.put(slot, def);
            slot += step;
        }

        menuSlots.put(player.getUniqueId(), slots);
        return inventory;
    }

    private Map<String, AchievementProgress> loadFromDatabase(UUID uuid) {
        Map<String, AchievementProgress> progress = new HashMap<>();
        String query = "SELECT achievement_id, progress, unlocked_at FROM player_achievements WHERE uuid = ?";
        try (ResultSet rs = database.executeQuery(query, uuid.toString())) {
            while (rs.next()) {
                String id = rs.getString("achievement_id");
                int value = rs.getInt("progress");
                long unlockedAt = rs.getLong("unlocked_at");
                progress.put(id, new AchievementProgress(value, unlockedAt));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load achievements: " + e.getMessage());
        }
        return progress;
    }

    private void saveToDatabase(UUID uuid, String id, AchievementProgress progress) {
        try {
            database.executeUpdate(
                    "INSERT INTO player_achievements (uuid, achievement_id, progress, unlocked_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?) "
                            + "ON CONFLICT(uuid, achievement_id) DO UPDATE SET progress = excluded.progress, "
                            + "unlocked_at = excluded.unlocked_at, updated_at = excluded.updated_at",
                    uuid.toString(),
                    id,
                    progress.getProgress(),
                    progress.getUnlockedAt(),
                    System.currentTimeMillis());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save achievement progress: " + e.getMessage());
        }
    }

    private int getProgressValue(UUID uuid, String id) {
        Map<String, AchievementProgress> progressMap = cache.getOrDefault(uuid, Collections.emptyMap());
        AchievementProgress progress = progressMap.get(id);
        return progress == null ? 0 : progress.getProgress();
    }
}
