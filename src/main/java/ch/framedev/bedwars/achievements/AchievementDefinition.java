package ch.framedev.bedwars.achievements;

import java.util.List;
import java.util.Objects;
import org.bukkit.Material;

/**
 * Defines a single achievement.
 */
public record AchievementDefinition(String id, String displayName, Material icon, List<String> lore,
        AchievementType type, int target, boolean enabled) {

    public AchievementDefinition {
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null ? Material.PAPER : icon;
        lore = lore == null ? List.of() : lore.stream().filter(Objects::nonNull).toList();
        type = type == null ? AchievementType.KILLS : type;
        target = Math.max(1, target);
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
