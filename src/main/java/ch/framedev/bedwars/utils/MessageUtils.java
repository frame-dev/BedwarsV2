package ch.framedev.bedwars.utils;

import org.bukkit.ChatColor;

/**
 * Utility class for message formatting
 */
public class MessageUtils {

    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "BedWars" + ChatColor.GRAY + "] ";

    public static String format(String message) {
        return PREFIX + ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String error(String message) {
        return PREFIX + ChatColor.RED + message;
    }

    public static String success(String message) {
        return PREFIX + ChatColor.GREEN + message;
    }

    public static String warning(String message) {
        return PREFIX + ChatColor.YELLOW + message;
    }

    public static String info(String message) {
        return PREFIX + ChatColor.GRAY + message;
    }
}
