package ch.framedev.bedwars.generators;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Generates resources at a specific location
 */
public class ResourceGenerator {

    private final Location location;
    private final ResourceType type;
    private int level;
    private final int tier1DelayTicks;
    private final int tier2DelayTicks;
    private final int spawnAmount;
    private final int maxStack;
    private BukkitTask task;
    private Plugin plugin;

    public ResourceGenerator(Location location, ResourceType type, int level) {
        this(location, type, level, defaultDelayTicks(type, 1), defaultDelayTicks(type, 2), 1, 0);
    }

    public ResourceGenerator(Location location, ResourceType type, int level, int tier1DelayTicks,
            int tier2DelayTicks, int spawnAmount, int maxStack) {
        this.location = location;
        this.type = type;
        this.level = level;
        this.tier1DelayTicks = tier1DelayTicks;
        this.tier2DelayTicks = tier2DelayTicks;
        this.spawnAmount = Math.max(1, spawnAmount);
        this.maxStack = Math.max(0, maxStack);
    }

    public void start(Plugin plugin) {
        this.plugin = plugin;
        int delay = getDelayTicks();

        task = new BukkitRunnable() {
            @Override
            public void run() {
                spawnResource();
            }
        }.runTaskTimer(plugin, delay, delay);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void spawnResource() {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        int existingCount = 0;
        if (maxStack > 0) {
            for (Entity entity : world.getNearbyEntities(location, 1.5, 1.5, 1.5)) {
                if (entity instanceof Item dropped) {
                    ItemStack stack = dropped.getItemStack();
                    if (stack.getType() == type.getMaterial()) {
                        existingCount += stack.getAmount();
                    }
                }
            }
            if (existingCount >= maxStack) {
                return;
            }
        }

        int amount = spawnAmount;
        if (maxStack > 0) {
            amount = Math.min(spawnAmount, maxStack - existingCount);
            if (amount <= 0) {
                return;
            }
        }

        ItemStack item = new ItemStack(type.getMaterial(), amount);
        Item droppedItem = world.dropItem(location, item);
        droppedItem.setVelocity(droppedItem.getVelocity().zero());
    }

    private int getDelayTicks() {
        int delay = level <= 1 ? tier1DelayTicks : tier2DelayTicks;
        return Math.max(1, delay);
    }

    public void upgrade() {
        level++;
        if (plugin != null) {
            restartTask();
        }
    }

    private void restartTask() {
        if (plugin == null) {
            return;
        }
        stop();
        int delay = getDelayTicks();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                spawnResource();
            }
        }.runTaskTimer(plugin, delay, delay);
    }

    public ResourceType getType() {
        return type;
    }

    private static int defaultDelayTicks(ResourceType type, int tier) {
        return switch (type) {
            case IRON -> 20;
            case GOLD -> 160;
            case DIAMOND -> tier <= 1 ? 600 : 400;
            case EMERALD -> tier <= 1 ? 1200 : 800;
        };
    }

    public enum ResourceType {
        IRON(Material.IRON_INGOT),
        GOLD(Material.GOLD_INGOT),
        DIAMOND(Material.DIAMOND),
        EMERALD(Material.EMERALD);

        private final Material material;

        ResourceType(Material material) {
            this.material = material;
        }

        public Material getMaterial() {
            return material;
        }
    }
}
