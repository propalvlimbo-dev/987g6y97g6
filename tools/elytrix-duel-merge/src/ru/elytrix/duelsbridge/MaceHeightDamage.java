package ru.elytrix.duelsbridge;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** One server-thread timer per player while continuously above Y=104 in a mace duel. */
public final class MaceHeightDamage {
 private static final double HEIGHT=104.0, DAMAGE=20.0;
 private static final long GRACE=5_000_000_000L, INTERVAL=1_000_000_000L;
 private static final Map<UUID, State> states=new HashMap<>();
 private MaceHeightDamage() {}
 private static final class State { final String world; final long since; long lastDamage; boolean hit; State(String world,long since){this.world=world;this.since=since;} }
 static void clear(UUID id){states.remove(id);}
 static void observe(UUID id,String world,double y,long now) {
  if(y<=HEIGHT){clear(id);return;}
  State state=states.get(id);
  if(state==null || !state.world.equals(world))states.put(id,new State(world,now));
 }
 static boolean due(UUID id,String world,double y,long now){
  observe(id,world,y,now); State state=states.get(id); if(state==null)return false;
  if(now-state.since<GRACE || state.hit && now-state.lastDamage<INTERVAL)return false;
  state.hit=true;state.lastDamage=now;return true;
 }
 static void tick(ArenaRules rules, Iterable<? extends Player> players, long now) {
  Set<UUID> retained=new HashSet<>();
  for(Player player:players) {
   UUID id=player.getUniqueId();ArenaRules.Bounds b=rules.resolve(player);Location loc=player.getLocation();
   if(player.isDead() || b==null || !ArenaBoundary.mace(b) || loc==null || loc.getWorld()==null || !b.world.equals(loc.getWorld().getName())) {clear(id);continue;}
   retained.add(id);
   if(due(id,b.world,loc.getY(),now))player.damage(DAMAGE);
  }
  states.keySet().retainAll(retained);
 }
 public static void start(ArenaRules rules,ElytrixDuelsBridge plugin) {
  Bukkit.getScheduler().runTaskTimer(plugin,()->tick(rules,Bukkit.getOnlinePlayers(),System.nanoTime()),10L,10L);
 }
}
