package ch.framedev.bedwars.commands;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.arena.*;
import ch.framedev.bedwars.support.BedBlocks;
import ch.framedev.bedwars.team.TeamColor;
import ch.framedev.bedwars.utils.*;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BedSetupTest {
    private ImprovedBedWarsCommand command;
    private Player player;
    private ArenaSetupSession session;
    private MessageManager messages;
    private World world;

    @Before
    public void setUp() {
        BedWarsPlugin plugin = mock(BedWarsPlugin.class);
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        messages = mock(MessageManager.class);
        when(plugin.getMessageManager()).thenReturn(messages);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission("bedwars.setup")).thenReturn(true);
        ArenaManager arenas = mock(ArenaManager.class);
        session = new ArenaSetupSession(player.getUniqueId());
        when(arenas.getSession(player.getUniqueId())).thenReturn(session);
        command = new ImprovedBedWarsCommand(plugin, arenas);
        world = mock(World.class);
    }

    private void setBed() {
        command.onCommand(player, mock(Command.class), "bw", new String[]{"setup", "setbed", "red"});
    }

    @Test
    public void lookingAtHeadRegistersExactFootInsteadOfPlayerPosition() {
        BedBlocks bed = new BedBlocks(world, 5, 70, 5, BlockFace.NORTH, Material.RED_BED);
        when(player.getTargetBlockExact(5)).thenReturn(bed.head);
        when(player.getLocation()).thenReturn(new Location(world, 9, 71, 9));

        setBed();

        assertEquals(bed.foot.getLocation(), session.getBedLocations().get(TeamColor.RED));
    }

    @Test
    public void lookingAtFootRegistersTheSameBed() {
        BedBlocks bed = new BedBlocks(world, 5, 70, 5, BlockFace.SOUTH, Material.RED_BED);
        when(player.getTargetBlockExact(5)).thenReturn(bed.foot);

        setBed();

        assertEquals(bed.foot.getLocation(), session.getBedLocations().get(TeamColor.RED));
    }

    @Test
    public void missingTargetDoesNotOverwritePreviouslyRegisteredBed() {
        Location original = new Location(world, 5, 70, 5);
        session.setBedLocation(TeamColor.RED, original);

        setBed();

        assertEquals(original, session.getBedLocations().get(TeamColor.RED));
        verify(messages).sendMessage(player, "command.setup-invalid-bed");
    }

    @Test
    public void incompleteBedIsRejected() {
        BedBlocks bed = new BedBlocks(world, 5, 70, 5, BlockFace.SOUTH, Material.RED_BED);
        when(bed.head.getType()).thenReturn(Material.AIR);
        when(player.getTargetBlockExact(5)).thenReturn(bed.foot);

        setBed();

        assertTrue(session.getBedLocations().isEmpty());
        verify(messages).sendMessage(player, "command.setup-invalid-bed");
    }
}
