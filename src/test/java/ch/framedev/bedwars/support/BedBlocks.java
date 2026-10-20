package ch.framedev.bedwars.support;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;

/** Mutable block doubles so destruction and restoration exercise the same positions. */
public final class BedBlocks {
    public final Block foot;
    public final Block head;
    public final Bed footData;
    public final Bed headData;

    public BedBlocks(World world, int x, int y, int z, BlockFace facing, Material material) {
        footData = data(facing, Bed.Part.FOOT);
        headData = data(facing, Bed.Part.HEAD);
        foot = block(world, x, y, z, material, footData);
        head = block(world, x + facing.getModX(), y, z + facing.getModZ(), material, headData);
        when(foot.getRelative(facing)).thenReturn(head);
        when(head.getRelative(facing.getOppositeFace())).thenReturn(foot);
    }

    private Bed data(BlockFace facing, Bed.Part part) {
        Bed bed = mock(Bed.class);
        when(bed.getFacing()).thenReturn(facing);
        when(bed.getPart()).thenReturn(part);
        when(bed.clone()).thenReturn(bed);
        return bed;
    }

    private Block block(World world, int x, int y, int z, Material initialType, Bed initialData) {
        Block block = mock(Block.class);
        AtomicReference<Material> type = new AtomicReference<>(initialType);
        AtomicReference<BlockData> data = new AtomicReference<>(initialData);
        when(block.getWorld()).thenReturn(world);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        when(block.getLocation()).thenAnswer(call -> new Location(world, x, y, z));
        when(block.getType()).thenAnswer(call -> type.get());
        when(block.getBlockData()).thenAnswer(call -> data.get());
        when(world.getBlockAt(x, y, z)).thenReturn(block);
        doAnswer(call -> {
            type.set(call.getArgument(0));
            data.set(mock(BlockData.class));
            return null;
        }).when(block).setType(any(Material.class), eq(false));
        doAnswer(call -> {
            data.set(call.getArgument(0));
            return null;
        }).when(block).setBlockData(any(BlockData.class), eq(false));
        return block;
    }
}
