package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameManager;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.team.Team;
import ch.framedev.bedwars.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;

public class ItemPickupListenerTest {
    private BedWarsPlugin plugin;
    private UpgradeManager upgrades;
    private Game game;
    private Player player;
    private Team team;
    private BukkitScheduler scheduler;
    private ItemPickupListener listener;

    @Before
    public void setUp() {
        plugin = mock(BedWarsPlugin.class);
        upgrades = mock(UpgradeManager.class);
        game = mock(Game.class);
        player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        GameManager games = mock(GameManager.class);
        when(plugin.getGameManager()).thenReturn(games);
        when(games.getPlayerGame(player)).thenReturn(game);
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        when(game.getState()).thenReturn(GameState.RUNNING);
        GamePlayer gp = mock(GamePlayer.class);
        team = mock(Team.class, RETURNS_DEEP_STUBS);
        when(gp.getTeam()).thenReturn(team);
        when(game.getGamePlayer(player)).thenReturn(gp);
        Server server = mock(Server.class);
        scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(plugin.getServer()).thenReturn(server);
        listener = new ItemPickupListener(plugin, upgrades);
    }

    @Test
    public void pickupWritesUpgradedCopyBackToItemEntity() {
        EntityPickupItemEvent event = mock(EntityPickupItemEvent.class);
        Item entity = mock(Item.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(Material.IRON_SWORD);
        when(event.getEntity()).thenReturn(player);
        when(event.getItem()).thenReturn(entity);
        when(entity.getItemStack()).thenReturn(stack);

        listener.onItemPickup(event);

        org.mockito.InOrder order = inOrder(upgrades, entity);
        order.verify(upgrades).applyUpgradesToItem(stack, team.getUpgrades());
        order.verify(entity).setItemStack(stack);
    }

    private Runnable scheduleShiftClick() {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.isShiftClick()).thenReturn(true);
        listener.onInventoryClick(event);
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), callback.capture());
        return callback.getValue();
    }

    @Test
    public void shiftClickUpgradesStoredEquipmentAndWritesItBack() {
        ItemStack storedSword = mock(ItemStack.class);
        ItemStack[] contents = {null, storedSword};
        when(player.getInventory().getContents()).thenReturn(contents);

        scheduleShiftClick().run();

        verify(upgrades).applyUpgradesToItem(storedSword, team.getUpgrades());
        verify(player.getInventory()).setContents(contents);
    }

    @Test
    public void deferredUpgradeDoesNotAffectPlayerWhoLeftMatch() {
        Runnable callback = scheduleShiftClick();
        when(plugin.getGameManager().getPlayerGame(player)).thenReturn(null);

        callback.run();

        verifyNoInteractions(upgrades);
        verify(player, never()).getInventory();
    }

    @Test
    public void deferredUpgradeDoesNotAffectPlayerWhoDied() {
        Runnable callback = scheduleShiftClick();
        when(game.isSpectator(player)).thenReturn(true);

        callback.run();

        verifyNoInteractions(upgrades);
        verify(player, never()).getInventory();
    }
}
