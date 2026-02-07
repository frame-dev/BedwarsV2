package ch.framedev.bedwars.cloudnet;

import ch.framedev.BedWarsPlugin;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * CloudNet v4 integration helper.
 */
public class CloudNetManager {

    private final BedWarsPlugin plugin;
    private final boolean available;

    public CloudNetManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.available = isBridgeAvailable();
        if (available) {
            plugin.getDebugLogger().debug("CloudNet Bridge detected");
        }
    }

    public boolean isEnabled() {
        return available && isModeEnabled();
    }

    public boolean isOneArenaPerServer() {
        return plugin.getConfig().getBoolean("cloudnet.one-arena-per-server", true);
    }

    public boolean useForMapVoting() {
        return plugin.getConfig().getBoolean("cloudnet.use-for-map-voting", true);
    }

    public boolean connectToLobby(Player player) {
        String lobbyService = plugin.getConfig().getString("cloudnet.lobby-service", "Lobby-1");
        if (lobbyService == null || lobbyService.isBlank()) {
            return false;
        }
        return connectToService(player, lobbyService);
    }

    public boolean connectToArena(Player player, String arenaName) {
        String service = resolveServiceForArena(arenaName);
        if (service == null) {
            plugin.getMessageManager().sendMessage(player, "cloudnet.service-not-found", arenaName);
            return false;
        }
        return connectToService(player, service);
    }

    public boolean connectToService(Player player, String service) {
        if (!isEnabled()) {
            return false;
        }

        try {
            Class<?> helperClass = Class.forName("eu.cloudnetservice.cloudnet.ext.bridge.BridgeServiceHelper");
            Method method = helperClass.getMethod("connectService", Player.class, String.class);
            method.invoke(null, player, service);
            plugin.getMessageManager().sendMessage(player, "cloudnet.connecting", service);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("CloudNet connect failed: " + e.getMessage());
            plugin.getMessageManager().sendMessage(player, "cloudnet.connect-failed", service);
            return false;
        }
    }

    public String resolveServiceForArena(String arenaName) {
        if (arenaName == null) {
            return null;
        }

        String mapped = plugin.getConfig().getString("cloudnet.arena-service-map." + arenaName);
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }

        String prefix = plugin.getConfig().getString("cloudnet.arena-service-prefix", "");
        return prefix + arenaName;
    }

    public List<String> getGameServices() {
        List<String> configured = plugin.getConfig().getStringList("cloudnet.game-services");
        if (configured != null && !configured.isEmpty()) {
            return new ArrayList<>(configured);
        }

        return Collections.emptyList();
    }

    private boolean isBridgeAvailable() {
        try {
            Class.forName("eu.cloudnetservice.cloudnet.ext.bridge.BridgeServiceHelper");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isModeEnabled() {
        String mode = plugin.getConfig().getString("network.mode", "");
        if (mode != null && !mode.isBlank()) {
            return "cloudnet".equalsIgnoreCase(mode);
        }
        return plugin.getConfig().getBoolean("cloudnet.enabled", false);
    }
}
