package ch.framedev.bedwars.manager;

import ch.framedev.BedWarsPlugin;
import ch.framedev.bedwars.upgrades.TeamUpgrades;
import ch.framedev.bedwars.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class UpgradeManagerTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private UpgradeManager manager;
    private TeamUpgrades team;
    private Enchantment enchantment;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        BedWarsPlugin plugin = mock(BedWarsPlugin.class);
        when(plugin.getDataFolder()).thenReturn(temporaryFolder.getRoot());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getDebugLogger()).thenReturn(mock(DebugLogger.class));
        Files.writeString(temporaryFolder.getRoot().toPath().resolve("upgrades.yml"), "upgrades: {}\n");
        manager = new UpgradeManager(plugin);
        enchantment = mock(Enchantment.class);
        UpgradeManager.Upgrade upgrade = new UpgradeManager.Upgrade("protection", "Protection", List.of(),
                Material.IRON_CHESTPLATE, 4, Map.of(), UpgradeManager.EffectType.ENCHANTMENT,
                "ARMOR", enchantment, null, 0, false);
        Field field = UpgradeManager.class.getDeclaredField("upgrades");
        field.setAccessible(true);
        ((Map<String, UpgradeManager.Upgrade>) field.get(manager)).put("protection", upgrade);
        team = new TeamUpgrades();
    }

    @Test
    public void existingProtectionIncreasesThroughAllFourTiers() {
        ItemStack armor = mock(ItemStack.class);
        when(armor.getType()).thenReturn(Material.IRON_CHESTPLATE);
        for (int tier = 1; tier <= 4; tier++) {
            when(armor.getEnchantmentLevel(enchantment)).thenReturn(tier - 1);
            team.setUpgradeLevel("protection", tier);
            manager.applyUpgradesToItem(armor, team);
            verify(armor).addEnchantment(enchantment, tier);
        }
    }

    @Test
    public void strongerExistingEnchantmentIsPreserved() {
        ItemStack armor = mock(ItemStack.class);
        when(armor.getType()).thenReturn(Material.DIAMOND_BOOTS);
        when(armor.getEnchantmentLevel(enchantment)).thenReturn(4);
        team.setUpgradeLevel("protection", 2);

        manager.applyUpgradesToItem(armor, team);

        verify(armor, never()).addEnchantment(any(), anyInt());
    }

    @Test
    public void armorUpgradeDoesNotEnchantWeapons() {
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.IRON_SWORD);
        team.setUpgradeLevel("protection", 4);

        manager.applyUpgradesToItem(sword, team);

        verify(sword, never()).addEnchantment(any(), anyInt());
    }
}
