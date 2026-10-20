package ch.framedev.bedwars.game;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.bungee.BungeeManager;
import ch.framedev.bedwars.listeners.PlayerQuitListener;
import ch.framedev.bedwars.generators.ResourceGenerator;
import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.stats.PlayerStats;
import ch.framedev.bedwars.stats.StatsManager;
import ch.framedev.bedwars.team.TeamColor;
import ch.framedev.bedwars.utils.DebugLogger;
import ch.framedev.bedwars.utils.MessageManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.*;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GameLifecycleTest {
    private MockedStatic<Bukkit> bukkit;
    private BedWarsPlugin plugin;
    private GameManager manager;
    private BukkitScheduler scheduler;
    private Game game;
    private World world;
    private Location lobby;
    private final Map<UUID, Player> online = new HashMap<>();
    private final List<Runnable> timers = new ArrayList<>();
    private final List<Runnable> delayed = new ArrayList<>();
    private final Map<UUID, PlayerStats> savedStats = new HashMap<>();
    private int nextTaskId;

    @Before
    public void setUp() {
        plugin = mock(BedWarsPlugin.class);
        manager = mock(GameManager.class);
        scheduler = mock(BukkitScheduler.class);
        world = mock(World.class);
        when(world.getName()).thenReturn("arena");
        lobby = new Location(world, 0, 100, 0);
        YamlConfiguration config = new YamlConfiguration();
        config.set("scoreboard.enabled", false);
        config.set("bungeecord.send-to-lobby-on-end", false);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        when(plugin.getMessageManager()).thenReturn(mock(MessageManager.class));
        when(plugin.getGameManager()).thenReturn(manager);
        when(plugin.getUpgradeManager()).thenReturn(mock(UpgradeManager.class));
        doAnswer(call -> {
            when(manager.getPlayerGame(call.getArgument(0, Player.class))).thenReturn(null);
            return null;
        }).when(manager).removePlayerFromGame(any(Player.class));
        when(plugin.getBungeeManager()).thenReturn(mock(BungeeManager.class));
        StatsManager stats = mock(StatsManager.class);
        when(stats.getPlayerStats(any())).thenAnswer(call ->
                savedStats.computeIfAbsent(call.getArgument(0), PlayerStats::new));
        when(plugin.getStatsManager()).thenReturn(stats);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(call -> online.values());
        bukkit.when(() -> Bukkit.getPlayer(any(UUID.class)))
                .thenAnswer(call -> online.get(call.getArgument(0)));
        bukkit.when(() -> Bukkit.getWorld("arena")).thenReturn(world);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                .thenAnswer(call -> {
                    timers.add(call.getArgument(1));
                    return task();
                });
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong()))
                .thenAnswer(call -> {
                    delayed.add(call.getArgument(1));
                    return task();
                });
        Arena arena = new Arena("test", lobby, lobby.clone().add(0, 10, 0), 2, 8);
        arena.setTeamSpawn(TeamColor.RED, lobby.clone().add(10, 0, 0));
        arena.setTeamSpawn(TeamColor.BLUE, lobby.clone().add(-10, 0, 0));
        arena.setTeamSpawn(TeamColor.GREEN, lobby.clone().add(0, 0, 10));
        game = new Game(plugin, arena);
    }

    private BukkitTask task() {
        BukkitTask task = mock(BukkitTask.class);
        when(task.getTaskId()).thenReturn(++nextTaskId);
        return task;
    }

    @SuppressWarnings("unchecked")
    private Player participant(TeamColor color) {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn(color.name());
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        when(player.getActivePotionEffects()).thenReturn(Collections.emptyList());
        when(manager.getPlayerGame(player)).thenReturn(game);
        GamePlayer gp = new GamePlayer(player);
        gp.setTeam(game.getTeams().get(color));
        gp.getTeam().addPlayer(gp);
        game.getPlayers().put(id, gp);
        try {
            Field field = Game.class.getDeclaredField("participants");
            field.setAccessible(true);
            ((Map<UUID, GamePlayer>) field.get(game)).put(id, gp);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        online.put(id, player);
        return player;
    }

    private void state(GameState state) throws Exception {
        Field field = Game.class.getDeclaredField("state");
        field.setAccessible(true);
        field.set(game, state);
    }

    @After
    public void tearDown() {
        bukkit.close();
    }

    @Test
    public void leavingDuringRespawnCancelsTimerAndRemovesTeamMembership() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);
        participant(TeamColor.BLUE);
        participant(TeamColor.GREEN);
        game.handlePlayerDeath(player);
        assertTrue(game.isSpectator(player));

        game.removeSpectator(player);

        assertNull(game.getGamePlayer(player));
        assertFalse(game.isSpectator(player));
        assertFalse(game.getTeams().get(TeamColor.RED).hasAlivePlayers());
        verify(scheduler).cancelTask(1);
        verify(manager).removePlayerFromGame(player);
        assertEquals(GameState.RUNNING, game.getState());
    }

    @Test
    public void quittingDuringRespawnCanFinishTheMatch() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);
        participant(TeamColor.BLUE);
        game.handlePlayerDeath(player);

        new PlayerQuitListener(plugin).onPlayerQuit(new PlayerQuitEvent(player, ""));

        assertNull(game.getGamePlayer(player));
        assertFalse(game.getTeams().get(TeamColor.RED).hasAlivePlayers());
        assertEquals(GameState.ENDING, game.getState());
        verify(scheduler).cancelTask(1);
    }

    @Test
    public void staleRespawnCallbackCannotTeleportPlayerIntoAnotherGame() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);
        game.handlePlayerDeath(player);
        clearInvocations(player);
        when(manager.getPlayerGame(player)).thenReturn(mock(Game.class));

        timers.get(0).run();

        verify(player, never()).teleport(any(Location.class));
        verify(player, never()).setGameMode(GameMode.SURVIVAL);
        verify(scheduler).cancelTask(1);
    }

    @Test
    public void shutdownCleansWaitingPlayersWithoutRecordingLossesOrSchedulingTasks() {
        Player player = participant(TeamColor.RED);

        game.shutdown();

        assertTrue(game.getPlayers().isEmpty());
        verify(player).teleport(lobby);
        verify(player).closeInventory();
        verify(manager).removePlayerFromGame(player);
        verifyNoInteractions(scheduler, plugin.getStatsManager());
    }

    @Test
    public void shutdownOfRunningMatchRestoresWorldImmediately() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(world, 5, 70, 5));
        when(world.getBlockAt(5, 70, 5)).thenReturn(block);
        game.getWorldResetManager().recordPlacedBlock(block);

        game.shutdown();

        verify(block).setType(Material.AIR, false);
        verify(player).teleport(lobby);
        assertTrue(game.getPlayers().isEmpty());
        assertEquals(0, game.getWorldResetManager().getBlockChangeCount());
        assertTrue(delayed.isEmpty());
    }

    @Test
    public void shutdownDuringEndingCancelsDelayedCleanupAndCleansSpectators() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);
        game.getSpectators().add(player.getUniqueId());
        game.endGame(null);
        assertEquals(GameState.ENDING, game.getState());
        assertEquals(1, delayed.size());

        game.shutdown();

        verify(scheduler).cancelTask(1);
        verify(player, times(1)).teleport(lobby);
        assertTrue(game.getPlayers().isEmpty());
        assertTrue(game.getSpectators().isEmpty());
        assertEquals(1, savedStats.get(player.getUniqueId()).getGamesPlayed());
    }

    @Test
    public void repeatedDeathHandlingDoesNotCountOrScheduleTwice() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);

        game.handlePlayerDeath(player);
        game.handlePlayerDeath(player);

        assertEquals(1, game.getGamePlayer(player).getDeaths());
        assertEquals(1, timers.size());
    }

    @Test
    public void ordinaryMatchEndKeepsTheFiveSecondDelayAndRecordsStatsOnce() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);
        game.getGamePlayer(player).addKill();

        game.endGame(game.getTeams().get(TeamColor.RED));
        game.endGame(game.getTeams().get(TeamColor.RED));

        assertEquals(GameState.ENDING, game.getState());
        assertNotNull(game.getGamePlayer(player));
        verify(player, never()).teleport(any(Location.class));
        assertEquals(1, savedStats.get(player.getUniqueId()).getWins());
        assertEquals(1, savedStats.get(player.getUniqueId()).getKills());
        assertEquals(1, delayed.size());
        delayed.get(0).run();
        assertTrue(game.getPlayers().isEmpty());
        assertEquals(GameState.WAITING, game.getState());
        verify(player).teleport(lobby);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void eliminatedParticipantLeavingUsesTheJoinSignReturnLocation() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);
        participant(TeamColor.BLUE);
        participant(TeamColor.GREEN);
        game.getGamePlayer(player).setEliminated(true);
        game.getTeams().get(TeamColor.RED).removePlayer(game.getGamePlayer(player));
        game.getSpectators().add(player.getUniqueId());
        Location returnLocation = lobby.clone().add(50, 0, 50);
        Field field = Game.class.getDeclaredField("returnLocations");
        field.setAccessible(true);
        ((Map<UUID, Location>) field.get(game)).put(player.getUniqueId(), returnLocation);

        game.removeSpectator(player);

        verify(player).teleport(returnLocation);
        assertNull(game.getGamePlayer(player));
        assertFalse(game.isSpectator(player));
    }

    @SuppressWarnings("unchecked")
    private void returnToSign(Player player, Location location) throws Exception {
        Field field = Game.class.getDeclaredField("returnLocations");
        field.setAccessible(true);
        ((Map<UUID, Location>) field.get(game)).put(player.getUniqueId(), location);
    }

    @Test
    public void matchEndReturnsWinnersRespawningPlayersEliminatedPlayersAndVisitors() throws Exception {
        state(GameState.RUNNING);
        Player winner = participant(TeamColor.RED);
        Player respawning = participant(TeamColor.BLUE);
        Player eliminated = participant(TeamColor.GREEN);
        Location winnerSign = lobby.clone().add(50, 0, 50);
        Location respawningSign = lobby.clone().add(-50, 0, -50);
        returnToSign(winner, winnerSign);
        returnToSign(respawning, respawningSign);
        game.handlePlayerDeath(respawning);
        game.getGamePlayer(eliminated).setEliminated(true);
        game.getTeams().get(TeamColor.GREEN).removePlayer(game.getGamePlayer(eliminated));
        game.getSpectators().add(eliminated.getUniqueId());
        Player visitor = mock(Player.class);
        UUID visitorId = UUID.randomUUID();
        when(visitor.getUniqueId()).thenReturn(visitorId);
        when(visitor.getInventory()).thenReturn(mock(PlayerInventory.class));
        when(visitor.getActivePotionEffects()).thenReturn(Collections.emptyList());
        when(manager.getPlayerGame(visitor)).thenReturn(game);
        online.put(visitorId, visitor);
        game.getSpectators().add(visitorId);
        clearInvocations(respawning, respawning.getInventory());

        game.endGame(game.getTeams().get(TeamColor.RED));
        delayed.get(0).run();

        verify(winner).teleport(winnerSign);
        verify(respawning).teleport(respawningSign);
        verify(eliminated).teleport(lobby);
        verify(visitor).teleport(lobby);
        verify(scheduler).cancelTask(1);
        for (Player player : List.of(winner, respawning, eliminated, visitor)) {
            verify(manager).removePlayerFromGame(player);
            verify(player).setGameMode(GameMode.SURVIVAL);
            verify(player.getInventory()).clear();
            assertNull(manager.getPlayerGame(player));
        }
        assertTrue(game.getPlayers().isEmpty());
        assertTrue(game.getSpectators().isEmpty());
        assertEquals(GameState.WAITING, game.getState());
    }

    @Test
    public void proxyEnabledStillTeleportsLocallyAndPreservesSignDestination() throws Exception {
        state(GameState.RUNNING);
        plugin.getConfig().set("bungeecord.send-to-lobby-on-end", true);
        when(plugin.getBungeeManager().isEnabled()).thenReturn(true);
        Player normal = participant(TeamColor.RED);
        Player signJoined = participant(TeamColor.BLUE);
        Location sign = lobby.clone().add(50, 0, 50);
        returnToSign(signJoined, sign);

        game.endGame(null);
        delayed.get(0).run();

        verify(normal).teleport(lobby);
        verify(signJoined).teleport(sign);
        assertEquals(2, delayed.size()); // Match cleanup plus only the non-sign player's transfer.
        delayed.get(1).run();
        verify(plugin.getBungeeManager()).sendPlayerToLobby(normal);
        verify(plugin.getBungeeManager(), never()).sendPlayerToLobby(signJoined);
    }

    @Test
    public void delayedProxyTransferDoesNotRemovePlayerFromNewMatch() throws Exception {
        state(GameState.RUNNING);
        plugin.getConfig().set("bungeecord.send-to-lobby-on-end", true);
        when(plugin.getBungeeManager().isEnabled()).thenReturn(true);
        Player player = participant(TeamColor.RED);
        game.endGame(null);
        delayed.get(0).run();
        when(manager.getPlayerGame(player)).thenReturn(mock(Game.class));

        delayed.get(1).run();

        verify(plugin.getBungeeManager(), never()).sendPlayerToLobby(player);
    }

    @Test
    public void deadPlayerIsRespawnedBeforeEndTeleport() throws Exception {
        state(GameState.RUNNING);
        Player player = participant(TeamColor.RED);
        when(player.isDead()).thenReturn(true);
        when(player.spigot()).thenReturn(mock(Player.Spigot.class));

        game.endGame(null);
        delayed.get(0).run();

        org.mockito.InOrder order = inOrder(player.spigot(), player);
        order.verify(player.spigot()).respawn();
        order.verify(player).teleport(lobby);
        assertEquals(GameState.WAITING, game.getState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void matchCleanupResetsGeneratorTiers() throws Exception {
        Field field = Game.class.getDeclaredField("generators");
        field.setAccessible(true);
        ResourceGenerator generator = mock(ResourceGenerator.class);
        ((List<ResourceGenerator>) field.get(game)).add(generator);

        game.shutdown();

        verify(generator).reset();
    }

    @Test
    public void customStartingWeaponIsRespectedAtStartAndRespawn() {
        List<Material> materials = startAndRespawnWith("DIAMOND_SWORD");
        assertEquals(2, Collections.frequency(materials, Material.DIAMOND_SWORD));
        assertFalse(materials.contains(Material.WOODEN_SWORD));
    }

    @Test
    public void disabledStartingItemDoesNotGiveWoodenSwordAtStartOrRespawn() {
        List<Material> materials = startAndRespawnWith("AIR");
        assertFalse(materials.contains(Material.WOODEN_SWORD));
        assertFalse(materials.contains(Material.AIR));
        assertEquals(8, materials.size()); // Four armor pieces for each loadout.
    }

    @Test
    public void matchAwardsWinAndGamePlayedToCorrectParticipantsOnce() throws Exception {
        state(GameState.RUNNING);
        Player winner = participant(TeamColor.RED);
        Player loser = participant(TeamColor.BLUE);
        var achievements = mock(ch.framedev.bedwars.achievements.AchievementsManager.class);
        when(plugin.getAchievementsManager()).thenReturn(achievements);

        game.endGame(game.getTeams().get(TeamColor.RED));
        game.endGame(game.getTeams().get(TeamColor.RED));

        verify(achievements).recordWin(winner.getUniqueId());
        verify(achievements, never()).recordWin(loser.getUniqueId());
        verify(achievements).recordGamePlayed(winner.getUniqueId());
        verify(achievements).recordGamePlayed(loser.getUniqueId());
    }

    @SuppressWarnings("try")
    private List<Material> startAndRespawnWith(String configuredItem) {
        plugin.getConfig().set("game.starting-item", configuredItem);
        Player player = participant(TeamColor.RED);
        List<Material> materials = new ArrayList<>();
        try (MockedConstruction<ItemStack> items = mockConstruction(ItemStack.class, (item, context) -> {
            Material material = (Material) context.arguments().get(0);
            materials.add(material);
            when(item.getType()).thenReturn(material);
        })) {
            game.forceStart();
            game.handlePlayerDeath(player);
            Runnable respawn = timers.get(timers.size() - 1);
            for (int tick = 0; tick <= 5; tick++) respawn.run();
        }
        assertFalse(game.isSpectator(player));
        return materials;
    }
}
