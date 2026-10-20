package ch.framedev.bedwars.support;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.utils.DebugLogger;
import ch.framedev.bedwars.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;

import java.nio.file.Files;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class PersistentFeatureTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    protected BedWarsPlugin plugin;
    protected DatabaseManager database;
    protected MessageManager messages;
    protected BukkitScheduler scheduler;
    protected final List<Runnable> asyncLoads = new ArrayList<>();
    protected final Map<UUID, Player> players = new HashMap<>();
    protected MockedStatic<Bukkit> bukkit;
    private MockedConstruction<ItemStack> stacks;

    @Before
    public void prepareDatabase() throws Exception {
        plugin = mock(BedWarsPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder.getRoot());
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        messages = mock(MessageManager.class);
        when(plugin.getMessageManager()).thenReturn(messages);
        for (String name : List.of("achievements.yml", "cosmetics.yml")) {
            try (var input = getClass().getClassLoader().getResourceAsStream(name)) {
                assertNotNull(input);
                Files.copy(input, folder.getRoot().toPath().resolve(name));
            }
        }
        database = new DatabaseManager(plugin);
        database.connect();
        assertTrue(database.isConnected());
        bukkit = mockStatic(Bukkit.class);
        scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.getPlayer(any(UUID.class)))
                .thenAnswer(call -> players.get(call.getArgument(0)));
        bukkit.when(() -> Bukkit.createInventory(isNull(), anyInt(), anyString()))
                .thenAnswer(call -> mock(Inventory.class));
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
            asyncLoads.add(call.getArgument(1));
            return null;
        });
        stacks = mockConstruction(ItemStack.class, (item, context) ->
                when(item.getItemMeta()).thenReturn(mock(ItemMeta.class)));
    }

    protected Player player(String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        players.put(player.getUniqueId(), player);
        return player;
    }

    protected int count(String table) throws Exception {
        try (ResultSet result = database.executeQuery("SELECT COUNT(*) AS total FROM " + table)) {
            assertTrue(result.next());
            return result.getInt("total");
        }
    }

    @After
    public void closeDatabase() {
        if (stacks != null) stacks.close();
        if (bukkit != null) bukkit.close();
        if (database != null) database.disconnect();
    }
}
