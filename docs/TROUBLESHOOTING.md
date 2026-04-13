# Troubleshooting Guide

## Scope

This page lists common BedWars operational issues and practical fixes.

## Plugin Does Not Load

Checks:

1. Verify Java version.
2. Verify server version compatibility.
3. Verify plugin file is in the plugins folder.
4. Review startup logs for stack traces.

Commands:

```bash
java -version
```

```bash
# Linux
grep "BedWars\|ERROR" logs/latest.log
```

```powershell
# Windows
Get-Content logs/latest.log | Select-String "BedWars|ERROR"
```

## Arenas Missing or Invalid

Checks:

1. Run `/bw setup info` for missing required fields.
2. Confirm at least two teams have spawn and bed configured.
3. Confirm configured world names exist and match exactly.
4. Avoid manual editing when setup commands are available.

Useful commands:

```text
/bw setup info
/bw setup list
/bw list
```

## Game Does Not Start

Checks:

1. Minimum players requirement is met.
2. Arena setup is complete.
3. Game state is not locked by another flow.

Useful commands:

```text
/bw start
/bw stop
/bw list
```

## Bed and Respawn Logic Looks Wrong

Checks:

1. Verify each team bed location is correct.
2. Confirm bed was properly registered during setup.
3. Restart the current match and retest with a clean arena state.

## Resource Generators Not Spawning

Checks:

1. Confirm generator entries exist in arena setup data.
2. Stand near generator chunks to ensure they are loaded.
3. Confirm generator intervals are not disabled in config.

## Performance and Lag

Checks:

1. Reduce arena concurrency and player counts.
2. Monitor TPS and timings while matches run.
3. Keep server software and Java updated.
4. Reduce expensive world activity around arenas.

## Database and Stats Issues

Checks:

1. Confirm database file path and write permissions.
2. Check startup logs for DB initialization errors.
3. Confirm stats save events on player quit and game end.

SQLite path:

```text
plugins/BedWars/bedwars.db
```

## Command and Permission Issues

Checks:

1. Confirm player has required permission nodes.
2. Reload permissions plugin after edits.
3. Verify command aliases and syntax.

Examples:

```text
/lp user <name> permission set bedwars.admin true
/lp user <name> permission set bedwars.setup true
```

## Message and Formatting Issues

Checks:

1. Verify `messages.yml` exists and syntax is valid.
2. Use `&` color codes consistently.
3. Reload plugin config after message edits.

## Quick Diagnostic Checklist

1. Reproduce issue once with minimal variables.
2. Capture exact command used and output.
3. Capture relevant log lines.
4. Confirm plugin and server versions.
5. Confirm config changes were reloaded.

## Reporting a Bug

Provide:

- Plugin version
- Server implementation and version
- Java version
- Exact reproduction steps
- Relevant log excerpt
- Sanitized config snippet when needed
