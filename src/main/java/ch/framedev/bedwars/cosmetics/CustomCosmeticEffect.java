package ch.framedev.bedwars.cosmetics;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Validated, particle-only effects configured by server owners. */
public record CustomCosmeticEffect(List<Burst> bursts) {
    public CustomCosmeticEffect {
        bursts = List.copyOf(bursts);
    }

    public static CustomCosmeticEffect load(ConfigurationSection section, Logger logger) {
        List<Burst> bursts = new ArrayList<>();
        List<Map<?, ?>> configured = section.getMapList("particles");
        for (int index = 0; index < configured.size() && index < 8; index++) {
            Map<?, ?> layer = configured.get(index);
            try {
                Object name = layer.get("type");
                if (!(name instanceof String)) throw new IllegalArgumentException("particle type is required");
                Particle particle = Particle.valueOf(((String) name).trim().toUpperCase(Locale.ROOT));
                Particle.DustOptions dust = null;
                if (particle.getDataType() == Particle.DustOptions.class) {
                    Object rawColor = layer.get("color");
                    String hex = rawColor instanceof String ? ((String) rawColor).replace("#", "") : "FFFFFF";
                    if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("color must be #RRGGBB");
                    dust = new Particle.DustOptions(Color.fromRGB(Integer.parseInt(hex, 16)),
                            (float) number(layer, "size", 1, 0.1, 4));
                } else if (particle.getDataType() != Void.class) {
                    throw new IllegalArgumentException("particle needs unsupported extra data: " + particle);
                }
                bursts.add(new Burst(particle, (int) number(layer, "count", 30, 1, 250),
                        number(layer, "spread-x", 0.5, 0, 4),
                        number(layer, "spread-y", 0.5, 0, 4),
                        number(layer, "spread-z", 0.5, 0, 4),
                        number(layer, "speed", 0, 0, 1),
                        number(layer, "height", 0.5, -4, 4), dust));
            } catch (IllegalArgumentException error) {
                logger.warning("Invalid cosmetic " + section.getCurrentPath() + " particle layer "
                        + (index + 1) + ": " + error.getMessage());
            }
        }
        if (bursts.isEmpty()) logger.warning("Custom cosmetic " + section.getCurrentPath() + " has no valid particles; disabled.");
        return new CustomCosmeticEffect(bursts);
    }

    private static double number(Map<?, ?> values, String key, double fallback, double min, double max) {
        Object raw = values.get(key);
        double value = raw instanceof Number ? ((Number) raw).doubleValue() : fallback;
        if (!Double.isFinite(value)) value = fallback;
        return Math.max(min, Math.min(max, value));
    }

    public void play(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        for (Burst burst : bursts) {
            Location point = location.clone().add(0, burst.height(), 0);
            if (burst.dust() == null) {
                world.spawnParticle(burst.particle(), point, burst.count(),
                        burst.spreadX(), burst.spreadY(), burst.spreadZ(), burst.speed());
            } else {
                world.spawnParticle(burst.particle(), point, burst.count(),
                        burst.spreadX(), burst.spreadY(), burst.spreadZ(), burst.speed(), burst.dust());
            }
        }
    }

    public record Burst(Particle particle, int count, double spreadX, double spreadY,
                        double spreadZ, double speed, double height, Particle.DustOptions dust) {}
}
