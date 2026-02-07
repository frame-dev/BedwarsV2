# Troubleshooting Guide

Common issues and their solutions.

---

## Installation Issues

### Plugin doesn't load

**Problem**: Plugin doesn't appear in `/pl` command

**Solutions**:
1. Check Java version: `java -version` (need Java 17+)
2. Verify plugin.yml exists in JAR
3. Check server logs for errors
4. Ensure file permissions allow reading

**Check logs**:
```
tail -f logs/latest.log | grep BedWars
```

### "BedWars not found" error

**Problem**: Commands say "Unknown command"

**Solution**: 
- Reload plugin: `/bw reload`
- Or restart server
- Check plugin is in plugins folder

---

## Configuration Issues

### Arenas don't load

**Problem**: Arenas not appearing in `/bw list`

**Solutions**:
1. Check `config.yml` syntax (use online YAML validator)
2. Verify arena world exists
3. Check console for parsing errors
4. Ensure indentation is correct (2 spaces, not tabs)

**Debug**:
```yaml
debug: true  # Enable debug logging in config.yml
```

### Wrong world name

**Problem**: "World not found" error

**Solution**: 
- List available worlds: `/bw worlds`
- Update config with correct world name
- World name is case-sensitive

### Invalid coordinates

**Problem**: Players spawn in wrong location

**Solutions**:
1. Use `/bw setup` commands instead of manual editing
2. Double-check coordinates are integers
3. Ensure Y coordinate is above ground
4. Verify `yaw` and `pitch` values (0 = looking east)

---

## Game Issues

### Game won't start

**Problem**: Countdown never reaches 0

**Solutions**:
1. Check minimum players: `/bw setup info`
2. Ensure arena is fully configured
3. Check all spawns are set correctly
4. Verify team count (need at least 2)

### Players respawn but shouldn't

**Problem**: Player respawns after bed destroyed

**Solutions**:
1. Check bed location is correct
2. Ensure team is marked as bed destroyed
3. Report as bug if persistent

### Bed not breaking

**Problem**: Can't destroy enemy bed

**Solutions**:
1. Ensure you have proper tools (pickaxe)
2. Bed might be protected (check permissions)
3. Try breaking nearby blocks first
4. Restart game to reset block data

### Resources not spawning

**Problem**: No items at generators

**Solutions**:
1. Check generator locations in config
2. Verify generator is still loaded
3. Check chunk is loaded (stand near it)
4. Ensure material isn't obstructed
5. Check drop-rate isn't set to 0

---

## Performance Issues

### Server lagging during games

**Problem**: FPS drops, server stuttering

**Solutions**:
1. Reduce player count per arena
2. Disable auto-reset: `world.auto-reset: false`
3. Reduce mob spawning distances
4. Update to latest Spigot/Paper version
5. Increase server RAM allocation

### High memory usage

**Problem**: Memory leak, server crashes

**Solutions**:
1. Check database isn't too large
2. Disable statistics if not needed
3. Clear old game data periodically
4. Increase heap size: `-Xmx2G` (or higher)

### Chunk errors

**Problem**: "Unloaded chunk" messages

**Solutions**:
1. Reduce world size
2. Increase chunk loading distance
3. Pre-generate arena chunks
4. Disable world auto-reset

**Pre-generate chunks**:
```bash
# Use Chunky plugin for better chunk generation
/chunky start
/chunky radius 100
```

---

## Database Issues

### SQLite database corrupted

**Problem**: "Database is locked" or "I/O error"

**Solutions**:
1. Stop server immediately
2. Delete `bedwars.db` file
3. Restart server (new DB created)
4. All statistics lost (backup first!)

### MySQL connection failed

**Problem**: "Cannot connect to database"

**Solutions**:
1. Verify MySQL is running: `mysql -u root -p`
2. Check host/port in config
3. Verify credentials are correct
4. Check firewall allows connection
5. Create database first: `CREATE DATABASE bedwars;`

**Test MySQL connection**:
```bash
mysql -h localhost -u bedwars_user -p bedwars
```

---

## Command Issues

### Tab completion not working

**Problem**: Commands don't auto-complete

**Solutions**:
1. Reload plugin: `/bw reload`
2. Restart server if still broken
3. Check permissions: `bedwars.admin`

### Command permission denied

**Problem**: "You don't have permission" message

**Solutions**:
1. Give permission: `/lp user <name> permission set bedwars.admin`
2. Or add to permissions file
3. Operator has all permissions by default (`/op <name>`)

---

## Chat/Message Issues

### Messages not appearing

**Problem**: Game announcements don't show

**Solutions**:
1. Check `messages.yml` exists
2. Verify message key exists
3. Check chat not muted
4. Reload config: `/bw reload`

### Color codes not working

**Problem**: Messages show `&6` instead of gold text

**Solutions**:
1. Ensure using `&` not `§`
2. Update messages.yml
3. Reload: `/bw reload`
4. Restart if still broken

---

## Player Issues

### Can't join arena

**Problem**: "Join failed" message

**Solutions**:
1. Arena might be full: `/bw list`
2. Game might be running: wait for next round
3. Try different arena
4. Check permissions: `bedwars.join`

### Stats not saving

**Problem**: Kills/wins don't persist

**Solutions**:
1. Check database configured correctly
2. Verify write permissions on DB file
3. Check console for errors
4. Restart plugin to flush stats

### Armor not colored

**Problem**: All armor appears gray/brown

**Solutions**:
1. Ensure leather armor used (not chainmail)
2. Check team color configured
3. Rejoin game to refresh
4. Try different armor piece

---

## Advanced Debugging

### Enable debug mode

```yaml
# In config.yml
debug: true
```

### Check logs for errors

```bash
# Windows
type logs\latest.log | findstr "BedWars ERROR"

# Linux
grep "BedWars\|ERROR" logs/latest.log
```

### Get detailed version info

```
/bw version
/bw info
```

### Common error messages

| Error | Cause | Fix |
|-------|-------|-----|
| "World not found" | Arena world doesn't exist | Correct world name |
| "Team spawn not set" | Missing spawn location | Use `/bw setup setspawn` |
| "Bed not found" | Bed location missing | Use `/bw setup setbed` |
| "Database locked" | Multiple connections | Restart server |
| "ClassCastException" | Version mismatch | Update plugin |

---

## Still Having Issues?

1. **Check latest version**: Make sure you have the newest build
2. **Verify Java version**: Need Java 17+ (`java -version`)
3. **Test vanilla**: Remove plugins one-by-one to find conflicts
4. **Check logs**: Always check `logs/latest.log` first
5. **Get help**: Post error in Discord or GitHub issues

---

## Report a Bug

Include:
- Error message (full stack trace)
- Server version (`/version`)
- Plugin version (`/bw version`)
- Reproduction steps
- Latest.log file (relevant portion)
- config.yml (if applicable)
