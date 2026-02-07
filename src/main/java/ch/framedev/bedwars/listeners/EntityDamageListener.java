package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Handles entity damage events
 */
public class EntityDamageListener implements Listener {

    private final BedWarsPlugin plugin;

    public EntityDamageListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Game game = plugin.getGameManager().getPlayerGame(player);

            if (game != null) {
                if (game.getState() != GameState.RUNNING) {
                    event.setCancelled(true);
                    return;
                }

                boolean allowFall = plugin.getConfig().getBoolean("game.allow-fall-damage", true);
                boolean allowProjectile = plugin.getConfig().getBoolean("game.allow-projectile-damage", true);
                boolean allowPvp = plugin.getConfig().getBoolean("game.allow-pvp", true);

                if (!allowFall && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                    return;
                }

                if (!allowProjectile && event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                    event.setCancelled(true);
                    return;
                }

                if (!allowPvp && event instanceof EntityDamageByEntityEvent) {
                    event.setCancelled(true);
                    return;
                }

                if (event instanceof EntityDamageByEntityEvent damageEvent) {
                    Player attacker = resolveAttacker(damageEvent.getDamager());
                    if (attacker == null) {
                        return;
                    }

                    Game attackerGame = plugin.getGameManager().getPlayerGame(attacker);
                    if (attackerGame != game) {
                        return;
                    }

                    GamePlayer victim = game.getGamePlayer(player);
                    GamePlayer attackerPlayer = game.getGamePlayer(attacker);
                    if (victim == null || attackerPlayer == null) {
                        return;
                    }

                    if (victim.getTeam() != null && victim.getTeam().equals(attackerPlayer.getTeam())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        return null;
    }
}
