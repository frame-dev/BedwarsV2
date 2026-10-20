package ch.framedev.bedwars.commands;

import ch.framedev.bedwars.arena.ArenaManager;
import ch.framedev.bedwars.game.*;
import ch.framedev.bedwars.party.PartyManager;
import ch.framedev.bedwars.support.PersistentFeatureTest;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class PartyCommandsTest extends PersistentFeatureTest {
    private ImprovedBedWarsCommand commands;
    private Game game;
    private GameManager games;
    private Player leader;
    private Player member;

    @Before
    public void prepareParty() {
        PartyManager parties = new PartyManager(plugin, database);
        when(plugin.getPartyManager()).thenReturn(parties);
        leader = player("Leader");
        member = player("Member");
        when(leader.hasPermission(anyString())).thenReturn(true);
        when(member.hasPermission(anyString())).thenReturn(true);
        parties.invitePlayer(leader, member);
        parties.acceptInvite(member, "Leader");
        games = mock(GameManager.class);
        game = mock(Game.class);
        Arena arena = mock(Arena.class);
        when(arena.getName()).thenReturn("sky");
        when(arena.getMaxPlayers()).thenReturn(8);
        when(game.getArena()).thenReturn(arena);
        when(game.getPlayers()).thenReturn(Map.of());
        when(games.getGame("sky")).thenReturn(game);
        when(plugin.getGameManager()).thenReturn(games);
        Server server = mock(Server.class);
        when(server.getPlayer(any(UUID.class))).thenAnswer(call -> players.get(call.getArgument(0)));
        when(plugin.getServer()).thenReturn(server);
        commands = new ImprovedBedWarsCommand(plugin, mock(ArenaManager.class));
    }

    private void command(Player player, String... args) {
        commands.onCommand(player, mock(Command.class), "bw", args);
    }

    @Test
    public void leaderJoinAddsAllOnlineMembers() {
        command(leader, "join", "sky");
        verify(game).addPlayer(leader);
        verify(game).addPlayer(member);
    }

    @Test
    public void ordinaryMemberCannotJoinArenaForWholeParty() {
        command(member, "join", "sky");
        verify(game, never()).addPlayer(any(Player.class));
        verify(messages).sendMessage(member, "party.only-leader");
    }

    @Test
    public void memberLeavingDoesNotRemoveLeaderFromMatch() {
        when(games.getPlayerGame(leader)).thenReturn(game);
        when(games.getPlayerGame(member)).thenReturn(game);
        command(member, "leave");
        verify(game).removePlayer(member);
        verify(game, never()).removePlayer(leader);
    }

    @Test
    public void leaderLeavingRemovesMembersInSameMatch() {
        when(games.getPlayerGame(leader)).thenReturn(game);
        when(games.getPlayerGame(member)).thenReturn(game);
        command(leader, "leave");
        verify(game).removePlayer(leader);
        verify(game).removePlayer(member);
    }

    @Test
    public void insufficientCapacityRejectsWholePartyWithoutPartialJoin() {
        when(game.getArena().getMaxPlayers()).thenReturn(1);
        command(leader, "join", "sky");
        verify(game, never()).addPlayer(any(Player.class));
        verify(messages).sendMessage(leader, "party.party-game-full");
    }
}
