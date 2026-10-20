# Gameplay regression checks

Run `mvn clean verify` to compile, run the automated regression tests, and build
`target/bedwars-1.2-SNAPSHOT.jar`.

The tests use mocked Bukkit services. They check game lifecycle, scheduled death
handling, shop restrictions, enchantment upgrades, and achievement writes during
shutdown. They do not replace a Spigot/Paper server test.

## Manual server checks

Use a development server with a configured arena and at least two players on
opposing teams. Check the console and `logs/latest.log` for exceptions while
performing these scenarios:

1. Die with your bed intact, then use `/bw leave` during the respawn countdown.
   You should stay out of the match after the countdown expires. Repeat by
   disconnecting instead; the remaining team should win if it is the last team.
2. Die after losing your bed, then leave. You should leave the player and
   spectator lists. If you joined through a sign, you should return to where you
   clicked it.
3. Buy Protection I, II, III and IV. Existing armor should increase its protection
   each time. Buy armor after an upgrade and check its enchantments immediately.
4. Buy Sharpness with swords in several inventory slots. All swords should gain
   the upgrade. Buy a new sword and check that it arrives enchanted.
5. Die while a shop is open. The shop should close; purchases should also be
   rejected during respawning and after the match ends.
6. Complete a match normally. Cleanup should happen after five seconds, stats
   should increment once, and the arena should become available again.
   Check winners, eliminated players, players waiting to respawn, and external
   spectators. Sign-joined players should return to their individual click
   locations; everyone else should teleport to the arena lobby. With proxy lobby
   transfers enabled, the local teleport should happen before the transfer delay.
   Joining another match during that delay should cancel the old transfer.
7. Disable/reload the plugin on the development server with players waiting,
   during a match, and during the five-second ending phase. Players should return
   immediately, timers should stop, and tracked blocks should reset when
   `world.reset-on-end` is enabled. Check saved stats and achievement progress
   after restart.

Live server checks have not been performed as part of this change.

## Additional interaction checks

- Place beds facing north, south, east and west. Register each by looking at
  either half with `/bw setup setbed <team>`. Breaking either half of an opposing
  team's bed should remove both halves without dropping a bed item or XP.
  With `world.reset-on-end: true`, ending the match should restore both halves
  at the original coordinates, with their original color and direction. A nearby
  separate bed must not be mistaken for the team's bed. Own-bed protection should
  prevent destruction when `game.bed-protection` is enabled.
- Open the team selector and change teams more than once without manually closing
  it. Each replacement menu should remain clickable.
- Start map voting, then end it using the admin controls before its timer expires.
  Start another vote and confirm the previous timer does not end the new vote.
- With team enchantment upgrades active, pick up equipment and shift-click it
  from a chest into a storage slot. Check that the enchantments are present.
  Leaving or dying before the deferred inventory update should not apply old
  team upgrades outside the match or during respawning.
- Let diamond or emerald generators upgrade, finish the round, then start another
  round. Their spawn intervals should return to the initial tier.
- Set `game.starting-item` to `DIAMOND_SWORD`, then to `AIR`, restarting the plugin
  between configurations. Check the loadout at match start and after respawning:
  the custom sword should appear once, and `AIR` should give no starting weapon.
  Team weapon enchantments should apply to configured starting weapons.
- Finish a match, disconnect, and reconnect quickly. Check that the latest stats
  remain intact after another restart. Repeat while stopping the server after
  disconnecting; outstanding quit saves should finish before database shutdown.
