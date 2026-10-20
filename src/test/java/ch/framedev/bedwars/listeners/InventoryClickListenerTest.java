package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameManager;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.shop.*;
import ch.framedev.bedwars.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class InventoryClickListenerTest {
    @Test
    public void respawningPlayersCannotPurchaseFromAnOpenShop() {
        assertPurchaseBlocked(GameState.RUNNING, true);
    }

    @Test
    public void endingMatchesRejectShopPurchases() {
        assertPurchaseBlocked(GameState.ENDING, false);
    }

    @Test
    public void waitingMatchesRejectShopPurchases() {
        assertPurchaseBlocked(GameState.WAITING, false);
    }

    private void assertPurchaseBlocked(GameState state, boolean spectator) {
        BedWarsPlugin plugin = mock(BedWarsPlugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        ShopManager shop = mock(ShopManager.class);
        when(plugin.getShopManager()).thenReturn(shop);
        when(plugin.getUpgradeManager()).thenReturn(mock(UpgradeManager.class));
        ShopCategory category = mock(ShopCategory.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.IRON_SWORD);
        when(category.getItems()).thenReturn(List.of(new ShopItem(item, item)));
        when(shop.getCategory("Weapons")).thenReturn(category);

        Player player = mock(Player.class);
        GameManager manager = mock(GameManager.class);
        Game game = mock(Game.class);
        when(plugin.getGameManager()).thenReturn(manager);
        when(manager.getPlayerGame(player)).thenReturn(game);
        when(game.getState()).thenReturn(state);
        when(game.isSpectator(player)).thenReturn(spectator);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        InventoryView view = mock(InventoryView.class);
        Inventory inventory = mock(Inventory.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getView()).thenReturn(view);
        when(view.getTitle()).thenReturn("Weapons");
        when(view.getTopInventory()).thenReturn(inventory);
        when(inventory.getSize()).thenReturn(54);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        when(event.getCurrentItem()).thenReturn(item);

        new InventoryClickListener(plugin).onInventoryClick(event);

        verify(event).setCancelled(true);
        verify(player, never()).getInventory();
        verify(player, never()).openInventory(any(Inventory.class));
    }
}
