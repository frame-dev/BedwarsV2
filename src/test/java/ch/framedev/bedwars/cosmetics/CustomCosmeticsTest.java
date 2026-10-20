package ch.framedev.bedwars.cosmetics;

import ch.framedev.bedwars.support.PersistentFeatureTest;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CustomCosmeticsTest extends PersistentFeatureTest {
    private void addCustom(boolean unlock, List<Map<String, Object>> layers) throws Exception {
        var file = folder.getRoot().toPath().resolve("cosmetics.yml").toFile();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("testing.unlock-all", unlock);
        for (String category : List.of("kill-effects", "bed-effects")) {
            String path = category + ".custom_magic";
            config.set(path + ".enabled", true);
            config.set(path + ".display-name", "&6Custom Magic");
            config.set(path + ".icon", "GOLD_NUGGET");
            config.set(path + ".effect", "CUSTOM");
            config.set(path + ".permission", "custom.magic");
            config.set(path + ".particles", layers);
        }
        config.save(file);
    }

    @Test
    public void customLayersAreSelectablePersistedAndRenderForKillsAndBeds() throws Exception {
        addCustom(true, List.of(
                Map.of("type", "FLAME", "count", 12, "height", 1.0),
                Map.of("type", "REDSTONE", "count", 20, "color", "#FFD700", "size", 1.5, "height", 1.0)));
        Player player = player("Tester");
        CosmeticsManager manager = new CosmeticsManager(plugin, database);
        manager.openCosmeticsMenu(player);
        manager.handleMenuClick(player, 21); // After 11 bundled kill effects.
        manager.handleMenuClick(player, 38); // After 10 bundled bed effects.
        try (ResultSet result = database.executeQuery("SELECT kill_effect, bed_effect FROM player_cosmetics")) {
            assertTrue(result.next());
            assertEquals("custom_magic", result.getString("kill_effect"));
            assertEquals("custom_magic", result.getString("bed_effect"));
        }
        World world = mock(World.class);
        Location location = new Location(world, 0, 70, 0);
        Location emitted = location.clone().add(0, 1, 0);
        CosmeticsManager reloaded = new CosmeticsManager(plugin, database);
        reloaded.applyKillEffect(player, location);
        reloaded.applyBedDestroyEffect(player, location);

        verify(world, times(2)).spawnParticle(Particle.FLAME, emitted, 12, 0.5, 0.5, 0.5, 0.0);
        ArgumentCaptor<Particle.DustOptions> dust = ArgumentCaptor.forClass(Particle.DustOptions.class);
        verify(world, times(2)).spawnParticle(eq(Particle.REDSTONE), eq(emitted), eq(20),
                eq(0.5), eq(0.5), eq(0.5), eq(0.0), dust.capture());
        assertEquals(Color.fromRGB(255, 215, 0), dust.getValue().getColor());
        assertEquals(1.5f, dust.getValue().getSize(), 0.001f);
        assertEquals(70.0, location.getY(), 0.0); // Shared event position is not mutated.
    }

    @Test
    public void customPermissionIsEnforcedOutsideTesting() throws Exception {
        addCustom(false, List.of(Map.of("type", "FLAME")));
        Player player = player("Restricted");
        CosmeticsManager manager = new CosmeticsManager(plugin, database);
        manager.openCosmeticsMenu(player);
        manager.handleMenuClick(player, 21);
        verify(messages).sendMessage(player, "cosmetics.locked");
        assertEquals(0, count("player_cosmetics"));
    }

    @Test
    public void invalidParticlesDisableCosmeticWithoutGameplayException() throws Exception {
        addCustom(true, List.of(Map.of("type", "DOES_NOT_EXIST"), Map.of("type", "ITEM_CRACK"),
                Map.of("type", "REDSTONE", "color", "invalid")));
        Player player = player("Tester");
        database.executeUpdate("INSERT INTO player_cosmetics VALUES (?, ?, ?, ?)",
                player.getUniqueId().toString(), "custom_magic", "custom_magic", 1L);
        World world = mock(World.class);
        CosmeticsManager manager = new CosmeticsManager(plugin, database);

        manager.applyKillEffect(player, new Location(world, 0, 70, 0));
        manager.applyBedDestroyEffect(player, new Location(world, 0, 70, 0));

        verifyNoInteractions(world);
    }

    @Test
    public void particleValuesAreClampedAndValidLayersSurviveInvalidOnes() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("custom.particles", List.of(Map.of("type", "INVALID"),
                Map.of("type", "FLAME", "count", 9999, "speed", -5, "spread-x", Double.NaN,
                        "spread-y", 50, "height", -99)));

        CustomCosmeticEffect effect = CustomCosmeticEffect.load(config.getConfigurationSection("custom"),
                plugin.getLogger());

        assertEquals(1, effect.bursts().size());
        var burst = effect.bursts().get(0);
        assertEquals(250, burst.count());
        assertEquals(0.0, burst.speed(), 0.0);
        assertEquals(0.5, burst.spreadX(), 0.0);
        assertEquals(4.0, burst.spreadY(), 0.0);
        assertEquals(-4.0, burst.height(), 0.0);
    }
}
