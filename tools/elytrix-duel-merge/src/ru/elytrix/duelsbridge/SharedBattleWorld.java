package ru.elytrix.duelsbridge;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Shared-world battle leases. Called on the main server thread. */
public final class SharedBattleWorld {
 private static final Map<String,Lease> leases=new HashMap<>();
 private static final Map<UUID,String> participants=new HashMap<>();
 private static List<String> configuration=List.of();
 private static final class Lease {
  final int slot; final List<Location> points; final Set<UUID> players; final Object battle; final double radius;
  Lease(int slot,Location a,Location b,Set<UUID> players,Object battle,double radius){this.slot=slot;this.points=List.of(a.clone(),b.clone());this.players=Set.copyOf(players);this.battle=battle;this.radius=radius;}
 }
 private SharedBattleWorld() {}
 private static List<String> signature(List<Location> points){
  List<String> result=new ArrayList<>();
  for(Location p:points)result.add(p==null||p.getWorld()==null?"-":p.getWorld().getName()+";"+p.getX()+";"+p.getY()+";"+p.getZ()+";"+p.getYaw()+";"+p.getPitch());
  return result;
 }
 private static boolean valid(Location p,World world){return p!=null && p.getWorld()!=null && p.getWorld().getName().equals(world.getName()) && Double.isFinite(p.getX()) && Double.isFinite(p.getY()) && Double.isFinite(p.getZ()) && Float.isFinite(p.getYaw()) && Float.isFinite(p.getPitch());}
 private static Set<UUID> members(Object battle){
  Set<UUID> ids=new LinkedHashSet<>();
  Object all=Duels.call(battle,"getAllParticipants");
  if(all instanceof Collection<?> c)for(Object v:c)if(v instanceof UUID)ids.add((UUID)v);
  Object a=Duels.call(battle,"getPlayer1"),b=Duels.call(battle,"getPlayer2");
  if(a instanceof UUID)ids.add((UUID)a);if(b instanceof UUID)ids.add((UUID)b);
  return ids;
 }
 private static Object battle(ElytrixDuelsBridge plugin,String token){
  Object duel=Duels.call(Duels.call(plugin,"getDuelManager"),"getDuelByWorld",token);
  return duel!=null?duel:Duels.call(Duels.call(plugin,"getTeamDuelManager"),"getByWorld",token);
 }
 public static CompletableFuture<World> create(ElytrixDuelsBridge plugin,Object arena,String token){
  try { return createChecked(plugin,arena,token); }
  catch(RuntimeException error){
   plugin.getLogger().log(java.util.logging.Level.SEVERE,"Cannot allocate shared duel arena; battle cancelled",error);
   return CompletableFuture.completedFuture(null);
  }
 }
 private static CompletableFuture<World> createChecked(ElytrixDuelsBridge plugin,Object arena,String token){
  String name=plugin.sharedArenaWorld();World world=name==null?null:Bukkit.getWorld(name);
  Object actual=Duels.call(arena,"getWorldName");Object battle=battle(plugin,token);
  Set<UUID> ids=members(battle);
  if(world==null || !"duel".equals(name) || actual==null || !name.equals(actual) || ids.size()<2 || token==null){notify(ids,"§cМир дуэлей не готов. Сообщите администратору.");return CompletableFuture.completedFuture(null);}
  List<Location> points=plugin.arenas().points();List<String> current=signature(points);
  if(!leases.isEmpty()&&!current.equals(configuration)){notify(ids,"§cТочки изменены во время боя. Подождите окончания дуэлей.");return CompletableFuture.completedFuture(null);}
  if(leases.containsKey(token)){notify(ids,"§cОшибка выделения арены. Попробуйте позже.");return CompletableFuture.completedFuture(null);}
  for(UUID id:ids)if(participants.containsKey(id)){notify(ids,"§cИгрок уже занимает арену.");return CompletableFuture.completedFuture(null);}
  for(int i=0;i+1<points.size();i+=2){
   Location a=points.get(i),b=points.get(i+1);if(!valid(a,world)||!valid(b,world))continue;
   if(!available(plugin,i,a,b))continue;
   if(leases.isEmpty())configuration=List.copyOf(current);
   Lease lease=new Lease(i,a,b,ids,battle,radius(plugin,a,b));leases.put(token,lease);for(UUID id:ids)participants.put(id,token);
   return CompletableFuture.completedFuture(world);
  }
  notify(ids,"§cВсе арены заняты. Подождите, пока одна из них освободится.");
  return CompletableFuture.completedFuture(null);
 }
 private static void notify(Set<UUID> ids,String text){for(UUID id:ids){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendMessage(text);}}
 public static void release(String token){
  Lease l=leases.remove(token);
  if(l!=null){World world=l.points.get(0).getWorld();if(world!=null)for(Entity entity:world.getEntities())if(entity instanceof Projectile projectile && projectile.getShooter() instanceof Player p && l.players.contains(p.getUniqueId()))entity.remove();
   for(UUID id:l.players)participants.remove(id,token);
  }
  if(leases.isEmpty())configuration=List.of();
 }
 public static void respawn(PlayerRespawnEvent event,Location to){
  Player player=event.getPlayer();Lease l=leases.get(participants.get(player.getUniqueId()));
  if(l!=null && Boolean.TRUE.equals(Duels.call(l.battle,"isActive")) && to!=null && to.getWorld()!=null && to.getWorld().getName().equals(l.points.get(0).getWorld().getName()))to=l.points.get(firstSide(l.battle,player.getUniqueId())?0:1).clone();
  event.setRespawnLocation(to);
 }
 public static int reserve(ElytrixDuelsBridge plugin){return full(plugin)?0:1;}
 public static boolean enforceBoundary(Player player){return player!=null && participants.containsKey(player.getUniqueId());}
 public static boolean full(ElytrixDuelsBridge plugin){
  List<Location> points=plugin.arenas().points();String name=plugin.sharedArenaWorld();World w=name==null?null:Bukkit.getWorld(name);
  if(w==null||!leases.isEmpty()&&!signature(points).equals(configuration))return true;
  for(int i=0;i+1<points.size();i+=2){if(!valid(points.get(i),w)||!valid(points.get(i+1),w))continue;if(available(plugin,i,points.get(i),points.get(i+1)))return false;}return true;
 }
 public static List<Location> points(Arenas ignored,World world,Player player){
  Lease l=leases.get(participants.get(player.getUniqueId()));
  if(l==null||world==null||!world.getName().equals(l.points.get(0).getWorld().getName()))return List.of();
  return List.of(l.points.get(0).clone(),l.points.get(1).clone());
 }
 public static void teleport(ElytrixDuelsBridge plugin,PlayerTeleportEvent event){
  if(event.getCause()!=PlayerTeleportEvent.TeleportCause.PLUGIN && event.getCause()!=PlayerTeleportEvent.TeleportCause.UNKNOWN)return;
  Player player=event.getPlayer();Location to=event.getTo();if(player==null||to==null||to.getWorld()==null)return;
  Lease l=leases.get(participants.get(player.getUniqueId()));if(l==null||!Boolean.TRUE.equals(Duels.call(l.battle,"isActive"))||!to.getWorld().getName().equals(plugin.sharedArenaWorld()))return;
  List<Location> defaults=plugin.arenas().points();if(defaults.size()<2||defaults.get(0)==null||defaults.get(1)==null)return;
  Location a=defaults.get(0),b=defaults.get(1);double da=horizontal(a,to),db=horizontal(b,to);
  int side=da<=db&&da<=4?0:db<=4?1:-1;if(side<0)return;
  Location base=side==0?a:b, dest=l.points.get(side);double x=to.getX()-base.getX(),z=to.getZ()-base.getZ(),d=Math.hypot(x,z);
  if(d>0.0001){x=x/d*0.5;z=z/d*0.5;}else{x=0;z=0;}
  event.setTo(new Location(to.getWorld(),dest.getX()+x,dest.getY(),dest.getZ()+z,dest.getYaw(),dest.getPitch()));
 }
 private static boolean firstSide(Object battle,UUID id){Object team=Duels.call(battle,"getTeamA");if(team instanceof Collection<?>)return ((Collection<?>)team).contains(id);return id.equals(Duels.call(battle,"getPlayer1"));}
 private static double radius(ElytrixDuelsBridge plugin,Location a,Location b){double margin=BridgeFiles.getConfig(plugin).getDouble("protection.arena-margin",20);return horizontal(a,b)/2+(Double.isFinite(margin)?Math.max(0,margin):20);}
 private static boolean available(ElytrixDuelsBridge plugin,int slot,Location a,Location b){
  double x=(a.getX()+b.getX())/2,z=(a.getZ()+b.getZ())/2,r=radius(plugin,a,b);
  for(Lease l:leases.values()){if(l.slot==slot)return false;Location p=l.points.get(0),q=l.points.get(1);if(Math.hypot(x-(p.getX()+q.getX())/2,z-(p.getZ()+q.getZ())/2)<r+l.radius)return false;}return true;
 }
 private static double horizontal(Location a,Location b){return Math.hypot(a.getX()-b.getX(),a.getZ()-b.getZ());}
 public static boolean checkedTeleport(Player player,Location to,Object manager,Object battle){
  boolean ok=false;
  try {
   ok=player.teleport(to);
   Lease lease=leases.get(participants.get(player.getUniqueId()));Location actual=player.getLocation();
   if(lease==null || actual==null || actual.getWorld()==null || !actual.getWorld().getName().equals(lease.points.get(0).getWorld().getName()) || Math.min(horizontal(actual,lease.points.get(0)),horizontal(actual,lease.points.get(1)))>4.0)ok=false;
  } catch(RuntimeException ex){ok=false;}
  if(!ok){
   player.sendMessage("§cНе удалось телепортироваться на арену. Бой отменён, восстанавливаем данные.");
   try{java.lang.reflect.Method finish=manager.getClass().getDeclaredMethod("finishReturn",battle.getClass());finish.setAccessible(true);finish.invoke(manager,battle);}
   catch(ReflectiveOperationException e){throw new IllegalStateException("Не удалось отменить бой после ошибки телепорта; резервные копии инвентарей сохранены",e);}
   throw new IllegalStateException("Телепорт на арену отменён или изменён другим плагином; бой отменён");
  }
  return true;
 }
 public static String realWorld(Object battle){Object world=Duels.call(battle,"getInstanceWorld");Object name=Duels.call(world,"getName");return name==null?"":name.toString();}
 public static String fullId(String id,int begin,int end){return id;}
 public static boolean startsWith(String name,String prefix){return name.startsWith(prefix) || prefix.equals("arena_instance_") && name.equals("duel");}
 public static void splash(PotionSplashEvent event){
  Object source=event.getPotion().getShooter();String owner=source instanceof Player p?participants.get(p.getUniqueId()):null;
  for(LivingEntity entity:event.getAffectedEntities()){
   String target=entity instanceof Player p?participants.get(p.getUniqueId()):null;
   if((owner!=null || target!=null) && (owner==null || !owner.equals(target)))event.setIntensity(entity,0);
  }
 }
 public static void lingering(LingeringPotionSplashEvent event){
  World world=event.getEntity().getWorld();
  if(world!=null && world.getName().equals("duel"))event.setCancelled(true);
 }
 public static void isolate(EntityDamageByEntityEvent event){
  if(!(event.getEntity() instanceof Player victim))return;
  Entity source=event.getDamager();Player attacker=null;
  if(source instanceof Player p)attacker=p;else if(source instanceof Projectile p && p.getShooter() instanceof Player shooter)attacker=shooter;
  if(attacker==null){if(participants.containsKey(victim.getUniqueId()))event.setCancelled(true);return;}
  String a=participants.get(attacker.getUniqueId()),b=participants.get(victim.getUniqueId());
  if((a!=null || b!=null) && (a==null || !a.equals(b)))event.setCancelled(true);
 }
}
