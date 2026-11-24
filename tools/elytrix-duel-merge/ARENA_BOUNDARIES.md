# Arena boundaries and mace-height damage

The active rotated pair of spawn points determines a circular virtual boundary. Crossing its sides is clamped to the edge, with outward velocity removed. This does not place any blocks into the world. This geometry is based on spawn points and `protection.arena-margin`, not actual wall blocks, so verify it against every map on an isolated server; it is not a guarantee against other plugins altering events.

For the **Булава / mace kit only**, the duel boundary has no upper or lower height limit. The side boundary remains in force for movement and pearls. A player continuously above absolute **Y=104** for at least **5 seconds** receives a Bukkit damage event for **20 damage points**, repeated every second while they stay above. This is raw damage before armor and other plugins' reductions or cancellation; it is not guaranteed to remove 10 hearts. At Y=104 or lower the timer resets. Changing worlds, leaving the duel, dying or changing kit also resets it. Movement observations plus a 10-tick timer provide the time check; the actual hit may be delayed by server ticks or lag. Other kits retain their previous vertical limits and are not subject to this height damage.

The default non-mace ceiling remains `protection.max-height` above arena spawn; the old `protection.mace-height` setting is no longer used as a ceiling for the mace kit. The bottom setting `protection.min-drop` still applies to other kits. Out-of-bounds pearls are cancelled, so players are not moved to spawn. Exceptional out-of-bounds recovery moves them toward the boundary, not to a spawn.

The template world `duel` remains required: instance worlds are copied from it. Do not delete or rename it. The server still saves it before copying and the copy is not guaranteed to be an atomic live snapshot.

Player-facing legacy bold formatting is filtered within the duel plugin; items and text owned by other plugins are not modified. Existing permissions and placeholder names remain unchanged.

Tests cover grace and repeat timing with simulated monotonic time, reset at Y=104, side boundaries, high pearls and non-mace behavior with Java stubs. The transformed original Bounds class is also tested for rotation. No real Leaf server, production map, player, armor, event protection plugin, actual mace or duel has been tested. Damage is subject to normal server/plugin event handling.
