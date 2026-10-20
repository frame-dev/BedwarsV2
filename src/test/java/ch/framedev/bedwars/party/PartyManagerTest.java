package ch.framedev.bedwars.party;

import ch.framedev.bedwars.support.PersistentFeatureTest;
import org.bukkit.entity.Player;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PartyManagerTest extends PersistentFeatureTest {
    @Test
    public void inviteAcceptPromoteAndReloadKeepMembershipAndSingleLeader() {
        PartyManager manager = new PartyManager(plugin, database);
        Player leader = player("Leader");
        Player member = player("Member");
        manager.invitePlayer(leader, member);
        manager.acceptInvite(member, "Leader");
        Party party = manager.getParty(leader.getUniqueId());
        assertEquals(2, party.getSize());
        manager.promoteMember(leader, member);
        assertEquals(Party.PartyRole.MEMBER, party.getRole(leader.getUniqueId()));
        assertEquals(Party.PartyRole.LEADER, party.getRole(member.getUniqueId()));

        PartyManager reloaded = new PartyManager(plugin, database);
        reloaded.loadParties();
        assertTrue(reloaded.isLeader(member.getUniqueId()));
        assertFalse(reloaded.isLeader(leader.getUniqueId()));
        assertEquals(2, reloaded.getParty(member.getUniqueId()).getSize());
    }

    @Test
    public void leaderCannotKickSelfAndCanTransferLeadershipByLeaving() {
        PartyManager manager = new PartyManager(plugin, database);
        Player leader = player("Leader");
        Player member = player("Member");
        manager.invitePlayer(leader, member);
        manager.acceptInvite(member, "Leader");

        manager.kickMember(leader, leader);
        assertTrue(manager.isLeader(leader.getUniqueId()));
        assertEquals(2, manager.getParty(leader.getUniqueId()).getSize());
        verify(messages).sendMessage(leader, "party.cannot-kick-self");
        manager.leaveParty(leader);
        assertNull(manager.getParty(leader.getUniqueId()));
        assertTrue(manager.isLeader(member.getUniqueId()));
    }

    @Test
    public void disbandRemovesInvitesAndCannotBeAcceptedLater() throws Exception {
        PartyManager manager = new PartyManager(plugin, database);
        Player leader = player("Leader");
        Player target = player("Target");
        manager.invitePlayer(leader, target);
        assertEquals(1, count("party_invites"));

        manager.disbandParty(leader);
        manager.acceptInvite(target, "Leader");

        assertNull(manager.getParty(target.getUniqueId()));
        assertEquals(0, count("parties"));
        assertEquals(0, count("party_members"));
        assertEquals(0, count("party_invites"));
    }

    @Test
    public void formerLeaderInvitationIsRejectedAfterPromotion() {
        PartyManager manager = new PartyManager(plugin, database);
        Player leader = player("Leader");
        Player member = player("Member");
        Player target = player("Target");
        manager.invitePlayer(leader, member);
        manager.acceptInvite(member, "Leader");
        manager.invitePlayer(leader, target);
        manager.promoteMember(leader, member);

        manager.acceptInvite(target, "Leader");

        assertNull(manager.getParty(target.getUniqueId()));
        verify(messages).sendMessage(target, "party.invite-expired");
    }

    @Test
    public void fullPartyAndNonLeaderInvitesAreRejected() {
        plugin.getConfig().set("party.max-size", 2);
        PartyManager manager = new PartyManager(plugin, database);
        Player leader = player("Leader");
        Player member = player("Member");
        Player target = player("Target");
        manager.invitePlayer(leader, member);
        manager.acceptInvite(member, "Leader");

        manager.invitePlayer(leader, target);
        manager.invitePlayer(member, target);

        verify(messages).sendMessage(leader, "party.party-full");
        verify(messages).sendMessage(member, "party.only-leader");
        assertNull(manager.getParty(target.getUniqueId()));
    }

    @Test
    public void expiredInvitationCannotBeAcceptedAfterManagerReload() throws Exception {
        PartyManager manager = new PartyManager(plugin, database);
        Player leader = player("Leader");
        Player target = player("Target");
        manager.invitePlayer(leader, target);
        database.executeUpdate("UPDATE party_invites SET expires_at = 1");
        PartyManager reloaded = new PartyManager(plugin, database);

        reloaded.acceptInvite(target, "Leader");

        assertNull(reloaded.getParty(target.getUniqueId()));
        assertEquals(0, count("party_invites"));
    }

    @Test
    public void denyAndKickArePersistedAcrossReload() {
        PartyManager manager = new PartyManager(plugin, database);
        Player leader = player("Leader");
        Player member = player("Member");
        manager.invitePlayer(leader, member);
        manager.denyInvite(member, "Leader");
        manager.acceptInvite(member, "Leader");
        assertNull(manager.getParty(member.getUniqueId()));
        manager.invitePlayer(leader, member);
        manager.acceptInvite(member, "Leader");
        manager.kickMember(leader, member);
        PartyManager reloaded = new PartyManager(plugin, database);
        reloaded.loadParties();
        assertNull(reloaded.getParty(member.getUniqueId()));
        assertEquals(1, reloaded.getParty(leader.getUniqueId()).getSize());
    }
}
