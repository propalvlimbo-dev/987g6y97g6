package ru.elytrix.duelsbridge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.bukkit.Bukkit;

/** Last-resort guard; independent of the unfinished shared-world rewrite. */
public final class SharedWorldSafety {
 private SharedWorldSafety() {}
 public static boolean protectedWorld(String worldName) {
  if (worldName == null || worldName.isBlank()) return true;
  if (worldName.equalsIgnoreCase("duel")) return true;
  return protectedPath(new File(Bukkit.getWorldContainer(), worldName));
 }
 public static boolean protectedPath(File candidate) {
  if (candidate == null) return true;
  try {
   Path root = Bukkit.getWorldContainer().getCanonicalFile().toPath();
   Path protectedWorld = root.resolve("duel").toFile().getCanonicalFile().toPath();
   Path path = candidate.getCanonicalFile().toPath();
   // Protect the world, all its children and any ancestor (especially the server root).
   return path.startsWith(protectedWorld) || protectedWorld.startsWith(path);
  } catch (IOException | SecurityException error) {
   return true; // Fail closed if a path cannot be checked.
  }
 }
}
