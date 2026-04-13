package ch.framedev.bedwars.achievements;

import java.util.List;
import org.bukkit.Material;

/**
 * Defines a single achievement.
 */
public record AchievementDefinition(String id, String displayName, Material icon, List<String> lore,
        AchievementType type, int target, boolean enabled) {

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

    public AchievementType getType() {
        return type();
    }

    public int getTarget() {
        return target();
    }

    public boolean isEnabled() {
        return enabled();
    }
}
