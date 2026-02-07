# 🎮 BedWars Plugin - Quick Reference Card

## 📦 Installation
```bash
# Build
mvn clean package

# Install
cp target/bedwars-1.0-SNAPSHOT.jar server/plugins/
```

## ⚙️ Basic Configuration
```yaml
arenas:
  myarena:
    lobby-spawn: world,0.0,100.0,0.0,0.0,0.0
    min-players: 2
    max-players: 8
    teams:
      red:
        spawn: world,50.0,64.0,0.0,0.0,0.0
        bed: world,55.0,64.0,0.0,0.0,0.0
```

## 🎮 Player Commands
| Command | Description |
|---------|-------------|
| `/bw join <arena>` | Join game |
| `/bw leave` | Leave game |
| `/bw spectate <arena>` | Spectate game |
| `/bw stats` | View stats |
| `/bw list` | List arenas |

## 🛠️ Admin Commands
| Command | Description |
|---------|-------------|
| `/bw setup create <name>` | Create arena |
| `/bw setup setlobby <arena>` | Set lobby |
| `/bw setup setspectator <arena>` | Set spectator |
| `/bw setup setspawn <arena> <team>` | Set spawn |
| `/bw setup setbed <arena> <team>` | Set bed |
| `/bw setup addgenerator <name>` | Add generator |

## 🏪 Shop Categories
1. **Blocks** - Building materials
2. **Weapons** - Swords
3. **Armor** - Protection
4. **Tools** - Pickaxes, axes
5. **Food** - Health items
6. **Potions** - Effects
7. **Special** - TNT, pearls

## ⬆️ Team Upgrades
1. **Sharpness** - Better swords (8 💎)
2. **Protection** - Better armor (2/4/8/16 💎)
3. **Haste** - Faster mining (2/4 💎)
4. **Heal Pool** - Regeneration (3 💎)
5. **Dragon** - Base defense (5 💚)

## ⚡ Resources
- **Iron** ⚔️ - Every 1s
- **Gold** 🏅 - Every 8s
- **Diamond** 💎 - Every 30s → 20s
- **Emerald** 💚 - Every 60s → 40s

## 🎯 Game Flow
```
WAITING → STARTING → RUNNING → ENDING → WAITING
  (2+)     (30s)    (gameplay)   (5s)
```

## 🏆 Win Conditions
- Last team with players alive
- All enemy beds destroyed + players eliminated

## 📊 Statistics
- Wins/Losses
- Kills/Deaths
- Final Kills
- Beds Broken

## 🎨 Team Colors
Red • Blue • Green • Yellow • Aqua • White • Pink • Gray

## 🔧 Requirements
- Spigot/Paper 1.20.4+
- Java 17+
- Maven 3.6+ (build only)

## 📁 Project Files
```
bedwars/
├── src/main/java/          (31 Java files)
├── src/main/resources/     (config.yml, arenas.yml, plugin.yml)
├── pom.xml                 (Maven config)
└── README.md               (Full docs - in root)
```

## 🚀 Quick Setup
1. Build with `mvn clean package`
2. Copy JAR to plugins/
3. Start server
4. Edit config.yml
5. Restart server
6. Place shop NPCs
7. `/bw join arena`

## 🐛 Troubleshooting
- Check Java 17+
- Verify Spigot 1.20.4+
- Check config syntax
- Review console errors
- Ensure world names match

## 📞 Documentation
- [README.md](../README.md) - Full docs
- [QUICK_START.md](QUICK_START.md) - Setup guide
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - Technical
- [COMPLETE_OVERVIEW.md](COMPLETE_OVERVIEW.md) - Overview

## ✅ Status
**COMPLETE & PRODUCTION READY**

---
**v1.0-SNAPSHOT** | Built with ❤️ by FrameDev
