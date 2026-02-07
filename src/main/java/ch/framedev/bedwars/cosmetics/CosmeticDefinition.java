package ch.framedev.bedwars.cosmetics;

import java.util.List;
import org.bukkit.Material;

/**
 * Defines a cosmetic option.
 */
public class CosmeticDefinition {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> lore;
    private final CosmeticType type;
    private final String effectKey;
    private final String permission;
    private final boolean enabled;

    public CosmeticDefinition(String id, String displayName, Material icon, List<String> lore,
            CosmeticType type, String effectKey, String permission, boolean enabled) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.lore = lore;
        this.type = type;
        this.effectKey = effectKey;
        this.permission = permission;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getLore() {
        return lore;
    }

    public CosmeticType getType() {
        return type;
    }

    public String getEffectKey() {
        return effectKey;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
