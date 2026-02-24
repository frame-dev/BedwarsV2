package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.shop.ShopGUI;
import ch.framedev.bedwars.team.TeamSelectionGUI;
import ch.framedev.bedwars.upgrades.UpgradeShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles player interaction:
 * - Lobby: Team selector (nether star)
 * - In-game: Shop villagers (item shop / upgrades)
 * <p>
 * Fixes / improvements:
 * - Uses priorities + ignoreCancelled to avoid double triggers
 * - Cancels selector interaction to prevent block interaction (e.g., clicking chests with star)
 * - Supports OFF_HAND double-trigger prevention
 * - Safer villager checks + cancels default trading
 * - Uses stripped title/name comparisons
 */
public class PlayerInteractListener implements Listener {

    private static final String TEAM_SELECTOR_NAME = "Team Selector";

    private final BedWarsPlugin plugin;
    private final ShopGUI shopGUI;
    private final UpgradeShopGUI upgradeShopGUI;

    public PlayerInteractListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin);
        this.upgradeShopGUI = new UpgradeShopGUI(plugin.getUpgradeManager());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) return;

        // Only in lobby states
        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // Prevent double trigger on 1.9+ (main-hand + off-hand)
        // If your server is 1.8, EquipmentSlot might not exist; keep it reflective-free by just canceling once.
        try {
            if (event.getHand() != null && event.getHand().name().equalsIgnoreCase("OFF_HAND")) return;
        } catch (Throwable ignored) {
            // 1.8 compatibility
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.NETHER_STAR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        } else {
            meta.getDisplayName();
        }

        String display = ChatColor.stripColor(meta.getDisplayName());
        if (!display.equalsIgnoreCase(TEAM_SELECTOR_NAME)) return;

        if (plugin.getTeamSelectionGUI() != null) {
            plugin.getTeamSelectionGUI().openTeamSelection(player, game);
        }

        // Cancel so the star doesn't interact with blocks / trigger other plugins
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null || game.getState() != GameState.RUNNING) return;

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Villager villager)) return;

        // Optional: if your spectators should not use shop
        if (game.isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        plugin.getDebugLogger().debug("Shop interact: " + player.getName()
                + ", profession=" + villager.getProfession());

        // Determine shop type by villager profession (as you do)
        if (villager.getProfession() == Villager.Profession.FARMER) {
            shopGUI.openMainShop(player, game);
            event.setCancelled(true);
            return;
        }

        if (villager.getProfession() == Villager.Profession.LIBRARIAN) {
            var gp = game.getGamePlayer(player);
            if (gp != null && gp.getTeam() != null) {
                upgradeShopGUI.openUpgradeShop(player, gp.getTeam());
            }
            event.setCancelled(true);
        }
    }
}