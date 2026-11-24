# Arena boundaries and mace-height damage

The active rotated pair of spawn points determines a circular virtual boundary. For mace, its radius is capped at 24 blocks from the midpoint (or the existing radius if smaller); other kits keep their original radius. Crossing its sides is clamped to the edge, with outward velocity removed. This does not place any blocks into the world. This geometry is based on spawn points and `protection.arena-margin`, not actual wall blocks, so verify it against every map on an isolated server; it is not a guarantee against other plugins altering events.

For the **Булава / mace kit only**, the duel boundary has no upper or lower height limit. The side boundary remains in force for movement and pearls; other kits retain their previous vertical limits.

In **every duel kit**, a player continuously above absolute **Y=104** gets a short on-screen title warning at **5 seconds**. If they remain above, the first hit is **2 raw damage points** five seconds after the warning; every following five seconds the damage increases by two points (4, 6, 8, ...). Damage is a normal Bukkit damage event and may be reduced or cancelled by armor or other plugins. At Y=104 or lower the timer resets. Changing worlds, leaving the duel, or dying also resets it. Movement observations plus a 10-tick timer provide the time check; the actual hit may be delayed by server ticks or lag.

The default non-mace ceiling remains `protection.max-height` above arena spawn; the old `protection.mace-height` setting is no longer used as a ceiling for the mace kit. The bottom setting `protection.min-drop` still applies to other kits. Out-of-bounds pearls are cancelled, so players are not moved to spawn. Exceptional out-of-bounds recovery moves them toward the boundary, not to a spawn.

The experimental shared-world build uses the loaded `duel` world directly without per-battle copies. The ordinary non-shared build still copies the template. Do not delete or rename the world. See `SHARED_WORLD_TEST.md` for limitations and testing notes.

The build script now adds a defensive deletion guard to ArenaManager's instance cleanup and world-folder deletion. It refuses attempts to delete the `duel` world, its ancestors or its descendants (including canonical symlink aliases); standard instance cleanup remains permitted. This is defense in depth for these plugin paths only, not a general guarantee against server operators or other plugins. The guard is included in the experimental shared-world build but has not been tested on a real server.

Player-facing legacy bold formatting is filtered within the duel plugin; items and text owned by other plugins are not modified. Existing permissions and placeholder names remain unchanged.

Tests cover grace and repeat timing with simulated monotonic time, reset at Y=104, side boundaries, high pearls and non-mace behavior with Java stubs. The transformed original Bounds class is also tested for rotation. No real Leaf server, production map, player, armor, event protection plugin, actual mace or duel has been tested. Damage is subject to normal server/plugin event handling.
