package ru.elytrix.duelsbridge;
import java.io.*; import java.nio.file.*; import java.util.logging.*; import org.bukkit.configuration.file.*;
public class BridgeFilesTest {
 static class Plugin extends ElytrixDuelsBridge {
  final File folder; Plugin(File f){folder=f;} public File getDataFolder(){return folder;}
  public InputStream getResource(String p){return p.equals("bridge-config.yml")?new ByteArrayInputStream("original-default".getBytes()):null;}
  public Logger getLogger(){return Logger.getAnonymousLogger();}
 }
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a) throws Exception {
  Path dir=Files.createTempDirectory("mergefiles"); Path epic=Files.createDirectory(dir.resolve("EpicDuels"));
  Plugin p=new Plugin(epic.toFile()); ck(BridgeFiles.getDataFolder(p).toPath().equals(dir.resolve("ElytrixDuelsBridge")),"bridge folder");
  Path b=dir.resolve("ElytrixDuelsBridge"); Files.createDirectory(b); Files.writeString(b.resolve("config.yml"),"existing config"); Files.writeString(b.resolve("spawns.yml"),"existing points");
  BridgeFiles.saveDefaultConfig(p); ck(Files.readString(b.resolve("config.yml")).equals("existing config"),"preserve config");
  ck(Files.readString(b.resolve("spawns.yml")).equals("existing points"),"preserve points");
  ck(((YamlConfiguration)BridgeFiles.getConfig(p)).loadedFrom.toPath().equals(b.resolve("config.yml")),"independent config path");
  ck(BridgeFiles.getConfig(p)==BridgeFiles.getConfig(p),"config cached");
  Path second=Files.createDirectory(dir.resolve("second")); Plugin other=new Plugin(second.resolve("EpicDuels").toFile()); BridgeFiles.saveDefaultConfig(other);
  ck(Files.readString(second.resolve("ElytrixDuelsBridge/config.yml")).equals("original-default"),"fresh default");
  System.out.println("Bridge data folder tests passed");
 }
}
