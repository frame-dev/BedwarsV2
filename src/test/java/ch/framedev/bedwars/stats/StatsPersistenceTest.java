package ch.framedev.bedwars.stats;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.utils.DebugLogger;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StatsPersistenceTest {
    private StatsManager manager;
    private final Deque<Runnable> tasks = new ArrayDeque<>();
    private final Map<String, Integer> storedWins = new HashMap<>();
    private final UUID uuid = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        BedWarsPlugin plugin = mock(BedWarsPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.executeQuery(anyString(), any(Object[].class))).thenAnswer(call -> {
            Object[] parameters = (Object[]) call.getRawArguments()[1];
            ResultSet result = mock(ResultSet.class);
            when(result.next()).thenReturn(true, false);
            when(result.getInt("wins")).thenReturn(storedWins.getOrDefault(parameters[0], 0));
            return result;
        });
        doAnswer(call -> {
            Object[] parameters = (Object[]) call.getRawArguments()[1];
            storedWins.put((String) parameters[0], (Integer) parameters[2]);
            return 1;
        }).when(database).executeUpdate(anyString(), any(Object[].class));
        manager = new StatsManager(plugin, database, tasks::addLast);
    }

    private PlayerStats load(UUID id) {
        CompletableFuture<PlayerStats> loaded = manager.loadPlayerStats(id);
        tasks.removeFirst().run();
        return loaded.join();
    }

    @Test
    public void savesForSamePlayerCannotOverwriteNewerSnapshotWithOlderOne() {
        PlayerStats stats = load(uuid);
        stats.addWin();
        CompletableFuture<Void> first = manager.savePlayerStats(uuid, "Player");
        stats.addWin();
        CompletableFuture<Void> second = manager.savePlayerStats(uuid, "Player");

        assertEquals(1, tasks.size()); // Only the first write may run until it finishes.
        tasks.removeFirst().run();
        assertTrue(first.isDone());
        assertFalse(second.isDone());
        assertEquals(Integer.valueOf(1), storedWins.get(uuid.toString()));
        tasks.removeFirst().run();
        second.join();
        assertEquals(Integer.valueOf(2), storedWins.get(uuid.toString()));
    }

    @Test
    public void reconnectLoadsOnlyAfterPendingQuitSaveCompletes() {
        PlayerStats stats = load(uuid);
        stats.addWin();
        manager.savePlayerStats(uuid, "Player");
        manager.clearCache(uuid);

        CompletableFuture<PlayerStats> reconnect = manager.loadPlayerStats(uuid);

        assertEquals(1, tasks.size());
        tasks.removeFirst().run();
        assertFalse(reconnect.isDone());
        tasks.removeFirst().run();
        assertEquals(1, reconnect.join().getWins());
    }

    @Test
    public void independentPlayersDoNotWaitOnEachOthersWrites() {
        load(uuid).addWin();
        UUID other = UUID.randomUUID();
        load(other).addWin();

        CompletableFuture<Void> first = manager.savePlayerStats(uuid, "First");
        CompletableFuture<Void> second = manager.savePlayerStats(other, "Second");

        assertEquals(2, tasks.size());
        tasks.removeLast().run();
        assertTrue(second.isDone());
        assertFalse(first.isDone());
        tasks.removeFirst().run();
        first.join();
    }

    @Test
    public void shutdownWaitsForQuitSavesEvenAfterCacheEviction() throws Exception {
        load(uuid).addWin();
        manager.savePlayerStats(uuid, "Player");
        manager.clearCache(uuid);
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<Void> shutdown = CompletableFuture.runAsync(() -> {
            started.countDown();
            manager.saveAllStats();
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        try {
            shutdown.get(200, TimeUnit.MILLISECONDS);
            fail("Shutdown returned with an unfinished save for an evicted player");
        } catch (TimeoutException expected) {
            // It must wait for the queued write.
        } finally {
            tasks.removeFirst().run();
        }
        shutdown.get(5, TimeUnit.SECONDS);
        assertEquals(Integer.valueOf(1), storedWins.get(uuid.toString()));
    }
}
