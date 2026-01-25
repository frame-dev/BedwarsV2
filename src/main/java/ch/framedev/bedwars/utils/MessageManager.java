package ch.framedev.bedwars.utils;

import ch.framedev.BedWarsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all plugin messages from messages.yml
 */
public class MessageManager {

    private final BedWarsPlugin plugin;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messageCache;

    public MessageManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<>();
        loadMessages();
    }

    /**
     * Load messages from messages.yml
     */
    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Create messages.yml if it doesn't exist
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from jar
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
        }

        // Clear cache to force reload
        messageCache.clear();

        plugin.getLogger().info("Loaded " + messagesConfig.getKeys(true).size() + " message keys");
    }

    /**
     * Reload messages configuration
     */
    public void reload() {
        loadMessages();
    }

    /**
     * Get a message by key
     */
    public String getMessage(String key) {
        // Check cache first
        if (messageCache.containsKey(key)) {
            return messageCache.get(key);
        }

        // Get from config
        String message = messagesConfig.getString(key);
        if (message == null) {
            plugin.getLogger().warning("Missing message key: " + key);
            return "&cMissing message: " + key;
        }

        // Translate color codes
        message = ChatColor.translateAlternateColorCodes('&', message);

        // Cache it
        messageCache.put(key, message);

        return message;
    }

    /**
     * Get a message with placeholders
     */
    public String getMessage(String key, Object... args) {
        String message = getMessage(key);

        // Replace placeholders
        try {
            return MessageFormat.format(message, args);
        } catch (IllegalArgumentException e) {
            plugin.getLogger()
                    .warning("Failed to format message: " + key + " with args: " + java.util.Arrays.toString(args));
            return message;
        }
    }

    /**
     * Send a message to a player
     */
    public void sendMessage(Player player, String key, Object... args) {
        player.sendMessage(getMessage(key, args));
    }

    /**
     * Send a message to a command sender
     */
    public void sendMessage(CommandSender sender, String key, Object... args) {
        sender.sendMessage(getMessage(key, args));
    }

    /**
     * Broadcast a message to all players
     */
    public void broadcast(String key, Object... args) {
        String message = getMessage(key, args);
        plugin.getServer().broadcastMessage(message);
    }
}
