package ru.rooyzee.elytrixairdrop.patch;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.types.ActiveFireAirdrop;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** Schematic contact starts the physical wall and server-side boundary. */
public final class FireArena implements Listener {
 private final Main plugin;
 private final ActiveFireAirdrop event;
 private final World world;
 private final Set<Location> footprint=new HashSet<>();
 private final Map<UUID,Location> locked=new HashMap<>();
 private final Map<UUID,Long> warnings=new HashMap<>();
 private final FireFootprint geometry;
 private final List<Location> edges;
 private final Map<UUID,FireWall> walls=new HashMap<>();
 private BukkitTask task;
 private boolean closed;
 private FireArena(Main plugin,ActiveFireAirdrop event,List<Location> surface){
  this.plugin=plugin;this.event=event;this.world=event.getCenter().getWorld();
  Map<Long,Location> columns=new HashMap<>();
  for(Location p:surface){
   long k=FireFootprint.key(p.getBlockX(),p.getBlockZ());Location old=columns.get(k);
   // Prefer the highest full, opaque block as the surface; fall back to the
   // highest partial block for columns made entirely of stairs or slabs.
   boolean full=p.getBlock().getType().isOccluding();
   boolean oldFull=old!=null && old.getBlock().getType().isOccluding();
   if(old==null || (full && !oldFull) || (full==oldFull && p.getBlockY()>old.getBlockY()))columns.put(k,p.getBlock().getLocation());
  }
  geometry=new FireFootprint(columns.keySet());edges=new ArrayList<>();for(long k:geometry.edges())edges.add(columns.get(k));
  for(Location pos:surface)if(world.equals(pos.getWorld()) && pos.getBlock().getType().isSolid())footprint.add(pos.getBlock().getLocation());
 }
 public static FireArena start(Main plugin,ActiveFireAirdrop event,ConfigurationSection config){
  Clipboard clipboard=plugin.getSchematicManager().loadSchematic(config.getString("schematic","fire.schem"));
  if(clipboard==null)throw new IllegalStateException("Fire schematic unavailable; cannot derive arena footprint");
  Location origin=event.getCenter();BlockVector3 offset=clipboard.getOrigin();List<Location> blocks=new ArrayList<>();
  for(BlockVector3 v:clipboard.getRegion()){
   if(clipboard.getFullBlock(v).getBlockType().equals(BlockTypes.AIR))continue;
   Location pos=new Location(origin.getWorld(),origin.getBlockX()+v.getBlockX()-offset.getBlockX(),origin.getBlockY()+v.getBlockY()-offset.getBlockY(),origin.getBlockZ()+v.getBlockZ()-offset.getBlockZ());
   if(pos.getBlock().getType().isSolid())blocks.add(pos);
  }
  if(blocks.isEmpty())throw new IllegalStateException("Fire schematic has no solid footprint");
  return start(plugin,event,blocks);
 }
 public static FireArena start(Main plugin,ActiveFireAirdrop event,List<Location> surface){
  if(surface.isEmpty() || event.getCenter().getWorld()==null)return null;
  FireArena arena=new FireArena(plugin,event,surface);
  Bukkit.getPluginManager().registerEvents(arena,plugin);
  arena.task=Bukkit.getScheduler().runTaskTimer(plugin,arena::tick,1L,2L);
  return arena;
 }
 public void close(){
  if(closed)return;closed=true;
  if(task!=null){task.cancel();task=null;}
  for(FireWall wall:walls.values())wall.close();walls.clear();
  HandlerList.unregisterAll(this);
  for(UUID id:locked.keySet()){Player p=Bukkit.getPlayer(id);if(p!=null && p.isOnline())p.sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &aОгненная преграда исчезла!"));}
  locked.clear();warnings.clear();
 }
 private boolean eligible(Player p){return p.isOnline() && !p.isDead() && p.getGameMode()!=GameMode.CREATIVE && p.getGameMode()!=GameMode.SPECTATOR;}
 private boolean inside(Location l){return l!=null && world.equals(l.getWorld()) && geometry.contains(l.getBlockX(),l.getBlockZ());}
 private boolean stepping(Location l){
  if(!inside(l))return false;
  Location below=l.clone().subtract(0,0.15,0).getBlock().getLocation();
  return footprint.contains(below) && below.getBlock().getType().isSolid();
 }
 private void lock(Player p,Location loc){
  if(locked.containsKey(p.getUniqueId()) || !eligible(p) || !p.isOnGround() || !stepping(loc))return;
  locked.put(p.getUniqueId(),loc.clone());
  if(p.isInsideVehicle())p.leaveVehicle();
  walls.put(p.getUniqueId(),FireWall.start(plugin,world,edges,p));
  p.playSound(loc,Sound.BLOCK_BEACON_ACTIVATE,0.8f,0.65f);
  p.sendTitle(ColorUtil.colorize("&cОГНЕННЫЙ АИРДРОП"),ColorUtil.colorize("&fПреграда откроется, когда заберут лут"),5,55,10);
  p.sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &fВыход откроется, когда сундук полностью оберут."));
 }
 private void warn(Player p){
  long now=System.currentTimeMillis();Long last=warnings.get(p.getUniqueId());
  if(last==null || now-last>=3000){warnings.put(p.getUniqueId(),now);p.sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &cПреграда откроется после сбора всего лута!"));}
 }
 private void tick(){
  if(plugin.getAirdropManager().getActive()!=event || event.isLooted()){close();return;}
  for(UUID id:new ArrayList<>(locked.keySet())){Player player=Bukkit.getPlayer(id);if(player!=null && player.isOnline() && !eligible(player))release(id);}
  for(Player p:world.getPlayers())if(eligible(p))lock(p,p.getLocation());
  for(FireWall wall:walls.values())wall.tick();
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void move(PlayerMoveEvent e){
  if(e instanceof PlayerTeleportEvent)return;
  Player p=e.getPlayer();Location to=e.getTo();if(to==null)return;
  UUID id=p.getUniqueId();
  if(!eligible(p)){release(id);return;}
  if(!locked.containsKey(id)){lock(p,e.getFrom());if(!locked.containsKey(id))lock(p,to);}
  if(!locked.containsKey(id))return;
  if(!inside(to)){Location safe=inside(e.getFrom())?e.getFrom():locked.get(id);e.setTo(safe.clone());warn(p);}
  else locked.put(id,to.clone());
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void teleport(PlayerTeleportEvent e){
  Player p=e.getPlayer();UUID id=p.getUniqueId();
  if(!locked.containsKey(id))return;
  if(!eligible(p)){release(id);return;}
  if(!inside(e.getTo())){e.setCancelled(true);warn(p);}
  else locked.put(id,e.getTo().clone());
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void vehicle(VehicleEnterEvent e){
  if(e.getEntered() instanceof Player && locked.containsKey(e.getEntered().getUniqueId()))e.setCancelled(true);
 }
 @EventHandler public void disable(org.bukkit.event.server.PluginDisableEvent e){if(e.getPlugin()==plugin)close();}
 private void release(UUID id){locked.remove(id);warnings.remove(id);FireWall wall=walls.remove(id);if(wall!=null)wall.close();}
 @EventHandler public void death(PlayerDeathEvent e){release(e.getEntity().getUniqueId());}
 @EventHandler public void quit(PlayerQuitEvent e){FireWall wall=walls.remove(e.getPlayer().getUniqueId());if(wall!=null)wall.close();}
 @EventHandler public void join(PlayerJoinEvent e){
  Player p=e.getPlayer();Location safe=locked.get(p.getUniqueId());
  if(safe==null)return;
  Bukkit.getScheduler().runTask(plugin,()->{
   if(plugin.getAirdropManager().getActive()!=event || event.isLooted() || !p.isOnline() || !locked.containsKey(p.getUniqueId()))return;
   if(!eligible(p)){release(p.getUniqueId());return;}
   if(!inside(p.getLocation()))p.teleport(safe);
   if(inside(p.getLocation()))walls.put(p.getUniqueId(),FireWall.start(plugin,world,edges,p));
  });
 }
}
