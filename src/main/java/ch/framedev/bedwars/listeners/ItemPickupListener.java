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
import org.bukkit.inventory.ItemStack;

/**
 * Applies team upgrades to items when they are obtained.
 * <p>
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

        // Apply upgrades directly to the picked-up stack before it is added to inventory
        upgradeManager.applyUpgradesToItem(stack, gp.getTeam().getUpgrades());
        event.getItem().setItemStack(stack);

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
                if (!player.isOnline() || plugin.getGameManager().getPlayerGame(player) != game
                        || game.getState() != GameState.RUNNING || game.getGamePlayer(player) != gp
                        || game.isSpectator(player)) return;

                // Shift-clicks can move equipment into any storage slot, not just the hand.
                var inventory = player.getInventory();
                ItemStack[] contents = inventory.getContents();
                for (ItemStack item : contents) {
                    upgradeManager.applyUpgradesToItem(item, gp.getTeam().getUpgrades());
                }
                inventory.setContents(contents);
            });
        }
    }
}
