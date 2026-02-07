package ch.framedev.bedwars.achievements;

/**
 * Tracks progress for a single achievement.
 */
public class AchievementProgress {

    private int progress;
    private long unlockedAt;

    public AchievementProgress(int progress, long unlockedAt) {
        this.progress = progress;
        this.unlockedAt = unlockedAt;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(long unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public boolean isUnlocked() {
        return unlockedAt > 0;
    }
}
