# Arena boundary changes

The existing per-instance radial boundary is kept, but is now based on the active rotated pair of spawn points rather than being expanded by the first arena's spawn. Movement crossing the limit is clamped to the edge, and outward/upward velocity is removed; the wind-charge/mace launch remains normal until the configured ceiling. This is a virtual boundary, not blocks written into the user's world. The original `protection.arena-margin`, `protection.max-height`, `protection.mace-height` and `protection.min-drop` values still apply. The default height above spawn is 7 blocks, or 15 for mace, unless the existing bridge config overrides them.

Out-of-bounds ender-pearl teleport is cancelled, including for operators, so it normally leaves the player where they were. If an external plugin or an existing illegal state gets a player outside the boundary, periodic recovery moves them to the nearest boundary position, not a spawn. Other plugins can still interact with/cancel/uncancel teleport and movement events; this is not an invulnerability or complete escape guarantee. Actual map shape is unknown; the radius/height must be tested and possibly adjusted against each arena on an isolated server.

The template world `duel` is still required: instance worlds are copied from it. Do not delete or rename it. The current server still saves it before copying and is not guaranteed to create a fully atomic live snapshot.

Player-facing legacy `&l`/`§l` and MiniMessage `<bold>` formatting are filtered in the bridge color formatter and engine text/presentation formatting; programmatic bold in built-in Component.text decoration arrays is filtered as well. Items and text owned by other plugins are not modified. Existing permissions and placeholder names remain unchanged.

Tests use Java test stubs for movement, pearl cancellation, velocity and formatting; a test with the transformed original Bounds class validates rotation. No real Leaf server or actual duel has been run; testing with real players, permissions, all three arena pairs, mace/wind charges, boundary corners, pearls, respawn and match cleanup remains necessary.
