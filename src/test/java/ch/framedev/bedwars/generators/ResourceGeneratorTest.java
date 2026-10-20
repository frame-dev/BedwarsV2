package ch.framedev.bedwars.generators;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

public class ResourceGeneratorTest {
    @Test
    public void nextRoundStartsAtOriginalTierAfterPreviousRoundUpgrade() {
        assertResetPeriod(1, 600L);
    }

    @Test
    public void resetPreservesExplicitInitialTier() {
        assertResetPeriod(2, 400L);
    }

    private void assertResetPeriod(int initialTier, long initialPeriod) {
        Plugin plugin = mock(Plugin.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask first = mock(BukkitTask.class);
        BukkitTask upgraded = mock(BukkitTask.class);
        BukkitTask nextRound = mock(BukkitTask.class);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(first, upgraded, nextRound);
        ResourceGenerator generator = new ResourceGenerator(new Location(mock(World.class), 0, 70, 0),
                ResourceGenerator.ResourceType.DIAMOND, initialTier, 600, 400, 1, 16);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            generator.start(plugin);
            generator.upgrade();
            generator.reset();
            generator.start(plugin);

            org.mockito.InOrder order = inOrder(scheduler, first, upgraded);
            order.verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(initialPeriod), eq(initialPeriod));
            order.verify(first).cancel();
            order.verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(400L), eq(400L));
            order.verify(upgraded).cancel();
            order.verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(initialPeriod), eq(initialPeriod));
        }
    }
}
