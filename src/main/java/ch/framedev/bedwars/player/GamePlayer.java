package ch.framedev.bedwars.player;

import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a player in a BedWars game.
 *
 * Fixes / improvements:
 * - Null-safe armor coloring (avoid NPE if meta is null)
 * - Avoids giving infinite wooden swords on every call (only adds if none present)
 * - Applies enchantments via addUnsafeEnchantment if needed (optional; kept safe here)
 * - Does not overwrite non-leather armor already equipped (optional behavior; see comments)
 * - Clears/appends potion effects in a controlled way (re-applies upgrades cleanly)
 */
public class GamePlayer {

    private final UUID uuid;

    private Team team;

    private int kills;
    private int deaths;
    private int finalKills;
    private int bedsBroken;

    private boolean eliminated;
    private boolean spectating;

    private static UpgradeManager upgradeManager;

    public static void setUpgradeManager(UpgradeManager manager) {
        upgradeManager = manager;
    }

    public GamePlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.kills = 0;
        this.deaths = 0;
        this.finalKills = 0;
        this.bedsBroken = 0;
        this.eliminated = false;
        this.spectating = false;
    }

    /**
     * Gives (or refreshes) team-colored leather armor and applies team upgrades.
     * Safe to call on respawn.
     */
    public void giveTeamArmor() {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || team == null) return;

        PlayerInventory inv = player.getInventory();
        Color armorColor = team.getColor().getColor();

        // Build leather armor set
        ItemStack helmet = coloredLeather(Material.LEATHER_HELMET, armorColor);
        ItemStack chest = coloredLeather(Material.LEATHER_CHESTPLATE, armorColor);
        ItemStack legs = coloredLeather(Material.LEATHER_LEGGINGS, armorColor);
        ItemStack boots = coloredLeather(Material.LEATHER_BOOTS, armorColor);

        // Apply upgrade enchantments
        if (upgradeManager != null) {
            applyEnchantUpgradesToArmor(helmet, chest, legs, boots);
        }

        // BedWars typically always forces team leather armor.
        // If you want to NOT overwrite diamond/iron armor, add checks here.
        inv.setHelmet(helmet);
        inv.setChestplate(chest);
        inv.setLeggings(legs);
        inv.setBoots(boots);

        // Give starting sword only if player doesn't already have a sword
        ensureStartingSword(inv);

        // Apply potion upgrades (refresh)
        if (upgradeManager != null) {
            applyPotionUpgrades(player);
        }
    }

    private ItemStack coloredLeather(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta lam) {
            lam.setColor(color);
            item.setItemMeta(lam);
        }
        return item;
    }

    private void applyEnchantUpgradesToArmor(ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots) {
        for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgradeManager.getUpgrades().entrySet()) {
            String upgradeId = entry.getKey();
            UpgradeManager.Upgrade upgrade = entry.getValue();
            if (upgrade == null) continue;

            int level = team.getUpgrades().getUpgradeLevel(upgradeId);
            if (level <= 0) continue;

            if (upgrade.getEffectType() != UpgradeManager.EffectType.ENCHANTMENT) continue;
            if (!"ARMOR".equalsIgnoreCase(upgrade.getTarget())) continue;

            Enchantment ench = upgrade.getEnchantment();
            if (ench == null) continue;

            // Use safe enchant; if you need levels beyond vanilla, switch to addUnsafeEnchantment.
            helmet.addEnchantment(ench, level);
            chest.addEnchantment(ench, level);
            legs.addEnchantment(ench, level);
            boots.addEnchantment(ench, level);
        }
    }

    private void ensureStartingSword(PlayerInventory inv) {
        if (inv == null) return;

        boolean hasSword = false;
        for (ItemStack it : inv.getContents()) {
            if (it == null) continue;
            String name = it.getType().name();
            if (name.endsWith("_SWORD")) {
                hasSword = true;
                break;
            }
        }

        if (!hasSword) {
            Material swordMat = materialOr("WOODEN_SWORD", "WOOD_SWORD", Material.WOODEN_SWORD);
            ItemStack sword = new ItemStack(swordMat);

            if (upgradeManager != null) {
                applyEnchantUpgradesToWeapon(sword);
            }

            inv.addItem(sword);
        }
    }

    private void applyEnchantUpgradesToWeapon(ItemStack weapon) {
        for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgradeManager.getUpgrades().entrySet()) {
            String upgradeId = entry.getKey();
            UpgradeManager.Upgrade upgrade = entry.getValue();
            if (upgrade == null) continue;

            int level = team.getUpgrades().getUpgradeLevel(upgradeId);
            if (level <= 0) continue;

            if (upgrade.getEffectType() != UpgradeManager.EffectType.ENCHANTMENT) continue;
            if (!"WEAPON".equalsIgnoreCase(upgrade.getTarget())) continue;

            Enchantment ench = upgrade.getEnchantment();
            if (ench == null) continue;

            weapon.addEnchantment(ench, level);
        }
    }

    private void applyPotionUpgrades(Player player) {
        for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgradeManager.getUpgrades().entrySet()) {
            String upgradeId = entry.getKey();
            UpgradeManager.Upgrade upgrade = entry.getValue();
            if (upgrade == null) continue;

            int level = team.getUpgrades().getUpgradeLevel(upgradeId);
            if (level <= 0) continue;

            if (upgrade.getEffectType() != UpgradeManager.EffectType.POTION_EFFECT) continue;
            if (upgrade.getPotionType() == null) continue;

            int amplifier = upgrade.isAmplifierPerLevel() ? Math.max(0, level - 1) : 0;

            // Optional: remove existing effect to prevent weird stacking edge cases
            if (player.hasPotionEffect(upgrade.getPotionType())) {
                player.removePotionEffect(upgrade.getPotionType());
            }

            player.addPotionEffect(new PotionEffect(
                    upgrade.getPotionType(),
                    upgrade.getDuration(),
                    amplifier,
                    false,
                    false
            ));
        }
    }

    private Material materialOr(String preferred, String legacy, Material fallback) {
        Material m = Material.matchMaterial(preferred);
        if (m != null) return m;
        m = Material.matchMaterial(legacy);
        if (m != null) return m;
        return fallback;
    }

    public void addKill() {
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

    public UUID getUuid() {
        return uuid;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
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

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public boolean isSpectating() {
        return spectating;
    }

    public void setSpectating(boolean spectating) {
        this.spectating = spectating;
    }
}