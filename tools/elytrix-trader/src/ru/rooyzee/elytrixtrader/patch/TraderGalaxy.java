package ru.rooyzee.elytrixtrader.patch;

import java.util.*;
import dev.by1337.virtualentity.api.tracker.PlayerTracker;
import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import dev.by1337.virtualentity.api.virtual.VirtualExperienceOrb;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.by1337.blib.geom.Vec3d;
import ru.rooyzee.elytrixtrader.Main;
import ru.rooyzee.elytrixtrader.model.TraderInstance;

/** Client-only experience orbs: no world entities, no XP pickup, no terrain changes. */
public final class TraderGalaxy {
 private static final Map<TraderInstance,Orbit> ACTIVE=new IdentityHashMap<>();
 private static final int COUNT=6;
 private TraderGalaxy(){}
 public static void start(Main plugin,TraderInstance npc){
  remove(npc);if(npc==null)return;
  Orbit o=new Orbit(plugin,npc);ACTIVE.put(npc,o);
  try{o.task=Bukkit.getScheduler().runTaskTimer((Plugin)plugin,o,1L,2L);}
  catch(RuntimeException ex){ACTIVE.remove(npc);plugin.getLogger().warning("Cannot start trader experience orbit: "+ex.getMessage());}
 }
 public static void remove(TraderInstance npc){Orbit o=ACTIVE.remove(npc);if(o!=null)o.close();}
 public static void stopAll(Main plugin){for(TraderInstance npc:new ArrayList<>(ACTIVE.keySet())){Orbit o=ACTIVE.get(npc);if(o!=null&&o.plugin==plugin)remove(npc);}}
 private static final class Orbit implements Runnable {
  final Main plugin;final TraderInstance npc;final List<VirtualExperienceOrb> orbs=new ArrayList<>();
  PlayerTracker tracker;World world;BukkitTask task;long tick,retry;boolean warned;
  Orbit(Main plugin,TraderInstance npc){this.plugin=plugin;this.npc=npc;}
  void clear(){if(tracker!=null)try{tracker.removeAll();}catch(RuntimeException ex){plugin.getLogger().warning("Cannot remove trader experience orbit: "+ex.getMessage());}tracker=null;world=null;orbs.clear();}
  void close(){if(task!=null)task.cancel();clear();}
  public void run(){
   Location center=npc.location();World current=center==null?null:center.getWorld();
   if(current==null){clear();return;}
   if(tracker!=null&&world!=current)clear();
   if(tracker==null){
    if(System.currentTimeMillis()<retry)return;
    try{
     for(int i=0;i<COUNT;i++){
      VirtualExperienceOrb orb=VirtualExperienceOrb.create();orb.setValue(i%3==0?17:3);
      orb.setNoGravity(true);orb.setSilent(true);orb.setPos(new Vec3d(point(center,i,tick)));orbs.add(orb);
     }
     List<VirtualEntity> entities=new ArrayList<>(orbs);
     tracker=new PlayerTracker(current,entities,new Vec3d(center),64);world=current;
     warned=false;retry=0;
    }catch(RuntimeException|LinkageError ex){fail(ex);return;}
   }
   tick+=2;
   try{for(int i=0;i<COUNT;i++)orbs.get(i).setPos(new Vec3d(point(center,i,tick)));tracker.tick();}
   catch(RuntimeException|LinkageError ex){fail(ex);}
  }
  void fail(Throwable ex){clear();retry=System.currentTimeMillis()+5000L;if(!warned){plugin.getLogger().warning("Cannot display trader experience orbit: "+ex.getMessage());warned=true;}}
 }
 /** Two staggered, counter-rotating rings with gentle vertical undulation. */
 static Location point(Location center,int i,long tick){
  int group=i%2;int slot=i/2;
  double phase=slot*(Math.PI*2/3)+(group==0?0:Math.PI/3);
  double angle=phase+tick*(group==0?.013:-.010);
  double radius=group==0?1.03:1.36;
  double height=(group==0?1.05:1.58)+.26*Math.sin(tick*.018+phase*1.3);
  return center.clone().add(radius*Math.cos(angle),height,radius*Math.sin(angle));
 }
}
