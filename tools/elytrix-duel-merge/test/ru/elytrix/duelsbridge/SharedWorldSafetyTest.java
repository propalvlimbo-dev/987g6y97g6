package ru.elytrix.duelsbridge;
import java.nio.file.*;
import org.bukkit.Bukkit;
public class SharedWorldSafetyTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  Path root=Files.createTempDirectory("shared-world");Bukkit.worldContainer=root.toFile();
  Path duel=Files.createDirectories(root.resolve("duel"));
  Path instance=Files.createDirectories(root.resolve("arena_instance_123"));
  check(SharedWorldSafety.protectedWorld("duel"),"duel name");
  check(SharedWorldSafety.protectedPath(duel.toFile()),"duel path");
  check(SharedWorldSafety.protectedPath(duel.resolve("region").toFile()),"world descendant");
  check(SharedWorldSafety.protectedPath(root.toFile()),"world parent");
  check(!SharedWorldSafety.protectedPath(instance.toFile()),"instance cleanup remains allowed");
  check(!SharedWorldSafety.protectedWorld("arena_instance_123"),"instance name allowed");
  check(SharedWorldSafety.protectedWorld("../"),"parent alias");
  Path alias=root.resolve("alias");Files.createSymbolicLink(alias,duel);
  check(SharedWorldSafety.protectedPath(alias.toFile()),"symlink to world");
  System.out.println("Shared world deletion guard tests passed");
 }
}
