package ru.elytrix.duelsbridge;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

/** Virtual arena boundary: no world blocks and no spawn reset for pearls or height. */
public final class ArenaBoundary {
 private ArenaBoundary() {}
 public static String displayLimit(String formatted,double value) { return Double.isInfinite(value) ? "без лимита" : formatted; }
 public static double effectiveHeight(double original, Object duel) {
  Object kit=duel == null ? null : Duels.call(duel,"getKitName");
  String name=kit == null ? "" : String.valueOf(kit).toLowerCase(java.util.Locale.ROOT);
  return name.contains("булав") || name.contains("mace") ? Double.POSITIVE_INFINITY : original;
 }
 static boolean mace(ArenaRules.Bounds bounds) { return Double.isInfinite(bounds.maxY) && bounds.maxY>0; }
 static double sideRadius(ArenaRules.Bounds b) { return mace(b) ? Math.min(b.radius,24.0) : b.radius; }
 static boolean inside(Location location, ArenaRules.Bounds b) {
  double dx=location.getX()-b.centerX,dz=location.getZ()-b.centerZ;
  return dx*dx+dz*dz<=sideRadius(b)*sideRadius(b) && (mace(b) || location.getY()<=b.maxY && location.getY()>=b.minY);
 }
 public static Location first(List<Location> points, Location fallback, int index) {
  return points!=null && points.size()>index && points.get(index)!=null ? points.get(index) : fallback;
 }
 public static void move(ArenaRules rules,ElytrixDuelsBridge plugin,Map<UUID,ArenaRules.Bounds> cache,PlayerMoveEvent event) {
  if(event instanceof PlayerTeleportEvent) return;
  Player player=event.getPlayer(); if(player==null)return;
  // A player may immediately enter a different battle; a cached boundary can belong to the old one.
  ArenaRules.Bounds b=rules.resolve(player);
  Location to=event.getTo();
  if(!SharedBattleWorld.enforceBoundary(player) && !BridgeFiles.getConfig(plugin).getBoolean("protection.leave",true))return;
  if(b==null || to==null || to.getWorld()==null || !b.world.equals(to.getWorld().getName()) || inside(to,b))return;
  Location bounded=clamp(to,b);
  event.setTo(bounded);
  limitVelocity(player,b,to);
 }
 public static void teleport(ArenaRules rules,ElytrixDuelsBridge plugin,PlayerTeleportEvent event) {
  Player p=event.getPlayer();if(p==null)return;
  ArenaRules.Bounds b=rules.resolve(p);Location to=event.getTo();
  if(b==null || to==null || to.getWorld()==null)return;
  boolean leave=SharedBattleWorld.enforceBoundary(p) || BridgeFiles.getConfig(plugin).getBoolean("protection.leave",true);
  boolean pearl=event.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL;
  boolean pearlLimit=BridgeFiles.getConfig(plugin).getBoolean("protection.pearl-limit",true);
  if(!b.world.equals(to.getWorld().getName())) { if(leave) event.setCancelled(true);return; }
  if(!inside(to,b) && (leave || pearl && pearlLimit))event.setCancelled(true);
 }
 public static void recover(Player player,ArenaRules.Bounds b) {
  Location loc=player.getLocation();
  if(loc==null || loc.getWorld()==null || !b.world.equals(loc.getWorld().getName()) || inside(loc,b))return;
  // Exceptional out-of-bounds state: pull to the nearest point on the boundary, never to a spawn.
  Location bounded=clamp(loc,b);limitVelocity(player,b,loc);player.teleport(bounded);
 }
 private static Location clamp(Location loc,ArenaRules.Bounds b) {
  Location out=loc.clone(); double dx=loc.getX()-b.centerX,dz=loc.getZ()-b.centerZ;
  double distance=Math.hypot(dx,dz); double radius=sideRadius(b), safe=Math.max(0,radius-0.01);
  if(distance>radius && distance>0){out.setX(b.centerX+dx*safe/distance);out.setZ(b.centerZ+dz*safe/distance);}
  if(!mace(b))out.setY(Math.max(b.minY,Math.min(b.maxY-0.01,loc.getY())));return out;
 }
 private static void limitVelocity(Player player,ArenaRules.Bounds b,Location attempted) {
  Vector v=player.getVelocity();if(v==null)return;boolean changed=false;
  if(!mace(b) && attempted.getY()>b.maxY && v.getY()>0){v.setY(0);changed=true;}
  if(!mace(b) && attempted.getY()<b.minY && v.getY()<0){v.setY(0);changed=true;}
  double dx=attempted.getX()-b.centerX,dz=attempted.getZ()-b.centerZ,dist=Math.hypot(dx,dz);
  if(dist>sideRadius(b) && dist>0){double nx=dx/dist,nz=dz/dist,dot=v.getX()*nx+v.getZ()*nz;if(dot>0){v.setX(v.getX()-dot*nx);v.setZ(v.getZ()-dot*nz);changed=true;}}
  if(changed)player.setVelocity(v);
 }
}
