package ru.rooyzee.elytrixtrader.patch;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.rooyzee.elytrixtrader.Main;
import ru.rooyzee.elytrixtrader.model.TraderInstance;

/** Graceful green cubes, rendered without collision or any changes to blocks. */
public final class TraderGalaxy {
 private static final Map<TraderInstance,Orbit> ACTIVE=new IdentityHashMap<>();
 private static final int COUNT=8;
 private static final Material[] MATERIALS={Material.EMERALD_BLOCK,Material.LIME_STAINED_GLASS,Material.LIME_CONCRETE,Material.GREEN_STAINED_GLASS};
 private TraderGalaxy(){}
 public static void start(Main plugin,TraderInstance npc){
  remove(npc);if(npc==null)return;
  Orbit o=new Orbit(plugin,npc);ACTIVE.put(npc,o);
  try{o.task=Bukkit.getScheduler().runTaskTimer((Plugin)plugin,o,1L,2L);}
  catch(RuntimeException ex){ACTIVE.remove(npc);plugin.getLogger().warning("Cannot start trader block orbit: "+ex.getMessage());}
 }
 public static void remove(TraderInstance npc){Orbit o=ACTIVE.remove(npc);if(o!=null)o.close();}
 public static void stopAll(Main plugin){for(TraderInstance npc:new ArrayList<>(ACTIVE.keySet())){Orbit o=ACTIVE.get(npc);if(o!=null&&o.plugin==plugin)remove(npc);}}
 private static final class Orbit implements Runnable {
  final Main plugin;final TraderInstance npc;final List<BlockDisplay> displays=new ArrayList<>();
  World world;BukkitTask task;long tick,retry;boolean warned;
  Orbit(Main plugin,TraderInstance npc){this.plugin=plugin;this.npc=npc;}
  void clear(){for(BlockDisplay d:displays)try{d.remove();}catch(RuntimeException ex){plugin.getLogger().warning("Cannot remove trader orbit block: "+ex.getMessage());}displays.clear();world=null;}
  void close(){if(task!=null)task.cancel();clear();}
  public void run(){
   Location center=npc.location();World current=center==null?null:center.getWorld();
   if(current==null || !chunksReady(current,center)){clear();return;}
   if(world!=null && world!=current)clear();
   if(displays.size()!=COUNT || !valid()){
    clear();if(System.currentTimeMillis()<retry)return;
    try{
     for(int i=0;i<COUNT;i++){
      BlockDisplay d=current.spawn(point(center,i,tick),BlockDisplay.class);displays.add(d);
      d.setPersistent(false);d.setInvulnerable(true);d.setGravity(false);
      d.setBlock(MATERIALS[(i*3)%MATERIALS.length].createBlockData());
      float size=i%3==0?.16f:.12f;
      d.setTransformation(new Transformation(new Vector3f(-size/2,-size/2,-size/2),new Quaternionf(),new Vector3f(size,size,size),new Quaternionf()));
      d.setBrightness(new Display.Brightness(15,15));d.setInterpolationDuration(2);d.setTeleportDuration(2);
      if(!d.isValid())throw new IllegalStateException("display spawn cancelled");
     }
     world=current;warned=false;retry=0;
    }catch(RuntimeException|LinkageError ex){fail(ex);return;}
   }
   tick+=2;
   try{for(int i=0;i<COUNT;i++)if(!displays.get(i).teleport(point(center,i,tick)))throw new IllegalStateException("display teleport cancelled");}
   catch(RuntimeException|LinkageError ex){fail(ex);}
  }
  boolean valid(){for(BlockDisplay d:displays)if(!d.isValid())return false;return true;}
  void fail(Throwable ex){clear();retry=System.currentTimeMillis()+5000L;if(!warned){plugin.getLogger().warning("Cannot display trader block orbit: "+ex.getMessage());warned=true;}}
 }
 /** Avoid forcing adjacent chunks to load just for cosmetic entities. */
 static boolean chunksReady(World world,Location c){
  int minX=c.clone().add(-1.5,0,0).getBlockX()>>4,maxX=c.clone().add(1.5,0,0).getBlockX()>>4;
  int minZ=c.clone().add(0,0,-1.5).getBlockZ()>>4,maxZ=c.clone().add(0,0,1.5).getBlockZ()>>4;
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!world.isChunkLoaded(x,z))return false;
  return true;
 }
 /** Two staggered, counter-rotating rings with gentle vertical undulation. */
 static Location point(Location center,int i,long tick){
  int group=i%2,slot=i/2;
  double phase=slot*(Math.PI/2)+(group==0?0:Math.PI/4);
  double angle=phase+tick*(group==0?.013:-.010);
  double radius=group==0?1.03:1.32;
  double height=(group==0?1.05:1.58)+.26*Math.sin(tick*.018+phase*1.3);
  return center.clone().add(radius*Math.cos(angle),height,radius*Math.sin(angle));
 }
}
