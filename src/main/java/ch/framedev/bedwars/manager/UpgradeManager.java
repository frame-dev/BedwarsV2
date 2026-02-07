package ch.framedev.bedwars.manager;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import ch.framedev.bedwars.upgrades.TeamUpgrades;

/**
 * Manages team upgrades loaded from upgrades.yml configuration file.
 * Allows server owners to add, remove, or modify upgrades without code changes.
 */
public class UpgradeManager {

    private final Plugin plugin;
    private FileConfiguration upgradesConfig;
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();

    public UpgradeManager(Plugin plugin) {
        this.plugin = plugin;
        loadUpgradesConfig();
    }

    /**
     * Load or create upgrades.yml configuration file
     */
    private void loadUpgradesConfig() {
        File upgradesFile = new File(plugin.getDataFolder(), "upgrades.yml");

        // Create default upgrades.yml if it doesn't exist
        if (!upgradesFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream input = plugin.getResource("upgrades.yml")) {
                if (input != null) {
                    Files.copy(input, upgradesFile.toPath());
                    plugin.getLogger().info("Created default upgrades.yml");
                } else {
                    plugin.getLogger().warning("Could not find default upgrades.yml in resources");
                    createDefaultUpgradesConfig(upgradesFile);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create upgrades.yml: " + e.getMessage());
                createDefaultUpgradesConfig(upgradesFile);
            }
        }

        upgradesConfig = YamlConfiguration.loadConfiguration(upgradesFile);
        loadUpgradesFromConfig();
    }

    /**
     * Create default upgrades configuration if resource file is missing
     */
    private void createDefaultUpgradesConfig(File file) {
        upgradesConfig = new YamlConfiguration();
        // Minimal fallback configuration
        upgradesConfig.set("upgrades.sharpness.enabled", true);
        upgradesConfig.set("upgrades.sharpness.display-name", "&cSharpened Swords");
        upgradesConfig.set("upgrades.sharpness.icon", "IRON_SWORD");
        upgradesConfig.set("upgrades.sharpness.max-level", 1);
        upgradesConfig.set("upgrades.sharpness.cost.level-1.material", "DIAMOND");
        upgradesConfig.set("upgrades.sharpness.cost.level-1.amount", 8);

        try {
            upgradesConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save default upgrades.yml: " + e.getMessage());
        }
    }

    /**
     * Load all upgrades from the configuration file
     */
    private void loadUpgradesFromConfig() {
        upgrades.clear();

        ConfigurationSection upgradesSection = upgradesConfig.getConfigurationSection("upgrades");
        if (upgradesSection == null) {
            plugin.getLogger().warning("No upgrades section found in upgrades.yml");
            return;
        }

        for (String key : upgradesSection.getKeys(false)) {
            try {
                Upgrade upgrade = loadUpgrade(key, upgradesSection.getConfigurationSection(key));
                if (upgrade != null && upgrade.isEnabled()) {
                    upgrades.put(key, upgrade);
                    plugin.getLogger().info("Loaded upgrade: " + key + " (" + upgrade.getMaxLevel() + " levels)");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load upgrade '" + key + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + upgrades.size() + " upgrades from upgrades.yml");
    }

    /**
     * Load a single upgrade from configuration
     */
    @SuppressWarnings("deprecation")
    private Upgrade loadUpgrade(String id, ConfigurationSection section) {
        if (section == null)
            return null;

        boolean enabled = section.getBoolean("enabled", true);
        if (!enabled)
            return null;

        String displayName = section.getString("display-name", "&7" + id);
        List<String> description = section.getStringList("description");
        String iconStr = section.getString("icon", "DIAMOND");
        int maxLevel = section.getInt("max-level", 1);

        // Load effect type and properties
        String effectTypeStr = section.getString("effect-type", "SPECIAL");
        EffectType effectType;
        try {
            effectType = EffectType.valueOf(effectTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger()
                    .warning("Invalid effect-type '" + effectTypeStr + "' for upgrade " + id + ", using SPECIAL");
            effectType = EffectType.SPECIAL;
        }

        // Load effect-specific properties
        String target = section.getString("target", "");
        String enchantmentStr = section.getString("enchantment", "");
        String potionTypeStr = section.getString("potion-type", "");
        int duration = section.getInt("duration", Integer.MAX_VALUE);
        boolean amplifierPerLevel = section.getBoolean("amplifier-per-level", false);

        Enchantment enchantment = null;
        if (!enchantmentStr.isEmpty()) {
            try {
                NamespacedKey key = NamespacedKey.minecraft(enchantmentStr.toLowerCase());
                enchantment = Enchantment.getByKey(key);
                if (enchantment == null) {
                    plugin.getLogger().warning("Invalid enchantment '" + enchantmentStr + "' for upgrade " + id);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid enchantment '" + enchantmentStr + "' for upgrade " + id);
            }
        }

        PotionEffectType potionType = null;
        if (!potionTypeStr.isEmpty()) {
            try {
                NamespacedKey key = NamespacedKey.minecraft(potionTypeStr.toLowerCase());
                potionType = PotionEffectType.getByKey(key);
                if (potionType == null) {
                    plugin.getLogger().warning("Invalid potion-type '" + potionTypeStr + "' for upgrade " + id);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid potion-type '" + potionTypeStr + "' for upgrade " + id);
            }
        }

        Material icon;
        try {
            icon = Material.valueOf(iconStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + iconStr + "' for upgrade " + id + ", using DIAMOND");
            icon = Material.DIAMOND;
        }

        // Load costs per level
        Map<Integer, UpgradeCost> costs = new HashMap<>();
        ConfigurationSection costSection = section.getConfigurationSection("cost");
        if (costSection != null) {
            for (String levelKey : costSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelKey.replace("level-", ""));
                    ConfigurationSection levelCost = costSection.getConfigurationSection(levelKey);
                    if (levelCost != null) {
                        String materialStr = levelCost.getString("material", "DIAMOND");
                        int amount = levelCost.getInt("amount", 1);

                        Material material;
                        try {
                            material = Material.valueOf(materialStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning(
                                    "Invalid material '" + materialStr + "' in cost for " + id + " level " + level);
                            material = Material.DIAMOND;
                        }

                        costs.put(level, new UpgradeCost(material, amount));
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level key '" + levelKey + "' for upgrade " + id);
                }
            }
        }

        return new Upgrade(id, displayName, description, icon, maxLevel, costs,
                effectType, target, enchantment, potionType, duration, amplifierPerLevel);
    }

    /**
     * Reload upgrades from configuration
     */
    public void reload() {
        File upgradesFile = new File(plugin.getDataFolder(), "upgrades.yml");
        upgradesConfig = YamlConfiguration.loadConfiguration(upgradesFile);
        loadUpgradesFromConfig();
    }

    /**
     * Get all loaded upgrades
     */
    public Map<String, Upgrade> getUpgrades() {
        return new LinkedHashMap<>(upgrades);
    }

    /**
     * Get a specific upgrade by ID
     */
    public Upgrade getUpgrade(String id) {
        return upgrades.get(id);
    }

    /**
     * Check if an upgrade exists
     */
    public boolean hasUpgrade(String id) {
        return upgrades.containsKey(id);
    }

    public void applyUpgradesToItem(ItemStack item, TeamUpgrades teamUpgrades) {
        if (item == null || teamUpgrades == null) {
            return;
        }

        for (Map.Entry<String, Upgrade> entry : upgrades.entrySet()) {
            Upgrade upgrade = entry.getValue();
            int level = teamUpgrades.getUpgradeLevel(entry.getKey());

            if (level > 0 && upgrade.getEffectType() == EffectType.ENCHANTMENT) {
                Enchantment enchantment = upgrade.getEnchantment();
                if (enchantment == null) {
                    continue;
                }

                String target = upgrade.getTarget();
                boolean shouldApply = false;

                if ("WEAPON".equalsIgnoreCase(target) && isMeleeWeapon(item.getType())) {
                    shouldApply = true;
                } else if ("ARMOR".equalsIgnoreCase(target) && isArmor(item.getType())) {
                    shouldApply = true;
                }

                if (shouldApply && !item.getEnchantments().containsKey(enchantment)) {
                    item.addEnchantment(enchantment, level);
                }
            }
        }
    }

    public void applyPotionUpgrades(Player player, TeamUpgrades teamUpgrades) {
        if (player == null || teamUpgrades == null) {
            return;
        }

        for (Map.Entry<String, Upgrade> entry : upgrades.entrySet()) {
            Upgrade upgrade = entry.getValue();
            int level = teamUpgrades.getUpgradeLevel(entry.getKey());

            if (level > 0 && upgrade.getEffectType() == EffectType.POTION_EFFECT) {
                if (upgrade.getPotionType() != null) {
                    int amplifier = upgrade.isAmplifierPerLevel() ? (level - 1) : 0;
                    player.addPotionEffect(new PotionEffect(
                            upgrade.getPotionType(),
                            upgrade.getDuration(),
                            amplifier,
                            false,
                            false));
                }
            }
        }
    }

    private boolean isMeleeWeapon(Material material) {
        return switch (material) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD,
                    WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE -> true;
            default -> false;
        };
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    /**
     * Enum for upgrade effect types
     */
    public enum EffectType {
        ENCHANTMENT, // Adds enchantments to items
        POTION_EFFECT, // Gives potion effects to players
        SPECIAL // Custom effects (heal pool, dragon buff, etc.)
    }

    /**
     * Represents a team upgrade configuration
     */
    public static class Upgrade {
        private final String id;
        private final String displayName;
        private final List<String> description;
        private final Material icon;
        private final int maxLevel;
        private final Map<Integer, UpgradeCost> costs;
        private final EffectType effectType;
        private final String target;
        private final Enchantment enchantment;
        private final PotionEffectType potionType;
        private final int duration;
        private final boolean amplifierPerLevel;

        public Upgrade(String id, String displayName, List<String> description,
                Material icon, int maxLevel, Map<Integer, UpgradeCost> costs,
                EffectType effectType, String target, Enchantment enchantment,
                PotionEffectType potionType, int duration, boolean amplifierPerLevel) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.icon = icon;
            this.maxLevel = maxLevel;
            this.costs = costs;
            this.effectType = effectType;
            this.target = target;
            this.enchantment = enchantment;
            this.potionType = potionType;
            this.duration = duration;
            this.amplifierPerLevel = amplifierPerLevel;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getDescription() {
            return new ArrayList<>(description);
        }

        public Material getIcon() {
            return icon;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public boolean isEnabled() {
            return true;
        }

        public UpgradeCost getCost(int level) {
            return costs.get(level);
        }

        public boolean hasCost(int level) {
            return costs.containsKey(level);
        }

        public EffectType getEffectType() {
            return effectType;
        }

        public String getTarget() {
            return target;
        }

        public Enchantment getEnchantment() {
            return enchantment;
        }

        public PotionEffectType getPotionType() {
            return potionType;
        }

        public int getDuration() {
            return duration;
        }

        public boolean isAmplifierPerLevel() {
            return amplifierPerLevel;
        }
    }

    /**
     * Represents the cost of upgrading to a specific level
     */
    public static class UpgradeCost {
        private final Material material;
        private final int amount;

        public UpgradeCost(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }
    }
}