package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameManager;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.player.GamePlayer;
import ch.framedev.bedwars.team.Team;
import ch.framedev.bedwars.team.TeamColor;
import ch.framedev.bedwars.utils.DebugLogger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class PlayerDeathListenerTest {
    private BedWarsPlugin plugin;
    private Game game;
    private Player victim;
    private PlayerDeathEvent event;
    private BukkitScheduler scheduler;

    @Before
    public void setUp() {
        plugin = mock(BedWarsPlugin.class);
        game = mock(Game.class);
        victim = mock(Player.class);
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        when(victim.isOnline()).thenReturn(true);
        when(victim.spigot()).thenReturn(mock(Player.Spigot.class));
        GameManager manager = mock(GameManager.class);
        when(manager.getPlayerGame(victim)).thenReturn(game);
        when(plugin.getGameManager()).thenReturn(manager);
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        when(game.getState()).thenReturn(GameState.RUNNING);
        Server server = mock(Server.class);
        scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        event = mock(PlayerDeathEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDrops()).thenReturn(new ArrayList<>());
    }

    @Test
    public void missingTeamStillRespawnsAfterDeathEventCompletes() {
        new PlayerDeathListener(plugin).onPlayerDeath(event);
        runScheduledRespawn();
    }

    @Test
    public void killerOutsideGameStillUsesDeferredRespawn() {
        GamePlayer gp = mock(GamePlayer.class);
        Team team = mock(Team.class);
        when(team.getColor()).thenReturn(TeamColor.RED);
        when(gp.getTeam()).thenReturn(team);
        when(game.getGamePlayer(victim)).thenReturn(gp);
        when(victim.getKiller()).thenReturn(mock(Player.class));

        new PlayerDeathListener(plugin).onPlayerDeath(event);
        runScheduledRespawn();
    }

    @Test
    public void pendingDeathCallbackDoesNotAffectPlayerWhoLeftGame() {
        new PlayerDeathListener(plugin).onPlayerDeath(event);
        when(plugin.getGameManager().getPlayerGame(victim)).thenReturn(null);
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), callback.capture());

        callback.getValue().run();

        verify(game, never()).handlePlayerDeath(victim);
        verify(victim.spigot(), never()).respawn();
    }

    private void runScheduledRespawn() {
        verify(game, never()).handlePlayerDeath(victim);
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), callback.capture());
        callback.getValue().run();
        InOrder order = inOrder(victim.spigot(), game);
        order.verify(victim.spigot()).respawn();
        order.verify(game).handlePlayerDeath(victim);
    }
}
