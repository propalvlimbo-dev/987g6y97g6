package ru.rooyzee.elytrixtrader.patch;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.rooyzee.elytrixtrader.Main;
import ru.rooyzee.elytrixtrader.model.TraderInstance;

/** A small rotating spiral of non-colliding display blocks; never changes terrain. */
public final class TraderGalaxy {
 private static final Map<TraderInstance,Orbit> ACTIVE=new IdentityHashMap<>();
 private static final int COUNT=28;
 private static final Material[] PALETTE={Material.PURPLE_STAINED_GLASS,Material.MAGENTA_STAINED_GLASS,Material.AMETHYST_BLOCK,Material.BLUE_STAINED_GLASS,Material.PINK_STAINED_GLASS};
 private TraderGalaxy(){}
 public static void start(Main plugin,TraderInstance npc){
  remove(npc); if(npc==null)return;
  Orbit orbit=new Orbit(plugin,npc);ACTIVE.put(npc,orbit);
  try{orbit.task=Bukkit.getScheduler().runTaskTimer((Plugin)plugin,orbit,1L,2L);}catch(RuntimeException ex){ACTIVE.remove(npc);plugin.getLogger().warning("Cannot start trader galaxy: "+ex.getMessage());}
 }
 public static void stopAll(Main plugin){for(TraderInstance npc:new ArrayList<>(ACTIVE.keySet())){Orbit o=ACTIVE.get(npc);if(o!=null && o.plugin==plugin)remove(npc);}}
 public static void remove(TraderInstance npc){Orbit o=ACTIVE.remove(npc);if(o!=null)o.close();}
 private static final class Orbit implements Runnable {
  final Main plugin;final TraderInstance npc;final List<BlockDisplay> blocks=new ArrayList<>();BukkitTask task;int tick;long retry;boolean warned;
  Orbit(Main plugin,TraderInstance npc){this.plugin=plugin;this.npc=npc;}
  void clear(){for(BlockDisplay b:blocks)try{b.remove();}catch(RuntimeException ignored){}blocks.clear();}
  void close(){if(task!=null)task.cancel();clear();}
  public void run(){
   Location center=npc.location();World world=center==null?null:center.getWorld();
   if(world==null || !world.isChunkLoaded(center.getBlockX()>>4,center.getBlockZ()>>4)){clear();return;}
   if(blocks.size()!=COUNT || !valid()){
    clear();if(System.currentTimeMillis()<retry)return;
    try{
     for(int i=0;i<COUNT;i++){
      BlockDisplay b=world.spawn(point(center,i,0),BlockDisplay.class);blocks.add(b);
      b.setPersistent(false);b.setInvulnerable(true);b.setBlock(PALETTE[i%PALETTE.length].createBlockData());
      float size=i<4?.20f:.15f;
      b.setTransformation(new Transformation(new Vector3f(-size/2,0,-size/2),new Quaternionf(),new Vector3f(size,.055f,size),new Quaternionf()));
      b.setInterpolationDuration(2);b.setTeleportDuration(2);
      if(!b.isValid())throw new IllegalStateException("display spawn cancelled");
     }
     warned=false;retry=0;
    }catch(RuntimeException ex){clear();retry=System.currentTimeMillis()+5000;if(!warned){plugin.getLogger().warning("Cannot display trader galaxy: "+ex.getMessage());warned=true;}return;}
   }
   tick+=2;
   try{for(int i=0;i<COUNT;i++){BlockDisplay b=blocks.get(i);if(tick%16==0)b.setBlock(PALETTE[(i+tick/16)%PALETTE.length].createBlockData());if(!b.teleport(point(center,i,tick)))throw new IllegalStateException("display teleport cancelled");}}
   catch(RuntimeException ex){clear();retry=System.currentTimeMillis()+5000;if(!warned){plugin.getLogger().warning("Cannot animate trader galaxy: "+ex.getMessage());warned=true;}}
  }
  boolean valid(){for(BlockDisplay b:blocks)if(!b.isValid())return false;return true;}
 }
 /** Two spiral arms rotate continuously, with the inner stars moving faster. */
 static Location point(Location c,int i,int tick){
  int arm=i%2;int n=i/2;double radius=.30+n*.065;
  double angle=arm*Math.PI+n*.34+tick*(.014+(13-n)*.0003);
  return c.clone().add(Math.cos(angle)*radius,.045+Math.sin(tick*.045+n*.65)*.018,Math.sin(angle)*radius);
 }
}
