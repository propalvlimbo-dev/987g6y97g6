package ru.elytrix.duelsbridge;
import java.io.*; import java.nio.file.*; import java.util.logging.Logger;
public class LegacyDataMigrationTest {
 static class Plugin extends ElytrixDuelsBridge { final File folder; Plugin(File f){folder=f;} public File getDataFolder(){return folder;} public Logger getLogger(){return Logger.getAnonymousLogger();} }
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] a)throws Exception{
  Path base=Files.createTempDirectory("elytrix-migration"), old=Files.createDirectory(base.resolve("EpicDuels")), target=base.resolve("ElytrixDuel");
  Files.writeString(old.resolve("arenas.yml"),"old arena"); Files.createDirectories(old.resolve("kits")); Files.writeString(old.resolve("kits/a.yml"),"kit");
  Plugin plugin=new Plugin(target.toFile()); LegacyDataMigration.migrate(plugin);
  check(Files.readString(target.resolve("arenas.yml")).equals("old arena"),"copied arena");check(Files.exists(target.resolve("kits/a.yml")),"copied subdir");check(Files.exists(old.resolve("arenas.yml")),"original intact");
  Files.writeString(target.resolve("arenas.yml"),"new arena");LegacyDataMigration.migrate(plugin);check(Files.readString(target.resolve("arenas.yml")).equals("new arena"),"no overwrite");
  Path emptybase=Files.createTempDirectory("elytrix-existing"), emptyold=Files.createDirectory(emptybase.resolve("EpicDuels")), emptynew=Files.createDirectory(emptybase.resolve("ElytrixDuel"));
  Files.writeString(emptyold.resolve("arenas.yml"),"original"); boolean refused=false;
  try{LegacyDataMigration.migrate(new Plugin(emptynew.toFile()));}catch(IllegalStateException e){refused=true;}
  check(refused && !Files.exists(emptynew.resolve("arenas.yml")),"refuse empty pre-existing target");
  Path other=Files.createTempDirectory("elytrix-links"), legacy=Files.createDirectory(other.resolve("EpicDuels"));Files.createSymbolicLink(legacy.resolve("link"), old.resolve("arenas.yml"));
  boolean failed=false; try{LegacyDataMigration.migrate(new Plugin(other.resolve("ElytrixDuel").toFile()));}catch(IllegalStateException e){failed=true;}
  check(failed && !Files.exists(other.resolve("ElytrixDuel")),"symlinks refuse and no partial target");
  System.out.println("Legacy migration tests passed");
 }
}
