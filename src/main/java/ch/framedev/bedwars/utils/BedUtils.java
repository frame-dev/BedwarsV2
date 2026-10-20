package ch.framedev.bedwars.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;

/** Resolves the two matching halves of a bed without changing the world. */
public final class BedUtils {
    private BedUtils() {}

    public static Block getOtherHalf(Block block) {
        if (block == null || !(block.getBlockData() instanceof Bed bed)) return null;
        BlockFace direction = bed.getPart() == Bed.Part.FOOT
                ? bed.getFacing() : bed.getFacing().getOppositeFace();
        Block other = block.getRelative(direction);
        if (other == null || other.getType() != block.getType()
                || !(other.getBlockData() instanceof Bed otherBed)
                || otherBed.getPart() == bed.getPart()
                || otherBed.getFacing() != bed.getFacing()) return null;
        return other;
    }

    public static Block getFoot(Block block) {
        Block other = getOtherHalf(block);
        if (other == null) return null;
        return ((Bed) block.getBlockData()).getPart() == Bed.Part.FOOT ? block : other;
    }

    public static boolean isBlockAt(Block block, Location location) {
        return block != null && location != null && block.getWorld().equals(location.getWorld())
                && block.getX() == location.getBlockX()
                && block.getY() == location.getBlockY()
                && block.getZ() == location.getBlockZ();
    }
}
