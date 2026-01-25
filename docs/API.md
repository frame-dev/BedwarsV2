# API Documentation

Developer guide for extending the BedWars plugin.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Core Classes](#core-classes)
3. [Event System](#event-system)
4. [Manager Classes](#manager-classes)
5. [Utilities](#utilities)
6. [Examples](#examples)

---

## Getting Started

### Maven Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>ch.framedev</groupId>
    <artifactId>bedwars</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

```java
import ch.framedev.bedwars.game.GameManager;
import ch.framedev.bedwars.game.Game;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    private GameManager gameManager;
    
    @Override
    public void onEnable() {
        // Get BedWars plugin
        BedWarsPlugin bedwars = (BedWarsPlugin) getServer().getPluginManager().getPlugin("BedWars");
        
        if (bedwars != null) {
            this.gameManager = bedwars.getGameManager();
        }
    }
}
```

---

## Core Classes

### Game Class

Main game logic and state management.

```java
public class Game {
    // Get game state
    public GameState getState();
    
    // Get arena
    public Arena getArena();
    
    // Player management
    public void addPlayer(Player player, Team team);
    public void removePlayer(Player player);
    public List<Player> getPlayers();
    public List<Player> getPlayersInTeam(Team team);
    
    // Team management
    public List<Team> getTeams();
    public Team getTeam(Player player);
    public Team getTeam(String teamColor);
    
    // Game control
    public void start();
    public void stop();
    public void reset();
    
    // Game info
    public boolean isRunning();
    public boolean canJoin();
    public int getPlayerCount();
    public int getMaxPlayers();
}
```

### Arena Class

Arena configuration and management.

```java
public class Arena {
    // Basic info
    public String getName();
    public String getDisplayName();
    
    // Locations
    public Location getLobbySpawn();
    public Location getSpectatorSpawn();
    
    // Teams
    public Map<String, Team> getTeams();
    public Team getTeam(String color);
    
    // Limits
    public int getMinPlayers();
    public int getMaxPlayers();
    
    // Generators
    public List<ResourceGenerator> getGenerators();
    public ResourceGenerator getGenerator(String name);
}
```

### Team Class

Team data and management.

```java
public class Team {
    // Team info
    public String getName();
    public TeamColor getColor();
    public Location getSpawn();
    public Location getBed();
    
    // Players
    public Set<GamePlayer> getPlayers();
    public void addPlayer(GamePlayer player);
    public void removePlayer(GamePlayer player);
    
    // Bed status
    public boolean hasBed();
    public void destroyBed();
    
    // Team state
    public boolean isEliminated();
    public boolean canRespawn();
    
    // Resources
    public int getIron();
    public int getGold();
    public int getDiamonds();
    public int getEmeralds();
    public void addResources(String type, int amount);
}
```

### GamePlayer Class

Player wrapper with stats and data.

```java
public class GamePlayer {
    // Player info
    public Player getBukkitPlayer();
    public Team getTeam();
    public Game getGame();
    
    // Statistics
    public int getKills();
    public int getDeaths();
    public int getFinalKills();
    public int getBedsDestroyed();
    public void addKill();
    public void addDeath();
    public void addFinalKill();
    public void addBedDestroyed();
    
    // Inventory
    public void setStartingItems();
    public void clearInventory();
    
    // Armor
    public void setTeamArmor();
    public void removeTeamArmor();
}
```

---

## Event System

### Custom Events

Listen to BedWars events in your plugin:

```java
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import ch.framedev.bedwars.events.*;

public class MyEventListener implements Listener {
    
    @EventHandler
    public void onGameStart(GameStartEvent event) {
        Game game = event.getGame();
        // Handle game start
    }
    
    @EventHandler
    public void onGameEnd(GameEndEvent event) {
        Game game = event.getGame();
        Team winner = event.getWinningTeam();
        // Handle game end
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinGameEvent event) {
        Player player = event.getPlayer();
        Game game = event.getGame();
        // Handle player join
    }
    
    @EventHandler
    public void onPlayerLeave(PlayerLeaveGameEvent event) {
        Player player = event.getPlayer();
        // Handle player leave
    }
    
    @EventHandler
    public void onBedDestroy(BedDestroyEvent event) {
        Team team = event.getTeam();
        Player destroyer = event.getPlayer();
        // Handle bed destruction
    }
}
```

### Available Events

**Game Events:**
- `GameStartEvent` - Game starts
- `GameEndEvent` - Game ends
- `GameResetEvent` - Game resets

**Player Events:**
- `PlayerJoinGameEvent` - Player joins game
- `PlayerLeaveGameEvent` - Player leaves game
- `PlayerRespawnEvent` - Player respawns
- `PlayerEliminationEvent` - Player eliminated

**Game Mechanic Events:**
- `BedDestroyEvent` - Bed destroyed
- `TeamEliminationEvent` - Team eliminated
- `ResourceGenerateEvent` - Resource drops
- `ShopPurchaseEvent` - Item purchased
- `UpgradePurchaseEvent` - Upgrade purchased

---

## Manager Classes

### GameManager

Manages all active games.

```java
public class GameManager {
    // Game creation
    public Game createGame(Arena arena);
    public void deleteGame(Game game);
    
    // Game lookup
    public Game getGame(Player player);
    public Game getGame(String arenaName);
    public List<Game> getAllGames();
    
    // Game status
    public List<Game> getRunningGames();
    public List<Game> getWaitingGames();
    public int getGameCount();
}
```

### ArenaManager

Manages arena configurations.

```java
public class ArenaManager {
    // Arena management
    public Arena createArena(String name);
    public Arena getArena(String name);
    public List<Arena> getAllArenas();
    
    // Save/Load
    public void saveArena(Arena arena);
    public void loadArenas();
    public void deleteArena(String name);
    
    // Validation
    public boolean isValid(Arena arena);
    public List<String> validate(Arena arena);
}
```

### ShopManager

Manages shop items and purchases.

```java
public class ShopManager {
    // Item management
    public ShopItem getItem(String id);
    public List<ShopItem> getItemsByCategory(String category);
    public List<ShopCategory> getCategories();
    
    // Purchase handling
    public boolean canPurchase(GamePlayer player, ShopItem item);
    public void purchase(GamePlayer player, ShopItem item);
}
```

### StatsManager

Manages player statistics.

```java
public class StatsManager {
    // Get statistics
    public PlayerStats getStats(UUID playerUuid);
    public PlayerStats getStats(String playerName);
    
    // Update statistics
    public void updateStats(UUID playerUuid, PlayerStats stats);
    public void incrementStat(UUID playerUuid, String stat, int amount);
    
    // Query
    public List<PlayerStats> getTopPlayers(String stat, int limit);
    public double getWinRate(UUID playerUuid);
    public double getKDRatio(UUID playerUuid);
}
```

### DatabaseManager

Handles database operations.

```java
public class DatabaseManager {
    // Connection
    public void connect();
    public void disconnect();
    public boolean isConnected();
    
    // Player stats
    public void savePlayerStats(UUID playerUuid, PlayerStats stats);
    public PlayerStats loadPlayerStats(UUID playerUuid);
    
    // Game history
    public void saveGame(Game game);
    public List<Game> getGameHistory(int limit);
}
```

---

## Utilities

### ItemBuilder

Helper for creating items.

```java
import ch.framedev.bedwars.utils.ItemBuilder;

ItemBuilder builder = new ItemBuilder(Material.DIAMOND_SWORD)
    .setDisplayName("§6Epic Sword")
    .addLore("§7A powerful weapon")
    .addEnchantment(Enchantment.SHARPNESS, 1)
    .setAmount(1);

ItemStack item = builder.build();
```

### MessageUtils

Format and send messages.

```java
import ch.framedev.bedwars.utils.MessageUtils;

// Send colored message
MessageUtils.sendMessage(player, "§aSuccess!");

// Broadcast to team
MessageUtils.broadcastToTeam(team, "§e{TEAM} is winning!");

// Broadcast to game
MessageUtils.broadcastToGame(game, "§6Game started!");
```

### LocationUtils

Location serialization utilities.

```java
import ch.framedev.bedwars.utils.LocationUtils;

// Serialize location to string
Location loc = new Location(world, 100, 64, 100);
String serialized = LocationUtils.serialize(loc);
// Result: "world,100.0,64.0,100.0,0.0,0.0"

// Deserialize location from string
Location restored = LocationUtils.deserialize(serialized);
```

---

## Examples

### Example 1: Listen to Game Events

```java
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import ch.framedev.bedwars.events.GameEndEvent;

public class MyGameListener extends JavaPlugin implements Listener {
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @EventHandler
    public void onGameEnd(GameEndEvent event) {
        Team winner = event.getWinningTeam();
        broadcastWinner(winner);
    }
    
    private void broadcastWinner(Team team) {
        getLogger().info("Game ended! Winner: " + team.getName());
    }
}
```

### Example 2: Get Player's Game and Team

```java
public void checkPlayerTeam(Player player) {
    BedWarsPlugin plugin = (BedWarsPlugin) Bukkit.getPluginManager().getPlugin("BedWars");
    GameManager gameManager = plugin.getGameManager();
    
    Game game = gameManager.getGame(player);
    if (game != null && game.isRunning()) {
        Team team = game.getTeam(player);
        player.sendMessage("You are on team: " + team.getName());
    } else {
        player.sendMessage("You are not in a game");
    }
}
```

### Example 3: Custom Shop Integration

```java
public void purchaseItem(Player player, ShopItem item) {
    GamePlayer gamePlayer = getGamePlayer(player);
    ShopManager shopManager = getShopManager();
    
    if (shopManager.canPurchase(gamePlayer, item)) {
        shopManager.purchase(gamePlayer, item);
        player.sendMessage("§aPurchased: " + item.getDisplayName());
    } else {
        player.sendMessage("§cInsufficient resources!");
    }
}
```

### Example 4: Track Statistics

```java
public void displayPlayerStats(String playerName) {
    StatsManager statsManager = getStatsManager();
    UUID playerUuid = getPlayerUuid(playerName);
    
    PlayerStats stats = statsManager.getStats(playerUuid);
    if (stats != null) {
        getLogger().info("Player: " + playerName);
        getLogger().info("Wins: " + stats.getWins());
        getLogger().info("K/D Ratio: " + statsManager.getKDRatio(playerUuid));
        getLogger().info("Win Rate: " + statsManager.getWinRate(playerUuid) + "%");
    }
}
```

### Example 5: Create Custom Arena

```java
public void createCustomArena() {
    ArenaManager arenaManager = getArenaManager();
    Arena arena = arenaManager.createArena("custom_arena");
    
    // Configure arena
    arena.setDisplayName("Custom Arena");
    arena.setMinPlayers(2);
    arena.setMaxPlayers(8);
    
    // Save
    arenaManager.saveArena(arena);
    getLogger().info("Arena created: " + arena.getName());
}
```

---

## Best Practices

### 1. Always Check if Game Exists
```java
Game game = gameManager.getGame(player);
if (game == null || !game.isRunning()) {
    return; // Player not in active game
}
```

### 2. Use Event System
Don't poll for changes, listen to events instead:
```java
@EventHandler
public void onGameStart(GameStartEvent event) {
    // Reacts automatically to game starts
}
```

### 3. Handle Async Operations
Some operations might be async:
```java
Bukkit.getScheduler().runTaskAsync(plugin, () -> {
    PlayerStats stats = statsManager.getStats(uuid);
    Bukkit.getScheduler().runTask(plugin, () -> {
        updateUI(stats);
    });
});
```

### 4. Check Permissions
Always verify player has necessary permissions:
```java
if (!player.hasPermission("bedwars.join")) {
    player.sendMessage("No permission!");
    return;
}
```

### 5. Handle Null Values
API methods can return null:
```java
Team team = game.getTeam(player);
if (team != null) {
    // Do something with team
}
```

---

## Debugging

### Enable Debug Mode

In `config.yml`:
```yaml
debug: true
```

### Check Logs

```bash
# View BedWars logs
grep "BedWars" logs/latest.log
```

### Common Issues

**ClassCastException**
- Ensure you're casting to correct type
- Verify plugin version compatibility

**NullPointerException**
- Always null-check return values
- Verify player is in active game

**Event Not Firing**
- Ensure event handler is registered
- Check @EventHandler annotation is present
- Verify plugin loads before BedWars

---

## Contributing

To contribute to BedWars API:

1. Fork the repository
2. Create feature branch
3. Add new features/fixes
4. Include documentation
5. Submit pull request

---

## Support

For API questions or issues:
1. Check this documentation
2. Review example code
3. Check plugin source
4. Ask in Discord/forums
