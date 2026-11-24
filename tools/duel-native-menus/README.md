# Native Elytrix duel menus — experimental bridge

This replaces only the `Menus` implementation inside the exact supplied ElytrixDuelsBridge 1.12.4. Its eight layouts are generated from the saved BMenu duel menu files in `reference/`; all final animation `st` slots are rendered immediately, no animation or console command execution. The original player-picker and kit-editor code remain in the bridge. The bridge no longer requires BMenu to open the eight duel menus, though BMenu may still be needed for the rest of the server. If BMenu is absent, the original bridge still logs a warning, but its text now explains that duel menus are built in.

It is **not** a merged ElytrixDuel JAR: EpicDuels must still be installed separately, and this replaces, not supplements, the old bridge. This is experimental: no real Paper/Leaf server, Bukkit linker, client, real duel, queue or BMenu integration has been tested. Do not install on production. The older teleport/inventory safety issues are not fixed here. Do not delete or rename the user's duel world. Back up both plugin data folders and all worlds.

The general-menu back button executes `/menu` as the player; its availability depends on the server's other plugins. Unknown/custom menu IDs are rejected instead of being opened via BMenu; existing custom `menus.*` IDs therefore need migration. Queue wait displays its initial value and does not animate or update it live. Legacy sound effects are not implemented. The bridge still calls its existing kit auto-install/migration logic at startup; existing configs and kits must be backed up before testing.

## Rebuild and tests

`generate_layouts.py` uses PyYAML only for generation; the generated `Layouts.java` is checked in. Compile `src` and the Bukkit **test stubs** with an Eclipse Java compiler, and run `ru.elytrix.duelsbridge.MenuTest`. The stubs emulate just the APIs used by this code and are never included in the output; passing tests does not verify linkage with Paper. Example with Java 21+ and `ECJ` set to the compiler jar:

```
mkdir -p /tmp/duel-menu-classes
java -jar "$ECJ" -21 -encoding UTF-8 -d /tmp/duel-menu-classes $(find tools/duel-native-menus/{src,stubs,test} -name '*.java')
java -cp /tmp/duel-menu-classes ru.elytrix.duelsbridge.MenuTest
python3 tools/duel-native-menus/package.py ElytrixDuelsBridge-1.12.4.jar /tmp/duel-menu-classes ElytrixDuelsBridge-native-menus-TEST.jar
```

The packager verifies the SHA-256 of the original bridge, replaces `Menus.class`, adds its helpers, changes two stale log strings, and changes plugin.yml's BMenu dependency to softdepend. The old runtime bridge lifecycle, data folder, `/duelx` command and remaining classes are unchanged. Test against the actual user's configured IDs, command permissions and server `/menu` before considering deployment.
