package ch.framedev.bedwars.party;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a party of players.
 */
public class Party {

    private final UUID id;
    private UUID leader;
    private final Map<UUID, PartyRole> members;

    public Party(UUID id, UUID leader) {
        this.id = id;
        this.leader = leader;
        this.members = new LinkedHashMap<>();
        this.members.put(leader, PartyRole.LEADER);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
        members.put(leader, PartyRole.LEADER);
    }

    public Map<UUID, PartyRole> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public void addMember(UUID member, PartyRole role) {
        members.put(member, role);
    }

    public void removeMember(UUID member) {
        members.remove(member);
    }

    public boolean isMember(UUID member) {
        return members.containsKey(member);
    }

    public PartyRole getRole(UUID member) {
        return members.get(member);
    }

    public int getSize() {
        return members.size();
    }

    public Collection<UUID> getMemberUuids() {
        return members.keySet();
    }

    public enum PartyRole {
        LEADER,
        MEMBER
    }
}
