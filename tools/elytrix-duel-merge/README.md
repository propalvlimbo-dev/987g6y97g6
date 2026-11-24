# ElytrixDuel combined experimental build

`finalbuild/ElytrixDuel.jar` is one Bukkit plugin containing the original EpicDuels 3.1.2 code, the original bridge, native non-animated duel menus, and the partial world-safety patch. Install **only this jar** in place of both old duel plugin jars. Do not install it alongside either old jar. It does not require BMenu for the eight built-in duel menus; BMenu may still be needed for other parts of the server.

This is an **untested-on-server prototype**. It has not been shown to start, link, preserve real player inventories, or complete a duel on Leaf/Paper. A Leaf 1.21.11 attempt could not start because its required Mojang server file download failed in the sandbox. The world `duel` was not provided, so its arenas and actual world-loading order are untested. Prior risks, including cancelled teleport handling and arbitrary world deletion through public low-level APIs, remain. Do not use on production. Retain verified backups of worlds, inventories and both old data folders. Test on an isolated server first.

For compatibility, the Bukkit plugin's **registered name remains `EpicDuels`** and its data folder remains `plugins/EpicDuels`; only the file is named `ElytrixDuel.jar`. This preserves reflection, permissions, existing commands and integrations that look up `EpicDuels`. The bridge's config, spawns and other data remain under `plugins/ElytrixDuelsBridge`. No automatic deletion or migration of either folder is attempted. The bridge's original kit auto-install/migration behavior remains, so back up kits before testing. Existing customised BMenu menu IDs are rejected by the native menu allowlist; the `/menu` back button depends on the server. Instance-world startup cleanup, arena rename and arena deletion are disabled as documented in `tools/duel-world-guard/README.md`; normal match-end instance deletion still exists. Interrupted-match instance directories may accumulate.

Construction: the bridge main class is transformed to extend EpicDuels, invokes its enable first and disable last, and accesses separate bridge config/data via `BridgeFiles`. Plugin metadata merges `/duel`, `/party`, and `/duelx` commands. This is bytecode integration of the exact supplied JARs, not a rewrite of all plugin logic. `EpicDuels` upstream is by nuroso000, https://github.com/nuroso000/EpicDuels , CC BY-NC-SA 4.0; see `META-INF/LICENSE-EpicDuels.txt` in the JAR and verify any additional rights in the custom builds before redistribution.

## Build

```
JAVA=/path/to/java ECJ=/path/to/ecj.jar ASM=/path/to/asm.jar \
  tools/elytrix-duel-merge/build.sh EpicDuels-ElyTriX-3.1.2.jar \
  finalbuild/duel-native-menus/ElytrixDuelsBridge-native-menus-TEST.jar finalbuild/ElytrixDuel.jar
```

Both inputs are SHA-256 checked. Build requires Java 21+, Eclipse compiler, ASM 9, Python 3, and unzip. The compiler stubs are not packaged. Existing tests cover menu rendering and bridge data-path behavior against local stubs only; bytecode transform counts and archive integrity are checked, but no genuine server integration or gameplay is covered.
