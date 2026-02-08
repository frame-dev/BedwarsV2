package ch.framedev.bedwars.shop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages the shop system - loads items from shop.yml
 */
public class ShopManager {

    private final Plugin plugin;
    private final List<ShopCategory> categories;
    private FileConfiguration shopConfig;

    public ShopManager(Plugin plugin) {
        this.plugin = plugin;
        this.categories = new ArrayList<>();
        loadShopConfig();
        loadShopFromConfig();
        plugin.getLogger().info("ShopManager initialized with " + categories.size() + " categories");
    }

    private void loadShopConfig() {
        File shopFile = new File(plugin.getDataFolder(), "shop.yml");

        // Create shop.yml if it doesn't exist
        if (!shopFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("shop.yml")) {
                if (in != null) {
                    Files.copy(in, shopFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create shop.yml", e);
            }
        }

        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }

    private void loadShopFromConfig() {
        categories.clear();

        ConfigurationSection categoriesSection = shopConfig.getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().warning("No categories found in shop.yml! Using default shop configuration.");
            initializeDefaultShop();
            return;
        }

        for (String categoryName : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryName);
            if (categorySection == null)
                continue;

            // Get category icon
            String iconName = categorySection.getString("icon", "STONE");
            Material icon;
            try {
                icon = Material.valueOf(iconName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(
                        "Invalid icon material '" + iconName + "' for category " + categoryName + ", using STONE");
                icon = Material.STONE;
            }

            ShopCategory category = new ShopCategory(categoryName, icon);

            // Load items
            List<?> itemsList = categorySection.getList("items");
            if (itemsList != null) {
                for (Object itemObj : itemsList) {
                    if (itemObj instanceof ConfigurationSection) {
                        ConfigurationSection itemSection = (ConfigurationSection) itemObj;
                        loadShopItem(category, itemSection);
                    } else if (itemObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> itemMap = (java.util.Map<String, Object>) itemObj;
                        loadShopItem(category, itemMap);
                    }
                }
            }

            categories.add(category);
        }

        plugin.getLogger().info("Loaded " + categories.size() + " shop categories from shop.yml");
    }

    private void loadShopItem(ShopCategory category, ConfigurationSection itemSection) {
        try {
            String itemName = itemSection.getString("item");
            int itemAmount = itemSection.getInt("amount", 1);
            String costItemName = itemSection.getString("cost-item");
            int costAmount = itemSection.getInt("cost-amount", 1);
            String displayName = itemSection.getString("display-name", null);

            Material itemMaterial = Material.valueOf(itemName.toUpperCase());
            Material costMaterial = Material.valueOf(costItemName.toUpperCase());

            ItemStack item = new ItemStack(itemMaterial, itemAmount);
            ItemStack cost = new ItemStack(costMaterial, costAmount);

            applyItemMeta(item, displayName);

            ShopItem shopItem;
            if (displayName != null && !displayName.isEmpty()) {
                shopItem = new ShopItem(item, cost, displayName);
            } else {
                shopItem = new ShopItem(item, cost);
            }

            category.addItem(shopItem);
        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Failed to load shop item in category " + category.getName() + ": " + e.getMessage());
        }
    }

    private void loadShopItem(ShopCategory category, java.util.Map<String, Object> itemMap) {
        try {
            String itemName = (String) itemMap.get("item");
            int itemAmount = itemMap.containsKey("amount") ? ((Number) itemMap.get("amount")).intValue() : 1;
            String costItemName = (String) itemMap.get("cost-item");
            int costAmount = itemMap.containsKey("cost-amount") ? ((Number) itemMap.get("cost-amount")).intValue() : 1;
            String displayName = (String) itemMap.get("display-name");

            Material itemMaterial = Material.valueOf(itemName.toUpperCase());
            Material costMaterial = Material.valueOf(costItemName.toUpperCase());

            ItemStack item = new ItemStack(itemMaterial, itemAmount);
            ItemStack cost = new ItemStack(costMaterial, costAmount);

            applyItemMeta(item, displayName);

            ShopItem shopItem;
            if (displayName != null && !displayName.isEmpty()) {
                shopItem = new ShopItem(item, cost, displayName);
            } else {
                shopItem = new ShopItem(item, cost);
            }

            category.addItem(shopItem);
        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Failed to load shop item in category " + category.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Reload shop configuration from file
     */
    public void reload() {
        loadShopConfig();
        loadShopFromConfig();
    }

    private void applyItemMeta(ItemStack item, String displayName) {
        if (item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        }

        if (meta instanceof PotionMeta potionMeta && displayName != null) {
            String lower = displayName.toLowerCase();
            PotionType potionType = null;
            if (lower.contains("speed")) {
                potionType = PotionType.SPEED;
            } else if (lower.contains("jump")) {
                potionType = PotionType.JUMP;
            } else if (lower.contains("invis")) {
                potionType = PotionType.INVISIBILITY;
            }

            if (potionType != null) {
                applyBasePotionType(potionMeta, potionType);
            }
        }

        item.setItemMeta(meta);

        if (item.getType() == Material.STICK && displayName != null) {
            String lower = displayName.toLowerCase();
            if (lower.contains("knockback")) {
                item.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
            }
        }
    }

    private void initializeDefaultShop() {
        // Blocks category
        ShopCategory blocks = new ShopCategory("Blocks", Material.SANDSTONE);
        blocks.addItem(new ShopItem(new ItemStack(Material.WHITE_WOOL, 16), new ItemStack(Material.IRON_INGOT, 4)));
        blocks.addItem(new ShopItem(new ItemStack(Material.TERRACOTTA, 16), new ItemStack(Material.IRON_INGOT, 12)));
        blocks.addItem(new ShopItem(new ItemStack(Material.END_STONE, 12), new ItemStack(Material.IRON_INGOT, 24)));
        blocks.addItem(new ShopItem(new ItemStack(Material.OBSIDIAN, 4), new ItemStack(Material.EMERALD, 4)));
        categories.add(blocks);

        // Weapons category
        ShopCategory weapons = new ShopCategory("Weapons", Material.GOLDEN_SWORD);
        weapons.addItem(new ShopItem(new ItemStack(Material.STONE_SWORD), new ItemStack(Material.IRON_INGOT, 10)));
        weapons.addItem(new ShopItem(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.GOLD_INGOT, 7)));
        weapons.addItem(new ShopItem(new ItemStack(Material.DIAMOND_SWORD), new ItemStack(Material.EMERALD, 4)));
        weapons.addItem(
                new ShopItem(new ItemStack(Material.STICK), new ItemStack(Material.GOLD_INGOT, 5), "Knockback Stick"));
        categories.add(weapons);

        // Armor category
        ShopCategory armor = new ShopCategory("Armor", Material.CHAINMAIL_CHESTPLATE);
        armor.addItem(
                new ShopItem(new ItemStack(Material.CHAINMAIL_CHESTPLATE), new ItemStack(Material.IRON_INGOT, 40)));
        armor.addItem(new ShopItem(new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.GOLD_INGOT, 12)));
        armor.addItem(new ShopItem(new ItemStack(Material.DIAMOND_CHESTPLATE), new ItemStack(Material.EMERALD, 6)));
        categories.add(armor);

        // Tools category
        ShopCategory tools = new ShopCategory("Tools", Material.STONE_PICKAXE);
        tools.addItem(new ShopItem(new ItemStack(Material.WOODEN_PICKAXE), new ItemStack(Material.IRON_INGOT, 10)));
        tools.addItem(new ShopItem(new ItemStack(Material.IRON_PICKAXE), new ItemStack(Material.GOLD_INGOT, 3)));
        tools.addItem(new ShopItem(new ItemStack(Material.DIAMOND_PICKAXE), new ItemStack(Material.GOLD_INGOT, 6)));
        tools.addItem(new ShopItem(new ItemStack(Material.WOODEN_AXE), new ItemStack(Material.IRON_INGOT, 10)));
        tools.addItem(new ShopItem(new ItemStack(Material.STONE_AXE), new ItemStack(Material.IRON_INGOT, 10)));
        tools.addItem(new ShopItem(new ItemStack(Material.SHEARS), new ItemStack(Material.IRON_INGOT, 20)));
        categories.add(tools);

        // Food category
        ShopCategory food = new ShopCategory("Food", Material.COOKED_BEEF);
        food.addItem(new ShopItem(new ItemStack(Material.APPLE, 1), new ItemStack(Material.IRON_INGOT, 4)));
        food.addItem(new ShopItem(new ItemStack(Material.COOKED_BEEF, 3), new ItemStack(Material.IRON_INGOT, 4)));
        food.addItem(new ShopItem(new ItemStack(Material.GOLDEN_APPLE, 1), new ItemStack(Material.GOLD_INGOT, 3)));
        categories.add(food);

        // Potions category
        ShopCategory potions = new ShopCategory("Potions", Material.POTION);
        potions.addItem(
                new ShopItem(new ItemStack(Material.POTION), new ItemStack(Material.EMERALD, 2), "Speed Potion"));
        potions.addItem(
                new ShopItem(new ItemStack(Material.POTION), new ItemStack(Material.EMERALD, 2), "Jump Potion"));
        potions.addItem(new ShopItem(new ItemStack(Material.POTION), new ItemStack(Material.EMERALD, 1),
                "Invisibility Potion"));
        categories.add(potions);

        // Special category
        ShopCategory special = new ShopCategory("Special", Material.TNT);
        special.addItem(new ShopItem(new ItemStack(Material.TNT, 1), new ItemStack(Material.GOLD_INGOT, 4)));
        special.addItem(new ShopItem(new ItemStack(Material.ENDER_PEARL, 1), new ItemStack(Material.EMERALD, 4)));
        special.addItem(new ShopItem(new ItemStack(Material.FIRE_CHARGE, 1), new ItemStack(Material.IRON_INGOT, 40)));
        special.addItem(new ShopItem(new ItemStack(Material.LADDER, 8), new ItemStack(Material.IRON_INGOT, 4)));
        categories.add(special);
    }

    @SuppressWarnings("deprecation")
    private void applyBasePotionType(PotionMeta potionMeta, PotionType potionType) {
        try {
            potionMeta.setBasePotionType(potionType);
        } catch (NoSuchMethodError e) {
            // 1.18 uses PotionData instead of setBasePotionType.
            potionMeta.setBasePotionData(new PotionData(potionType));
        }
    }

    public List<ShopCategory> getCategories() {
        return categories;
    }

    public ShopCategory getCategory(String name) {
        for (ShopCategory category : categories) {
            if (category.getName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }
}
