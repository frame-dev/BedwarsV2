package ch.framedev.bedwars.generators;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
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
    private BukkitTask task;

    public ResourceGenerator(Location location, ResourceType type, int level) {
        this.location = location;
        this.type = type;
        this.level = level;
    }

    public void start(Plugin plugin) {
        int delay = getDelay();

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
        }
    }

    private void spawnResource() {
        ItemStack item = new ItemStack(type.getMaterial());
        Item droppedItem = location.getWorld().dropItem(location, item);
        droppedItem.setVelocity(droppedItem.getVelocity().zero());
    }

    private int getDelay() {
        return switch (type) {
            case IRON -> 20; // 1 second
            case GOLD -> 160; // 8 seconds
            case DIAMOND -> level == 1 ? 600 : 400; // 30s or 20s
            case EMERALD -> level == 1 ? 1200 : 800; // 60s or 40s
        };
    }

    public void upgrade() {
        level++;
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
