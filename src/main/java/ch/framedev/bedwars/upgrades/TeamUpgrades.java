package ch.framedev.bedwars.upgrades;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages team upgrades dynamically
 * Stores upgrade levels in a Map to support any upgrade from config
 */
public class TeamUpgrades {

    private final Map<String, Integer> upgradeLevels = new HashMap<>();

    public TeamUpgrades() {
        // Initialize with default upgrades for backwards compatibility
        upgradeLevels.put("sharpness", 0);
        upgradeLevels.put("protection", 0);
        upgradeLevels.put("haste", 0);
        upgradeLevels.put("heal-pool", 0);
        upgradeLevels.put("dragon-buff", 0);
    }

    /**
     * Set upgrade level for any upgrade ID
     */
    public void setUpgradeLevel(String upgradeId, int level) {
        upgradeLevels.put(upgradeId, level);
    }

    /**
     * Get upgrade level for any upgrade ID
     */
    public int getUpgradeLevel(String upgradeId) {
        return upgradeLevels.getOrDefault(upgradeId, 0);
    }

    /**
     * Upgrade an upgrade by ID (increment level)
     */
    public void upgrade(String upgradeId) {
        int currentLevel = getUpgradeLevel(upgradeId);
        setUpgradeLevel(upgradeId, currentLevel + 1);
    }

    /**
     * Check if upgrade is unlocked (level > 0)
     */
    public boolean hasUpgrade(String upgradeId) {
        return getUpgradeLevel(upgradeId) > 0;
    }

    /**
     * Get all upgrade levels
     */
    public Map<String, Integer> getAllUpgrades() {
        return new HashMap<>(upgradeLevels);
    }

    public void reset() {
        upgradeLevels.clear();
    }

    // Legacy methods for backwards compatibility
    public void upgradeSharpness() {
        upgrade("sharpness");
    }

    public void upgradeProtection() {
        upgrade("protection");
    }

    public void upgradeHaste() {
        upgrade("haste");
    }

    public void enableHealPool() {
        setUpgradeLevel("heal-pool", 1);
    }

    public void enableDragonBuff() {
        setUpgradeLevel("dragon-buff", 1);
    }

    public int getSharpnessLevel() {
        return getUpgradeLevel("sharpness");
    }

    public int getProtectionLevel() {
        return getUpgradeLevel("protection");
    }

    public int getHasteLevel() {
        return getUpgradeLevel("haste");
    }

    public boolean hasHealPool() {
        return hasUpgrade("heal-pool");
    }

    public boolean hasDragonBuff() {
        return hasUpgrade("dragon-buff");
    }
}
