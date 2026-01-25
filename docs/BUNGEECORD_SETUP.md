# BungeeCord Setup Guide

This guide explains how to configure and use the BedWars plugin on a BungeeCord network.

## 📋 Overview

The BedWars plugin now supports BungeeCord, allowing players to:
- Automatically be sent to a lobby server when games end
- Use `/bedwars lobby` command to return to lobby
- Seamlessly integrate with multi-server networks

## 🔧 Configuration

### 1. Enable BungeeCord in `config.yml`

```yaml
# BungeeCord Settings
bungeecord:
  # Enable BungeeCord support
  enabled: true
  
  # Lobby server name (must match BungeeCord config)
  lobby-server: "lobby"
  
  # Auto-send to lobby when game ends
  send-to-lobby-on-end: true
  
  # Auto-send to lobby when player leaves game
  send-to-lobby-on-leave: false
  
  # Delay before sending to lobby (in seconds)
  lobby-send-delay: 3
```

### 2. BungeeCord Configuration

In your BungeeCord `config.yml`, ensure you have servers configured:

```yaml
servers:
  lobby:
    address: localhost:25565
    motd: 'Lobby Server'
  bedwars1:
    address: localhost:25566
    motd: 'BedWars #1'
  bedwars2:
    address: localhost:25567
    motd: 'BedWars #2'
```

### 3. Server Names

⚠️ **Important**: The `lobby-server` name in your BedWars config.yml must **exactly match** a server name in your BungeeCord configuration.

## 📝 Features

### Automatic Lobby Redirection

When `send-to-lobby-on-end` is enabled:
- Players are automatically sent to the lobby server when a game ends
- Configurable delay before sending (default: 3 seconds)
- Shows "Sending you to lobby..." message

### Manual Lobby Command

Players can use `/bedwars lobby` to:
- Leave their current game
- Return to the lobby server instantly
- Works from any game state

### Permission-Based Access

```yaml
permissions:
  bedwars.bungee:
    description: BungeeCord server switching
    default: true
```

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/bedwars lobby` | Return to lobby server | `bedwars.bungee` |

## 🔍 How It Works

### Plugin Messaging Channels

The plugin registers the `BungeeCord` plugin messaging channel to communicate with the proxy:

```java
// Automatically registered when bungeecord.enabled = true
plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
```

### Server Switching

When sending a player to another server:

1. Plugin creates a BungeeCord message with "Connect" subchannel
2. Specifies target server name
3. Sends message through player's connection
4. BungeeCord proxy handles the actual server switch

### Game End Flow

```
Game Ends → Wait 5 seconds (show winners) → 
Wait lobby-send-delay seconds → Send to lobby server
```

## 🛠️ Advanced Configuration

### Multiple Lobby Servers

If you have multiple lobby servers, you can implement load balancing:

```java
// Example: Round-robin lobby selection
String[] lobbyServers = {"lobby1", "lobby2", "lobby3"};
String selectedLobby = lobbyServers[playerCount % lobbyServers.length];
bungeeManager.sendPlayerToServer(player, selectedLobby);
```

### Custom Server Routing

You can use the `BungeeManager` API in custom code:

```java
// Send to specific server
plugin.getBungeeManager().sendPlayerToServer(player, "minigames");

// Get player count on a server
plugin.getBungeeManager().getPlayerCount(player, "bedwars1");

// Get list of all servers
plugin.getBungeeManager().getServerList(player);

// Forward custom messages
plugin.getBungeeManager().forwardToServer(player, "lobby", "MyChannel", data);
```

## 📊 BungeeManager API

### Public Methods

```java
// Check if BungeeCord is enabled
boolean isEnabled()

// Get configured lobby server name
String getLobbyServer()

// Send player to specific server
void sendPlayerToServer(Player player, String server)

// Send player to lobby server
void sendPlayerToLobby(Player player)

// Get player count on server
void getPlayerCount(Player player, String server)

// Get player count across all servers
void getPlayerCountAll(Player player)

// Get list of servers
void getServerList(Player player)

// Get current server name
void getCurrentServer(Player player)

// Send message to player on another server
void sendMessage(Player sender, String targetPlayer, String message)

// Forward plugin message to server(s)
void forwardToServer(Player player, String server, String subchannel, byte[] data)
void forwardToAll(Player player, String subchannel, byte[] data)
```

## 🐛 Troubleshooting

### Players aren't being sent to lobby

1. Check `bungeecord.enabled` is `true`
2. Verify `lobby-server` name matches BungeeCord config exactly
3. Ensure lobby server is running and reachable
4. Check BungeeCord console for connection errors

### "BungeeCord is not enabled" message

1. Confirm `bungeecord.enabled: true` in config.yml
2. Restart the server after config changes
3. Check server logs for BungeeManager initialization message

### Permission denied for /bedwars lobby

1. Grant `bedwars.bungee` permission
2. Check your permissions plugin configuration
3. Default is `true`, but may be overridden

## 🚀 Network Setup Example

### Typical BungeeCord Network Structure

```
BungeeCord Proxy (port 25577)
├── Lobby Server (port 25565)
│   └── Hub plugin, spawn management
├── BedWars Server 1 (port 25566)
│   └── BedWars plugin (arena: Ancient)
├── BedWars Server 2 (port 25567)
│   └── BedWars plugin (arena: Castle)
└── BedWars Server 3 (port 25568)
    └── BedWars plugin (arena: Space)
```

### Configuration on Each BedWars Server

```yaml
# BedWars Server 1, 2, 3 config.yml
bungeecord:
  enabled: true
  lobby-server: "lobby"
  send-to-lobby-on-end: true
  send-to-lobby-on-leave: false
  lobby-send-delay: 3
```

## 📝 Notes

- Players are sent to lobby with a configurable delay
- The `send-to-lobby-on-leave` option allows instant lobby sending when players manually leave
- BungeeCord must be properly configured with all server addresses
- Plugin messaging channels are automatically registered and unregistered

## 🔒 Security

- Only registered plugin messaging channels are used
- Server switching is handled entirely by BungeeCord proxy
- No direct server-to-server communication occurs
- All messages follow BungeeCord protocol specifications

## 📚 Additional Resources

- [BungeeCord Plugin Messages](https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/)
- [Spigot BungeeCord Tutorial](https://www.spigotmc.org/wiki/spigot-bungeecord-configuration/)
- [BungeeCord Configuration Guide](https://www.spigotmc.org/wiki/bungeecord-configuration-guide/)
