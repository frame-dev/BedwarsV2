package ch.framedev.bedwars.utils;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            if (str == null || str.isBlank()) {
                return null;
            }

            String[] parts = str.split(",\\s*");
            if (parts.length < 4) {
                return null;
            }

            String worldName = parts[0];
            double[] values = parseLocationNumbers(parts);
            if (values == null) {
                return null;
            }

            double x = values[0];
            double y = values[1];
            double z = values[2];
            float yaw = values.length > 3 ? (float) values[3] : 0.0f;
            float pitch = values.length > 4 ? (float) values[4] : 0.0f;

            return new Location(
                    org.bukkit.Bukkit.getWorld(worldName),
                    x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    private static double[] parseLocationNumbers(String[] parts) {
        int count = parts.length - 1;
        List<Double> numbers = new ArrayList<>();

        if (count == 3 || count == 5) {
            for (int i = 1; i < parts.length; i++) {
                numbers.add(Double.parseDouble(parts[i].trim()));
            }
        } else if (count == 6 || count == 10) {
            for (int i = 1; i < parts.length; i += 2) {
                String combined = parts[i].trim() + "." + parts[i + 1].trim();
                numbers.add(Double.parseDouble(combined));
            }
        } else {
            return null;
        }

        double[] values = new double[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            values[i] = numbers.get(i);
        }

        return values;
    }
}
