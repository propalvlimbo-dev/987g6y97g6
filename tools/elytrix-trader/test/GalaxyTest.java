package ru.rooyzee.elytrixtrader.patch;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.*;
import org.bukkit.util.Transformation;
import ru.rooyzee.elytrixtrader.Main;
import ru.rooyzee.elytrixtrader.model.TraderInstance;
public class GalaxyTest {
 static class Task implements BukkitTask {boolean cancelled;public void cancel(){cancelled=true;}}
 static class Scheduler implements BukkitScheduler {List<Runnable> jobs=new ArrayList<>();List<Task> tasks=new ArrayList<>();public BukkitTask runTaskTimer(Plugin p,Runnable r,long d,long period){if(d!=1||period!=2)throw new AssertionError();jobs.add(r);Task t=new Task();tasks.add(t);return t;}void run(){for(int i=0;i<jobs.size();i++)if(!tasks.get(i).cancelled)jobs.get(i).run();}}
 static class DisplayImpl implements BlockDisplay {boolean valid=true,persistent=true,gravity=true,teleportOk=true;Location loc;int moves;public java.util.UUID getUniqueId(){return java.util.UUID.randomUUID();}public void remove(){valid=false;}public boolean isValid(){return valid;}public void setPersistent(boolean b){persistent=b;}public void setGravity(boolean b){gravity=b;}public void setInvulnerable(boolean b){}public boolean teleport(Location l){loc=l;moves++;return teleportOk;}public void setBlock(BlockData b){}public void setTransformation(Transformation t){}public void setInterpolationDuration(int t){}public void setTeleportDuration(int t){if(t!=2)throw new AssertionError();}public void setBrightness(Display.Brightness b){}}
 static class WorldImpl implements World {boolean loaded=true;int deniedX=999;List<DisplayImpl> spawned=new ArrayList<>();public boolean isChunkLoaded(int x,int z){return loaded && x!=deniedX;}public <T>void spawnParticle(Particle p,Location l,int n,double x,double y,double z,double s,T data){throw new AssertionError("No particles");}@SuppressWarnings("unchecked")public <T extends Entity>T spawn(Location loc,Class<T> type){DisplayImpl d=new DisplayImpl();d.loc=loc;spawned.add(d);return (T)d;}}
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  Scheduler s=new Scheduler();Bukkit.scheduler=s;Main p=new Main();WorldImpl w=new WorldImpl();TraderInstance n=new TraderInstance();n.loc=new Location(w,10,64,20);
  TraderGalaxy.start(p,n);check(w.spawned.isEmpty());s.run();check(w.spawned.size()==8);DisplayImpl first=w.spawned.get(0);double x=first.loc.x,z=first.loc.z;for(DisplayImpl d:w.spawned)check(!d.persistent && !d.gravity);s.run();check(first.moves==2 && (x!=first.loc.x||z!=first.loc.z));
  w.loaded=false;s.run();check(!first.valid);int count=w.spawned.size();s.run();check(w.spawned.size()==count);w.loaded=true;s.run();check(w.spawned.size()==16);
  WorldImpl other=new WorldImpl();n.loc.world=other;s.run();check(other.spawned.size()==8);for(DisplayImpl d:w.spawned)check(!d.valid);
  TraderGalaxy.remove(n);check(s.tasks.get(0).cancelled);for(DisplayImpl d:other.spawned)check(!d.valid);
  TraderGalaxy.start(p,n);s.run();TraderGalaxy.start(p,n);check(s.tasks.get(1).cancelled);s.run();TraderGalaxy.stopAll(p);for(Task t:s.tasks)check(t.cancelled);for(DisplayImpl d:other.spawned)check(!d.valid);
  WorldImpl failed=new WorldImpl();TraderInstance retryNpc=new TraderInstance();retryNpc.loc=new Location(failed,8,64,8);TraderGalaxy.start(p,retryNpc);s.run();failed.spawned.get(0).teleportOk=false;s.run();for(DisplayImpl d:failed.spawned)check(!d.valid);int before=failed.spawned.size();s.run();check(failed.spawned.size()==before);TraderGalaxy.remove(retryNpc);
  w.deniedX=1;check(!TraderGalaxy.chunksReady(w,new Location(w,15,64,8)));check(TraderGalaxy.chunksReady(w,new Location(w,8,64,8)));w.deniedX=999;
  Location c=new Location(w,0,64,0);for(int i=0;i<8;i++){Location v=TraderGalaxy.point(c,i,100);double r=Math.hypot(v.x,v.z);check(r>=1.0 && r<1.33 && v.y>64.7 && v.y<65.85);}
  System.out.println("Flying green blocks: motion, gravity, chunk and lifecycle OK");
 }
}
