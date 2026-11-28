package ru.rooyzee.elytrixairdrop.patch;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.types.ActiveFireAirdrop;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** Visual wall plus server-side boundary: no blocks are changed. */
public final class FireArena implements Listener {
 private final Main plugin;
 private final ActiveFireAirdrop event;
 private final World world;
 private final Set<Location> footprint=new HashSet<>();
 private final Map<UUID,Location> locked=new HashMap<>();
 private final Map<UUID,Long> warnings=new HashMap<>();
 private final double minX,maxX,minZ,maxZ,baseY;
 private BukkitTask task;
 private int age; private boolean closing;
 private FireArena(Main plugin,ActiveFireAirdrop event,List<Location> surface){
  this.plugin=plugin;this.event=event;this.world=event.getCenter().getWorld();
  int minx=Integer.MAX_VALUE,maxx=Integer.MIN_VALUE,minz=Integer.MAX_VALUE,maxz=Integer.MIN_VALUE;
  for(Location p:surface){minx=Math.min(minx,p.getBlockX());maxx=Math.max(maxx,p.getBlockX());minz=Math.min(minz,p.getBlockZ());maxz=Math.max(maxz,p.getBlockZ());}
  minX=minx-1;maxX=maxx+2;minZ=minz-1;maxZ=maxz+2;baseY=event.getCenter().getY()+1;
  for(Location pos:event.getBackup().keySet())if(world.equals(pos.getWorld()) && pos.getBlock().getType().isSolid())footprint.add(pos.getBlock().getLocation());
 }
 public static FireArena start(Main plugin,ActiveFireAirdrop event,List<Location> surface){
  if(surface.isEmpty() || event.getCenter().getWorld()==null)return null;
  FireArena arena=new FireArena(plugin,event,surface);
  Bukkit.getPluginManager().registerEvents(arena,plugin);
  arena.task=Bukkit.getScheduler().runTaskTimer(plugin,arena::tick,1L,5L);
  return arena;
 }
 public void close(){
  if(task!=null){task.cancel();task=null;}
  HandlerList.unregisterAll(this);
  for(UUID id:locked.keySet()){Player p=Bukkit.getPlayer(id);if(p!=null && p.isOnline())p.sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &aОгненная преграда исчезла!"));}
  locked.clear();warnings.clear();
 }
 private boolean eligible(Player p){return p.isOnline() && !p.isDead() && p.getGameMode()!=GameMode.CREATIVE && p.getGameMode()!=GameMode.SPECTATOR;}
 private boolean inside(Location l){return l!=null && world.equals(l.getWorld()) && l.getX()>=minX && l.getX()<maxX && l.getZ()>=minZ && l.getZ()<maxZ;}
 private boolean stepping(Location l){
  if(!inside(l))return false;
  Location below=l.clone().subtract(0,0.15,0).getBlock().getLocation();
  return footprint.contains(below) && below.getBlock().getType().isSolid();
 }
 private void lock(Player p,Location loc){
  if(locked.containsKey(p.getUniqueId()) || !eligible(p) || !stepping(loc))return;
  locked.put(p.getUniqueId(),loc.clone());
  if(p.isInsideVehicle())p.leaveVehicle();
  if(!closing){closing=true;age=0;world.playSound(loc,Sound.BLOCK_BEACON_ACTIVATE,0.8f,0.65f);}
  p.sendTitle(ColorUtil.colorize("&cОГНЕННЫЙ АИРДРОП"),ColorUtil.colorize("&fПреграда откроется, когда заберут лут"),5,55,10);
  p.sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &fВыход откроется, когда сундук полностью оберут."));
 }
 private void warn(Player p){
  long now=System.currentTimeMillis();Long last=warnings.get(p.getUniqueId());
  if(last==null || now-last>=3000){warnings.put(p.getUniqueId(),now);p.sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &cПреграда откроется после сбора всего лута!"));}
 }
 private void tick(){
  if(plugin.getAirdropManager().getActive()!=event || event.isLooted()){close();return;}
  for(Player p:world.getPlayers())if(eligible(p))lock(p,p.getLocation());
  if(!closing)return;
  age+=5;
  // A rising particle curtain, capped in density regardless of schematic size.
  double perimeter=2*((maxX-minX)+(maxZ-minZ));int samples=Math.min(100,Math.max(24,(int)Math.ceil(perimeter)));
  double height=Math.min(7.0,0.5+age*0.13);
  for(int i=0;i<samples;i++){
   double distance=perimeter*(i+(age%20)/20.0)/samples;
   double width=maxX-minX,depth=maxZ-minZ,x,z;
   if(distance<width){x=minX+distance;z=minZ;}
   else if((distance-=width)<depth){x=maxX;z=minZ+distance;}
   else if((distance-=depth)<width){x=maxX-distance;z=maxZ;}
   else {distance-=width;x=minX;z=maxZ-distance;}
   double y=baseY+((i*37+age*3)%100)/100.0*height;
   world.spawnParticle(i%5==0?Particle.SMOKE:Particle.FLAME,x,y,z,1,0,0,0,0);
  }
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void move(PlayerMoveEvent e){
  if(e instanceof PlayerTeleportEvent)return;
  Player p=e.getPlayer();Location to=e.getTo();if(to==null)return;
  UUID id=p.getUniqueId();
  if(!eligible(p)){locked.remove(id);warnings.remove(id);return;}
  if(!locked.containsKey(id)){lock(p,e.getFrom());if(!locked.containsKey(id))lock(p,to);}
  if(!locked.containsKey(id))return;
  if(!inside(to)){Location safe=inside(e.getFrom())?e.getFrom():locked.get(id);e.setTo(safe.clone());warn(p);}
  else locked.put(id,to.clone());
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void teleport(PlayerTeleportEvent e){
  Player p=e.getPlayer();UUID id=p.getUniqueId();
  if(!locked.containsKey(id))return;
  if(!eligible(p)){locked.remove(id);warnings.remove(id);return;}
  if(!inside(e.getTo())){e.setCancelled(true);warn(p);}
  else locked.put(id,e.getTo().clone());
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void vehicle(VehicleEnterEvent e){
  if(e.getEntered() instanceof Player && locked.containsKey(e.getEntered().getUniqueId()))e.setCancelled(true);
 }
 @EventHandler public void death(PlayerDeathEvent e){locked.remove(e.getEntity().getUniqueId());warnings.remove(e.getEntity().getUniqueId());}
 @EventHandler public void join(PlayerJoinEvent e){
  Player p=e.getPlayer();Location safe=locked.get(p.getUniqueId());
  if(safe==null)return;
  Bukkit.getScheduler().runTask(plugin,()->{
   if(plugin.getAirdropManager().getActive()!=event || event.isLooted() || !p.isOnline() || !locked.containsKey(p.getUniqueId()))return;
   if(eligible(p) && !inside(p.getLocation()))p.teleport(safe);
  });
 }
}
