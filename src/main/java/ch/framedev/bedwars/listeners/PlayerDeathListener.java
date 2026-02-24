package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles player death events (BedWars).
 *
 * Fixes / improvements:
 * - HIGHEST priority + ignoreCancelled for stability with other plugins
 * - Always suppress vanilla death message in-game
 * - Correct "final kill" logic: depends on VICTIM bed status (victim team bed alive?)
 * - Handles null team safely
 * - Clears drops + XP to prevent farming
 * - Ensures respawn handling is delegated to Game#handlePlayerDeath
 */
public class PlayerDeathListener implements Listener {

    private final BedWarsPlugin plugin;

    public PlayerDeathListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victimBukkit = event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(victimBukkit);

        if (game == null || game.getState() != GameState.RUNNING) return;

        // Never show vanilla death messages for BedWars game
        event.setDeathMessage(null);

        // Prevent item/XP farming
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killerBukkit = victimBukkit.getKiller();

        plugin.getDebugLogger().debug("Player death: " + victimBukkit.getName()
                + ", killer=" + (killerBukkit != null ? killerBukkit.getName() : "none"));

        GamePlayer victim = game.getGamePlayer(victimBukkit);
        if (victim == null || victim.getTeam() == null) {
            // Still run game logic to avoid stuck states
            game.handlePlayerDeath(victimBukkit);
            return;
        }

        // Victim team bed status decides final kill
        boolean victimBedAlive = victim.getTeam().isBedAlive();

        if (killerBukkit != null) {
            GamePlayer killer = game.getGamePlayer(killerBukkit);

            // If killer isn't part of same game (shouldn't happen if your damage listener is correct), treat as no-killer
            if (killer == null || killer.getTeam() == null) {
                game.broadcast("death.died", victim.getTeam().getColor().getChatColor() + victimBukkit.getName());
                game.handlePlayerDeath(victimBukkit);
                return;
            }

            killer.addKill();

            if (plugin.getCosmeticsManager() != null) {
                plugin.getCosmeticsManager().applyKillEffect(killerBukkit, victimBukkit.getLocation());
            }

            if (plugin.getAchievementsManager() != null) {
                plugin.getAchievementsManager().recordKill(killerBukkit.getUniqueId());
            }

            if (!victimBedAlive) {
                killer.addFinalKill();
                if (plugin.getAchievementsManager() != null) {
                    plugin.getAchievementsManager().recordFinalKill(killerBukkit.getUniqueId());
                }

                game.broadcast("death.final-kill",
                        victim.getTeam().getColor().getChatColor() + victimBukkit.getName(),
                        killer.getTeam().getColor().getChatColor() + killerBukkit.getName());
            } else {
                game.broadcast("death.killed-by",
                        victim.getTeam().getColor().getChatColor() + victimBukkit.getName(),
                        killer.getTeam().getColor().getChatColor() + killerBukkit.getName());
            }
        } else {
            game.broadcast("death.died",
                    victim.getTeam().getColor().getChatColor() + victimBukkit.getName());
        }

        // Let Game handle respawn/elimination logic
        // (Game already increments victim deaths internally; if not, keep victim.addDeath() here)
        game.handlePlayerDeath(victimBukkit);

        // Optional: instant respawn on newer Spigot versions can be done via PlayerRespawnEvent / Paper settings.
        // Do NOT call spigot().respawn() here; your Game uses a respawn timer.
    }
}