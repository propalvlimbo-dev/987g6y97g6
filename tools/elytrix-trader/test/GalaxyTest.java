package ru.rooyzee.elytrixtrader.patch;
import java.util.*;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.*;
import org.bukkit.entity.Entity;
import dev.by1337.virtualentity.api.virtual.VirtualExperienceOrb.Orb;
import dev.by1337.virtualentity.api.tracker.PlayerTracker;
import ru.rooyzee.elytrixtrader.Main;
import ru.rooyzee.elytrixtrader.model.TraderInstance;
public class GalaxyTest {
 static class Task implements BukkitTask {boolean cancelled;public void cancel(){cancelled=true;}}
 static class Scheduler implements BukkitScheduler {List<Runnable> jobs=new ArrayList<>();List<Task> tasks=new ArrayList<>();public BukkitTask runTaskTimer(Plugin p,Runnable r,long d,long period){if(d!=1||period!=2)throw new AssertionError();jobs.add(r);Task t=new Task();tasks.add(t);return t;}void run(){for(int i=0;i<jobs.size();i++)if(!tasks.get(i).cancelled)jobs.get(i).run();}}
 static class WorldImpl implements World {public boolean isChunkLoaded(int x,int z){throw new AssertionError("Must not load chunks");}public <T extends Entity>T spawn(Location loc,Class<T> type){throw new AssertionError("Must not spawn server entities");}public <T>void spawnParticle(Particle p,Location l,int n,double x,double y,double z,double s,T data){throw new AssertionError();}}
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  Scheduler s=new Scheduler();Bukkit.scheduler=s;Main p=new Main();WorldImpl w=new WorldImpl();TraderInstance n=new TraderInstance();n.loc=new Location(w,10,64,20);
  TraderGalaxy.start(p,n);check(Orb.created.isEmpty());s.run();check(Orb.created.size()==6);check(PlayerTracker.created.size()==1);PlayerTracker tracker=PlayerTracker.created.get(0);check(tracker.entities.size()==6 && tracker.ticks==1);
  Orb first=Orb.created.get(0);double x=first.pos.x,z=first.pos.z;for(Orb o:Orb.created)check(o.noGravity && o.silent && (o.value==17||o.value==3));s.run();check(first.moves==3 && (x!=first.pos.x||z!=first.pos.z));
  n.loc.world=new WorldImpl();s.run();check(tracker.removed==1 && PlayerTracker.created.size()==2 && Orb.created.size()==12);TraderGalaxy.remove(n);check(s.tasks.get(0).cancelled && PlayerTracker.created.get(1).removed==1);
  TraderGalaxy.start(p,n);s.run();TraderGalaxy.start(p,n);check(s.tasks.get(1).cancelled);s.run();TraderGalaxy.stopAll(p);for(Task t:s.tasks)check(t.cancelled);for(PlayerTracker t:PlayerTracker.created)check(t.removed==1);
  Location c=new Location(w,0,64,0);for(int i=0;i<6;i++){Location v=TraderGalaxy.point(c,i,100);double r=Math.hypot(v.x,v.z);check(r>=1.0 && r<1.37 && v.y>64.7 && v.y<65.85);}
  System.out.println("Virtual experience orb motion and lifecycle OK");
 }
}
