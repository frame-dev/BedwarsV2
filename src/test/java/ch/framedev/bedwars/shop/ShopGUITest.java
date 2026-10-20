package ch.framedev.bedwars.shop;

import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.team.Team;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ShopGUITest {
    @Test
    public void appliesTeamUpgradesBeforeGivingPurchasedSword() {
        purchaseWithUpgrades(Material.IRON_SWORD);
    }

    @Test
    public void appliesTeamUpgradesBeforeEquippingPurchasedArmor() {
        purchaseWithUpgrades(Material.IRON_CHESTPLATE);
    }

    private void purchaseWithUpgrades(Material material) {
        ShopManager manager = mock(ShopManager.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack cost = mock(ItemStack.class);
        when(cost.clone()).thenReturn(cost);
        when(cost.getAmount()).thenReturn(4);
        when(inventory.containsAtLeast(cost, 4)).thenReturn(true);
        when(inventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());
        ItemStack template = mock(ItemStack.class);
        ItemStack purchased = mock(ItemStack.class);
        when(template.clone()).thenReturn(purchased);
        when(purchased.getType()).thenReturn(material);
        Game game = mock(Game.class);
        GamePlayer buyer = mock(GamePlayer.class);
        Team team = mock(Team.class, RETURNS_DEEP_STUBS);
        when(game.getGamePlayer(player)).thenReturn(buyer);
        when(buyer.getTeam()).thenReturn(team);

        ItemStack result = new ShopGUI(manager).purchaseItem(player, new ShopItem(template, cost), game);

        assertSame(purchased, result);
        InOrder order = inOrder(manager, inventory);
        order.verify(inventory).removeItem(cost);
        order.verify(manager).applyTeamUpgrades(purchased, team.getUpgrades());
        if (material == Material.IRON_CHESTPLATE) {
            order.verify(inventory).setChestplate(purchased);
        } else {
            order.verify(inventory).addItem(purchased);
        }
    }
}
