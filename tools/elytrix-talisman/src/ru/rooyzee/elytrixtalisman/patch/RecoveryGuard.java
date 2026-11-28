package ru.rooyzee.elytrixtalisman.patch;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;
import org.bukkit.plugin.Plugin;
/** Fail closed: never overwrite a pending recovery snapshot. */
public final class RecoveryGuard {
 private RecoveryGuard(){}
 public static boolean canStart(Plugin plugin,boolean pendingMemory,boolean demolishing){
  File dir=new File(plugin.getDataFolder(),"restore");
  if(pendingMemory||demolishing||new File(dir,"pending.schem").exists()||new File(dir,"pending.yml").exists()){
   plugin.getLogger().warning("Талисман не запущен: предыдущий откат ещё не завершён или требует восстановления. Сохраните папку restore и проверьте консоль.");return false;
  }
  return true;
 }
 public static void requireSaved(Plugin plugin)throws IOException{
  File dir=new File(plugin.getDataFolder(),"restore"),schem=new File(dir,"pending.schem"),meta=new File(dir,"pending.yml");
  if(!schem.isFile()||schem.length()==0||!meta.isFile()||meta.length()==0)throw new IOException("Snapshot files missing; paste cancelled");
  String data=Files.readString(meta.toPath(),StandardCharsets.UTF_8);
  if(!Pattern.compile("(?m)^world:\\s*\\S+").matcher(data).find())throw new IOException("Snapshot world missing");
  for(String key:new String[]{"x","y","z"})if(!Pattern.compile("(?m)^"+key+":\\s*-?\\d+\\s*$").matcher(data).find())throw new IOException("Snapshot coordinates invalid");
 }
}
