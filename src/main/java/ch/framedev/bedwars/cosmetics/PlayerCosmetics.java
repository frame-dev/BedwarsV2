package ch.framedev.bedwars.cosmetics;

import java.util.UUID;

/**
 * Stores player cosmetic selections.
 */
public class PlayerCosmetics {

    private final UUID uuid;
    private String killEffectId;
    private String bedEffectId;

    public PlayerCosmetics(UUID uuid, String killEffectId, String bedEffectId) {
        this.uuid = uuid;
        this.killEffectId = killEffectId;
        this.bedEffectId = bedEffectId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getKillEffectId() {
        return killEffectId;
    }

    public void setKillEffectId(String killEffectId) {
        this.killEffectId = killEffectId;
    }

    public String getBedEffectId() {
        return bedEffectId;
    }

    public void setBedEffectId(String bedEffectId) {
        this.bedEffectId = bedEffectId;
    }
}
