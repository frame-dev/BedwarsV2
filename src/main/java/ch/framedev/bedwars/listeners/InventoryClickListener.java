package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.shop.ShopCategory;
import ch.framedev.bedwars.shop.ShopGUI;
import ch.framedev.bedwars.shop.ShopItem;
import ch.framedev.bedwars.team.Team;
import ch.framedev.bedwars.team.TeamSelectionGUI;
import ch.framedev.bedwars.upgrades.UpgradeShopGUI;
import ch.framedev.bedwars.utils.MessageManager;
import ch.framedev.bedwars.manager.UpgradeManager.EffectType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Handles inventory click events (shop, upgrades, cosmetics, achievements, team selector, map vote).
 *
 * Fixes / improvements:
 * - ignoreCancelled + highest priority: avoids double-handling and item movement
 * - centralized null/air checks
 * - robust title matching (stripColor)
 * - safe slot mapping for upgrades (iterating keySet is non-deterministic)
 * - prevent clicking player inventory when GUI is open from causing purchases
 * - fewer repeated getCurrentItem calls
 */
public class InventoryClickListener implements Listener {

    private final BedWarsPlugin plugin;
    private final ShopGUI shopGUI;
    private final UpgradeShopGUI upgradeShopGUI;

    public InventoryClickListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin);
        this.upgradeShopGUI = new UpgradeShopGUI(plugin.getUpgradeManager());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // We only care about clicks inside the top inventory (the GUI), not the player's own inventory.
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().equals(player.getInventory())) {
            // If a GUI is open we typically still cancel movement to avoid shift-click exploits.
            // But only cancel when the open inventory is one of ours.
            String title = event.getView().getTitle();
            if (isAnyPluginGui(title)) {
                event.setCancelled(true);
            }
            return;
        }

        final String title = event.getView().getTitle();
        final ItemStack clicked = event.getCurrentItem();

        // For our GUIs: always cancel to prevent item taking/moving.
        if (isAnyPluginGui(title)) {
            event.setCancelled(true);
        }

        if (isNullOrAir(clicked)) return;

        // Cosmetics GUI
        if (plugin.getCosmeticsManager() != null && plugin.getCosmeticsManager().isCosmeticsTitle(title)) {
            plugin.getCosmeticsManager().handleMenuClick(player, event.getSlot());
            return;
        }

        // Achievements GUI
        if (plugin.getAchievementsManager() != null && plugin.getAchievementsManager().isAchievementsTitle(title)) {
            plugin.getAchievementsManager().handleMenuClick(player, event.getSlot());
            return;
        }

        // Team Selection GUI
        if (plugin.getTeamSelectionGUI() != null && TeamSelectionGUI.isTeamSelectionTitle(title)) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game != null) {
                plugin.getTeamSelectionGUI().handleMenuClick(player, event.getSlot(), game);
            }
            return;
        }

        // Map voting GUI
        String voteTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("map-voting.gui-title", "Map Voting"));
        if (equalsTitle(title, voteTitle)) {
            if (plugin.getMapVoteManager() != null) {
                plugin.getMapVoteManager().handleVoteClick(player, clicked, event.getSlot());
            }
            return;
        }

        // Shop / upgrades GUIs
        if (isShopGui(title)) {
            // Already cancelled above via isAnyPluginGui()
            plugin.getDebugLogger().debug("Shop click: " + player.getName() + ", title=" + title
                    + ", slot=" + event.getSlot());

            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game == null) return;

            MessageManager mm = plugin.getMessageManager();

            if (equalsTitle(title, ChatColor.BOLD + "Item Shop")) {
                handleMainShopClick(player, clicked, game);
                return;
            }

            if (isShopCategory(title)) {
                handleCategoryShopClick(player, clicked, event.getSlot(), title, game, mm);
                return;
            }

            if (equalsTitle(title, ChatColor.BOLD + "Team Upgrades")) {
                handleUpgradeShopClick(player, event.getSlot(), game, mm);
            }
        }
    }

    private boolean isAnyPluginGui(String title) {
        if (title == null) return false;

        if (plugin.getCosmeticsManager() != null && plugin.getCosmeticsManager().isCosmeticsTitle(title)) return true;
        if (plugin.getAchievementsManager() != null && plugin.getAchievementsManager().isAchievementsTitle(title)) return true;
        if (plugin.getTeamSelectionGUI() != null && TeamSelectionGUI.isTeamSelectionTitle(title)) return true;

        String voteTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("map-voting.gui-title", "Map Voting"));
        if (equalsTitle(title, voteTitle)) return true;

        return isShopGui(title);
    }

    private boolean isShopGui(String title) {
        if (title == null) return false;
        // Strip color to avoid issues across versions / formatting
        String clean = ChatColor.stripColor(title);
        if (clean == null) return false;

        return clean.equalsIgnoreCase(ChatColor.stripColor(ChatColor.BOLD + "Item Shop"))
                || clean.equalsIgnoreCase(ChatColor.stripColor(ChatColor.BOLD + "Team Upgrades"))
                || isShopCategory(title);
    }

    private boolean isShopCategory(String title) {
        String cleanTitle = ChatColor.stripColor(title);
        if (cleanTitle == null) return false;

        for (ShopCategory category : shopGUI.getShopManager().getCategories()) {
            if (cleanTitle.equalsIgnoreCase(category.getName())) {
                return true;
            }
        }
        return false;
    }

    private void handleMainShopClick(Player player, ItemStack clickedItem, Game game) {
        for (ShopCategory category : shopGUI.getShopManager().getCategories()) {
            if (clickedItem.getType() == category.getIcon()) {
                plugin.getDebugLogger().debug("Open shop category: " + player.getName()
                        + ", category=" + category.getName());
                shopGUI.openCategory(player, category, game);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                return;
            }
        }
    }

    private void handleCategoryShopClick(Player player, ItemStack clickedItem, int slot, String title, Game game,
                                         MessageManager mm) {
        // Back button
        if (slot == 49 && clickedItem.getType() == Material.ARROW) {
            plugin.getDebugLogger().debug("Shop back: " + player.getName() + ", category=" + title);
            shopGUI.openMainShop(player, game);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Find the shop item by slot
        String categoryName = ChatColor.stripColor(title);
        ShopCategory category = shopGUI.getShopManager().getCategory(categoryName);
        if (category == null) return;

        // IMPORTANT: Many GUIs place items with padding/fillers. If your ShopGUI places items directly by slot index
        // in the inventory, this is fine. If it uses specific slots, you should map slot -> ShopItem in ShopGUI.
        if (slot < 0 || slot >= category.getItems().size()) return;

        ShopItem shopItem = category.getItems().get(slot);
        if (shopItem == null) return;

        ItemStack purchased = shopGUI.purchaseItem(player, shopItem, game);
        if (purchased != null) {
            plugin.getDebugLogger().debug("Shop purchase: " + player.getName() + ", item="
                    + shopItem.getItem().getType());
            mm.sendMessage(player, "shop.purchase-successful");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            GamePlayer buyer = game.getGamePlayer(player);
            if (buyer != null && buyer.getTeam() != null) {
                plugin.getUpgradeManager().applyUpgradesToItem(purchased, buyer.getTeam().getUpgrades());
            }

            // Refresh category view
            shopGUI.openCategory(player, category, game);
        } else {
            plugin.getDebugLogger().debug("Shop purchase failed: " + player.getName() + ", item="
                    + shopItem.getItem().getType());

            ItemStack cost = shopItem.getCost();
            String resourceName = formatMaterialName(cost.getType());
            mm.sendMessage(player, "shop.not-enough-resources", resourceName);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleUpgradeShopClick(Player player, int slot, Game game, MessageManager mm) {
        GamePlayer gamePlayer = game.getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.getTeam() == null) return;

        Team team = gamePlayer.getTeam();

        // FIX: Iterating upgrades.keySet() is non-deterministic (HashMap order changes).
        // Sort keys so slot->upgrade mapping is stable across restarts.
        List<String> upgradeIds = new ArrayList<>(upgradeShopGUI.getUpgradeManager().getUpgrades().keySet());
        Collections.sort(upgradeIds);

        // Your GUI seems to start upgrades at slot 10 and go sequentially.
        // If UpgradeShopGUI uses a different layout, move slot mapping into UpgradeShopGUI.
        int baseSlot = 10;
        int index = slot - baseSlot;
        if (index < 0 || index >= upgradeIds.size()) return;

        String upgradeId = upgradeIds.get(index);

        if (upgradeShopGUI.purchaseUpgrade(player, team, upgradeId)) {
            var upgrade = upgradeShopGUI.getUpgradeManager().getUpgrade(upgradeId);

            plugin.getDebugLogger().debug("Upgrade purchase: " + player.getName() + ", upgrade=" + upgradeId);

            mm.sendMessage(player, "shop.upgrade-purchased");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Notify all team members
            for (GamePlayer teamPlayer : team.getPlayers()) {
                Player p = plugin.getServer().getPlayer(teamPlayer.getUuid());
                if (p != null && !p.equals(player)) {
                    mm.sendMessage(p, "shop.team-upgrade-purchased", player.getName(), upgradeId);
                }
            }

            if (upgrade != null && upgrade.getEffectType() == EffectType.POTION_EFFECT) {
                for (GamePlayer teamPlayer : team.getPlayers()) {
                    Player p = plugin.getServer().getPlayer(teamPlayer.getUuid());
                    if (p != null) {
                        plugin.getUpgradeManager().applyPotionUpgrades(p, team.getUpgrades());
                    }
                }
            }

            if (upgrade != null && upgrade.getEffectType() == EffectType.ENCHANTMENT) {
                for (GamePlayer teamPlayer : team.getPlayers()) {
                    Player p = plugin.getServer().getPlayer(teamPlayer.getUuid());
                    if (p == null) continue;

                    var inv = p.getInventory();
                    if (inv.getItemInMainHand() != null) plugin.getUpgradeManager().applyUpgradesToItem(inv.getItemInMainHand(), team.getUpgrades());
                    if (inv.getItemInOffHand() != null) plugin.getUpgradeManager().applyUpgradesToItem(inv.getItemInOffHand(), team.getUpgrades());

                    if (inv.getHelmet() != null) plugin.getUpgradeManager().applyUpgradesToItem(inv.getHelmet(), team.getUpgrades());
                    if (inv.getChestplate() != null) plugin.getUpgradeManager().applyUpgradesToItem(inv.getChestplate(), team.getUpgrades());
                    if (inv.getLeggings() != null) plugin.getUpgradeManager().applyUpgradesToItem(inv.getLeggings(), team.getUpgrades());
                    if (inv.getBoots() != null) plugin.getUpgradeManager().applyUpgradesToItem(inv.getBoots(), team.getUpgrades());
                }
            }

            if (upgrade != null && upgrade.getEffectType() == EffectType.SPECIAL) {
                game.applySpecialUpgrade(team, upgradeId);
            }

            upgradeShopGUI.openUpgradeShop(player, team);
        } else {
            plugin.getDebugLogger().debug("Upgrade purchase failed: " + player.getName() + ", upgrade=" + upgradeId);
            mm.sendMessage(player, "shop.upgrade-max-level");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private boolean isNullOrAir(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private boolean equalsTitle(String a, String b) {
        String sa = ChatColor.stripColor(a);
        String sb = ChatColor.stripColor(b);
        if (sa == null || sb == null) return false;
        return sa.equalsIgnoreCase(sb);
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase(Locale.ROOT).replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}