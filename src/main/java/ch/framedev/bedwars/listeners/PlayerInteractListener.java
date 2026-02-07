package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.shop.ShopGUI;
import ch.framedev.bedwars.upgrades.UpgradeShopGUI;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Handles player interact events
 */
public class PlayerInteractListener implements Listener {

    private final BedWarsPlugin plugin;
    private final ShopGUI shopGUI;
    private final UpgradeShopGUI upgradeShopGUI;

    public PlayerInteractListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin);
        this.upgradeShopGUI = new UpgradeShopGUI(plugin.getUpgradeManager());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game != null && game.getState() == GameState.RUNNING) {
            if (event.getRightClicked().getType() == EntityType.VILLAGER) {
                Villager villager = (Villager) event.getRightClicked();

                plugin.getDebugLogger().debug("Shop interact: " + player.getName()
                        + ", profession=" + villager.getProfession());

                // Check villager profession to determine shop type
                if (villager.getProfession() == Villager.Profession.FARMER) {
                    shopGUI.openMainShop(player, game);
                } else if (villager.getProfession() == Villager.Profession.LIBRARIAN) {
                    var gamePlayer = game.getGamePlayer(player);
                    if (gamePlayer != null && gamePlayer.getTeam() != null) {
                        upgradeShopGUI.openUpgradeShop(player, gamePlayer.getTeam());
                    }
                }

                event.setCancelled(true);
            }
        }
    }
}
