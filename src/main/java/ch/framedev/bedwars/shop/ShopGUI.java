package ch.framedev.bedwars.shop;

import ch.framedev.bedwars.game.Game;
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
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + category.getName());
            icon.setItemMeta(meta);
            inventory.setItem(i + 19, icon);
        }

        player.openInventory(inventory);
    }

    public void openCategory(Player player, ShopCategory category) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BOLD + category.getName());

        List<ShopItem> items = category.getItems();
        for (int i = 0; i < items.size() && i < 45; i++) {
            ShopItem shopItem = items.get(i);
            ItemStack displayItem = shopItem.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + shopItem.getCost().getAmount() + " " +
                    formatMaterialName(shopItem.getCost().getType()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to purchase!");
            meta.setLore(lore);

            if (shopItem.getDisplayName() != null) {
                meta.setDisplayName(ChatColor.GREEN + shopItem.getDisplayName());
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

    public boolean purchaseItem(Player player, ShopItem shopItem) {
        ItemStack cost = shopItem.getCost();

        if (player.getInventory().containsAtLeast(cost, cost.getAmount())) {
            player.getInventory().removeItem(cost);
            player.getInventory().addItem(shopItem.getItem());
            return true;
        } else {
            return false;
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}
