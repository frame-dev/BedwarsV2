package ch.framedev.bedwars.party;

import java.util.UUID;

/**
 * Represents a pending party invite.
 */
public record PartyInvite(UUID partyId, UUID inviterUuid, String inviterName, long expiresAt) {

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
