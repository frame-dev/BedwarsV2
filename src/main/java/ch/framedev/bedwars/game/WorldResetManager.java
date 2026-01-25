package ch.framedev.bedwars.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.*;

/**
 * Manages world changes during a game for reset after game ends
 */
public class WorldResetManager {

    private final Set<BlockState> placedBlocks;
    private final Set<BlockState> brokenBlocks;
    private final Set<Location> originalBedLocations;

    public WorldResetManager() {
        this.placedBlocks = new HashSet<>();
        this.brokenBlocks = new HashSet<>();
        this.originalBedLocations = new HashSet<>();
    }

    /**
     * Record a block that was placed by a player
     */
    public void recordPlacedBlock(Block block) {
        placedBlocks.add(new BlockState(block.getLocation(), block.getType(), block.getBlockData()));
    }

    /**
     * Record a block that was broken by a player
     */
    public void recordBrokenBlock(Block block) {
        brokenBlocks.add(new BlockState(block.getLocation(), block.getType(), block.getBlockData()));
    }

    /**
     * Record original bed locations for restoration
     */
    public void recordBedLocation(Location location, Material material, BlockData data) {
        originalBedLocations.add(location);
        brokenBlocks.add(new BlockState(location, material, data));
    }

    /**
     * Check if a block was placed by a player
     */
    public boolean isPlayerPlacedBlock(Location location) {
        return placedBlocks.stream().anyMatch(bs -> bs.location.equals(location));
    }

    /**
     * Reset the world to its original state
     */
    public void resetWorld() {
        // Remove all player-placed blocks
        for (BlockState blockState : placedBlocks) {
            Block block = blockState.location.getBlock();
            block.setType(Material.AIR);
        }

        // Restore all broken blocks
        for (BlockState blockState : brokenBlocks) {
            Block block = blockState.location.getBlock();
            block.setType(blockState.material);
            if (blockState.blockData != null) {
                block.setBlockData(blockState.blockData);
            }
        }

        // Clear tracking sets
        placedBlocks.clear();
        brokenBlocks.clear();
        originalBedLocations.clear();
    }

    /**
     * Get the number of blocks that need to be reset
     */
    public int getBlockChangeCount() {
        return placedBlocks.size() + brokenBlocks.size();
    }

    /**
     * Clear all tracked blocks without resetting
     */
    public void clear() {
        placedBlocks.clear();
        brokenBlocks.clear();
        originalBedLocations.clear();
    }

    /**
     * Internal class to store block state
     */
    private static class BlockState {
        private final Location location;
        private final Material material;
        private final BlockData blockData;

        public BlockState(Location location, Material material, BlockData blockData) {
            this.location = location.clone();
            this.material = material;
            this.blockData = blockData != null ? blockData.clone() : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof BlockState))
                return false;
            BlockState that = (BlockState) o;
            return Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location);
        }
    }
}
