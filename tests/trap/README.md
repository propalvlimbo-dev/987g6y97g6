# TrapSafety tests

Compile with JDK 21 from the repository root and run:

```sh
mkdir -p /tmp/trap-test
javac --release 21 -d /tmp/trap-test $(find tests/trap/bukkit-stubs -name '*.java') tools/items-src/ru/rooyzee/elytrixitem/item/impl/TrapSafety.java tests/trap/TrapSafetyTest.java
java -Xverify:all -cp /tmp/trap-test TrapSafetyTest
```

These are isolated tests using minimal Bukkit stubs, not a substitute for a Leaf/FAWE integration test. To rebuild the helper classes for `tools/build_items.py`, compile the helper against the actual 1.21.11 Bukkit API and copy the two resulting `TrapSafety*.class` files to `tools/items-classes/`.

Fix8 clears the schematic footprint from above the lowest layer through two blocks above the top of the schematic, rather than up to build height. It retains schematic blocks and does not clear a rim or outside-footprint columns, even near players; the previous repeated corner-fill routine remains disabled. The preflight checks still protect tile entities, overlapping traps, WorldGuard regions and bedrock. Normal expiry restores recorded blocks and defers occupied cells. The original plugin does not persist restoration state across a crash or restart while the trap is active; back up the world.
