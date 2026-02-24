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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cosmetics selection and effects.
 */
public class CosmeticsManager {

    private static final String DEFAULT_KILL = "none";
    private static final String DEFAULT_BED = "none";

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
        this.killEffects = new HashMap<>();
        this.bedEffects = new HashMap<>();
        this.cache = new ConcurrentHashMap<>();
        this.menuSlots = new ConcurrentHashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        File cosmeticsFile = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!cosmeticsFile.exists()) {
            if(!plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().severe("Failed to create Directory: " + plugin.getDataFolder().getAbsolutePath());
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
        loadDefinitions();
    }

    public boolean isEnabled() {
        return cosmeticsConfig.getBoolean("enabled", true);
    }

    public void loadPlayerCosmetics(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerCosmetics cosmetics = loadFromDatabase(uuid);
            cache.put(uuid, cosmetics);
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
        return ChatColor.stripColor(title).equalsIgnoreCase(ChatColor.stripColor(expected));
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

        String permission = definition.getPermission();
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
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
        if (definition == null) {
            return;
        }
        applyEffect(definition.getEffectKey(), location, killer);
    }

    public void applyBedDestroyEffect(Player breaker, Location location) {
        if (!isEnabled()) {
            return;
        }
        PlayerCosmetics cosmetics = getPlayerCosmetics(breaker.getUniqueId());
        CosmeticDefinition definition = bedEffects.get(cosmetics.getBedEffectId());
        if (definition == null) {
            return;
        }
        applyEffect(definition.getEffectKey(), location, breaker);
    }

    public void shutdown() {
        menuSlots.clear();
        cache.clear();
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
            Material icon = Material.matchMaterial(iconName.toUpperCase());
            if (icon == null) {
                icon = Material.PAPER;
            }
            List<String> lore = entry.getStringList("lore");
            String effectKey = entry.getString("effect", "NONE");
            String permission = entry.getString("permission", "");

            CosmeticDefinition definition = new CosmeticDefinition(
                    key,
                    displayName,
                    icon,
                    lore,
                    type,
                    effectKey,
                    permission,
                    enabled);

            target.put(key, definition);
        }
    }

    private Inventory buildMenu(Player player) {
        int size = cosmeticsConfig.getInt("gui.size", 54);
        String title = cosmeticsConfig.getString("gui.title", "&dCosmetics");
        Inventory inventory = Bukkit.createInventory(null, size,
                ChatColor.translateAlternateColorCodes('&', title));

        PlayerCosmetics cosmetics = getPlayerCosmetics(player.getUniqueId());
        int killStart = cosmeticsConfig.getInt("gui.kill-slot-start", 10);
        int bedStart = cosmeticsConfig.getInt("gui.bed-slot-start", 28);
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

    private ItemStack buildCosmeticItem(CosmeticDefinition definition, String selectedId, Player player,
            String selectedLore, String lockedLore) {
        List<String> lore = new ArrayList<>();
        if (definition.getLore() != null && !definition.getLore().isEmpty()) {
            lore.addAll(definition.getLore());
        }

        boolean locked = definition.getPermission() != null && !definition.getPermission().isEmpty()
                && !player.hasPermission(definition.getPermission());

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
        PlayerCosmetics cosmetics = cache.get(uuid);
        if (cosmetics != null) {
            return cosmetics;
        }
        cosmetics = loadFromDatabase(uuid);
        cache.put(uuid, cosmetics);
        return cosmetics;
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
        saveToDatabase(cosmetics);
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

    private void applyEffect(String effectKey, Location location, Player player) {
        if (effectKey == null) {
            return;
        }

        String key = effectKey.trim().toUpperCase();
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
            default -> plugin.getDebugLogger().debug("Unknown cosmetic effect: " + effectKey);
        }
    }

    private void spawnFirework(World world, Location location) {
        Firework firework = world.spawn(location, Firework.class);
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
