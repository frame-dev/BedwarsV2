package ch.framedev.bedwars.listeners;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Handles entity damage events for BedWars.
 */
public class EntityDamageListener implements Listener {

    private final BedWarsPlugin plugin;

    // Cache config flags (recreate listener on reload or add setters)
    private final boolean allowFall;
    private final boolean allowProjectile;
    private final boolean allowPvp;
    private final boolean allowExplosionDamage;

    public EntityDamageListener(BedWarsPlugin plugin) {
        this.plugin = plugin;

        this.allowFall = plugin.getConfig().getBoolean("game.allow-fall-damage", true);
        this.allowProjectile = plugin.getConfig().getBoolean("game.allow-projectile-damage", true);
        this.allowPvp = plugin.getConfig().getBoolean("game.allow-pvp", true);
        this.allowExplosionDamage = plugin.getConfig().getBoolean("game.allow-explosion-damage", false); // default BedWars: off
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victimBukkit)) return;

        Game game = plugin.getGameManager().getPlayerGame(victimBukkit);
        if (game == null) return; // not in a BedWars game -> don't interfere

        // Game not running -> no damage at all
        if (game.getState() != GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }

        // Cause-based rules (non-PVP too)
        EntityDamageEvent.DamageCause cause = event.getCause();

        if (!allowFall && cause == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }

        // Explosion damage toggle
        if (!allowExplosionDamage &&
                (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                        || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) {
            event.setCancelled(true);
            return;
        }

        // If it's not damage by an entity/projectile, we're done here
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;

        Player attackerBukkit = resolveAttacker(byEntity.getDamager());
        if (attackerBukkit == null) {
            // Example: mobs, TNT source unknown, etc.
            return;
        }

        // Attacker must be in same game (prevents cross-world / cross-arena abuse)
        Game attackerGame = plugin.getGameManager().getPlayerGame(attackerBukkit);
        if (attackerGame != game) {
            event.setCancelled(true);
            return;
        }

        // Global PVP toggle
        if (!allowPvp) {
            event.setCancelled(true);
            return;
        }

        // Projectile toggle (applies to arrows/snowballs/etc.)
        if (!allowProjectile && byEntity.getDamager() instanceof Projectile) {
            event.setCancelled(true);
            return;
        }

        // Friendly fire check
        GamePlayer victim = game.getGamePlayer(victimBukkit);
        GamePlayer attacker = game.getGamePlayer(attackerBukkit);
        if (victim == null || attacker == null) return;

        if (victim.getTeam() != null && victim.getTeam().equals(attacker.getTeam())) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) return p;

        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }
}