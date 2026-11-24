# Experimental EpicDuels world guard

This is a **partial test patch**, not the combined `ElytrixDuel.jar` and not production-ready. Rebuild with Python 3:

```
python3 tools/duel-world-guard/patch.py EpicDuels-ElyTriX-3.1.2.jar EpicDuels-world-guard-TEST.jar
```

The script refuses any input other than the exact supplied JAR, SHA-256 `35590f2a7dafb915fd1c454284e1029f871aadda269dc4774f9cd6c6fce636e7`. It modifies only three methods:

- `ArenaManager.deleteArena`: always returns false, without touching any world or arena record.
- `ArenaManager.renameArena`: always reports an error, without touching any world or arena record.
- `EpicDuels.cleanupLeftoverInstances`: disabled: arbitrary existing `arena_instance_*` directories are no longer deleted on startup. **After an unclean shutdown, temporary instance folders accumulate and must only be removed manually after verifying ownership, with the server stopped and backups available.**

The bridge, its numbered points, kit and arena files are not modified. Existing code still copies the template world for each fight and deletes generated instances during normal match cleanup. This patch does not harden the public `deleteInstanceWorld` / `deleteWorldFolder` methods against arbitrary external callers, and does not protect against other plugins or manual filesystem operations. It also does not fix the teleport-failure and `/duelx open` security issues described in the audit. No runtime server, actual world, BMenu interactions, or player tests have been performed.

EpicDuels is based on the work of nuroso000: https://github.com/nuroso000/EpicDuels . Its upstream v3.1.0 license is CC BY-NC-SA 4.0; the copy included alongside the test JAR is upstream's license, not a determination of additional rights in the modified 3.1.2 build.
