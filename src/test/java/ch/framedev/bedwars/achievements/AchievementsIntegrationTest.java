package ch.framedev.bedwars.achievements;

import ch.framedev.bedwars.support.PersistentFeatureTest;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AchievementsIntegrationTest extends PersistentFeatureTest {
    @Test
    public void allSevenDefaultAchievementsTrackAndUnlockExactlyOnce() throws Exception {
        AchievementsManager manager = new AchievementsManager(plugin, database);
        Player player = player("Player");
        for (int event = 0; event < 30; event++) {
            manager.recordKill(player.getUniqueId());
            manager.recordFinalKill(player.getUniqueId());
            manager.recordBedBroken(player.getUniqueId());
            manager.recordWin(player.getUniqueId());
            manager.recordGamePlayed(player.getUniqueId());
        }
        Map<String, Integer> targets = Map.of("first_kill", 1, "killer_10", 10, "final_kill", 1,
                "bed_breaker", 1, "bed_breaker_10", 10, "first_win", 1, "veteran", 25);
        assertEquals(7, count("player_achievements"));
        for (var entry : targets.entrySet()) {
            try (ResultSet result = database.executeQuery(
                    "SELECT progress, unlocked_at FROM player_achievements WHERE uuid = ? AND achievement_id = ?",
                    player.getUniqueId().toString(), entry.getKey())) {
                assertTrue(entry.getKey(), result.next());
                assertEquals(entry.getValue().intValue(), result.getInt("progress"));
                assertTrue(result.getLong("unlocked_at") > 0);
            }
        }
        verify(messages, times(7)).sendMessage(eq(player), eq("achievements.unlocked"), anyString());
    }

    @Test
    public void eventBeforeAsyncLoadAddsToSavedProgressInsteadOfLosingIt() throws Exception {
        Player player = player("Player");
        database.executeUpdate("INSERT INTO player_achievements VALUES (?, ?, ?, ?, ?)",
                player.getUniqueId().toString(), "killer_10", 9, 0L, 1L);
        AchievementsManager manager = new AchievementsManager(plugin, database);
        manager.loadPlayer(player.getUniqueId());

        manager.recordKill(player.getUniqueId());
        asyncLoads.get(0).run();

        try (ResultSet result = database.executeQuery(
                "SELECT progress, unlocked_at FROM player_achievements WHERE achievement_id = 'killer_10'")) {
            assertTrue(result.next());
            assertEquals(10, result.getInt("progress"));
            assertTrue(result.getLong("unlocked_at") > 0);
        }
        AchievementsManager reloaded = new AchievementsManager(plugin, database);
        reloaded.recordKill(player.getUniqueId());
        verify(messages, times(1)).sendMessage(player, "achievements.unlocked", "\u00a7cKiller I");
    }

    @Test
    public void lateOlderSaveCannotReduceProgressOrRelockAchievement() throws Exception {
        AchievementsManager manager = new AchievementsManager(plugin, database);
        Player player = player("Player");
        Method save = AchievementsManager.class.getDeclaredMethod("saveToDatabase",
                java.util.UUID.class, String.class, AchievementProgress.class);
        save.setAccessible(true);
        save.invoke(manager, player.getUniqueId(), "killer_10", new AchievementProgress(10, 123L));
        save.invoke(manager, player.getUniqueId(), "killer_10", new AchievementProgress(3, 0L));

        try (ResultSet result = database.executeQuery("SELECT progress, unlocked_at FROM player_achievements")) {
            assertTrue(result.next());
            assertEquals(10, result.getInt("progress"));
            assertEquals(123L, result.getLong("unlocked_at"));
        }
    }

    @Test
    public void enabledAsyncSavesFinishBeforeShutdownReturns() throws Exception {
        AchievementsManager manager = new AchievementsManager(plugin, database);
        Player player = player("Player");
        when(plugin.isEnabled()).thenReturn(true);
        manager.recordKill(player.getUniqueId());
        manager.shutdown();

        assertEquals(2, count("player_achievements"));
    }

    @Test
    public void allDefaultAchievementsOccupyTheConfiguredMenuSlots() {
        AchievementsManager manager = new AchievementsManager(plugin, database);
        Player player = player("Player");
        org.bukkit.inventory.Inventory inventory = mock(org.bukkit.inventory.Inventory.class);
        bukkit.when(() -> org.bukkit.Bukkit.createInventory(isNull(), eq(54), anyString())).thenReturn(inventory);

        manager.openMenu(player);

        for (int slot = 10; slot <= 16; slot++) verify(inventory).setItem(eq(slot), any());
        manager.handleMenuClick(player, 10);
        verify(messages).sendMessage(player, "achievements.progress", "\u00a7aFirst Blood", 0, 1);
    }
}
