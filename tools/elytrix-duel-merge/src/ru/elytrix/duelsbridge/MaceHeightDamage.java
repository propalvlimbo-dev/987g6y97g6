package ru.elytrix.duelsbridge;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Server-thread timer for continuous time above Y=104 in a mace duel. */
public final class MaceHeightDamage {
 private static final double HEIGHT=104.0;
 private static final long INTERVAL=5_000_000_000L;
 private static final Map<UUID, State> states=new HashMap<>();
 private MaceHeightDamage() {}
 private static final class State {
  final String world; final long since;
  long lastAction; boolean warned; int hits;
  State(String world,long since){this.world=world;this.since=since;}
 }
 static void clear(UUID id){states.remove(id);}
 /** Observe only uncancelled final move and teleport destinations. */
 public static void onMovement(ArenaRules rules,org.bukkit.event.player.PlayerMoveEvent event){
  Player player=event.getPlayer();if(player==null)return;
  Location to=event.getTo();ArenaRules.Bounds b=rules.resolve(player);
  if(player.isDead() || b==null || !ArenaBoundary.mace(b) || to==null || to.getWorld()==null || !b.world.equals(to.getWorld().getName())){clear(player.getUniqueId());return;}
  observe(player.getUniqueId(),b.world,to.getY(),System.nanoTime());
 }
 static void observe(UUID id,String world,double y,long now) {
  if(y<=HEIGHT){clear(id);return;}
  State state=states.get(id);
  if(state==null || !state.world.equals(world))states.put(id,new State(world,now));
 }
 /** -1 is a warning, zero means nothing is due, a positive value is raw damage. */
 static int step(UUID id,String world,double y,long now){
  observe(id,world,y,now); State state=states.get(id); if(state==null)return 0;
  if(!state.warned){
   if(now-state.since<INTERVAL)return 0;
   state.warned=true;state.lastAction=now;return -1;
  }
  if(now-state.lastAction<INTERVAL)return 0;
  state.lastAction=now;
  if(state.hits<Integer.MAX_VALUE)state.hits++;
  return state.hits;
 }
 static void tick(ArenaRules rules, Iterable<? extends Player> players, long now) {
  Set<UUID> retained=new HashSet<>();
  for(Player player:players) {
   UUID id=player.getUniqueId();ArenaRules.Bounds b=rules.resolve(player);Location loc=player.getLocation();
   if(player.isDead() || b==null || !ArenaBoundary.mace(b) || loc==null || loc.getWorld()==null || !b.world.equals(loc.getWorld().getName())) {clear(id);continue;}
   retained.add(id);
   int action=step(id,b.world,loc.getY(),now);
   if(action<0)player.sendMessage("§dДуэли §8» §eТы слишком высоко! Спустись до Y=104, иначе через 5 секунд начнётся нарастающий урон.");
   else if(action>0)player.damage(action);
  }
  states.keySet().retainAll(retained);
 }
 public static void start(ArenaRules rules,ElytrixDuelsBridge plugin) {
  Bukkit.getScheduler().runTaskTimer(plugin,()->tick(rules,Bukkit.getOnlinePlayers(),System.nanoTime()),10L,10L);
 }
}
