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
import java.util.function.BiConsumer;

/**
 * Represents a player in a BedWars game.
 * <p>
 * De-duplication:
 * - One generic method for applying ENCHANTMENT upgrades to any item by target ("ARMOR"/"WEAPON")
 * - One helper for iterating upgrades and resolving levels
 * - Potion upgrade loop stays separate (different effect type)
 */
@SuppressWarnings("unused")
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

        // Apply upgrade enchantments (ARMOR)
        applyEnchantmentUpgrades("ARMOR", (ench, level) -> {
            helmet.addEnchantment(ench, level);
            chest.addEnchantment(ench, level);
            legs.addEnchantment(ench, level);
            boots.addEnchantment(ench, level);
        });

        // BedWars typically always forces team leather armor.
        inv.setHelmet(helmet);
        inv.setChestplate(chest);
        inv.setLeggings(legs);
        inv.setBoots(boots);

        // Ensure starting sword & apply WEAPON upgrades
        ensureStartingSword(inv);

        // Apply potion upgrades (refresh)
        applyPotionUpgrades(player);
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

    private void ensureStartingSword(PlayerInventory inv) {
        if (inv == null) return;

        if (!hasSword(inv)) {
            Material swordMat = materialOr();
            ItemStack sword = new ItemStack(swordMat);

            applyEnchantmentUpgradesToItem(sword);
            inv.addItem(sword);
        }
    }

    private boolean hasSword(PlayerInventory inv) {
        for (ItemStack it : inv.getContents()) {
            if (it == null) continue;
            if (it.getType().name().endsWith("_SWORD")) return true;
        }
        return false;
    }

    /**
     * Apply ENCHANTMENT upgrades with matching target to an item.
     */
    private void applyEnchantmentUpgradesToItem(ItemStack item) {
        if (item == null) return;
        applyEnchantmentUpgrades("WEAPON", item::addEnchantment);
    }

    /**
     * Generic iterator over ENCHANTMENT upgrades for a target.
     * De-duplicates the upgrade loops for armor/weapon.
     */
    private void applyEnchantmentUpgrades(String target, BiConsumer<Enchantment, Integer> applier) {
        if (upgradeManager == null || team == null || team.getUpgrades() == null) return;
        if (target == null || applier == null) return;

        forEachUpgradeLevel((id, upgrade, level) -> {
            if (upgrade.getEffectType() != UpgradeManager.EffectType.ENCHANTMENT) return;
            if (!target.equalsIgnoreCase(upgrade.getTarget())) return;

            Enchantment enchantment = upgrade.getEnchantment();
            if (enchantment == null) return;

            // If you need levels above vanilla limits, change to addUnsafeEnchantment in the applier usage.
            applier.accept(enchantment, level);
        });
    }

    /**
     * Applies POTION_EFFECT upgrades.
     */
    private void applyPotionUpgrades(Player player) {
        if (player == null) return;
        if (upgradeManager == null || team == null || team.getUpgrades() == null) return;

        forEachUpgradeLevel((id, upgrade, level) -> {
            if (upgrade.getEffectType() != UpgradeManager.EffectType.POTION_EFFECT) return;
            if (upgrade.getPotionType() == null) return;

            int amplifier = upgrade.isAmplifierPerLevel() ? Math.max(0, level - 1) : 0;

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
        });
    }

    /**
     * Central helper to iterate over upgrades and resolve levels once.
     */
    private void forEachUpgradeLevel(UpgradeConsumer consumer) {
        if (upgradeManager == null || team == null || team.getUpgrades() == null || consumer == null) return;

        for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgradeManager.getUpgrades().entrySet()) {
            String id = entry.getKey();
            UpgradeManager.Upgrade upgrade = entry.getValue();
            if (upgrade == null) continue;

            int level = team.getUpgrades().getUpgradeLevel(id);
            if (level <= 0) continue;

            consumer.accept(id, upgrade, level);
        }
    }

    @FunctionalInterface
    private interface UpgradeConsumer {
        void accept(String id, UpgradeManager.Upgrade upgrade, int level);
    }

    private Material materialOr() {
        Material m = Material.matchMaterial("WOODEN_SWORD");
        if (m != null) return m;
        m = Material.matchMaterial("WOOD_SWORD");
        if (m != null) return m;
        return Material.WOODEN_SWORD;
    }

    /* --------------------------------------------------------------------- */
    /* Stats                                                                  */
    /* --------------------------------------------------------------------- */

    public void addKill() { kills++; }
    public void addDeath() { deaths++; }
    public void addFinalKill() { finalKills++; }
    public void addBedBroken() { bedsBroken++; }

    public UUID getUuid() { return uuid; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getFinalKills() { return finalKills; }
    public int getBedsBroken() { return bedsBroken; }

    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }

    public boolean isSpectating() { return spectating; }
    public void setSpectating(boolean spectating) { this.spectating = spectating; }
}