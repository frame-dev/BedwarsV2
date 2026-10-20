package ch.framedev.bedwars.achievements;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class AchievementsShutdownTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shutdownProgressSavesWithoutSchedulingOnDisabledPlugin() throws Exception {
        BedWarsPlugin plugin = mock(BedWarsPlugin.class);
        when(plugin.getDataFolder()).thenReturn(temporaryFolder.getRoot());
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        when(plugin.isEnabled()).thenReturn(false);
        Files.writeString(temporaryFolder.getRoot().toPath().resolve("achievements.yml"), """
                enabled: true
                achievements:
                  games:
                    type: GAMES_PLAYED
                    target: 10
                """);
        DatabaseManager database = mock(DatabaseManager.class);
        java.sql.ResultSet empty = mock(java.sql.ResultSet.class);
        when(database.executeQuery(anyString(), any(Object[].class))).thenReturn(empty);
        AchievementsManager achievements = new AchievementsManager(plugin, database);
        UUID uuid = UUID.randomUUID();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            achievements.recordGamePlayed(uuid);

            verify(database).executeUpdate(anyString(), eq(uuid.toString()), eq("games"),
                    eq(1), eq(0L), anyLong());
            bukkit.verifyNoInteractions();
        }
    }
}
