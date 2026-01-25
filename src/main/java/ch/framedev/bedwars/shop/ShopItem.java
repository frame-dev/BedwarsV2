package ch.framedev.bedwars.shop;

import org.bukkit.inventory.ItemStack;

/**
 * Represents an item that can be purchased in the shop
 */
public class ShopItem {

    private final ItemStack item;
    private final ItemStack cost;
    private final String displayName;

    public ShopItem(ItemStack item, ItemStack cost) {
        this.item = item;
        this.cost = cost;
        this.displayName = null;
    }

    public ShopItem(ItemStack item, ItemStack cost, String displayName) {
        this.item = item;
        this.cost = cost;
        this.displayName = displayName;
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemStack getCost() {
        return cost;
    }

    public String getDisplayName() {
        return displayName;
    }
}
