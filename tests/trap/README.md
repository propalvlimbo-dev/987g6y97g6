# TrapSafety tests

Compile with JDK 21 from the repository root and run:

```sh
mkdir -p /tmp/trap-test
javac --release 21 -d /tmp/trap-test $(find tests/trap/bukkit-stubs -name '*.java') tools/items-src/ru/rooyzee/elytrixitem/item/impl/TrapSafety.java tests/trap/TrapSafetyTest.java
java -Xverify:all -cp /tmp/trap-test TrapSafetyTest
```

These are isolated tests using minimal Bukkit stubs, not a substitute for a Leaf/FAWE integration test. To rebuild the helper classes for `tools/build_items.py`, compile the helper against the actual 1.21.11 Bukkit API and copy the two resulting `TrapSafety*.class` files to `tools/items-classes/`.
