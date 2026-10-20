package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.*;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.support.BedBlocks;
import ch.framedev.bedwars.team.*;
import ch.framedev.bedwars.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BedBreakTest {
    private static class Fixture {
        final World world = mock(World.class);
        final BedWarsPlugin plugin = mock(BedWarsPlugin.class);
        final Player player = mock(Player.class);
        final Game game = mock(Game.class);
        final GamePlayer breaker;
        final WorldResetManager reset = new WorldResetManager();
        final Map<TeamColor, Team> teams = new LinkedHashMap<>();
        final BlockBreakListener listener;

        Fixture() {
            when(world.getName()).thenReturn("arena");
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            breaker = new GamePlayer(player);
            breaker.setTeam(new Team(TeamColor.BLUE, new Location(world, 30, 70, 0), null));
            when(plugin.getConfig()).thenReturn(new YamlConfiguration());
            when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
            when(plugin.getMessageManager()).thenReturn(mock(MessageManager.class));
            GameManager manager = mock(GameManager.class);
            when(plugin.getGameManager()).thenReturn(manager);
            when(manager.getPlayerGame(player)).thenReturn(game);
            when(game.getState()).thenReturn(GameState.RUNNING);
            when(game.getTeams()).thenReturn(teams);
            when(game.getGamePlayer(player)).thenReturn(breaker);
            when(game.getWorldResetManager()).thenReturn(reset);
            listener = new BlockBreakListener(plugin);
        }

        Team register(Location bed) {
            Team team = new Team(TeamColor.RED, new Location(world, 0, 70, 10), bed);
            teams.put(TeamColor.RED, team);
            return team;
        }
    }

    @Test
    public void eitherHalfBreaksWithoutDropsAndBothHalvesRestoreInEveryDirection() {
        for (BlockFace facing : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            for (boolean breakHead : List.of(false, true)) {
                for (boolean configuredHead : List.of(false, true)) {
                    Fixture f = new Fixture();
                    BedBlocks bed = new BedBlocks(f.world, 0, 70, 0, facing, Material.RED_BED);
                    Team team = f.register((configuredHead ? bed.head : bed.foot).getLocation());
                    BlockBreakEvent event = new BlockBreakEvent(breakHead ? bed.head : bed.foot, f.player);
                    event.setExpToDrop(3);

                    f.listener.onBlockBreak(event);

                    assertTrue(event.isCancelled());
                    assertFalse(event.isDropItems());
                    assertEquals(0, event.getExpToDrop());
                    assertFalse(team.isBedAlive());
                    assertEquals(1, f.breaker.getBedsBroken());
                    assertEquals(Material.AIR, bed.foot.getType());
                    assertEquals(Material.AIR, bed.head.getType());
                    assertEquals(2, f.reset.getBlockChangeCount());
                    verify(bed.foot).setType(Material.AIR, false);
                    verify(bed.head).setType(Material.AIR, false);
                    verify(bed.foot, never()).breakNaturally();
                    verify(bed.head, never()).breakNaturally();
                    verify(f.world, never()).dropItem(any(Location.class), any(ItemStack.class));
                    verify(f.world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));

                    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                        bukkit.when(() -> Bukkit.getWorld("arena")).thenReturn(f.world);
                        f.reset.resetWorld();
                    }
                    assertEquals(Material.RED_BED, bed.foot.getType());
                    assertEquals(Material.RED_BED, bed.head.getType());
                    assertEquals(Bed.Part.FOOT, ((Bed) bed.foot.getBlockData()).getPart());
                    assertEquals(Bed.Part.HEAD, ((Bed) bed.head.getBlockData()).getPart());
                    assertEquals(facing, ((Bed) bed.foot.getBlockData()).getFacing());
                    assertEquals(facing, ((Bed) bed.head.getBlockData()).getFacing());
                }
            }
        }
    }

    @Test
    public void nearbyBedsAreNotMistakenForTheConfiguredTeamBed() {
        Fixture f = new Fixture();
        BedBlocks bed = new BedBlocks(f.world, 0, 70, 0, BlockFace.EAST, Material.RED_BED);
        BedBlocks nearby = new BedBlocks(f.world, 0, 70, 1, BlockFace.EAST, Material.WHITE_BED);
        Team team = f.register(bed.foot.getLocation());

        BlockBreakEvent event = new BlockBreakEvent(nearby.foot, f.player);
        f.listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
        assertTrue(team.isBedAlive());
        assertEquals(Material.WHITE_BED, nearby.foot.getType());
        assertEquals(0, f.breaker.getBedsBroken());
    }

    @Test
    public void ownBedCannotBeDestroyed() {
        Fixture f = new Fixture();
        BedBlocks bed = new BedBlocks(f.world, 0, 70, 0, BlockFace.EAST, Material.RED_BED);
        Team team = f.register(bed.head.getLocation());
        f.breaker.setTeam(team);
        BlockBreakEvent event = new BlockBreakEvent(bed.foot, f.player);

        f.listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
        assertFalse(event.isDropItems());
        assertTrue(team.isBedAlive());
        assertEquals(Material.RED_BED, bed.foot.getType());
        assertEquals(Material.RED_BED, bed.head.getType());
        assertEquals(0, f.reset.getBlockChangeCount());
    }

    @Test
    public void mismatchedNeighborIsNotRemovedAsTheOtherHalf() {
        Fixture f = new Fixture();
        BedBlocks bed = new BedBlocks(f.world, 0, 70, 0, BlockFace.EAST, Material.RED_BED);
        when(bed.head.getType()).thenReturn(Material.BLUE_BED);
        f.register(bed.foot.getLocation());

        f.listener.onBlockBreak(new BlockBreakEvent(bed.foot, f.player));

        verify(bed.head, never()).setType(any(Material.class), anyBoolean());
        assertEquals(1, f.reset.getBlockChangeCount());
    }

    @Test
    public void bedBreakRecordsAchievementAndPlaysSelectedCosmeticOnce() {
        Fixture f = new Fixture();
        BedBlocks bed = new BedBlocks(f.world, 0, 70, 0, BlockFace.EAST, Material.RED_BED);
        f.register(bed.foot.getLocation());
        var achievements = mock(ch.framedev.bedwars.achievements.AchievementsManager.class);
        var cosmetics = mock(ch.framedev.bedwars.cosmetics.CosmeticsManager.class);
        when(f.plugin.getAchievementsManager()).thenReturn(achievements);
        when(f.plugin.getCosmeticsManager()).thenReturn(cosmetics);

        f.listener.onBlockBreak(new BlockBreakEvent(bed.head, f.player));

        verify(achievements).recordBedBroken(f.player.getUniqueId());
        verify(cosmetics).applyBedDestroyEffect(f.player, bed.head.getLocation());
    }
}
