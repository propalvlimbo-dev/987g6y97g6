package ru.rooyzee.elytrixtrader.patch;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
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

/** A small rotating spiral of non-colliding display blocks; never changes terrain. */
public final class TraderGalaxy {
 private static final Map<TraderInstance,Orbit> ACTIVE=new IdentityHashMap<>();
 private static final int COUNT=72;
 private static final Particle.DustOptions SPARK=new Particle.DustOptions(Color.fromRGB(112,255,157),0.8f);
 private static final Material[] PALETTE={Material.EMERALD_BLOCK,Material.GREEN_STAINED_GLASS,Material.LIME_STAINED_GLASS,Material.LIME_CONCRETE,Material.PRISMARINE,Material.LIME_STAINED_GLASS,Material.VERDANT_FROGLIGHT,Material.GREEN_STAINED_GLASS};
 private TraderGalaxy(){}
 public static void start(Main plugin,TraderInstance npc){
  remove(npc); if(npc==null)return;
  Orbit orbit=new Orbit(plugin,npc);ACTIVE.put(npc,orbit);
  try{orbit.task=Bukkit.getScheduler().runTaskTimer((Plugin)plugin,orbit,1L,3L);}catch(RuntimeException ex){ACTIVE.remove(npc);plugin.getLogger().warning("Cannot start trader galaxy: "+ex.getMessage());}
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
   if(world==null || !chunksReady(world,center)){clear();return;}
   if(blocks.size()!=COUNT || !valid()){
    clear();if(System.currentTimeMillis()<retry)return;
    try{
     for(int i=0;i<COUNT;i++){
      BlockDisplay b=world.spawn(point(center,i,0),BlockDisplay.class);blocks.add(b);
      b.setPersistent(false);b.setInvulnerable(true);b.setBlock(PALETTE[(i*7)%PALETTE.length].createBlockData());
      float size=i>=60?.055f:(i%11==0?.105f:.072f+(i%4)*.009f);
      b.setTransformation(new Transformation(new Vector3f(-size/2,0,-size/2),new Quaternionf(),new Vector3f(size,.035f,size),new Quaternionf()));
      b.setBrightness(new Display.Brightness(15,15));
      b.setInterpolationDuration(3);b.setTeleportDuration(3);
      if(!b.isValid())throw new IllegalStateException("display spawn cancelled");
     }
     warned=false;retry=0;
    }catch(RuntimeException ex){clear();retry=System.currentTimeMillis()+5000;if(!warned){plugin.getLogger().warning("Cannot display trader galaxy: "+ex.getMessage());warned=true;}return;}
   }
   tick+=3;
   try{
    for(int i=0;i<COUNT;i++){BlockDisplay b=blocks.get(i);if(!b.teleport(point(center,i,tick)))throw new IllegalStateException("display teleport cancelled");}
    int twinkle=(tick/3)%COUNT;
    blocks.get(twinkle).setBlock(PALETTE[(twinkle*7+tick/108)%PALETTE.length].createBlockData());
    if(tick%9==0){
     Location spark=point(center,(tick/9*7)%60,tick).add(0,.06,0);
     world.spawnParticle(Particle.DUST,spark,1,0,0,0,0,SPARK);
    }
   }
   catch(RuntimeException ex){clear();retry=System.currentTimeMillis()+5000;if(!warned){plugin.getLogger().warning("Cannot animate trader galaxy: "+ex.getMessage());warned=true;}}
  }
  boolean valid(){for(BlockDisplay b:blocks)if(!b.isValid())return false;return true;}
 }
 /** Never load neighboring chunks just for cosmetic displays. */
 static boolean chunksReady(World world,Location c){
  int minX=c.clone().add(-1.8,0,0).getBlockX()>>4,maxX=c.clone().add(1.8,0,0).getBlockX()>>4;
  int minZ=c.clone().add(0,0,-1.8).getBlockZ()>>4,maxZ=c.clone().add(0,0,1.8).getBlockZ()>>4;
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!world.isChunkLoaded(x,z))return false;
  return true;
 }
 /** Three trailing arms and a sparse outer orbit. */
 static Location point(Location c,int i,int tick){
  if(i>=60){
   double angle=(i-60)*Math.PI/6-tick*.0035;
   double radius=1.68+.035*Math.sin(tick*.015+i);
   return c.clone().add(Math.cos(angle)*radius,.055+.02*Math.sin(tick*.025+i),Math.sin(angle)*radius);
  }
  int arm=i%3,n=i/3;double radius=.32+n*.063;
  double angle=arm*(Math.PI*2/3)+n*.27+tick*.007;
  return c.clone().add(Math.cos(angle)*radius,.045+.018*Math.sin(tick*.022+n*.55+arm),Math.sin(angle)*radius);
 }
}
