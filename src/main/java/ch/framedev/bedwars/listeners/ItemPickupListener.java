package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

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
                plugin.getDebugLogger().debug("Item pickup: " + player.getName() + " " + item.getType()
                        + " x" + item.getAmount());
                upgradeManager.applyUpgradesToItem(item, gamePlayer.getTeam().getUpgrades());
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
                    plugin.getDebugLogger().debug("Apply upgrades on inventory click: " + player.getName()
                            + " " + event.getCurrentItem().getType());
                    upgradeManager.applyUpgradesToItem(event.getCurrentItem(), gamePlayer.getTeam().getUpgrades());
                }
            }
        }
    }
}
