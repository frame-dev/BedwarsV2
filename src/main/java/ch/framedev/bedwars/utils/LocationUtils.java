package ch.framedev.bedwars.utils;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for location serialization
 */
public class LocationUtils {

    public static Map<String, Object> serialize(Location location) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());
        return map;
    }

    public static Location deserialize(ConfigurationSection section) {
        try {
            String worldName = section.getString("world");
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw", 0.0);
            float pitch = (float) section.getDouble("pitch", 0.0);

            return new Location(
                    org.bukkit.Bukkit.getWorld(worldName),
                    x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toString(Location location) {
        return String.format("%s, %.1f, %.1f, %.1f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ());
    }

    public static Location fromString(String str) {
        try {
            String[] parts = str.split(",\\s*");
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            return new Location(
                    org.bukkit.Bukkit.getWorld(worldName),
                    x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
}
