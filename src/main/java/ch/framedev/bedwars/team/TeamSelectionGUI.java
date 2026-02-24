package ch.framedev.bedwars.team;

import ch.framedev.bedwars.game.Game;
import ch.framedev.bedwars.game.GameState;
import ch.framedev.bedwars.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the team selection GUI in the lobby.
 */
public class TeamSelectionGUI {

    private static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&6Team Selection");
    private static final int GUI_SIZE = 54;

    // Nice layout slots (4 rows x 7 columns)
    private static final int[] TEAM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final Map<UUID, Map<Integer, TeamColor>> menuSlots = new ConcurrentHashMap<>();

    /**
     * Open the team selection GUI for a player.
     */
    public void openTeamSelection(Player player, Game game) {
        if (player == null || game == null) return;

        // Only allow in lobby states
        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
            player.closeInventory();
            return;
        }

        // Clear any old mapping for this player
        menuSlots.remove(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        // Fill background
        fill(inv, createFiller());

        GamePlayer gp = game.getGamePlayer(player);
        TeamColor currentTeam = (gp != null && gp.getTeam() != null) ? gp.getTeam().getColor() : null;

        int teamCount = Math.max(1, game.getTeams().size());
        int maxTeamSize = Math.max(1, game.getArena().getMaxPlayers() / teamCount);

        Map<Integer, TeamColor> slotToTeam = new HashMap<>();
        int index = 0;

        // Only show teams that exist in this arena
        for (TeamColor color : TeamColor.values()) {
            Team team = game.getTeams().get(color);
            if (team == null) continue;
            if (index >= TEAM_SLOTS.length) break;

            int slot = TEAM_SLOTS[index++];
            inv.setItem(slot, createTeamItem(color, team, currentTeam, maxTeamSize));
            slotToTeam.put(slot, color);
        }

        // Optionally: show "Random Team" button
        // inv.setItem(49, createRandomTeamItem());

        menuSlots.put(player.getUniqueId(), slotToTeam);
        player.openInventory(inv);
    }

    /**
     * Handle click on team selection GUI.
     */
    public void handleMenuClick(Player player, int slot, Game game) {
        if (player == null || game == null) return;

        Map<Integer, TeamColor> slots = menuSlots.get(player.getUniqueId());
        if (slots == null) return;

        TeamColor selected = slots.get(slot);
        if (selected == null) return;

        // Only allow changes in lobby
        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
            player.closeInventory();
            return;
        }

        // Attempt team change
        boolean changed = game.changeTeam(player, selected);

        // Refresh only if it actually changed
        if (changed) {
            openTeamSelection(player, game);
        } else {
            // Still refresh so lore updates (e.g., team became full)
            openTeamSelection(player, game);
        }
    }

    /**
     * Check if a title matches the team selection GUI.
     */
    public static boolean isTeamSelectionTitle(String title) {
        return ChatColor.stripColor(title).equalsIgnoreCase(ChatColor.stripColor(GUI_TITLE));
    }

    /**
     * Clear menu slots for a player (call when they close inventory).
     */
    public void clearMenuSlots(UUID uuid) {
        if (uuid != null) menuSlots.remove(uuid);
    }

    /* --------------------------------------------------------------------- */
    /* Item building                                                          */
    /* --------------------------------------------------------------------- */

    private ItemStack createTeamItem(TeamColor color, Team team, TeamColor currentTeam, int maxTeamSize) {
        Material material = getTeamMaterialSafe(color);
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Color leather boots if we used them as fallback
        if (meta instanceof LeatherArmorMeta lam) {
            lam.setColor(org.bukkit.Color.fromRGB(getTeamRGB(color)));
            meta = lam;
        }

        String displayName = color.getChatColor() + "" + ChatColor.BOLD + color.name() + " Team";
        if (color == currentTeam) {
            displayName += ChatColor.GRAY + " (Current)";
        }
        meta.setDisplayName(displayName);

        int size = team.getPlayers().size();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + " ");
        lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + size + "/" + maxTeamSize);

        if (size > 0) {
            lore.add(ChatColor.DARK_GRAY + " ");
            lore.add(ChatColor.GRAY + "Members:");
            for (GamePlayer teamPlayer : team.getPlayers()) {
                Player p = Bukkit.getPlayer(teamPlayer.getUuid());
                if (p != null) {
                    lore.add(ChatColor.WHITE + " • " + p.getName());
                }
            }
        }

        lore.add(ChatColor.DARK_GRAY + " ");

        if (color == currentTeam) {
            lore.add(ChatColor.RED + "You are already on this team!");
        } else if (size >= maxTeamSize) {
            lore.add(ChatColor.RED + "This team is full!");
        } else {
            lore.add(ChatColor.GREEN + "Click to join this team!");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        Material mat = materialOr(Material.GRAY_STAINED_GLASS_PANE, Material.GLASS_PANE);
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inv, ItemStack filler) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Version-safe materials                                                 */
    /* --------------------------------------------------------------------- */

    private Material getTeamMaterialSafe(TeamColor color) {
        // 1.13+ wool materials
        Material modern = switch (color) {
            case RED -> Material.matchMaterial("RED_WOOL");
            case BLUE -> Material.matchMaterial("BLUE_WOOL");
            case GREEN -> Material.matchMaterial("GREEN_WOOL");
            case YELLOW -> Material.matchMaterial("YELLOW_WOOL");
            case AQUA -> Material.matchMaterial("CYAN_WOOL");
            case WHITE -> Material.matchMaterial("WHITE_WOOL");
            case PINK -> Material.matchMaterial("PINK_WOOL");
            case GRAY -> Material.matchMaterial("GRAY_WOOL");
            default -> null;
        };
        if (modern != null) return modern;

        // Legacy fallback (1.8–1.12): wool is "WOOL" with durability, but we’re not using durability here.
        // To keep it simple and stable without NMS: fallback to leather boots colored.
        return Material.LEATHER_BOOTS;
    }

    private Material materialOr(Material preferred, Material fallback) {
        return preferred != null ? preferred : fallback;
    }

    private int getTeamRGB(TeamColor color) {
        return switch (color) {
            case RED -> 0xFF5555;
            case BLUE -> 0x5555FF;
            case GREEN -> 0x55FF55;
            case YELLOW -> 0xFFFF55;
            case AQUA -> 0x55FFFF;
            case WHITE -> 0xFFFFFF;
            case PINK -> 0xFF55FF;
            case GRAY -> 0xAAAAAA;
            default -> 0xFFFFFF;
        };
    }
}