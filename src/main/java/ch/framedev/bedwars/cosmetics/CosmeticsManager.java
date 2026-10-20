package ch.framedev.bedwars.cosmetics;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cosmetics selection and effects.
 */
public class CosmeticsManager {

    private static final String DEFAULT_KILL = "none";
    private static final String DEFAULT_BED = "none";
    public static final String COSMETIC_FIREWORK = "bedwars_cosmetic_firework";

    private final BedWarsPlugin plugin;
    private final DatabaseManager database;
    private final Map<String, CosmeticDefinition> killEffects;
    private final Map<String, CosmeticDefinition> bedEffects;
    private final Map<UUID, PlayerCosmetics> cache;
    private final Map<UUID, Map<Integer, CosmeticDefinition>> menuSlots;
    private FileConfiguration cosmeticsConfig;

    public CosmeticsManager(BedWarsPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.killEffects = new LinkedHashMap<>();
        this.bedEffects = new LinkedHashMap<>();
        this.cache = new ConcurrentHashMap<>();
        this.menuSlots = new ConcurrentHashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        File cosmeticsFile = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!cosmeticsFile.exists()) {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().severe("Failed to create Directory: " + dataFolder.getAbsolutePath());
            }
            try (InputStream in = plugin.getResource("cosmetics.yml")) {
                if (in != null) {
                    Files.copy(in, cosmeticsFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create cosmetics.yml: " + e.getMessage());
            }
        }

        cosmeticsConfig = YamlConfiguration.loadConfiguration(cosmeticsFile);
        addMissingDefaultEffects(cosmeticsFile);
        loadDefinitions();
    }

    private void addMissingDefaultEffects(File file) {
        try (InputStream input = plugin.getResource("cosmetics.yml")) {
            if (input == null) return;
            var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String section : List.of("kill-effects", "bed-effects")) {
                ConfigurationSection definitions = defaults.getConfigurationSection(section);
                if (definitions == null) continue;
                for (String id : definitions.getKeys(false)) {
                    String path = section + "." + id;
                    if (cosmeticsConfig.contains(path)) continue;
                    ConfigurationSection entry = definitions.getConfigurationSection(id);
                    if (entry == null) continue;
                    cosmeticsConfig.createSection(path, entry.getValues(false));
                    changed = true;
                }
            }
            if (changed) cosmeticsConfig.save(file);
        } catch (IOException error) {
            plugin.getLogger().warning("Could not add new default cosmetics: " + error.getMessage());
        }
    }

    public boolean isEnabled() {
        return cosmeticsConfig.getBoolean("enabled", true);
    }

    public void loadPlayerCosmetics(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            getPlayerCosmetics(uuid);
        });
    }

    public void openCosmeticsMenu(Player player) {
        if (!isEnabled()) {
            plugin.getMessageManager().sendMessage(player, "cosmetics.disabled");
            return;
        }

        Inventory inventory = buildMenu(player);
        player.openInventory(inventory);
        plugin.getMessageManager().sendMessage(player, "cosmetics.opened");
    }

    public boolean isCosmeticsTitle(String title) {
        String expected = ChatColor.translateAlternateColorCodes('&',
                cosmeticsConfig.getString("gui.title", "Cosmetics"));
        return title != null && ChatColor.stripColor(title).equalsIgnoreCase(ChatColor.stripColor(expected));
    }

    public void handleMenuClick(Player player, int slot) {
        Map<Integer, CosmeticDefinition> slots = menuSlots.get(player.getUniqueId());
        if (slots == null) {
            return;
        }

        CosmeticDefinition definition = slots.get(slot);
        if (definition == null || !definition.isEnabled()) {
            return;
        }

        if (!hasAccess(player, definition)) {
            plugin.getMessageManager().sendMessage(player, "cosmetics.locked");
            return;
        }

        PlayerCosmetics cosmetics = getPlayerCosmetics(player.getUniqueId());
        if (definition.getType() == CosmeticType.KILL) {
            cosmetics.setKillEffectId(definition.getId());
        } else {
            cosmetics.setBedEffectId(definition.getId());
        }

        cache.put(player.getUniqueId(), cosmetics);
        saveToDatabase(cosmetics);
        String name = ChatColor.translateAlternateColorCodes('&', definition.getDisplayName());
        plugin.getMessageManager().sendMessage(player, "cosmetics.selected", name);

        Inventory inventory = buildMenu(player);
        player.openInventory(inventory);
    }

    public void applyKillEffect(Player killer, Location location) {
        if (!isEnabled()) {
            return;
        }
        PlayerCosmetics cosmetics = getPlayerCosmetics(killer.getUniqueId());
        CosmeticDefinition definition = killEffects.get(cosmetics.getKillEffectId());
        if (!canUse(killer, definition)) {
            return;
        }
        applyDefinition(definition, location);
    }

    public void applyBedDestroyEffect(Player breaker, Location location) {
        if (!isEnabled()) {
            return;
        }
        PlayerCosmetics cosmetics = getPlayerCosmetics(breaker.getUniqueId());
        CosmeticDefinition definition = bedEffects.get(cosmetics.getBedEffectId());
        if (!canUse(breaker, definition)) {
            return;
        }
        applyDefinition(definition, location);
    }

    public void shutdown() {
        menuSlots.clear();
        cache.clear();
    }

    private boolean canUse(Player player, CosmeticDefinition definition) {
        return definition != null && definition.isEnabled()
                && hasAccess(player, definition);
    }

    private boolean hasAccess(Player player, CosmeticDefinition definition) {
        return cosmeticsConfig.getBoolean("testing.unlock-all", false)
                || definition.getPermission() == null || definition.getPermission().isEmpty()
                || player.hasPermission(definition.getPermission());
    }

    private void loadDefinitions() {
        killEffects.clear();
        bedEffects.clear();

        loadSection("kill-effects", CosmeticType.KILL, killEffects);
        loadSection("bed-effects", CosmeticType.BED_DESTROY, bedEffects);

        plugin.getDebugLogger().debug("Cosmetics loaded: kill=" + killEffects.size()
                + " bed=" + bedEffects.size());
    }

    private void loadSection(String path, CosmeticType type, Map<String, CosmeticDefinition> target) {
        ConfigurationSection section = cosmeticsConfig.getConfigurationSection(path);
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
            Material icon = iconName == null ? null : Material.matchMaterial(iconName.toUpperCase(Locale.ROOT));
            if (icon == null) {
                icon = Material.PAPER;
            }
            List<String> lore = entry.getStringList("lore");
            String effectKey = entry.getString("effect", "NONE");
            String permission = entry.getString("permission", "");
            CustomCosmeticEffect custom = null;
            if ("CUSTOM".equalsIgnoreCase(effectKey)) {
                custom = CustomCosmeticEffect.load(entry, plugin.getLogger());
                if (custom.bursts().isEmpty()) enabled = false;
            }

            CosmeticDefinition definition = new CosmeticDefinition(
                    key,
                    displayName,
                    icon,
                    lore,
                    type,
                    effectKey,
                    permission,
                    enabled,
                    custom);

            target.put(key, definition);
        }
    }

    private Inventory buildMenu(Player player) {
        int size = sanitizeInventorySize(cosmeticsConfig.getInt("gui.size", 54));
        String title = cosmeticsConfig.getString("gui.title", "&dCosmetics");
        Inventory inventory = Bukkit.createInventory(null, size,
                ChatColor.translateAlternateColorCodes('&', title));

        PlayerCosmetics cosmetics = getPlayerCosmetics(player.getUniqueId());
        int killStart = clampSlot(cosmeticsConfig.getInt("gui.kill-slot-start", 10), size);
        int bedStart = clampSlot(cosmeticsConfig.getInt("gui.bed-slot-start", 28), size);
        int step = cosmeticsConfig.getInt("gui.slot-step", 1);
        if (step < 1) {
            step = 1;
        }

        String selectedLore = cosmeticsConfig.getString("gui.selected-lore", "&aSelected");
        String lockedLore = cosmeticsConfig.getString("gui.locked-lore", "&cLocked");

        Map<Integer, CosmeticDefinition> slots = new HashMap<>();

        int slot = killStart;
        for (CosmeticDefinition definition : killEffects.values()) {
            if (!definition.isEnabled()) {
                continue;
            }
            ItemStack item = buildCosmeticItem(definition, cosmetics.getKillEffectId(), player,
                    selectedLore, lockedLore);
            if (slot < size) {
                inventory.setItem(slot, item);
                slots.put(slot, definition);
            }
            slot += step;
        }

        slot = bedStart;
        for (CosmeticDefinition definition : bedEffects.values()) {
            if (!definition.isEnabled()) {
                continue;
            }
            while (slot < size && slots.containsKey(slot)) slot += step;
            ItemStack item = buildCosmeticItem(definition, cosmetics.getBedEffectId(), player,
                    selectedLore, lockedLore);
            if (slot < size) {
                inventory.setItem(slot, item);
                slots.put(slot, definition);
            }
            slot += step;
        }

        menuSlots.put(player.getUniqueId(), slots);
        return inventory;
    }

    private int sanitizeInventorySize(int configuredSize) {
        int clamped = Math.max(9, Math.min(54, configuredSize));
        return ((clamped + 8) / 9) * 9;
    }

    private int clampSlot(int slot, int inventorySize) {
        return Math.max(0, Math.min(inventorySize - 1, slot));
    }

    private ItemStack buildCosmeticItem(CosmeticDefinition definition, String selectedId, Player player,
            String selectedLore, String lockedLore) {
        List<String> lore = new ArrayList<>();
        if (definition.getLore() != null && !definition.getLore().isEmpty()) {
            lore.addAll(definition.getLore());
        }

        boolean locked = !hasAccess(player, definition);

        if (definition.getId().equalsIgnoreCase(selectedId)) {
            lore.add(selectedLore);
        } else if (locked) {
            lore.add(lockedLore);
        }

        return new ItemBuilder(definition.getIcon())
                .setName(definition.getDisplayName())
                .setLore(new ArrayList<>(lore))
                .build();
    }

    private PlayerCosmetics getPlayerCosmetics(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDatabase);
    }

    private PlayerCosmetics loadFromDatabase(UUID uuid) {
        String query = "SELECT kill_effect, bed_effect FROM player_cosmetics WHERE uuid = ?";
        try (ResultSet rs = database.executeQuery(query, uuid.toString())) {
            if (rs.next()) {
                String kill = rs.getString("kill_effect");
                String bed = rs.getString("bed_effect");
                return new PlayerCosmetics(uuid,
                        kill == null ? DEFAULT_KILL : kill,
                        bed == null ? DEFAULT_BED : bed);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load cosmetics: " + e.getMessage());
        }

        PlayerCosmetics cosmetics = new PlayerCosmetics(uuid, DEFAULT_KILL, DEFAULT_BED);
        return cosmetics;
    }

    private void saveToDatabase(PlayerCosmetics cosmetics) {
        try {
            database.executeUpdate("INSERT INTO player_cosmetics (uuid, kill_effect, bed_effect, updated_at) "
                    + "VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(uuid) DO UPDATE SET kill_effect = excluded.kill_effect, "
                    + "bed_effect = excluded.bed_effect, updated_at = excluded.updated_at",
                    cosmetics.getUuid().toString(),
                    cosmetics.getKillEffectId(),
                    cosmetics.getBedEffectId(),
                    System.currentTimeMillis());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save cosmetics: " + e.getMessage());
        }
    }

    private void applyDefinition(CosmeticDefinition definition, Location location) {
        if (definition.customEffect() != null) {
            definition.customEffect().play(location);
        } else {
            applyEffect(definition.getEffectKey(), location);
        }
    }

    private void applyEffect(String effectKey, Location location) {
        if (effectKey == null) {
            return;
        }

        String key = effectKey.trim().toUpperCase(Locale.ROOT);
        if ("NONE".equals(key)) {
            return;
        }

        World world = location.getWorld();
        if (world == null) {
            return;
        }

        switch (key) {
            case "FIREWORK" -> spawnFirework(world, location);
            case "LIGHTNING" -> world.strikeLightningEffect(location);
            case "HEART" -> world.spawnParticle(Particle.HEART, location, 12, 0.5, 0.5, 0.5, 0.0);
            case "SMOKE" -> world.spawnParticle(Particle.SMOKE_NORMAL, location, 20, 0.6, 0.6, 0.6, 0.0);
            case "EXPLOSION" -> world.spawnParticle(Particle.EXPLOSION_LARGE, location, 1, 0, 0, 0, 0.0);
            case "FLAME" -> world.spawnParticle(Particle.FLAME, location, 35, 0.6, 0.8, 0.6, 0.02);
            case "SOUL" -> world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 30, 0.6, 0.8, 0.6, 0.02);
            case "PORTAL" -> world.spawnParticle(Particle.PORTAL, location, 60, 0.7, 0.8, 0.7, 0.3);
            case "ENCHANT" -> world.spawnParticle(Particle.ENCHANTMENT_TABLE, location, 50, 0.8, 0.8, 0.8, 0.5);
            case "MUSIC" -> world.spawnParticle(Particle.NOTE, location, 16, 0.6, 0.8, 0.6, 0.0);
            case "TOTEM" -> world.spawnParticle(Particle.TOTEM, location, 45, 0.6, 0.8, 0.6, 0.1);
            default -> plugin.getDebugLogger().debug("Unknown cosmetic effect: " + effectKey);
        }
    }

    private void spawnFirework(World world, Location location) {
        Firework firework = world.spawn(location, Firework.class);
        firework.setMetadata(COSMETIC_FIREWORK, new FixedMetadataValue(plugin, true));
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(Color.RED, Color.WHITE)
                .with(org.bukkit.FireworkEffect.Type.BALL)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);

        new BukkitRunnable() {
            @Override
            public void run() {
                firework.detonate();
            }
        }.runTaskLater(plugin, 1L);
    }
}
