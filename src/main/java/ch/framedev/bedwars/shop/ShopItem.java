package ch.framedev.bedwars.shop;

import org.bukkit.inventory.ItemStack;

/**
 * Represents an item that can be purchased in the shop
 */
public record ShopItem(ItemStack item, ItemStack cost, String displayName) {

    public ShopItem(ItemStack item, ItemStack cost) {
        this(item, cost, null);
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
