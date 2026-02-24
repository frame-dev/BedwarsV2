package ch.framedev.bedwars.achievements;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.utils.ItemBuilder;
import ch.framedev.bedwars.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Manages achievements and progress.
 *
 * De-duplication:
 * - Single "record(...)" method used by all recordX calls
 * - Single "stripTitleEquals(...)" helper for GUI title comparisons
 * - Single "getOrCreateProgressMap(...)" helper for cache access
 * - Single "safeMaterial(...)" helper for icon parsing
 * - Single "buildLore(...)" helper for GUI lore rendering
 */
public class AchievementsManager {

    private final BedWarsPlugin plugin;
    private final DatabaseManager database;

    private final Map<String, AchievementDefinition> definitions = new HashMap<>();
    private final Map<UUID, Map<String, AchievementProgress>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, AchievementDefinition>> menuSlots = new ConcurrentHashMap<>();

    private FileConfiguration achievementsConfig;

    public AchievementsManager(BedWarsPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        loadConfig();
    }

    /* --------------------------------------------------------------------- */
    /* Config / enable                                                        */
    /* --------------------------------------------------------------------- */

    public void loadConfig() {
        File achievementsFile = new File(plugin.getDataFolder(), "achievements.yml");
        if (!achievementsFile.exists()) {
            if (!plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().severe("Failed to create Directory: " + plugin.getDataFolder().getAbsolutePath());
            }
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

    /* --------------------------------------------------------------------- */
    /* Player load                                                            */
    /* --------------------------------------------------------------------- */

    public void loadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> cache.put(uuid, loadFromDatabase(uuid)));
    }

    /* --------------------------------------------------------------------- */
    /* GUI                                                                     */
    /* --------------------------------------------------------------------- */

    public boolean isAchievementsTitle(String title) {
        String expected = color(achievementsConfig.getString("gui.title", "Achievements"));
        return stripTitleEquals(title, expected);
    }

    public void openMenu(Player player) {
        if (!isEnabled()) {
            plugin.getMessageManager().sendMessage(player, "achievements.disabled");
            return;
        }

        player.openInventory(buildMenu(player));
        plugin.getMessageManager().sendMessage(player, "achievements.opened");
    }

    public void handleMenuClick(Player player, int slot) {
        Map<Integer, AchievementDefinition> slots = menuSlots.get(player.getUniqueId());
        if (slots == null) return;

        AchievementDefinition def = slots.get(slot);
        if (def == null || !def.isEnabled()) return;

        String name = color(def.getDisplayName());
        plugin.getMessageManager().sendMessage(
                player,
                "achievements.progress",
                name,
                getProgressValue(player.getUniqueId(), def.getId()),
                def.getTarget()
        );
    }

    private Inventory buildMenu(Player player) {
        int size = achievementsConfig.getInt("gui.size", 54);
        String title = achievementsConfig.getString("gui.title", "&6Achievements");
        Inventory inventory = Bukkit.createInventory(null, size, color(title));

        int start = achievementsConfig.getInt("gui.slot-start", 10);
        int step = Math.max(1, achievementsConfig.getInt("gui.slot-step", 1));

        String lockedLore = achievementsConfig.getString("gui.locked-lore", "&cLocked");
        String unlockedLore = achievementsConfig.getString("gui.unlocked-lore", "&aUnlocked");
        String progressLore = achievementsConfig.getString("gui.progress-lore", "&eProgress: {progress}/{target}");

        final int[] slot = {start};
        Map<Integer, AchievementDefinition> slotMap = new HashMap<>();
        Map<String, AchievementProgress> progressMap = getOrCreateProgressMap(player.getUniqueId());

        forEachEnabledDefinition(def -> {
            if (slot[0] >= size) return;

            AchievementProgress progress = progressMap.getOrDefault(def.getId(), new AchievementProgress(0, 0));

            List<String> lore = buildLore(def, progress, progressLore, lockedLore, unlockedLore);

            ItemStack item = new ItemBuilder(def.getIcon())
                    .setName(def.getDisplayName())
                    .setLore(new ArrayList<>(lore))
                    .build();

            inventory.setItem(slot[0], item);
            slotMap.put(slot[0], def);
            slot[0] += step;
        });

        menuSlots.put(player.getUniqueId(), slotMap);
        return inventory;
    }

    private List<String> buildLore(AchievementDefinition def,
                                   AchievementProgress progress,
                                   String progressTemplate,
                                   String lockedLore,
                                   String unlockedLore) {
        List<String> lore = new ArrayList<>();

        if (def.getLore() != null && !def.getLore().isEmpty()) {
            lore.addAll(def.getLore());
        }

        lore.add(progressTemplate
                .replace("{progress}", String.valueOf(progress.getProgress()))
                .replace("{target}", String.valueOf(def.getTarget())));

        lore.add(progress.isUnlocked() ? unlockedLore : lockedLore);
        return lore;
    }

    /* --------------------------------------------------------------------- */
    /* Recording                                                              */
    /* --------------------------------------------------------------------- */

    public void recordKill(UUID uuid)       { record(uuid, AchievementType.KILLS, 1); }
    public void recordFinalKill(UUID uuid)  { record(uuid, AchievementType.FINAL_KILLS, 1); }
    public void recordBedBroken(UUID uuid)  { record(uuid, AchievementType.BEDS_BROKEN, 1); }
    public void recordWin(UUID uuid)        { record(uuid, AchievementType.WINS, 1); }
    public void recordGamePlayed(UUID uuid) { record(uuid, AchievementType.GAMES_PLAYED, 1); }

    private void record(UUID uuid, AchievementType type, int amount) {
        if (!isEnabled() || uuid == null || type == null || amount <= 0) return;

        Map<String, AchievementProgress> progressMap = getOrCreateProgressMap(uuid);

        forEachEnabledDefinition(def -> {
            if (def.getType() != type) return;

            AchievementProgress progress = progressMap.computeIfAbsent(def.getId(), k -> new AchievementProgress(0, 0));
            if (progress.isUnlocked()) return;

            int newProgress = progress.getProgress() + amount;
            progress.setProgress(newProgress);

            if (newProgress >= def.getTarget()) {
                progress.setUnlockedAt(System.currentTimeMillis());
                notifyUnlock(uuid, def);
            }

            saveToDatabase(uuid, def.getId(), progress);
        });
    }

    private void notifyUnlock(UUID uuid, AchievementDefinition def) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        MessageManager mm = plugin.getMessageManager();
        mm.sendMessage(player, "achievements.unlocked", color(def.getDisplayName()));
    }

    /* --------------------------------------------------------------------- */
    /* Definitions                                                            */
    /* --------------------------------------------------------------------- */

    private void loadDefinitions() {
        definitions.clear();

        ConfigurationSection section = achievementsConfig.getConfigurationSection("achievements");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            boolean enabled = entry.getBoolean("enabled", true);
            String displayName = entry.getString("display-name", key);

            Material icon = safeMaterial(entry.getString("icon", "PAPER"), Material.PAPER);
            List<String> lore = entry.getStringList("lore");

            AchievementType type = safeEnum(
                    entry.getString("type", "KILLS"),
                    AchievementType.KILLS,
                    AchievementType::valueOf,
                    () -> plugin.getLogger().warning("Invalid achievement type for " + key + ", defaulting to KILLS")
            );

            int target = entry.getInt("target", 1);

            definitions.put(key, new AchievementDefinition(key, displayName, icon, lore, type, target, enabled));
        }

        plugin.getDebugLogger().debug("Achievements loaded: " + definitions.size());
    }

    private void forEachEnabledDefinition(java.util.function.Consumer<AchievementDefinition> action) {
        for (AchievementDefinition def : definitions.values()) {
            if (def != null && def.isEnabled()) {
                action.accept(def);
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /* Database                                                               */
    /* --------------------------------------------------------------------- */

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
                    "INSERT INTO player_achievements (uuid, achievement_id, progress, unlocked_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?) " +
                            "ON CONFLICT(uuid, achievement_id) DO UPDATE SET progress = excluded.progress, " +
                            "unlocked_at = excluded.unlocked_at, updated_at = excluded.updated_at",
                    uuid.toString(),
                    id,
                    progress.getProgress(),
                    progress.getUnlockedAt(),
                    System.currentTimeMillis()
            );
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save achievement progress: " + e.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    /* Utilities                                                              */
    /* --------------------------------------------------------------------- */

    public void shutdown() {
        menuSlots.clear();
        cache.clear();
    }

    private Map<String, AchievementProgress> getOrCreateProgressMap(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    private int getProgressValue(UUID uuid, String id) {
        Map<String, AchievementProgress> progressMap = cache.getOrDefault(uuid, Collections.emptyMap());
        AchievementProgress progress = progressMap.get(id);
        return progress == null ? 0 : progress.getProgress();
    }

    private boolean stripTitleEquals(String a, String b) {
        String sa = ChatColor.stripColor(a);
        String sb = ChatColor.stripColor(b);
        if (sa == null || sb == null) return false;
        return sa.equalsIgnoreCase(sb);
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private Material safeMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return (m != null) ? m : fallback;
    }

    private <T> T safeEnum(String raw,
                           T fallback,
                           java.util.function.Function<String, T> parser,
                           Runnable onError) {
        try {
            return parser.apply(raw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            if (onError != null) onError.run();
            return fallback;
        }
    }
}