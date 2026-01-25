package ch.framedev.bedwars.game;

/**
 * Represents the different states of a BedWars game
 */
public enum GameState {
    WAITING, // Waiting for players
    STARTING, // Countdown before game starts
    RUNNING, // Game in progress
    ENDING // Game ending/cleanup
}
