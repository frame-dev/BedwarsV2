package ch.framedev.bedwars.party;

import java.util.UUID;

/**
 * Represents a pending party invite.
 */
public class PartyInvite {

    private final UUID partyId;
    private final UUID inviterUuid;
    private final String inviterName;
    private final long expiresAt;

    public PartyInvite(UUID partyId, UUID inviterUuid, String inviterName, long expiresAt) {
        this.partyId = partyId;
        this.inviterUuid = inviterUuid;
        this.inviterName = inviterName;
        this.expiresAt = expiresAt;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getInviterUuid() {
        return inviterUuid;
    }

    public String getInviterName() {
        return inviterName;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
