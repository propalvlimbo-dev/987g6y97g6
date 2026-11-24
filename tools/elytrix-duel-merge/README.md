# ElytrixDuel combined build (experimental)

`finalbuild/ElytrixDuel.jar` contains the duel engine, bridge and native duel menus in a single Bukkit plugin. Its registered Bukkit name is `ElytrixDuel`. It replaces both old plugin JARs; never run it alongside them. Legacy Java package names, `epicduels.*` permissions and `%epicduels_*%` placeholders remain for compatibility. BMenu is not needed for the eight built-in duel menus, but may be required elsewhere on the server.

## Data and world safety

On first startup, if `plugins/EpicDuels` exists and `plugins/ElytrixDuel` does not, the plugin copies the old data to `plugins/ElytrixDuel` before the engine starts. The original is not removed. If the new folder exists, it is never overwritten; if it lacks `arenas.yml` while the old folder has it, startup is refused until the data is reviewed. A copy failure also aborts startup. The original bridge data stays in `plugins/ElytrixDuelsBridge`. Back up both folders, all worlds and player inventories before testing. Generator references in Bukkit or other plugin configs that name the old plugin may need changing to `ElytrixDuel`.

The world copy code skips only root-level transient `level<digits>.dat` files and `session.lock`; a disappearing transient file is ignored. Missing other files still fail the copy. The existing world-save call remains, so the server may still log its manual-save warning. Removing that save would risk copying unsaved arena changes. A live asynchronously copied world is not an atomic snapshot, and no gameplay test has validated the copies.

Arena rename/deletion and startup deletion of old instance folders are disabled; normal match-end instance deletion is still performed. Abandoned instances after crashes may accumulate. Other known risks (including cancelled teleports and low-level world-delete APIs) are not comprehensively fixed.

## Verification and limitations

The build and unit tests exercise copy behavior with concurrent temporary files, migration with existing data and symbolic links, and menu behavior using minimal stubs. ZIP contents and selected decompiled bytecode were inspected. No real Paper/Leaf runtime, live duel, real world, actual server integration or compatibility with other plugins has been tested. Leaf could not download its Mojang server JAR in this environment. Test on an isolated copy before production; no guarantee of full safety is made.

Construction: the bridge main class is transformed to extend EpicDuels, invokes its enable first and disable last, and accesses separate bridge config/data via `BridgeFiles`. Plugin metadata merges `/duel`, `/party`, and `/duelx`. This is integration of the supplied binaries, not a complete source-level rewrite. EpicDuels upstream is by nuroso000, https://github.com/nuroso000/EpicDuels , CC BY-NC-SA 4.0; see `META-INF/LICENSE-EpicDuels.txt` in the JAR. Verify any additional rights in the custom builds before redistribution.

## Build

```
JAVA=/path/to/java ECJ=/path/to/ecj.jar ASM=/path/to/asm.jar \
  tools/elytrix-duel-merge/build.sh EpicDuels-ElyTriX-3.1.2.jar \
  finalbuild/duel-native-menus/ElytrixDuelsBridge-native-menus-TEST.jar finalbuild/ElytrixDuel.jar
```

Both inputs are SHA-256 checked. Build requires Java 21+, Eclipse compiler, ASM 9, Python 3, and unzip. Compiler test stubs are not packaged.
