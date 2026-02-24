package ch.framedev.bedwars.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages world changes during a game for reset after game ends.
 * <p>
 * Fixes / improvements:
 * - O(1) lookup for "isPlayerPlacedBlock" (no stream scan)
 * - Correctly records BROKEN block "original state" BEFORE it is changed
 * - Avoids duplicate entries by keying by immutable BlockPos
 * - Safer world handling (skips if world unloaded)
 * - Uses setType(..., false) / setBlockData(..., false) to avoid physics explosions during reset
 */
public class WorldResetManager {

    /** Blocks placed by players during the match -> remove (set AIR) on reset. */
    private final Set<BlockPos> placedBlocks = new HashSet<>();

    /**
     * Blocks broken/modified during the match -> restore original state on reset.
     * Keyed by position to keep only the first/original state.
     */
    private final Map<BlockPos, SavedBlockState> brokenBlocks = new HashMap<>();

    /** Optional tracking (not strictly needed, but kept in case you want special bed logic later). */
    private final Set<BlockPos> originalBedLocations = new HashSet<>();

    /**
     * Record a block that was placed by a player.
     * On reset, this block will be removed.
     */
    public void recordPlacedBlock(Block block) {
        if (block == null) {
            return;
        } else {
            block.getWorld();
        }
        placedBlocks.add(BlockPos.from(block.getLocation()));
    }

    /**
     * Record a block that is about to be broken/changed by a player.
     * IMPORTANT: call this BEFORE you set the block to AIR or change its data.
     *
     * This stores the ORIGINAL state so we can restore it at reset.
     */
    public void recordBrokenBlock(Block block) {
        if (block == null) {
            return;
        } else {
            block.getWorld();
        }

        BlockPos pos = BlockPos.from(block.getLocation());

        // Only store the first/original state (don't overwrite if it gets modified multiple times)
        brokenBlocks.putIfAbsent(pos, new SavedBlockState(block.getType(), block.getBlockData()));
    }

    /**
     * Record original bed location for restoration.
     * Also stores the original bed block state in brokenBlocks (only once).
     */
    public void recordBedLocation(Location location, Material material, BlockData data) {
        if (location == null || location.getWorld() == null) return;

        BlockPos pos = BlockPos.from(location);
        originalBedLocations.add(pos);
        brokenBlocks.putIfAbsent(pos, new SavedBlockState(material, data));
    }

    /**
     * Check if a block at a location was placed by a player.
     */
    public boolean isPlayerPlacedBlock(Location location) {
        if (location == null || location.getWorld() == null) return false;
        return placedBlocks.contains(BlockPos.from(location));
    }

    /**
     * Reset the world to its original state.
     * Should be called on the main server thread.
     */
    public void resetWorld() {
        // Remove all player-placed blocks
        for (BlockPos pos : placedBlocks) {
            Block block = pos.getBlock();
            if (block == null) continue;
            block.setType(Material.AIR, false);
        }

        // Restore all broken blocks to their original state
        for (Map.Entry<BlockPos, SavedBlockState> entry : brokenBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            if (block == null) continue;

            SavedBlockState state = entry.getValue();
            block.setType(state.material, false);
            if (state.blockData != null) {
                block.setBlockData(state.blockData, false);
            }
        }

        clear();
    }

    /**
     * Number of blocks that need to be reset.
     */
    public int getBlockChangeCount() {
        return placedBlocks.size() + brokenBlocks.size();
    }

    /**
     * Clear all tracked blocks without resetting.
     */
    public void clear() {
        placedBlocks.clear();
        brokenBlocks.clear();
        originalBedLocations.clear();
    }

    /* --------------------------------------------------------------------- */
    /* Internal                                                               */
    /* --------------------------------------------------------------------- */

    private static final class SavedBlockState {
        private final Material material;
        private final BlockData blockData;

        private SavedBlockState(Material material, BlockData blockData) {
            this.material = material == null ? Material.AIR : material;
            this.blockData = blockData != null ? blockData.clone() : null;
        }
    }

    /**
     * Immutable block position (world + x/y/z). Much cheaper than Location as a key.
     */
    private static final class BlockPos {
        private final String worldName;
        private final int x, y, z;

        private BlockPos(String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        static BlockPos from(Location loc) {
            return new BlockPos(
                    loc.getWorld().getName(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ()
            );
        }

        Block getBlock() {
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) return null;
            return world.getBlockAt(x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPos other)) return false;
            return x == other.x && y == other.y && z == other.z
                    && Objects.equals(worldName, other.worldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldName, x, y, z);
        }
    }
}