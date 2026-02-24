package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Applies team upgrades to items when they are obtained.
 *
 * Fixes / improvements:
 * - Uses HIGH priority + ignoreCancelled to avoid applying to cancelled pickups/clicks
 * - Applies to the ACTUAL item that ends up in the player's inventory:
 *     - pickup: apply to item entity stack
 *     - inventory: applies to cursor/current as appropriate, and on shift-click schedules a 1-tick sync re-apply
 * - Avoids upgrading non-equipment / non-upgradable items (optional; keep if your UpgradeManager is already safe)
 * - Prevents NPEs and unnecessary work outside RUNNING state
 */
public class ItemPickupListener implements Listener {

    private final BedWarsPlugin plugin;
    private final UpgradeManager upgradeManager;

    public ItemPickupListener(BedWarsPlugin plugin, UpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.upgradeManager = upgradeManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null || game.getState() != GameState.RUNNING) return;

        GamePlayer gp = game.getGamePlayer(player);
        if (gp == null || gp.getTeam() == null) return;

        ItemStack stack = event.getItem().getItemStack();
        if (stack == null) return;

        // Apply upgrades directly to the picked-up stack before it is added to inventory
        upgradeManager.applyUpgradesToItem(stack, gp.getTeam().getUpgrades());

        plugin.getDebugLogger().debug("Item pickup upgraded: " + player.getName() + " "
                + stack.getType() + " x" + stack.getAmount());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null || game.getState() != GameState.RUNNING) return;

        GamePlayer gp = game.getGamePlayer(player);
        if (gp == null || gp.getTeam() == null) return;

        // Only care about inventories where items are commonly obtained/manipulated
        InventoryType type = event.getView().getTopInventory() != null
                ? event.getView().getTopInventory().getType()
                : null;

        // If you want: restrict further, e.g. CHEST, ENDER_CHEST, CRAFTING, ANVIL, etc.
        // if (type != InventoryType.CHEST && type != InventoryType.CRAFTING && type != InventoryType.WORKBENCH) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Apply to the item being moved / taken
        if (current != null) {
            upgradeManager.applyUpgradesToItem(current, gp.getTeam().getUpgrades());
            plugin.getDebugLogger().debug("Inventory click upgraded current: " + player.getName() + " " + current.getType());
        }

        // Apply to the cursor item too (placing/swapping items)
        if (cursor != null) {
            upgradeManager.applyUpgradesToItem(cursor, gp.getTeam().getUpgrades());
        }

        // SHIFT-click & some container moves can result in Bukkit cloning/moving stacks AFTER the event.
        // Re-apply one tick later to the actual inventory contents.
        if (event.isShiftClick()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Re-apply to main hand + armor as the common important targets
                ItemStack main = player.getInventory().getItemInMainHand();
                if (main != null) upgradeManager.applyUpgradesToItem(main, gp.getTeam().getUpgrades());

                ItemStack off = player.getInventory().getItemInOffHand();
                if (off != null) upgradeManager.applyUpgradesToItem(off, gp.getTeam().getUpgrades());

                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet != null) upgradeManager.applyUpgradesToItem(helmet, gp.getTeam().getUpgrades());

                ItemStack chest = player.getInventory().getChestplate();
                if (chest != null) upgradeManager.applyUpgradesToItem(chest, gp.getTeam().getUpgrades());

                ItemStack legs = player.getInventory().getLeggings();
                if (legs != null) upgradeManager.applyUpgradesToItem(legs, gp.getTeam().getUpgrades());

                ItemStack boots = player.getInventory().getBoots();
                if (boots != null) upgradeManager.applyUpgradesToItem(boots, gp.getTeam().getUpgrades());
            });
        }
    }
}