package ch.framedev.bedwars.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category in the shop
 */
public class ShopCategory {

    private final String name;
    private final Material icon;
    private final List<ShopItem> items;

    public ShopCategory(String name, Material icon) {
        this.name = name;
        this.icon = icon;
        this.items = new ArrayList<>();
    }

    public void addItem(ShopItem item) {
        items.add(item);
    }

    public String getName() {
        return name;
    }

    public Material getIcon() {
        return icon;
    }

    public List<ShopItem> getItems() {
        return items;
    }
}
