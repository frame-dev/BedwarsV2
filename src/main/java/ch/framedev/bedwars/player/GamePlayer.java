package ch.framedev.bedwars.player;

import ch.framedev.bedwars.manager.UpgradeManager;
import ch.framedev.bedwars.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a player in a BedWars game
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

    public void giveTeamArmor() {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || team == null)
            return;

        Color color = team.getColor().getColor();

        // Leather armor with team color
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

        colorArmor(helmet, color);
        colorArmor(chestplate, color);
        colorArmor(leggings, color);
        colorArmor(boots, color);

        // Apply all ENCHANTMENT upgrades to armor
        if (upgradeManager != null) {
            for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgradeManager.getUpgrades().entrySet()) {
                UpgradeManager.Upgrade upgrade = entry.getValue();
                int level = team.getUpgrades().getUpgradeLevel(entry.getKey());

                if (level > 0 && upgrade.getEffectType() == UpgradeManager.EffectType.ENCHANTMENT) {
                    if ("ARMOR".equalsIgnoreCase(upgrade.getTarget()) && upgrade.getEnchantment() != null) {
                        helmet.addEnchantment(upgrade.getEnchantment(), level);
                        chestplate.addEnchantment(upgrade.getEnchantment(), level);
                        leggings.addEnchantment(upgrade.getEnchantment(), level);
                        boots.addEnchantment(upgrade.getEnchantment(), level);
                    }
                }
            }
        }

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        // Give wooden sword with weapon enchantments
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        if (upgradeManager != null) {
            for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgradeManager.getUpgrades().entrySet()) {
                UpgradeManager.Upgrade upgrade = entry.getValue();
                int level = team.getUpgrades().getUpgradeLevel(entry.getKey());

                if (level > 0 && upgrade.getEffectType() == UpgradeManager.EffectType.ENCHANTMENT) {
                    if ("WEAPON".equalsIgnoreCase(upgrade.getTarget()) && upgrade.getEnchantment() != null) {
                        sword.addEnchantment(upgrade.getEnchantment(), level);
                    }
                }
            }
        }
        player.getInventory().addItem(sword);

        // Apply all POTION_EFFECT upgrades
        if (upgradeManager != null) {
            for (Map.Entry<String, UpgradeManager.Upgrade> entry : upgradeManager.getUpgrades().entrySet()) {
                UpgradeManager.Upgrade upgrade = entry.getValue();
                int level = team.getUpgrades().getUpgradeLevel(entry.getKey());

                if (level > 0 && upgrade.getEffectType() == UpgradeManager.EffectType.POTION_EFFECT) {
                    if (upgrade.getPotionType() != null) {
                        int amplifier = upgrade.isAmplifierPerLevel() ? (level - 1) : 0;
                        player.addPotionEffect(new PotionEffect(
                                upgrade.getPotionType(),
                                upgrade.getDuration(),
                                amplifier,
                                false,
                                false));
                    }
                }
            }
        }
    }

    private void colorArmor(ItemStack item, Color color) {
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
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
