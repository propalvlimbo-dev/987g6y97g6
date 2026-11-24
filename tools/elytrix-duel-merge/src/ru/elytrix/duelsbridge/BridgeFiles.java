package ru.elytrix.duelsbridge;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/** Preserve the old bridge's independent data and configuration without migration. */
public final class BridgeFiles {
 private static final Map<ElytrixDuelsBridge, FileConfiguration> CONFIGS = new WeakHashMap<>();
 private BridgeFiles() {}
 public static File getDataFolder(ElytrixDuelsBridge plugin) {
   return new File(plugin.getDataFolder().getParentFile(), "ElytrixDuelsBridge");
 }
 public static void saveDefaultConfig(ElytrixDuelsBridge plugin) {
   File dir=getDataFolder(plugin), target=new File(dir,"config.yml");
   if(target.exists()) return;
   if(!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("Cannot create bridge data folder: "+dir);
   try(InputStream stream=plugin.getResource("bridge-config.yml")) {
     if(stream==null) throw new IllegalStateException("Missing bridge-config.yml resource");
     Files.copy(stream,target.toPath());
   } catch(IOException e) { throw new IllegalStateException("Cannot create bridge config: "+target,e); }
 }
 public static FileConfiguration getConfig(ElytrixDuelsBridge plugin) {
   return CONFIGS.computeIfAbsent(plugin, p -> { saveDefaultConfig(p); return YamlConfiguration.loadConfiguration(new File(getDataFolder(p),"config.yml")); });
 }
 public static void saveConfig(ElytrixDuelsBridge plugin) {
   try { getConfig(plugin).save(new File(getDataFolder(plugin),"config.yml")); }
   catch(IOException e) { plugin.getLogger().log(Level.SEVERE,"Cannot save bridge config",e); }
 }
}
