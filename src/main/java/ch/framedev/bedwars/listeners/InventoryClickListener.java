package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.shop.ShopCategory;
import ch.framedev.bedwars.shop.ShopGUI;
import ch.framedev.bedwars.shop.ShopItem;
import ch.framedev.bedwars.team.Team;
import ch.framedev.bedwars.upgrades.UpgradeShopGUI;
import ch.framedev.bedwars.utils.MessageManager;
import ch.framedev.bedwars.manager.UpgradeManager.EffectType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles inventory click events for shop
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if it's a shop inventory
        if (title.contains("Item Shop") || title.contains("Team Upgrades") || isShopCategory(title)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
                return;

            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game == null)
                return;

            MessageManager mm = plugin.getMessageManager();

            // Handle main shop navigation
            if (title.equals(ChatColor.BOLD + "Item Shop")) {
                handleMainShopClick(player, event.getCurrentItem(), game);
            }
            // Handle category shop purchases
            else if (isShopCategory(title)) {
                handleCategoryShopClick(player, event.getCurrentItem(), event.getSlot(), title, game, mm);
            }
            // Handle upgrade shop
            else if (title.equals(ChatColor.BOLD + "Team Upgrades")) {
                handleUpgradeShopClick(player, event.getSlot(), game, mm);
            }
        }
    }

    private boolean isShopCategory(String title) {
        String cleanTitle = ChatColor.stripColor(title);
        for (ShopCategory category : shopGUI.getShopManager().getCategories()) {
            if (cleanTitle.equals(category.getName())) {
                return true;
            }
        }
        return false;
    }

    private void handleMainShopClick(Player player, ItemStack clickedItem, Game game) {
        // Open category based on clicked icon
        for (ShopCategory category : shopGUI.getShopManager().getCategories()) {
            if (clickedItem.getType() == category.getIcon()) {
                shopGUI.openCategory(player, category);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                return;
            }
        }
    }

    private void handleCategoryShopClick(Player player, ItemStack clickedItem, int slot, String title, Game game,
            MessageManager mm) {
        // Back button
        if (slot == 49 && clickedItem.getType() == Material.ARROW) {
            shopGUI.openMainShop(player, game);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Find the shop item
        String categoryName = ChatColor.stripColor(title);
        ShopCategory category = shopGUI.getShopManager().getCategory(categoryName);

        if (category == null || slot >= category.getItems().size())
            return;

        ShopItem shopItem = category.getItems().get(slot);
        if (shopItem == null)
            return;

        // Attempt purchase
        ItemStack purchased = shopGUI.purchaseItem(player, shopItem);
        if (purchased != null) {
            mm.sendMessage(player, "shop.purchase-successful");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            GamePlayer buyer = game.getGamePlayer(player);
            if (buyer != null && buyer.getTeam() != null) {
                plugin.getUpgradeManager().applyUpgradesToItem(purchased, buyer.getTeam().getUpgrades());
            }

            // Refresh the inventory to show updated purchase options
            shopGUI.openCategory(player, category);
        } else {
            ItemStack cost = shopItem.getCost();
            String resourceName = formatMaterialName(cost.getType());
            mm.sendMessage(player, "shop.not-enough-resources", resourceName);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleUpgradeShopClick(Player player, int slot, Game game, MessageManager mm) {
        GamePlayer gamePlayer = game.getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.getTeam() == null)
            return;

        Team team = gamePlayer.getTeam();

        // Get all available upgrades and find which one matches this slot
        var upgrades = upgradeShopGUI.getUpgradeManager().getUpgrades();
        int currentSlot = 10;
        String upgradeId = null;

        for (String id : upgrades.keySet()) {
            if (currentSlot == slot) {
                upgradeId = id;
                break;
            }
            currentSlot++;
        }

        if (upgradeId == null)
            return;

        // Attempt upgrade purchase
        if (upgradeShopGUI.purchaseUpgrade(player, team, upgradeId)) {
            var upgrade = upgradeShopGUI.getUpgradeManager().getUpgrade(upgradeId);

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
                    if (p == null) {
                        continue;
                    }

                    var inventory = p.getInventory();
                    if (inventory.getItemInMainHand() != null) {
                        plugin.getUpgradeManager().applyUpgradesToItem(inventory.getItemInMainHand(),
                                team.getUpgrades());
                    }
                    if (inventory.getItemInOffHand() != null) {
                        plugin.getUpgradeManager().applyUpgradesToItem(inventory.getItemInOffHand(),
                                team.getUpgrades());
                    }
                    if (inventory.getHelmet() != null) {
                        plugin.getUpgradeManager().applyUpgradesToItem(inventory.getHelmet(), team.getUpgrades());
                    }
                    if (inventory.getChestplate() != null) {
                        plugin.getUpgradeManager().applyUpgradesToItem(inventory.getChestplate(), team.getUpgrades());
                    }
                    if (inventory.getLeggings() != null) {
                        plugin.getUpgradeManager().applyUpgradesToItem(inventory.getLeggings(), team.getUpgrades());
                    }
                    if (inventory.getBoots() != null) {
                        plugin.getUpgradeManager().applyUpgradesToItem(inventory.getBoots(), team.getUpgrades());
                    }
                }
            }

            if (upgrade != null && upgrade.getEffectType() == EffectType.SPECIAL) {
                game.applySpecialUpgrade(team, upgradeId);
            }

            // Refresh the inventory
            upgradeShopGUI.openUpgradeShop(player, team);
        } else {
            mm.sendMessage(player, "shop.upgrade-max-level");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
