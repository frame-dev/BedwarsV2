package ch.framedev.bedwars.cosmetics;

import java.util.List;
import org.bukkit.Material;

/**
 * Defines a cosmetic option.
 */
public record CosmeticDefinition(String id, String displayName, Material icon, List<String> lore,
        CosmeticType type, String effectKey, String permission, boolean enabled,
        CustomCosmeticEffect customEffect) {

    public CosmeticDefinition(String id, String displayName, Material icon, List<String> lore,
            CosmeticType type, String effectKey, String permission, boolean enabled) {
        this(id, displayName, icon, lore, type, effectKey, permission, enabled, null);
    }

    public String getId() {
        return id();
    }

    public String getDisplayName() {
        return displayName();
    }

    public Material getIcon() {
        return icon();
    }

    public List<String> getLore() {
        return lore();
    }

    public CosmeticType getType() {
        return type();
    }

    public String getEffectKey() {
        return effectKey();
    }

    public String getPermission() {
        return permission();
    }

    public boolean isEnabled() {
        return enabled();
    }
}
