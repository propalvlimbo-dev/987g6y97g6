package ru.rooyzee.elytrixtrader.patch;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import ru.rooyzee.elytrixtrader.Main;
import ru.rooyzee.elytrixtrader.model.TraderInstance;

/** Invisible server-side target covering the packet-only NPC's hitbox. */
public final class TraderHitbox implements Listener {
 private static final Map<TraderInstance,Target> TARGETS=new IdentityHashMap<>();
 private static final Map<UUID,Target> BY_ENTITY=new HashMap<>();
 private final Main plugin;
 private TraderHitbox(Main p){plugin=p;}
 public static void install(Main p){Bukkit.getPluginManager().registerEvents(new TraderHitbox(p),p);}
 public static void start(Main p,TraderInstance npc){
  remove(npc);if(npc==null)return;
  Target t=new Target(p,npc);TARGETS.put(npc,t);
  try{t.run();t.task=Bukkit.getScheduler().runTaskTimer((Plugin)p,t,20L,20L);}
  catch(RuntimeException ex){TARGETS.remove(npc);t.close();p.getLogger().warning("Cannot start trader hitbox: "+ex.getMessage());}
 }
 public static void remove(TraderInstance npc){Target t=TARGETS.remove(npc);if(t!=null)t.close();}
 public static void stopAll(Main plugin){for(TraderInstance npc:new ArrayList<>(TARGETS.keySet())){Target t=TARGETS.get(npc);if(t!=null&&t.plugin==plugin)remove(npc);}}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void interact(PlayerInteractEntityEvent event){
  Target t=BY_ENTITY.get(event.getRightClicked().getUniqueId());
  if(t==null||t.plugin!=plugin)return;
  event.setCancelled(true);
  if(event.getHand()!=EquipmentSlot.HAND || !t.npc.alive() || t.npc.isDespawning())return;
  Player player=event.getPlayer();if(player==null||!player.isOnline())return;
  long now=System.currentTimeMillis();Long last=t.lastClick.get(player.getUniqueId());if(last!=null&&now-last<250L)return;
  t.lastClick.put(player.getUniqueId(),now);
  plugin.menus().interact(player,t.npc); // Original permission, distance and trading checks.
 }
 @EventHandler public void quit(PlayerQuitEvent event){for(Target t:TARGETS.values())if(t.plugin==plugin)t.lastClick.remove(event.getPlayer().getUniqueId());}
 private static final class Target implements Runnable {
  final Main plugin;final TraderInstance npc;final Map<UUID,Long> lastClick=new HashMap<>();
  Interaction entity;BukkitTask task;long retry;boolean warned;
  Target(Main plugin,TraderInstance npc){this.plugin=plugin;this.npc=npc;}
  void clear(){if(entity!=null){BY_ENTITY.remove(entity.getUniqueId());try{entity.remove();}catch(RuntimeException ex){plugin.getLogger().warning("Cannot remove trader hitbox: "+ex.getMessage());}entity=null;}}
  void close(){if(task!=null)task.cancel();clear();lastClick.clear();}
  public void run(){
   Location loc=npc.location();World world=loc==null?null:loc.getWorld();
   if(world==null || !world.isChunkLoaded(loc.getBlockX()>>4,loc.getBlockZ()>>4)){clear();return;}
   if(entity!=null && entity.isValid())return;
   clear();if(System.currentTimeMillis()<retry)return;
   try{
    entity=world.spawn(loc.clone().add(0,-.02,0),Interaction.class);
    entity.setPersistent(false);entity.setGravity(false);entity.setInvulnerable(true);
    entity.setInteractionWidth(.95f);entity.setInteractionHeight(1.98f);entity.setResponsive(false);
    if(!entity.isValid())throw new IllegalStateException("hitbox spawn cancelled");
    BY_ENTITY.put(entity.getUniqueId(),this);retry=0;warned=false;
   }catch(RuntimeException|LinkageError ex){clear();retry=System.currentTimeMillis()+5000L;if(!warned){plugin.getLogger().warning("Cannot create trader hitbox: "+ex.getMessage());warned=true;}}
  }
 }
}
