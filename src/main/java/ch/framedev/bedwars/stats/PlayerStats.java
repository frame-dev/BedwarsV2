package ch.framedev.bedwars.stats;

import java.util.UUID;

/**
 * Stores player statistics
 */
public class PlayerStats {

    private final UUID uuid;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int finalKills;
    private int bedsBroken;
    private int gamesPlayed;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.wins = 0;
        this.losses = 0;
        this.kills = 0;
        this.deaths = 0;
        this.finalKills = 0;
        this.bedsBroken = 0;
        this.gamesPlayed = 0;
    }

    public void addWin() {
        wins++;
        gamesPlayed++;
    }

    public void addLoss() {
        losses++;
        gamesPlayed++;
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public void addFinalKill() {
        finalKills++;
    }

    public void addBedBroken() {
        bedsBroken++;
    }

    public double getKDRatio() {
        if (deaths == 0)
            return kills;
        return (double) kills / deaths;
    }

    public double getWinRate() {
        int total = wins + losses;
        if (total == 0)
            return 0;
        return (double) wins / total * 100;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getFinalKills() {
        return finalKills;
    }

    public int getBedsBroken() {
        return bedsBroken;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    // Setters for database loading
    public void setWins(int wins) {
        this.wins = wins;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void setFinalKills(int finalKills) {
        this.finalKills = finalKills;
    }

    public void setBedsBroken(int bedsBroken) {
        this.bedsBroken = bedsBroken;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }
}
