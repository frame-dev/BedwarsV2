package ch.framedev.bedwars.achievements;

import java.util.List;
import org.bukkit.Material;

/**
 * Defines a single achievement.
 */
public class AchievementDefinition {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> lore;
    private final AchievementType type;
    private final int target;
    private final boolean enabled;

    public AchievementDefinition(String id, String displayName, Material icon, List<String> lore,
            AchievementType type, int target, boolean enabled) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.lore = lore;
        this.type = type;
        this.target = target;
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

    public AchievementType getType() {
        return type;
    }

    public int getTarget() {
        return target;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
