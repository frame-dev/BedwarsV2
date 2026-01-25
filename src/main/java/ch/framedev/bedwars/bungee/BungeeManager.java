package ch.framedev.bedwars.bungee;

import ch.framedev.BedWarsPlugin;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Handles BungeeCord messaging for cross-server communication
 */
public class BungeeManager implements PluginMessageListener {

    private final BedWarsPlugin plugin;
    private boolean bungeeCordEnabled;
    private String lobbyServer;

    public BungeeManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.bungeeCordEnabled = plugin.getConfig().getBoolean("bungeecord.enabled", false);
        this.lobbyServer = plugin.getConfig().getString("bungeecord.lobby-server", "lobby");

        if (bungeeCordEnabled) {
            // Register BungeeCord channels
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
            plugin.getLogger().info("BungeeCord support enabled! Lobby server: " + lobbyServer);
        } else {
            plugin.getLogger().info("BungeeCord support disabled in config");
        }
    }

    /**
     * Check if BungeeCord is enabled
     */
    public boolean isEnabled() {
        return bungeeCordEnabled;
    }

    /**
     * Get the lobby server name
     */
    public String getLobbyServer() {
        return lobbyServer;
    }

    /**
     * Send a player to another server
     */
    public void sendPlayerToServer(Player player, String server) {
        if (!bungeeCordEnabled) {
            player.sendMessage("§cBungeeCord is not enabled!");
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        plugin.getLogger().info("Sending player " + player.getName() + " to server: " + server);
    }

    /**
     * Send a player to the lobby server
     */
    public void sendPlayerToLobby(Player player) {
        sendPlayerToServer(player, lobbyServer);
    }

    /**
     * Get the player count on a server
     */
    public void getPlayerCount(Player player, String server) {
        if (!bungeeCordEnabled)
            return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF(server);

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Get the player count on all servers
     */
    public void getPlayerCountAll(Player player) {
        if (!bungeeCordEnabled)
            return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF("ALL");

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Get list of servers
     */
    public void getServerList(Player player) {
        if (!bungeeCordEnabled)
            return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServers");

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Get the current server name
     */
    public void getCurrentServer(Player player) {
        if (!bungeeCordEnabled)
            return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Send a message to a player on another server
     */
    public void sendMessage(Player sender, String targetPlayer, String message) {
        if (!bungeeCordEnabled)
            return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Message");
        out.writeUTF(targetPlayer);
        out.writeUTF(message);

        sender.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Forward a plugin message to another server
     */
    public void forwardToServer(Player player, String server, String subchannel, byte[] data) {
        if (!bungeeCordEnabled)
            return;

        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);

        try {
            msgout.writeUTF("Forward");
            msgout.writeUTF(server);
            msgout.writeUTF(subchannel);
            msgout.writeShort(data.length);
            msgout.write(data);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to forward message: " + e.getMessage());
            return;
        }

        player.sendPluginMessage(plugin, "BungeeCord", msgbytes.toByteArray());
    }

    /**
     * Forward a plugin message to all servers
     */
    public void forwardToAll(Player player, String subchannel, byte[] data) {
        forwardToServer(player, "ALL", subchannel, data);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        // Guard against potential null messages to satisfy non-null contract
        if (message == null) {
            plugin.getLogger().warning("Received null plugin message on channel " + channel);
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();

        switch (subchannel) {
            case "PlayerCount":
                String server = in.readUTF();
                int playerCount = in.readInt();
                plugin.getLogger().info("Server " + server + " has " + playerCount + " players");
                break;

            case "PlayerList":
                String serverName = in.readUTF();
                String[] players = in.readUTF().split(", ");
                plugin.getLogger().info("Players on " + serverName + ": " + String.join(", ", players));
                break;

            case "GetServers":
                String[] servers = in.readUTF().split(", ");
                plugin.getLogger().info("Available servers: " + String.join(", ", servers));
                break;

            case "GetServer":
                String currentServer = in.readUTF();
                plugin.getLogger().info("Current server: " + currentServer);
                break;

            default:
                plugin.getLogger().warning("Unknown BungeeCord subchannel: " + subchannel);
                break;
        }
    }

    /**
     * Unregister BungeeCord channels
     */
    public void disable() {
        if (bungeeCordEnabled) {
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
        }
    }
}
