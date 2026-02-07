package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles player death events
 */
public class PlayerDeathListener implements Listener {

    private final BedWarsPlugin plugin;

    public PlayerDeathListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game != null && game.getState() == GameState.RUNNING) {
            Player killer = player.getKiller();
            plugin.getDebugLogger().debug("Player death: " + player.getName()
                    + ", killer=" + (killer != null ? killer.getName() : "none"));
            event.setDeathMessage(null);

            GamePlayer gamePlayer = game.getGamePlayer(player);
            if (gamePlayer != null) {
                if (killer != null) {
                    GamePlayer killerPlayer = game.getGamePlayer(killer);
                    if (killerPlayer != null) {
                        killerPlayer.addKill();

                        if (plugin.getCosmeticsManager() != null) {
                            plugin.getCosmeticsManager().applyKillEffect(killer, player.getLocation());
                        }

                        if (plugin.getAchievementsManager() != null) {
                            plugin.getAchievementsManager().recordKill(killer.getUniqueId());
                        }

                        if (!gamePlayer.getTeam().isBedAlive()) {
                            killerPlayer.addFinalKill();
                            if (plugin.getAchievementsManager() != null) {
                                plugin.getAchievementsManager().recordFinalKill(killer.getUniqueId());
                            }
                            game.broadcast("death.final-kill",
                                    gamePlayer.getTeam().getColor().getChatColor() + player.getName(),
                                    killerPlayer.getTeam().getColor().getChatColor() + killer.getName());
                        } else {
                            game.broadcast("death.killed-by",
                                    gamePlayer.getTeam().getColor().getChatColor() + player.getName(),
                                    killerPlayer.getTeam().getColor().getChatColor() + killer.getName());
                        }
                    }
                } else {
                    game.broadcast("death.died",
                            gamePlayer.getTeam().getColor().getChatColor() + player.getName());
                }
            }

            event.getDrops().clear();
            game.handlePlayerDeath(player);
        }
    }
}
