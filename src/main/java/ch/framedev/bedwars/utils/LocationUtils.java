package ch.framedev.bedwars.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for location serialization.
 * <p>
 * De-duplication / fixes:
 * - One canonical parser that supports:
 *   - "world, x, y, z"
 *   - "world, x, y, z, yaw, pitch"
 * - toString always writes yaw/pitch only when requested (default: 3D)
 * - serialize/deserialize use the same internal helpers
 * - Proper null checks and no silent world-null Location creation
 * - No weird "count==6/10" decimal re-join logic (that was a symptom of bad CSV formatting)
 */
public final class LocationUtils {

    private LocationUtils() {}

    /* --------------------------------------------------------------------- */
    /* Map serialize                                                          */
    /* --------------------------------------------------------------------- */

    public static Map<String, Object> serialize(Location location) {
        Map<String, Object> map = new HashMap<>();
        if (location == null) return map;

        World world = location.getWorld();
        if (world == null) return map;

        map.put("world", world.getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());
        return map;
    }

    public static Location deserialize(ConfigurationSection section) {
        if (section == null) return null;

        String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    /* --------------------------------------------------------------------- */
    /* String serialize                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Default string format (no yaw/pitch):
     * "world, x, y, z"
     */
    public static String toString(Location location) {
        return toString(location, false);
    }

    /**
     * Optional yaw/pitch string format:
     * includeYawPitch=false -> "world, x, y, z"
     * includeYawPitch=true  -> "world, x, y, z, yaw, pitch"
     */
    public static String toString(Location location, boolean includeYawPitch) {
        if (location == null) return null;

        World world = location.getWorld();
        if (world == null) return null;

        if (!includeYawPitch) {
            return String.format(Locale.ROOT, "%s, %.3f, %.3f, %.3f",
                    world.getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ());
        }

        return String.format(Locale.ROOT, "%s, %.3f, %.3f, %.3f, %.3f, %.3f",
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Parses:
     * - "world, x, y, z"
     * - "world, x, y, z, yaw, pitch"
     */
    public static Location fromString(String str) {
        ParsedLocation parsed = parse(str);
        if (parsed == null) return null;

        World world = Bukkit.getWorld(parsed.worldName);
        if (world == null) return null;

        return new Location(world, parsed.x, parsed.y, parsed.z, parsed.yaw, parsed.pitch);
    }

    /* --------------------------------------------------------------------- */
    /* Internal parser (single source of truth)                               */
    /* --------------------------------------------------------------------- */

    private static ParsedLocation parse(String str) {
        if (str == null) return null;

        String trimmed = str.trim();
        if (trimmed.isEmpty()) return null;

        // Split by comma, ignoring extra whitespace
        String[] parts = trimmed.split("\\s*,\\s*");
        if (parts.length != 4 && parts.length != 6) return null;

        String worldName = parts[0];
        if (worldName == null || worldName.isBlank()) return null;

        Double x = parseDouble(parts[1]);
        Double y = parseDouble(parts[2]);
        Double z = parseDouble(parts[3]);
        if (x == null || y == null || z == null) return null;

        float yaw = 0.0f;
        float pitch = 0.0f;

        if (parts.length == 6) {
            Double yawD = parseDouble(parts[4]);
            Double pitchD = parseDouble(parts[5]);
            if (yawD == null || pitchD == null) return null;
            yaw = yawD.floatValue();
            pitch = pitchD.floatValue();
        }

        return new ParsedLocation(worldName, x, y, z, yaw, pitch);
    }

    private static Double parseDouble(String s) {
        if (s == null) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class ParsedLocation {
        private final String worldName;
        private final double x, y, z;
        private final float yaw, pitch;

        private ParsedLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}