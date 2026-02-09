package ch.framedev.bedwars.team;

import ch.framedev.bedwars.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the team selection GUI in the lobby
 */
public class TeamSelectionGUI {

    private static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&6Team Selection");
    private final Map<UUID, Map<Integer, TeamColor>> menuSlots = new ConcurrentHashMap<>();

    /**
     * Open the team selection GUI for a player
     */
    public void openTeamSelection(Player player, Game game) {
        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Get player's current team
        ch.framedev.bedwars.player.GamePlayer gamePlayer = game.getGamePlayer(player);
        TeamColor currentTeam = gamePlayer != null && gamePlayer.getTeam() != null 
                ? gamePlayer.getTeam().getColor() 
                : null;

        // Calculate max team size
        int maxTeamSize = game.getArena().getMaxPlayers() / game.getTeams().size();

        // Create team items
        int slot = 10;
        Map<Integer, TeamColor> slotToTeam = new HashMap<>();

        for (TeamColor color : TeamColor.values()) {
            Team team = game.getTeams().get(color);
            if (team == null) {
                continue; // Skip teams that don't exist in this arena
            }

            if (slot >= 45) {
                break; // Don't exceed inventory size
            }

            ItemStack item = createTeamItem(color, team, currentTeam, maxTeamSize);
            inventory.setItem(slot, item);
            slotToTeam.put(slot, color);

            // Move to next row every 9 slots
            slot++;
            if ((slot - 10) % 9 == 0) {
                slot += 1; // Skip a column
            }
        }

        // Store slot mapping for click handling
        menuSlots.put(player.getUniqueId(), slotToTeam);

        player.openInventory(inventory);
    }

    /**
     * Handle click on team selection GUI
     */
    public void handleMenuClick(Player player, int slot, Game game) {
        Map<Integer, TeamColor> slots = menuSlots.get(player.getUniqueId());
        if (slots == null) {
            return;
        }

        TeamColor selectedColor = slots.get(slot);
        if (selectedColor == null) {
            return;
        }

        // Change team
        game.changeTeam(player, selectedColor);
        
        // Refresh the GUI
        openTeamSelection(player, game);
    }

    /**
     * Check if a title matches the team selection GUI
     */
    public static boolean isTeamSelectionTitle(String title) {
        return ChatColor.stripColor(title).equalsIgnoreCase(ChatColor.stripColor(GUI_TITLE));
    }

    /**
     * Clear menu slots for a player (call when they close inventory)
     */
    public void clearMenuSlots(UUID uuid) {
        menuSlots.remove(uuid);
    }

    /**
     * Create an item stack representing a team
     */
    private ItemStack createTeamItem(TeamColor color, Team team, TeamColor currentTeam, int maxTeamSize) {
        Material material = getTeamMaterial(color);
        ItemStack item = new ItemStack(material);

        // Use leather armor for colored display
        if (material == Material.LEATHER_BOOTS) {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            if (meta != null) {
                meta.setColor(org.bukkit.Color.fromRGB(getTeamRGB(color)));
                item.setItemMeta(meta);
            }
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                item.setItemMeta(meta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Set display name
        String displayName = color.getChatColor() + ChatColor.BOLD.toString() + color.name() + " Team";
        if (color == currentTeam) {
            displayName += ChatColor.GRAY + " (Current)";
        }
        meta.setDisplayName(displayName);

        // Build lore
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + team.getPlayers().size() + "/" + maxTeamSize);
        
        if (team.getPlayers().size() > 0) {
            lore.add("");
            lore.add(ChatColor.GRAY + "Members:");
            for (ch.framedev.bedwars.player.GamePlayer teamPlayer : team.getPlayers()) {
                Player p = Bukkit.getPlayer(teamPlayer.getUuid());
                if (p != null) {
                    lore.add(ChatColor.WHITE + " • " + p.getName());
                }
            }
        }

        lore.add("");
        if (color == currentTeam) {
            lore.add(ChatColor.RED + "You are already on this team!");
        } else if (team.getPlayers().size() >= maxTeamSize) {
            lore.add(ChatColor.RED + "This team is full!");
        } else {
            lore.add(ChatColor.GREEN + "Click to join this team!");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Get material for team color
     */
    private Material getTeamMaterial(TeamColor color) {
        switch (color) {
            case RED:
                return Material.RED_WOOL;
            case BLUE:
                return Material.BLUE_WOOL;
            case GREEN:
                return Material.GREEN_WOOL;
            case YELLOW:
                return Material.YELLOW_WOOL;
            case AQUA:
                return Material.CYAN_WOOL;
            case WHITE:
                return Material.WHITE_WOOL;
            case PINK:
                return Material.PINK_WOOL;
            case GRAY:
                return Material.GRAY_WOOL;
            default:
                return Material.LEATHER_BOOTS;
        }
    }

    /**
     * Get RGB color for team
     */
    private int getTeamRGB(TeamColor color) {
        switch (color) {
            case RED:
                return 0xFF5555;
            case BLUE:
                return 0x5555FF;
            case GREEN:
                return 0x55FF55;
            case YELLOW:
                return 0xFFFF55;
            case AQUA:
                return 0x55FFFF;
            case WHITE:
                return 0xFFFFFF;
            case PINK:
                return 0xFF55FF;
            case GRAY:
                return 0xAAAAAA;
            default:
                return 0xFFFFFF;
        }
    }
}
