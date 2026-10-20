package ch.framedev.bedwars.team;

import ch.framedev.bedwars.game.Arena;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TeamSelectionGUITest {
    @Test
    @SuppressWarnings("try")
    public void replacingMenuKeepsNewSlotMappingAfterOldMenuCloses() {
        TeamSelectionGUI gui = new TeamSelectionGUI();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Game game = mock(Game.class);
        when(game.getState()).thenReturn(GameState.WAITING);
        when(game.getTeams()).thenReturn(Map.of(TeamColor.RED, mock(Team.class)));
        Arena arena = mock(Arena.class);
        when(arena.getMaxPlayers()).thenReturn(8);
        when(game.getArena()).thenReturn(arena);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getSize()).thenReturn(54);
        // Bukkit fires InventoryCloseEvent for the old view during openInventory.
        doAnswer(call -> {
            gui.clearMenuSlots(player.getUniqueId());
            return null;
        }).when(player).openInventory(any(Inventory.class));
        when(game.changeTeam(player, TeamColor.RED)).thenReturn(true, false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedConstruction<ItemStack> stacks = mockConstruction(ItemStack.class)) {
            bukkit.when(() -> Bukkit.createInventory(isNull(), eq(54), anyString())).thenReturn(inventory);
            gui.openTeamSelection(player, game);
            gui.handleMenuClick(player, 10, game); // Opens a replacement after successful change.
            gui.handleMenuClick(player, 10, game); // The replacement must still respond.

            verify(game, times(2)).changeTeam(player, TeamColor.RED);
        }
    }

    @Test
    public void nullTitleIsNotATeamSelectionMenu() {
        assertFalse(TeamSelectionGUI.isTeamSelectionTitle(null));
    }
}
