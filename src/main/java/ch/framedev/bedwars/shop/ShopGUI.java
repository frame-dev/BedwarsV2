package ch.framedev.bedwars.shop;

import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.team.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the shop GUI
 */
public class ShopGUI {

    private final ShopManager shopManager;

    public ShopGUI(Plugin plugin) {
        this.shopManager = new ShopManager(plugin);
    }

    public void openMainShop(Player player, Game game) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BOLD + "Item Shop");

        List<ShopCategory> categories = shopManager.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            ShopCategory category = categories.get(i);
            ItemStack icon = new ItemStack(category.getIcon());
            ItemMeta meta = icon.getItemMeta();
            String categoryName = ChatColor.translateAlternateColorCodes('&', category.getName());
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + categoryName);
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
            meta.setLore(lore);

            if (shopItem.getDisplayName() != null) {
                String displayName = ChatColor.translateAlternateColorCodes('&', shopItem.getDisplayName());
                meta.setDisplayName(ChatColor.GREEN + displayName);
            }

            displayItem.setItemMeta(meta);
            inventory.setItem(i, displayItem);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
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

        ItemStack cost = shopItem.getCost();

        if (player.getInventory().containsAtLeast(cost, cost.getAmount())) {
            player.getInventory().removeItem(cost);
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
            if (purchased.getType() == Material.CHAINMAIL_CHESTPLATE || purchased.getType() == Material.IRON_CHESTPLATE ||
                    purchased.getType() == Material.DIAMOND_CHESTPLATE) {
                player.getInventory().setChestplate(purchased);
                return purchased;
            }

            player.getInventory().addItem(purchased);
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

    public ShopManager getShopManager() {
        return shopManager;
    }
}
