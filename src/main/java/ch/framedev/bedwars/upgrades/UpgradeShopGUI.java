package ch.framedev.bedwars.upgrades;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles the upgrades shop GUI
 * Now configurable via upgrades.yml
 */
public class UpgradeShopGUI {

    private final UpgradeManager upgradeManager;

    public UpgradeShopGUI(BedWarsPlugin plugin) {
        this(new UpgradeManager(plugin));
    }

    public UpgradeShopGUI(UpgradeManager upgradeManager) {
        this.upgradeManager = upgradeManager;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public void openUpgradeShop(Player player, Team team) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.BOLD + "Team Upgrades");

        TeamUpgrades teamUpgrades = team.getUpgrades();
        Map<String, UpgradeManager.Upgrade> upgrades = upgradeManager.getUpgrades();

        int slot = 10; // Start placing items at slot 10

        for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgrades.entrySet()) {
            String upgradeId = entry.getKey();
            UpgradeManager.Upgrade upgrade = entry.getValue();

            ItemStack item = new ItemStack(upgrade.getIcon());
            ItemMeta meta = item.getItemMeta();

            // Set display name with color codes
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', upgrade.getDisplayName()));

            // Set lore with description and cost
            List<String> lore = new ArrayList<>();
            for (String desc : upgrade.getDescription()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', desc));
            }
            lore.add("");

            // Add cost information
            int currentLevel = getCurrentLevel(teamUpgrades, upgradeId);
            int maxLevel = upgrade.getMaxLevel();

            if (currentLevel < maxLevel) {
                // Show cost for next level
                int nextLevel = currentLevel + 1;
                UpgradeManager.UpgradeCost cost = upgrade.getCost(nextLevel);
                if (cost != null) {
                    if (maxLevel > 1) {
                        lore.add(ChatColor.GRAY + "Tier " + nextLevel + ": " + ChatColor.AQUA +
                                cost.getAmount() + " " + formatMaterial(cost.getMaterial()));
                    } else {
                        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.AQUA +
                                cost.getAmount() + " " + formatMaterial(cost.getMaterial()));
                    }
                }
            }

            // Add status
            if (currentLevel > 0) {
                if (maxLevel > 1) {
                    lore.add(ChatColor.GREEN + "TIER " + currentLevel);
                } else {
                    lore.add(ChatColor.GREEN + "UNLOCKED");
                }
            }
            if (currentLevel >= maxLevel) {
                lore.add(ChatColor.GOLD + "MAX LEVEL");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot, item);
            slot++;

            // Don't overflow the inventory
            if (slot >= 17)
                break;
        }

        player.openInventory(inventory);
    }

    /**
     * Get current level of an upgrade for a team
     */
    private int getCurrentLevel(TeamUpgrades teamUpgrades, String upgradeId) {
        return teamUpgrades.getUpgradeLevel(upgradeId);
    }

    /**
     * Format material name for display
     */
    private String formatMaterial(Material material) {
        String name = material.name().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0)
                result.append(" ");
            result.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    public boolean purchaseUpgrade(Player player, Team team, String upgradeId) {
        UpgradeManager.Upgrade upgrade = upgradeManager.getUpgrade(upgradeId);
        if (upgrade == null)
            return false;

        TeamUpgrades teamUpgrades = team.getUpgrades();
        int currentLevel = getCurrentLevel(teamUpgrades, upgradeId);
        int nextLevel = currentLevel + 1;

        // Check if can upgrade
        if (currentLevel >= upgrade.getMaxLevel()) {
            return false; // Already at max level
        }

        // Get cost for next level
        UpgradeManager.UpgradeCost cost = upgrade.getCost(nextLevel);
        if (cost == null)
            return false;

        ItemStack costItem = new ItemStack(cost.getMaterial(), cost.getAmount());

        // Check if player has enough
        if (player.getInventory().containsAtLeast(costItem, cost.getAmount())) {
            player.getInventory().removeItem(costItem);
            teamUpgrades.upgrade(upgradeId);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Legacy method for backwards compatibility
     */
    public boolean purchaseUpgrade(Player player, Team team, UpgradeType type) {
        return purchaseUpgrade(player, team, type.name().toLowerCase().replace('_', '-'));
    }

    public enum UpgradeType {
        SHARPNESS,
        PROTECTION,
        HASTE,
        HEAL_POOL,
        DRAGON_BUFF
    }
}