# TrapSafety tests

Compile with JDK 21 from the repository root and run:

```sh
mkdir -p /tmp/trap-test
javac --release 21 -d /tmp/trap-test $(find tests/trap/bukkit-stubs -name '*.java') tools/items-src/ru/rooyzee/elytrixitem/item/impl/TrapSafety.java tests/trap/TrapSafetyTest.java
java -Xverify:all -cp /tmp/trap-test TrapSafetyTest
```

These are isolated tests using minimal Bukkit stubs, not a substitute for a Leaf/FAWE integration test. To rebuild the helper classes for `tools/build_items.py`, compile the helper against the actual 1.21.11 Bukkit API and copy the two resulting `TrapSafety*.class` files to `tools/items-classes/`.

Fix5 clears existing non-air blocks in the schematic footprint, from immediately above the lowest schematic layer to the world's build limit; actual schematic blocks are retained. The existing placement checks reject protected tile entities, overlapping traps and WorldGuard-protected changes. Bedrock in the clearance area also rejects placement. Normal expiry restores the recorded block states, deferring occupied cells as before. The active trap's placement/fluid/physics protection extends upwards as well. This can temporarily clear a tall column, so back up the world; the original plugin does not persist restoration state across a crash or restart while the trap is active.

Fix6 additionally disables the old repeated corner-fill routine and temporarily clears adjacent one-block rim terrain that would remain as pillars. The rim clearance uses the same preflight/protection checks and restoration as the rest of the trap.
