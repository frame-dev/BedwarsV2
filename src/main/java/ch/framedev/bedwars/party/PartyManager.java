package ch.framedev.bedwars.party;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.database.DatabaseManager;
import ch.framedev.bedwars.party.Party.PartyRole;
import ch.framedev.bedwars.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages party creation, invites, and persistence.
 */
public class PartyManager {

    private final BedWarsPlugin plugin;
    private final DatabaseManager database;
    private final Map<UUID, Party> parties;
    private final Map<UUID, UUID> playerToParty;
    private final Map<UUID, PartyInvite> invites;

    public PartyManager(BedWarsPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.parties = new HashMap<>();
        this.playerToParty = new HashMap<>();
        this.invites = new HashMap<>();
    }

    public void loadParties() {
        parties.clear();
        playerToParty.clear();

        try (ResultSet rs = database.executeQuery("SELECT id, leader_uuid FROM parties")) {
            while (rs.next()) {
                UUID partyId = UUID.fromString(rs.getString("id"));
                UUID leader = UUID.fromString(rs.getString("leader_uuid"));
                parties.put(partyId, new Party(partyId, leader));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load parties: " + e.getMessage());
        }

        try (ResultSet rs = database.executeQuery("SELECT party_id, member_uuid, role FROM party_members")) {
            while (rs.next()) {
                UUID partyId = UUID.fromString(rs.getString("party_id"));
                UUID member = UUID.fromString(rs.getString("member_uuid"));
                String role = rs.getString("role");

                Party party = parties.get(partyId);
                if (party == null) {
                    continue;
                }

                PartyRole partyRole = PartyRole.MEMBER;
                if ("LEADER".equalsIgnoreCase(role)) {
                    partyRole = PartyRole.LEADER;
                    party.setLeader(member);
                }

                party.addMember(member, partyRole);
                playerToParty.put(member, partyId);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load party members: " + e.getMessage());
        }

        plugin.getDebugLogger().debug("Loaded parties: count=" + parties.size());
    }

    public Party getParty(UUID playerUuid) {
        UUID partyId = playerToParty.get(playerUuid);
        if (partyId == null) {
            partyId = getPartyIdForPlayer(playerUuid);
            if (partyId == null) {
                return null;
            }
            playerToParty.put(playerUuid, partyId);
        }

        Party party = parties.get(partyId);
        if (party == null) {
            party = loadParty(partyId);
        }
        return party;
    }

    public boolean isLeader(UUID playerUuid) {
        Party party = getParty(playerUuid);
        return party != null && party.getLeader().equals(playerUuid);
    }

    public Party createParty(Player leader) {
        MessageManager mm = plugin.getMessageManager();
        if (getParty(leader.getUniqueId()) != null) {
            mm.sendMessage(leader, "party.already-in-party");
            return null;
        }

        UUID partyId = UUID.randomUUID();
        Party party = new Party(partyId, leader.getUniqueId());

        parties.put(partyId, party);
        playerToParty.put(leader.getUniqueId(), partyId);

        persistParty(party);
        persistMember(partyId, leader.getUniqueId(), PartyRole.LEADER);

        mm.sendMessage(leader, "party.created");
        plugin.getDebugLogger().debug("Party created: " + partyId + " leader=" + leader.getName());
        return party;
    }

    public void invitePlayer(Player leader, Player target) {
        invitePlayer(leader, target.getUniqueId(), target.getName());
    }

    public void invitePlayer(Player leader, UUID targetUuid, String targetName) {
        MessageManager mm = plugin.getMessageManager();
        if (leader.getUniqueId().equals(targetUuid)) {
            mm.sendMessage(leader, "party.cannot-invite-self");
            return;
        }

        Party party = getParty(leader.getUniqueId());
        if (party == null) {
            party = createParty(leader);
            if (party == null) {
                return;
            }
        } else if (!party.getLeader().equals(leader.getUniqueId())) {
            mm.sendMessage(leader, "party.only-leader");
            return;
        }

        if (getPartyIdForPlayer(targetUuid) != null) {
            mm.sendMessage(leader, "party.invite-already-in-party");
            return;
        }

        int maxSize = plugin.getConfig().getInt("party.max-size", 4);
        if (party.getSize() >= maxSize) {
            mm.sendMessage(leader, "party.party-full");
            return;
        }

        PartyInvite existingInvite = loadInvite(targetUuid);
        if (existingInvite != null && !existingInvite.isExpired()) {
            mm.sendMessage(leader, "party.invite-already-sent");
            return;
        }

        long expiresAt = System.currentTimeMillis()
                + (plugin.getConfig().getInt("party.invite-expire-seconds", 60) * 1000L);
        PartyInvite invite = new PartyInvite(party.getId(), leader.getUniqueId(), leader.getName(), expiresAt);
        invites.put(targetUuid, invite);
        saveInvite(targetUuid, invite);

        String resolvedName = targetName == null ? getPlayerName(targetUuid) : targetName;
        mm.sendMessage(leader, "party.invited", resolvedName);

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null) {
            mm.sendMessage(targetPlayer, "party.invite-received", leader.getName());
        } else if (plugin.getBungeeManager().isEnabled()) {
            plugin.getBungeeManager().forwardBedWarsMessage(leader, "party-invite",
                    targetUuid.toString(), leader.getName());
        }

        plugin.getDebugLogger().debug("Party invite: leader=" + leader.getName() + " target=" + resolvedName);
    }

    public void acceptInvite(Player player, String inviterName) {
        MessageManager mm = plugin.getMessageManager();
        PartyInvite invite = loadInvite(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            invites.remove(player.getUniqueId());
            deleteInvite(player.getUniqueId());
            mm.sendMessage(player, "party.invite-expired");
            return;
        }

        if (inviterName != null && !inviterName.equalsIgnoreCase(invite.getInviterName())) {
            mm.sendMessage(player, "party.invite-not-found", inviterName);
            return;
        }

        if (getPartyIdForPlayer(player.getUniqueId()) != null) {
            mm.sendMessage(player, "party.already-in-party");
            return;
        }

        Party party = parties.get(invite.getPartyId());
        if (party == null) {
            party = loadParty(invite.getPartyId());
        }
        if (party == null) {
            mm.sendMessage(player, "party.invite-expired");
            invites.remove(player.getUniqueId());
            deleteInvite(player.getUniqueId());
            return;
        }

        int maxSize = plugin.getConfig().getInt("party.max-size", 4);
        if (party.getSize() >= maxSize) {
            mm.sendMessage(player, "party.party-full");
            invites.remove(player.getUniqueId());
            deleteInvite(player.getUniqueId());
            return;
        }

        party.addMember(player.getUniqueId(), PartyRole.MEMBER);
        playerToParty.put(player.getUniqueId(), party.getId());
        persistMember(party.getId(), player.getUniqueId(), PartyRole.MEMBER);

        invites.remove(player.getUniqueId());
        deleteInvite(player.getUniqueId());

        sendPartyMessage(party, "party.joined", player.getName());
        plugin.getDebugLogger().debug("Party join: player=" + player.getName() + " party=" + party.getId());
    }

    public void denyInvite(Player player, String inviterName) {
        MessageManager mm = plugin.getMessageManager();
        PartyInvite invite = loadInvite(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            invites.remove(player.getUniqueId());
            deleteInvite(player.getUniqueId());
            mm.sendMessage(player, "party.invite-expired");
            return;
        }

        if (inviterName != null && !inviterName.equalsIgnoreCase(invite.getInviterName())) {
            mm.sendMessage(player, "party.invite-not-found", inviterName);
            return;
        }

        invites.remove(player.getUniqueId());
        deleteInvite(player.getUniqueId());
        mm.sendMessage(player, "party.invite-denied", invite.getInviterName());

        Player inviter = Bukkit.getPlayer(invite.getInviterUuid());
        if (inviter != null) {
            mm.sendMessage(inviter, "party.invite-denied-notify", player.getName());
        }
    }

    public void leaveParty(Player player) {
        MessageManager mm = plugin.getMessageManager();
        Party party = getParty(player.getUniqueId());
        if (party == null) {
            mm.sendMessage(player, "party.not-in-party");
            return;
        }

        boolean wasLeader = party.getLeader().equals(player.getUniqueId());
        party.removeMember(player.getUniqueId());
        playerToParty.remove(player.getUniqueId());
        deleteMember(party.getId(), player.getUniqueId());

        if (party.getSize() == 0) {
            deleteParty(party.getId());
            parties.remove(party.getId());
            return;
        }

        if (wasLeader) {
            Optional<UUID> nextLeader = party.getMemberUuids().stream().findFirst();
            if (nextLeader.isPresent()) {
                UUID newLeader = nextLeader.get();
                party.setLeader(newLeader);
                updateLeader(party.getId(), newLeader);
                updateMemberRole(party.getId(), newLeader, PartyRole.LEADER);
                sendPartyMessage(party, "party.promoted", getPlayerName(newLeader));
            }
        }

        sendPartyMessage(party, "party.left", player.getName());
        mm.sendMessage(player, "party.left-self");
    }

    public void disbandParty(Player leader) {
        MessageManager mm = plugin.getMessageManager();
        Party party = getParty(leader.getUniqueId());
        if (party == null) {
            mm.sendMessage(leader, "party.not-in-party");
            return;
        }
        if (!party.getLeader().equals(leader.getUniqueId())) {
            mm.sendMessage(leader, "party.only-leader");
            return;
        }

        sendPartyMessage(party, "party.disbanded");
        for (UUID member : party.getMemberUuids()) {
            playerToParty.remove(member);
        }

        deleteParty(party.getId());
        parties.remove(party.getId());
        plugin.getDebugLogger().debug("Party disbanded: " + party.getId());
    }

    public void kickMember(Player leader, Player target) {
        MessageManager mm = plugin.getMessageManager();
        Party party = getParty(leader.getUniqueId());
        if (party == null) {
            mm.sendMessage(leader, "party.not-in-party");
            return;
        }
        if (!party.getLeader().equals(leader.getUniqueId())) {
            mm.sendMessage(leader, "party.only-leader");
            return;
        }
        if (!party.isMember(target.getUniqueId())) {
            mm.sendMessage(leader, "party.not-in-party");
            return;
        }

        party.removeMember(target.getUniqueId());
        playerToParty.remove(target.getUniqueId());
        deleteMember(party.getId(), target.getUniqueId());

        mm.sendMessage(target, "party.kicked");
        sendPartyMessage(party, "party.kicked-other", target.getName());
    }

    public void promoteMember(Player leader, Player target) {
        MessageManager mm = plugin.getMessageManager();
        Party party = getParty(leader.getUniqueId());
        if (party == null) {
            mm.sendMessage(leader, "party.not-in-party");
            return;
        }
        if (!party.getLeader().equals(leader.getUniqueId())) {
            mm.sendMessage(leader, "party.only-leader");
            return;
        }
        if (!party.isMember(target.getUniqueId())) {
            mm.sendMessage(leader, "party.not-in-party");
            return;
        }

        party.addMember(party.getLeader(), PartyRole.MEMBER);
        updateMemberRole(party.getId(), party.getLeader(), PartyRole.MEMBER);

        party.setLeader(target.getUniqueId());
        updateLeader(party.getId(), target.getUniqueId());
        updateMemberRole(party.getId(), target.getUniqueId(), PartyRole.LEADER);

        sendPartyMessage(party, "party.promoted", target.getName());
    }

    public void listParty(Player player) {
        MessageManager mm = plugin.getMessageManager();
        Party party = getParty(player.getUniqueId());
        if (party == null) {
            mm.sendMessage(player, "party.not-in-party");
            return;
        }

        mm.sendMessage(player, "party.list-header");
        for (UUID memberId : party.getMemberUuids()) {
            String name = getPlayerName(memberId);
            String role = party.getLeader().equals(memberId) ? "LEADER" : "MEMBER";
            mm.sendMessage(player, "party.list-line", name, role);
        }
        mm.sendMessage(player, "party.list-footer");
    }

    public void sendPartyChat(Player sender, String message) {
        MessageManager mm = plugin.getMessageManager();
        Party party = getParty(sender.getUniqueId());
        if (party == null) {
            mm.sendMessage(sender, "party.not-in-party");
            return;
        }

        String formatted = mm.getMessage("party.party-chat-format", sender.getName(), message);
        for (UUID memberId : party.getMemberUuids()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }
    }

    public void shutdown() {
        invites.clear();
    }

    private UUID getPartyIdForPlayer(UUID uuid) {
        UUID partyId = playerToParty.get(uuid);
        if (partyId != null) {
            return partyId;
        }

        try (ResultSet rs = database.executeQuery(
                "SELECT party_id FROM party_members WHERE member_uuid = ?",
                uuid.toString())) {
            if (rs.next()) {
                return UUID.fromString(rs.getString("party_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to lookup party membership: " + e.getMessage());
        }

        return null;
    }

    private Party loadParty(UUID partyId) {
        try (ResultSet rs = database.executeQuery(
                "SELECT id, leader_uuid FROM parties WHERE id = ?", partyId.toString())) {
            if (!rs.next()) {
                return null;
            }
            UUID leader = UUID.fromString(rs.getString("leader_uuid"));
            Party party = new Party(partyId, leader);

            try (ResultSet members = database.executeQuery(
                    "SELECT member_uuid, role FROM party_members WHERE party_id = ?", partyId.toString())) {
                while (members.next()) {
                    UUID member = UUID.fromString(members.getString("member_uuid"));
                    String role = members.getString("role");
                    PartyRole partyRole = "LEADER".equalsIgnoreCase(role) ? PartyRole.LEADER : PartyRole.MEMBER;
                    if (partyRole == PartyRole.LEADER) {
                        party.setLeader(member);
                    }
                    party.addMember(member, partyRole);
                    playerToParty.put(member, partyId);
                }
            }

            parties.put(partyId, party);
            return party;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load party: " + e.getMessage());
            return null;
        }
    }

    private PartyInvite loadInvite(UUID targetUuid) {
        PartyInvite cached = invites.get(targetUuid);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        try (ResultSet rs = database.executeQuery(
                "SELECT party_id, inviter_uuid, inviter_name, expires_at FROM party_invites WHERE target_uuid = ?",
                targetUuid.toString())) {
            if (rs.next()) {
                PartyInvite invite = new PartyInvite(
                        UUID.fromString(rs.getString("party_id")),
                        UUID.fromString(rs.getString("inviter_uuid")),
                        rs.getString("inviter_name"),
                        rs.getLong("expires_at"));
                invites.put(targetUuid, invite);
                return invite;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load party invite: " + e.getMessage());
        }

        return null;
    }

    private void saveInvite(UUID targetUuid, PartyInvite invite) {
        try {
            database.executeUpdate(
                    "INSERT OR REPLACE INTO party_invites (target_uuid, party_id, inviter_uuid, inviter_name, expires_at) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    targetUuid.toString(),
                    invite.getPartyId().toString(),
                    invite.getInviterUuid().toString(),
                    invite.getInviterName(),
                    invite.getExpiresAt());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to persist party invite: " + e.getMessage());
        }
    }

    private void deleteInvite(UUID targetUuid) {
        try {
            database.executeUpdate("DELETE FROM party_invites WHERE target_uuid = ?", targetUuid.toString());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete party invite: " + e.getMessage());
        }
    }

    private void sendPartyMessage(Party party, String key, Object... args) {
        MessageManager mm = plugin.getMessageManager();
        for (UUID member : party.getMemberUuids()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                mm.sendMessage(player, key, args);
            }
        }
    }

    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "Unknown" : name;
    }

    private void persistParty(Party party) {
        try {
            database.executeUpdate("INSERT INTO parties (id, leader_uuid, created_at) VALUES (?, ?, ?)",
                    party.getId().toString(),
                    party.getLeader().toString(),
                    System.currentTimeMillis());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to persist party: " + e.getMessage());
        }
    }

    private void persistMember(UUID partyId, UUID memberUuid, PartyRole role) {
        try {
            database.executeUpdate("INSERT OR REPLACE INTO party_members (party_id, member_uuid, role, joined_at) "
                    + "VALUES (?, ?, ?, ?)",
                    partyId.toString(),
                    memberUuid.toString(),
                    role.name(),
                    System.currentTimeMillis());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to persist party member: " + e.getMessage());
        }
    }

    private void updateLeader(UUID partyId, UUID leaderUuid) {
        try {
            database.executeUpdate("UPDATE parties SET leader_uuid = ? WHERE id = ?",
                    leaderUuid.toString(),
                    partyId.toString());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update party leader: " + e.getMessage());
        }
    }

    private void updateMemberRole(UUID partyId, UUID memberUuid, PartyRole role) {
        try {
            database.executeUpdate("UPDATE party_members SET role = ? WHERE party_id = ? AND member_uuid = ?",
                    role.name(),
                    partyId.toString(),
                    memberUuid.toString());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update party member role: " + e.getMessage());
        }
    }

    private void deleteMember(UUID partyId, UUID memberUuid) {
        try {
            database.executeUpdate("DELETE FROM party_members WHERE party_id = ? AND member_uuid = ?",
                    partyId.toString(),
                    memberUuid.toString());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete party member: " + e.getMessage());
        }
    }

    private void deleteParty(UUID partyId) {
        try {
            database.executeUpdate("DELETE FROM party_members WHERE party_id = ?", partyId.toString());
            database.executeUpdate("DELETE FROM parties WHERE id = ?", partyId.toString());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete party: " + e.getMessage());
        }
    }
}
