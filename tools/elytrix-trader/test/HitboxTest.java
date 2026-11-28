package ru.rooyzee.elytrixtrader.patch;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.*;
import org.bukkit.scheduler.*;
import ru.rooyzee.elytrixtrader.Main;
import ru.rooyzee.elytrixtrader.model.TraderInstance;
public class HitboxTest {
 static class EntityBase implements Entity {final UUID id=UUID.randomUUID();boolean valid=true,persistent=true,gravity=true;public UUID getUniqueId(){return id;}public void remove(){valid=false;}public boolean isValid(){return valid;}public void setGravity(boolean b){gravity=b;}public void setPersistent(boolean b){persistent=b;}public void setInvulnerable(boolean b){}public boolean teleport(Location l){return true;}}
 static class Hit extends EntityBase implements Interaction {float width,height;boolean responsive=true;public void setInteractionWidth(float f){width=f;}public void setInteractionHeight(float f){height=f;}public void setResponsive(boolean b){responsive=b;}}
 static class Person extends EntityBase implements Player {public boolean isOnline(){return true;}}
 static class WorldImpl implements World {boolean loaded=true;List<Hit> entities=new ArrayList<>();public boolean isChunkLoaded(int x,int z){return loaded;}@SuppressWarnings("unchecked")public <T extends Entity>T spawn(Location loc,Class<T> type){if(type!=Interaction.class)throw new AssertionError();Hit h=new Hit();entities.add(h);return (T)h;}public <T>void spawnParticle(Particle p,Location l,int n,double x,double y,double z,double s,T data){throw new AssertionError();}}
 static class Task implements BukkitTask {boolean canceled;public void cancel(){canceled=true;}}
 static class Scheduler implements BukkitScheduler {List<Runnable> jobs=new ArrayList<>();List<Task> tasks=new ArrayList<>();public BukkitTask runTaskTimer(Plugin p,Runnable r,long delay,long period){if(delay!=20 || period!=20)throw new AssertionError();jobs.add(r);Task t=new Task();tasks.add(t);return t;}void run(){for(int i=0;i<jobs.size();i++)if(!tasks.get(i).canceled)jobs.get(i).run();}}
 static class Manager implements PluginManager {TraderHitbox listener;public void registerEvents(Listener l,Plugin p){listener=(TraderHitbox)l;}}
 static void check(boolean b){if(!b)throw new AssertionError();}
 static PlayerInteractEntityEvent event(Person p,Entity e,EquipmentSlot hand){return new PlayerInteractEntityEvent(p,e,hand);}
 public static void main(String[] args){
  Scheduler scheduler=new Scheduler();Manager pm=new Manager();Bukkit.scheduler=scheduler;Bukkit.manager=pm;Main plugin=new Main();TraderHitbox.install(plugin);check(pm.listener!=null);
  WorldImpl world=new WorldImpl();TraderInstance npc=new TraderInstance();npc.loc=new Location(world,10,64,10);TraderHitbox.start(plugin,npc);check(world.entities.size()==1);Hit hit=world.entities.get(0);check(!hit.persistent&&!hit.gravity&&!hit.responsive && hit.width>.6f && hit.height>1.8f);
  Person player=new Person();PlayerInteractEntityEvent other=event(player,new EntityBase(),EquipmentSlot.HAND);pm.listener.interact(other);check(!other.isCancelled()&&plugin.menus().calls==0);
  PlayerInteractEntityEvent off=event(player,hit,EquipmentSlot.OFF_HAND);pm.listener.interact(off);check(off.isCancelled()&&plugin.menus().calls==0);
  PlayerInteractEntityEvent click=event(player,hit,EquipmentSlot.HAND);pm.listener.interact(click);check(click.isCancelled()&&plugin.menus().calls==1&&plugin.menus().lastTrader==npc);
  pm.listener.interact(event(player,hit,EquipmentSlot.HAND));check(plugin.menus().calls==1);
  pm.listener.quit(new PlayerQuitEvent(player));npc.despawning=true;pm.listener.interact(event(player,hit,EquipmentSlot.HAND));check(plugin.menus().calls==1);npc.despawning=false;
  world.loaded=false;scheduler.run();check(!hit.valid);pm.listener.interact(event(player,hit,EquipmentSlot.HAND));check(plugin.menus().calls==1);world.loaded=true;scheduler.run();check(world.entities.size()==2);Hit second=world.entities.get(1);pm.listener.interact(event(player,second,EquipmentSlot.HAND));check(plugin.menus().calls==2);
  TraderHitbox.remove(npc);check(scheduler.tasks.get(0).canceled&&!second.valid);pm.listener.interact(event(player,second,EquipmentSlot.HAND));check(plugin.menus().calls==2);
  TraderHitbox.start(plugin,npc);Hit third=world.entities.get(2);TraderHitbox.stopAll(plugin);check(!third.valid&&scheduler.tasks.get(1).canceled);
  System.out.println("NPC hitbox routing, debounce and cleanup OK");
 }
}
