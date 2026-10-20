package ch.framedev.bedwars.cosmetics;

import ch.framedev.bedwars.listeners.EntityDamageListener;
import ch.framedev.bedwars.support.PersistentFeatureTest;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.junit.Test;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CosmeticsIntegrationTest extends PersistentFeatureTest {
    @Test
    public void selectionsUseConfiguredSlotsSurviveLateLoadAndPersistAcrossReload() throws Exception {
        CosmeticsManager manager = new CosmeticsManager(plugin, database);
        Player player = player("Player");
        when(player.hasPermission(anyString())).thenReturn(true);
        manager.loadPlayerCosmetics(player.getUniqueId());
        manager.openCosmeticsMenu(player);
        manager.handleMenuClick(player, 12); // Heart, after none and firework in config order.
        manager.handleMenuClick(player, 29); // Lightning, after none in bed effects.
        asyncLoads.get(0).run();

        try (ResultSet result = database.executeQuery("SELECT kill_effect, bed_effect FROM player_cosmetics")) {
            assertTrue(result.next());
            assertEquals("heart", result.getString("kill_effect"));
            assertEquals("lightning", result.getString("bed_effect"));
        }
        CosmeticsManager reloaded = new CosmeticsManager(plugin, database);
        World world = mock(World.class);
        Location location = new Location(world, 0, 70, 0);
        reloaded.applyKillEffect(player, location);
        reloaded.applyBedDestroyEffect(player, location);
        verify(world).spawnParticle(Particle.HEART, location, 12, 0.5, 0.5, 0.5, 0.0);
        verify(world).strikeLightningEffect(location);
    }

    @Test
    public void disabledOrPermissionRevokedSelectionsDoNotPlay() throws Exception {
        Player player = player("Player");
        database.executeUpdate("INSERT INTO player_cosmetics VALUES (?, ?, ?, ?)",
                player.getUniqueId().toString(), "heart", "lightning", 1L);
        var file = folder.getRoot().toPath().resolve("cosmetics.yml").toFile();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("kill-effects.heart.enabled", false);
        config.set("bed-effects.lightning.permission", "bedwars.cosmetics.vip");
        config.save(file);
        CosmeticsManager manager = new CosmeticsManager(plugin, database);
        World world = mock(World.class);

        manager.applyKillEffect(player, new Location(world, 0, 70, 0));
        manager.applyBedDestroyEffect(player, new Location(world, 0, 70, 0));

        verifyNoInteractions(world);
    }

    @Test
    public void lockedMenuSelectionIsRejected() throws Exception {
        var file = folder.getRoot().toPath().resolve("cosmetics.yml").toFile();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("kill-effects.heart.permission", "bedwars.cosmetics.vip");
        config.save(file);
        CosmeticsManager manager = new CosmeticsManager(plugin, database);
        Player player = player("Player");
        manager.openCosmeticsMenu(player);

        manager.handleMenuClick(player, 12);

        verify(messages).sendMessage(player, "cosmetics.locked");
        assertEquals(0, count("player_cosmetics"));
    }

    @Test
    public void cosmeticFireworksCannotDamagePlayersOrOtherEntities() {
        EntityDamageListener listener = new EntityDamageListener(plugin);
        Firework firework = mock(Firework.class);
        when(firework.getMetadata(CosmeticsManager.COSMETIC_FIREWORK))
                .thenReturn(List.of(new FixedMetadataValue(plugin, true)));
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(firework);
        when(event.getEntity()).thenReturn(mock(Villager.class));

        listener.onEntityDamage(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void allDefaultEffectsUseTheirIntendedVisuals() throws Exception {
        Player player = player("Player");
        when(player.hasPermission(anyString())).thenReturn(true);
        World world = mock(World.class);
        Location location = new Location(world, 0, 70, 0);
        Firework firework = mock(Firework.class);
        when(world.spawn(location, Firework.class)).thenReturn(firework);
        when(firework.getFireworkMeta()).thenReturn(mock(org.bukkit.inventory.meta.FireworkMeta.class));
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(1L)))
                .thenReturn(mock(org.bukkit.scheduler.BukkitTask.class));
        for (String effect : List.of("none", "heart", "smoke", "explosion", "firework")) {
            database.executeUpdate("INSERT OR REPLACE INTO player_cosmetics VALUES (?, ?, ?, ?)",
                    player.getUniqueId().toString(), effect, "none", 1L);
            new CosmeticsManager(plugin, database).applyKillEffect(player, location);
        }
        for (String effect : List.of("none", "lightning", "firework", "explosion")) {
            database.executeUpdate("INSERT OR REPLACE INTO player_cosmetics VALUES (?, ?, ?, ?)",
                    player.getUniqueId().toString(), "none", effect, 1L);
            new CosmeticsManager(plugin, database).applyBedDestroyEffect(player, location);
        }
        verify(world).spawnParticle(Particle.HEART, location, 12, 0.5, 0.5, 0.5, 0.0);
        verify(world).spawnParticle(Particle.SMOKE_NORMAL, location, 20, 0.6, 0.6, 0.6, 0.0);
        verify(world, times(2)).spawnParticle(Particle.EXPLOSION_LARGE, location, 1, 0, 0, 0, 0.0);
        verify(world).strikeLightningEffect(location);
        verify(world, times(2)).spawn(location, Firework.class);
        verify(firework, times(2)).setMetadata(eq(CosmeticsManager.COSMETIC_FIREWORK), any());
    }

    @Test
    public void testingUnlockAllowsRestrictedKillAndBedEffectsForEveryone() throws Exception {
        var file = folder.getRoot().toPath().resolve("cosmetics.yml").toFile();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("testing.unlock-all", true);
        config.set("kill-effects.heart.permission", "bedwars.cosmetics.vip");
        config.set("bed-effects.lightning.permission", "bedwars.cosmetics.vip");
        config.save(file);
        Player player = player("Tester");
        CosmeticsManager manager = new CosmeticsManager(plugin, database);
        manager.openCosmeticsMenu(player);
        manager.handleMenuClick(player, 12);
        manager.handleMenuClick(player, 29);
        World world = mock(World.class);
        Location location = new Location(world, 0, 70, 0);

        manager.applyKillEffect(player, location);
        manager.applyBedDestroyEffect(player, location);

        verify(messages, never()).sendMessage(player, "cosmetics.locked");
        verify(world).spawnParticle(Particle.HEART, location, 12, 0.5, 0.5, 0.5, 0.0);
        verify(world).strikeLightningEffect(location);

        config.set("testing.unlock-all", false);
        config.save(file);
        CosmeticsManager normal = new CosmeticsManager(plugin, database);
        clearInvocations(world);
        normal.applyKillEffect(player, location);
        normal.applyBedDestroyEffect(player, location);
        verifyNoInteractions(world);
    }

    @Test
    public void testingUnlockDoesNotPlayDisabledEffects() throws Exception {
        var file = folder.getRoot().toPath().resolve("cosmetics.yml").toFile();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("testing.unlock-all", true);
        config.set("kill-effects.heart.enabled", false);
        config.save(file);
        Player player = player("Tester");
        database.executeUpdate("INSERT INTO player_cosmetics VALUES (?, ?, ?, ?)",
                player.getUniqueId().toString(), "heart", "none", 1L);
        World world = mock(World.class);

        new CosmeticsManager(plugin, database).applyKillEffect(player, new Location(world, 0, 70, 0));

        verifyNoInteractions(world);
    }

    @Test
    public void testingUnlockBypassesEveryDefaultEffectPermission() throws Exception {
        var file = folder.getRoot().toPath().resolve("cosmetics.yml").toFile();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("testing.unlock-all", true);
        config.save(file);
        Player player = player("Tester"); // No permissions granted by this player double.
        CosmeticsManager manager = new CosmeticsManager(plugin, database);
        manager.openCosmeticsMenu(player);
        String[] kills = config.getConfigurationSection("kill-effects").getKeys(false).toArray(new String[0]);
        String[] beds = config.getConfigurationSection("bed-effects").getKeys(false).toArray(new String[0]);
        for (int index = 0; index < kills.length; index++) {
            assertFalse(config.getString("kill-effects." + kills[index] + ".permission").isEmpty());
            manager.handleMenuClick(player, 10 + index);
            try (ResultSet result = database.executeQuery("SELECT kill_effect FROM player_cosmetics WHERE uuid = ?",
                    player.getUniqueId().toString())) {
                assertTrue(result.next());
                assertEquals(kills[index], result.getString("kill_effect"));
            }
        }
        for (int index = 0; index < beds.length; index++) {
            assertFalse(config.getString("bed-effects." + beds[index] + ".permission").isEmpty());
            manager.handleMenuClick(player, 28 + index);
            try (ResultSet result = database.executeQuery("SELECT bed_effect FROM player_cosmetics WHERE uuid = ?",
                    player.getUniqueId().toString())) {
                assertTrue(result.next());
                assertEquals(beds[index], result.getString("bed_effect"));
            }
        }
        verify(messages, never()).sendMessage(player, "cosmetics.locked");
    }

    @Test
    public void defaultPermissionsAreDeclaredAndEnforcedWithoutTestingFlag() throws Exception {
        var config = YamlConfiguration.loadConfiguration(folder.getRoot().toPath().resolve("cosmetics.yml").toFile());
        assertFalse(config.getBoolean("testing.unlock-all"));
        try (var input = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(input);
            var permissions = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(input,
                    java.nio.charset.StandardCharsets.UTF_8));
            for (String section : List.of("kill-effects", "bed-effects")) {
                for (String id : config.getConfigurationSection(section).getKeys(false)) {
                    String node = config.getString(section + "." + id + ".permission");
                    assertNotNull(permissions.getConfigurationSection("permissions." + node));
                }
            }
        }
        CosmeticsManager manager = new CosmeticsManager(plugin, database);
        Player player = player("Restricted");
        manager.openCosmeticsMenu(player);
        manager.handleMenuClick(player, 12);
        verify(messages).sendMessage(player, "cosmetics.locked");
        assertEquals(0, count("player_cosmetics"));
    }

    @Test
    public void newEffectsPlayForKillsAndBedsWithTestingUnlockAndNoPermissions() throws Exception {
        var file = folder.getRoot().toPath().resolve("cosmetics.yml").toFile();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("testing.unlock-all", true);
        config.save(file);
        Player player = player("Tester");
        World world = mock(World.class);
        Location location = new Location(world, 0, 70, 0);
        for (String effect : List.of("flame", "soul", "portal", "enchant", "music", "totem")) {
            database.executeUpdate("INSERT OR REPLACE INTO player_cosmetics VALUES (?, ?, ?, ?)",
                    player.getUniqueId().toString(), effect, effect, 1L);
            CosmeticsManager manager = new CosmeticsManager(plugin, database);
            manager.applyKillEffect(player, location);
            manager.applyBedDestroyEffect(player, location);
        }
        verify(world, times(2)).spawnParticle(Particle.FLAME, location, 35, 0.6, 0.8, 0.6, 0.02);
        verify(world, times(2)).spawnParticle(Particle.SOUL_FIRE_FLAME, location, 30, 0.6, 0.8, 0.6, 0.02);
        verify(world, times(2)).spawnParticle(Particle.PORTAL, location, 60, 0.7, 0.8, 0.7, 0.3);
        verify(world, times(2)).spawnParticle(Particle.ENCHANTMENT_TABLE, location, 50, 0.8, 0.8, 0.8, 0.5);
        verify(world, times(2)).spawnParticle(Particle.NOTE, location, 16, 0.6, 0.8, 0.6, 0.0);
        verify(world, times(2)).spawnParticle(Particle.TOTEM, location, 45, 0.6, 0.8, 0.6, 0.1);
        verifyNoMoreInteractions(world);
    }

    @Test
    public void upgradeAddsNewEffectsWithoutOverwritingExistingSettings() throws Exception {
        var file = folder.getRoot().toPath().resolve("cosmetics.yml").toFile();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("testing.unlock-all", true);
        config.set("kill-effects.heart.permission", "custom.heart");
        config.set("kill-effects.smoke.enabled", false);
        for (String effect : List.of("flame", "soul", "portal", "enchant", "music", "totem")) {
            config.set("kill-effects." + effect, null);
            config.set("bed-effects." + effect, null);
        }
        config.save(file);
        when(plugin.getResource("cosmetics.yml"))
                .thenAnswer(call -> getClass().getClassLoader().getResourceAsStream("cosmetics.yml"));

        new CosmeticsManager(plugin, database);

        var updated = YamlConfiguration.loadConfiguration(file);
        assertTrue(updated.getBoolean("testing.unlock-all"));
        assertEquals("custom.heart", updated.getString("kill-effects.heart.permission"));
        assertFalse(updated.getBoolean("kill-effects.smoke.enabled"));
        for (String effect : List.of("flame", "soul", "portal", "enchant", "music", "totem")) {
            assertEquals("bedwars.cosmetics.kill." + effect,
                    updated.getString("kill-effects." + effect + ".permission"));
            assertEquals("bedwars.cosmetics.bed." + effect,
                    updated.getString("bed-effects." + effect + ".permission"));
        }
    }
}
