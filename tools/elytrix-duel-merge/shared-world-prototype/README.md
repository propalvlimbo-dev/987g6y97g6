# Shared-world migration: NOT integrated

The shipped JAR still uses instance-world copies. `SlotAllocator` is an isolated prototype, not included by build.sh. It assigns complete pairs by battle UUID, keeps reservations until explicit cleanup, and refuses reconfiguration while battles are reserved. With 8 configured points it supports four pairs; incomplete pairs are skipped. No geometrical isolation is implied by a pair of points.

Do not substitute `duel` for an instance name without further work. Audit findings that must be addressed before shipping:

* Arena creation, both 1v1 and team starts, all failure/cancel/quit/disable paths and delayed player return must acquire/release exactly once; full-capacity handling must inform participants without losing their inventory or leaving them busy. Direct/queue/party starts must not bypass reservation. Preserve slots throughout result screens and pending restore.
* The current Arenas maps are keyed by world name, teleport redirection explicitly requires `arena_instance_`, and `ArenaRules.resolve` looks up pair points by world. Sharing one world requires battle-specific point selection for spawn, respawn, next rounds and boundaries, not world-specific state. Teleport listeners for forfeits and kit effects also assume instance world names.
* Instance cleanup unloads and deletes the world directory; it must never unload/delete `duel`. Missing `duel` must fail closed rather than create a blank world. Existing block protection, explosions, liquids, fire, entity and inventory cleanup must be adapted and tested. In particular `DuelManager.resetPlayerPlacedBlocks` removes non-player entities across the entire instance; this would affect other battles and builds in a shared world.
* Both the lobby protection listener and arena guards identify instance worlds by prefix; blindly changing just the world returned by createInstanceWorld can break PvP and protection. Attacks and projectiles must be isolated between battles and outsiders, including spectators and delayed effects. Effects must not leak between duels or wipe restored player effects.
* Existing radial bounds derive from point distance and margin; verify regions do not overlap or allow interference. World terrain/buildings are not available in this repository. Gameplay, death, team duels, reconnect and cleanup must be checked on a backup on the actual Leaf server before deployment.

Prototype test (Java 21+, using ECJ):

```
java -jar "$ECJ" -21 -d /tmp/slot-test shared-world-prototype/SlotAllocator.java shared-world-prototype/SlotAllocatorTest.java
java -cp /tmp/slot-test ru.elytrix.duelsbridge.shared.SlotAllocatorTest
```
