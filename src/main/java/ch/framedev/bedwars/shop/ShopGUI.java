package ch.framedev.bedwars.shop;

import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.team.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles the shop GUI
 */
public record ShopGUI(ShopManager shopManager) {

    public static final String MAIN_TITLE = ChatColor.BOLD + "Item Shop";

    public ShopGUI {
        shopManager = Objects.requireNonNull(shopManager, "shopManager");
    }

    public void openMainShop(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, MAIN_TITLE);

        List<ShopCategory> categories = shopManager.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            ShopCategory category = categories.get(i);
            ItemStack icon = new ItemStack(category.getIcon());
            ItemMeta meta = icon.getItemMeta();
            String categoryName = ChatColor.translateAlternateColorCodes('&', category.getName());
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + categoryName);
            }
            icon.setItemMeta(meta);
            inventory.setItem(i + 19, icon);
        }

        player.openInventory(inventory);
    }

    public void openCategory(Player player, ShopCategory category) {
        openCategory(player, category, null);
    }

    public void openCategory(Player player, ShopCategory category, Game game) {
        if (player == null) {
            return;
        }

        String categoryName = ChatColor.translateAlternateColorCodes('&', category.getName());
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BOLD + categoryName);

        // Get player's team color for wool conversion
        TeamColor teamColor = null;
        if (game != null) {
            ch.framedev.bedwars.player.GamePlayer gamePlayer = game.getGamePlayer(player);
            if (gamePlayer != null && gamePlayer.getTeam() != null) {
                teamColor = gamePlayer.getTeam().getColor();
            }
        }

        List<ShopItem> items = category.getItems();
        for (int i = 0; i < items.size() && i < 45; i++) {
            ShopItem shopItem = items.get(i);
            ItemStack displayItem = shopItem.getItem().clone();

            // Convert white wool to team-colored wool
            if (teamColor != null && displayItem.getType() == Material.WHITE_WOOL) {
                Material teamWool = getTeamWool(teamColor);
                if (teamWool != null) {
                    displayItem.setType(teamWool);
                }
            }

            ItemMeta meta = displayItem.getItemMeta();

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + shopItem.getCost().getAmount() + " " +
                    formatMaterialName(shopItem.getCost().getType()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to purchase!");
            if (meta != null) {
                meta.setLore(lore);
            }

            if (shopItem.getDisplayName() != null) {
                String displayName = ChatColor.translateAlternateColorCodes('&', shopItem.getDisplayName());
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + displayName);
                }
            }

            displayItem.setItemMeta(meta);
            inventory.setItem(i, displayItem);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Back");
        }
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);

        player.openInventory(inventory);
    }

    public ItemStack purchaseItem(Player player, ShopItem shopItem) {
        return purchaseItem(player, shopItem, null);
    }

    public ItemStack purchaseItem(Player player, ShopItem shopItem, Game game) {
        if (player == null) {
            return null;
        }

        ItemStack cost = shopItem.getCost().clone();

        if (player.getInventory().containsAtLeast(cost, cost.getAmount())) {
            player.getInventory().removeItem(cost.clone());
            ItemStack purchased = shopItem.getItem().clone();

            // Convert white wool to team-colored wool when purchasing
            if (purchased.getType() == Material.WHITE_WOOL && game != null) {
                ch.framedev.bedwars.player.GamePlayer gamePlayer = game.getGamePlayer(player);
                if (gamePlayer != null && gamePlayer.getTeam() != null) {
                    TeamColor teamColor = gamePlayer.getTeam().getColor();
                    Material teamWool = getTeamWool(teamColor);
                    if (teamWool != null) {
                        purchased.setType(teamWool);
                    }
                }
            }
            // Apply before inventory insertion/equipping, which can copy the stack.
            if (game != null) {
                var buyer = game.getGamePlayer(player);
                if (buyer != null && buyer.getTeam() != null) {
                    shopManager.applyTeamUpgrades(purchased, buyer.getTeam().getUpgrades());
                }
            }
            if (isArmor(purchased.getType())) {
                equipArmor(player, purchased);
                return purchased;
            }

            giveOrDrop(player, purchased);
            return purchased;
        } else {
            return null;
        }
    }

    /**
     * Get the wool material for a team color
     */
    private Material getTeamWool(TeamColor teamColor) {
        return switch (teamColor) {
            case RED -> Material.RED_WOOL;
            case BLUE -> Material.BLUE_WOOL;
            case GREEN -> Material.GREEN_WOOL;
            case YELLOW -> Material.YELLOW_WOOL;
            case AQUA -> Material.CYAN_WOOL;
            case PINK -> Material.PINK_WOOL;
            case GRAY -> Material.GRAY_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void equipArmor(Player player, ItemStack armor) {
        PlayerInventory inventory = player.getInventory();
        ItemStack previous = null;

        String typeName = armor.getType().name();
        if (typeName.endsWith("_HELMET")) {
            previous = inventory.getHelmet();
            inventory.setHelmet(armor);
        } else if (typeName.endsWith("_CHESTPLATE")) {
            previous = inventory.getChestplate();
            inventory.setChestplate(armor);
        } else if (typeName.endsWith("_LEGGINGS")) {
            previous = inventory.getLeggings();
            inventory.setLeggings(armor);
        } else if (typeName.endsWith("_BOOTS")) {
            previous = inventory.getBoots();
            inventory.setBoots(armor);
        } else {
            giveOrDrop(player, armor);
            return;
        }

        if (previous != null && previous.getType() != Material.AIR) {
            giveOrDrop(player, previous);
        }
    }

    private boolean isArmor(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}
