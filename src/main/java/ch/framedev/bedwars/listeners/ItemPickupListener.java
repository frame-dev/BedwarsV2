package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Handles item pickup events to apply team upgrades dynamically
 */
public class ItemPickupListener implements Listener {

    private final BedWarsPlugin plugin;
    private final UpgradeManager upgradeManager;

    public ItemPickupListener(BedWarsPlugin plugin, UpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.upgradeManager = upgradeManager;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game != null && game.getState() == GameState.RUNNING) {
            GamePlayer gamePlayer = game.getGamePlayer(player);
            if (gamePlayer != null && gamePlayer.getTeam() != null) {
                ItemStack item = event.getItem().getItemStack();
                applyTeamUpgrades(item, gamePlayer);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game != null && game.getState() == GameState.RUNNING) {
            GamePlayer gamePlayer = game.getGamePlayer(player);
            if (gamePlayer != null && gamePlayer.getTeam() != null) {
                // Apply upgrades when player crafts or gets items from containers
                if (event.getCurrentItem() != null) {
                    applyTeamUpgrades(event.getCurrentItem(), gamePlayer);
                }
            }
        }
    }

    private void applyTeamUpgrades(ItemStack item, GamePlayer gamePlayer) {
        if (item == null || item.getType() == Material.AIR || upgradeManager == null)
            return;

        // Apply all ENCHANTMENT type upgrades dynamically
        for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgradeManager.getUpgrades().entrySet()) {
            UpgradeManager.Upgrade upgrade = entry.getValue();
            int level = gamePlayer.getTeam().getUpgrades().getUpgradeLevel(entry.getKey());

            if (level > 0 && upgrade.getEffectType() == UpgradeManager.EffectType.ENCHANTMENT) {
                Enchantment enchantment = upgrade.getEnchantment();
                if (enchantment == null)
                    continue;

                String target = upgrade.getTarget();
                boolean shouldApply = false;

                if ("WEAPON".equalsIgnoreCase(target) && isMeleeWeapon(item.getType())) {
                    shouldApply = true;
                } else if ("ARMOR".equalsIgnoreCase(target) && isArmor(item.getType())) {
                    shouldApply = true;
                }

                if (shouldApply && !item.getEnchantments().containsKey(enchantment)) {
                    item.addEnchantment(enchantment, level);
                }
            }
        }
    }

    private boolean isMeleeWeapon(Material material) {
        switch (material) {
            case WOODEN_SWORD:
            case STONE_SWORD:
            case IRON_SWORD:
            case GOLDEN_SWORD:
            case DIAMOND_SWORD:
            case WOODEN_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case GOLDEN_AXE:
            case DIAMOND_AXE:
                return true;
            default:
                return false;
        }
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
}
