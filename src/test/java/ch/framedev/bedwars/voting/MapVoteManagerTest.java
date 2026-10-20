package ch.framedev.bedwars.voting;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.GameManager;
import ch.framedev.bedwars.utils.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MapVoteManagerTest {
    @Test
    public void manuallyEndingVoteStopsCountdownEvenWhenNoArenaWins() throws Exception {
        BedWarsPlugin plugin = mock(BedWarsPlugin.class);
        when(plugin.getMessageManager()).thenReturn(mock(MessageManager.class));
        GameManager games = mock(GameManager.class);
        when(plugin.getGameManager()).thenReturn(games);
        when(games.getGames()).thenReturn(List.of());
        MapVoteManager votes = new MapVoteManager(plugin);
        BukkitRunnable timer = mock(BukkitRunnable.class);
        Field activeField = MapVoteManager.class.getDeclaredField("votingActive");
        activeField.setAccessible(true);
        activeField.set(votes, true);
        Field taskField = MapVoteManager.class.getDeclaredField("voteTask");
        taskField.setAccessible(true);
        taskField.set(votes, timer);

        votes.forceEndVoting(mock(Player.class));

        verify(timer).cancel();
        assertFalse(votes.isVotingActive());
        assertNull(taskField.get(votes));
    }
}
