package ch.framedev.bedwars.team;

import org.bukkit.ChatColor;

/**
 * Represents the different team colors in BedWars
 */
public enum TeamColor {
    RED(ChatColor.RED, org.bukkit.Color.RED),
    BLUE(ChatColor.BLUE, org.bukkit.Color.BLUE),
    GREEN(ChatColor.GREEN, org.bukkit.Color.GREEN),
    YELLOW(ChatColor.YELLOW, org.bukkit.Color.YELLOW),
    AQUA(ChatColor.AQUA, org.bukkit.Color.AQUA),
    WHITE(ChatColor.WHITE, org.bukkit.Color.WHITE),
    PINK(ChatColor.LIGHT_PURPLE, org.bukkit.Color.FUCHSIA),
    GRAY(ChatColor.GRAY, org.bukkit.Color.GRAY);

    private final ChatColor chatColor;
    private final org.bukkit.Color color;

    TeamColor(ChatColor chatColor, org.bukkit.Color color) {
        this.chatColor = chatColor;
        this.color = color;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public org.bukkit.Color getColor() {
        return color;
    }
}
