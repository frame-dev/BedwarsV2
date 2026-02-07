package ch.framedev.bedwars.utils;

import ch.framedev.BedWarsPlugin;

/**
 * Centralized debug logger controlled by config.
 */
public class DebugLogger {

    private static final String PREFIX = "[Debug] ";
    private final BedWarsPlugin plugin;

    public DebugLogger(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("debug.enabled", false);
    }

    public boolean isVerbose() {
        return isEnabled() && plugin.getConfig().getBoolean("debug.verbose-logging", false);
    }

    public void debug(String message) {
        if (isEnabled()) {
            plugin.getLogger().info(PREFIX + message);
        }
    }

    public void verbose(String message) {
        if (isVerbose()) {
            plugin.getLogger().info(PREFIX + message);
        }
    }
}
