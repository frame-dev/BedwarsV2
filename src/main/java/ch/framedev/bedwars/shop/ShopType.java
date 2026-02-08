package ch.framedev.bedwars.shop;

import org.bukkit.entity.Villager;

public enum ShopType {
    ITEM("item", Villager.Profession.FARMER, "Shop"),
    UPGRADE("upgrade", Villager.Profession.LIBRARIAN, "Upgrades");

    private final String configKey;
    private final Villager.Profession profession;
    private final String displayName;

    ShopType(String configKey, Villager.Profession profession, String displayName) {
        this.configKey = configKey;
        this.profession = profession;
        this.displayName = displayName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public Villager.Profession getProfession() {
        return profession;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ShopType fromString(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        if (normalized.equals("item") || normalized.equals("items")) {
            return ITEM;
        }
        if (normalized.equals("upgrade") || normalized.equals("upgrades")) {
            return UPGRADE;
        }

        return null;
    }
}
