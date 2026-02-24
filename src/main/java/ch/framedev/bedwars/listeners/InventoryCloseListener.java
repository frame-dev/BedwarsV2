package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.team.TeamSelectionGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.UUID;

/**
 * Handles GUI cleanup when inventories close.
 */
public class InventoryCloseListener implements Listener {

    private final BedWarsPlugin plugin;

    public InventoryCloseListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();

        UUID uuid = player.getUniqueId();

        // --- Team selector cleanup ---
        if (plugin.getTeamSelectionGUI() != null
                && TeamSelectionGUI.isTeamSelectionTitle(title)) {

            plugin.getTeamSelectionGUI().clearMenuSlots(uuid);
            return;
        }

        // --- Optional future GUI cleanups ---
        // If later you track per-player shop states, add them here.
        // Example:
        // if (plugin.getShopGUI().isShopTitle(title)) { ... }

        // Cosmetics / achievements GUIs usually don't need cleanup,
        // but this is where you'd add it if they ever store state.
    }
}