package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Handles inventory click events (shop, upgrades, cosmetics, achievements, team selector, map vote).
 * <p>
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
        this.shopGUI = new ShopGUI(plugin.getShopManager());
        this.upgradeShopGUI = new UpgradeShopGUI(plugin.getUpgradeManager());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        final String title = event.getView().getTitle();
        if (!isAnyPluginGui(title)) return;

        // Cancel first. Number-key/hotbar swaps can otherwise move real inventory items
        // through decorative GUI slots before our purchase logic finishes.
        event.setCancelled(true);

        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topSize) {
            return;
        }

        if (!isNormalGuiClick(event.getClick())) {
            return;
        }

        final ItemStack clicked = event.getCurrentItem();

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
            if (game == null || game.getState() != GameState.RUNNING || game.isSpectator(player)) return;

            MessageManager mm = plugin.getMessageManager();

            if (equalsTitle(title, ShopGUI.MAIN_TITLE)) {
                handleMainShopClick(player, event.getSlot(), game);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isAnyPluginGui(event.getView().getTitle())) {
            event.setCancelled(true);
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

        return clean.equalsIgnoreCase(ChatColor.stripColor(ShopGUI.MAIN_TITLE))
                || clean.equalsIgnoreCase(ChatColor.stripColor(ChatColor.BOLD + "Team Upgrades"))
                || isShopCategory(title);
    }

    private boolean isShopCategory(String title) {
        String cleanTitle = ChatColor.stripColor(title);
        if (cleanTitle == null) return false;

        return shopGUI.getShopManager().getCategory(cleanTitle) != null;
    }

    private void handleMainShopClick(Player player, int slot, Game game) {
        int index = slot - 19;
        List<ShopCategory> categories = shopGUI.getShopManager().getCategories();
        if (index < 0 || index >= categories.size()) return;

        ShopCategory category = categories.get(index);
        plugin.getDebugLogger().debug("Open shop category: " + player.getName()
                + ", category=" + category.getName());
        shopGUI.openCategory(player, category, game);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private void handleCategoryShopClick(Player player, ItemStack clickedItem, int slot, String title, Game game,
                                         MessageManager mm) {
        // Back button
        if (slot == 49 && clickedItem.getType() == Material.ARROW) {
            plugin.getDebugLogger().debug("Shop back: " + player.getName() + ", category=" + title);
            shopGUI.openMainShop(player);
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

        List<String> upgradeIds = upgradeShopGUI.getOrderedUpgradeIds();

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
                    // Include stored swords/tools, offhand and armor; write back copied stacks.
                    ItemStack[] contents = inv.getContents();
                    for (ItemStack item : contents) {
                        plugin.getUpgradeManager().applyUpgradesToItem(item, team.getUpgrades());
                    }
                    inv.setContents(contents);
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

    private boolean isNormalGuiClick(ClickType clickType) {
        return clickType == ClickType.LEFT || clickType == ClickType.RIGHT;
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
