package ru.rooyzee.elytrixairdrop.patch;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;

/** Individual throws near the peaceful chest, ending 15 seconds before unlock. */
public final class PeacefulMechanics {
 private final Main plugin;
 private ActiveAirdrop current;
 private final Map<UUID,Long> next=new HashMap<>(), slam=new HashMap<>();
 private PeacefulMechanics(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){PeacefulMechanics m=new PeacefulMechanics(plugin);Bukkit.getScheduler().runTaskTimer(plugin,m::tick,20L,2L);}
 private static Object field(ActiveAirdrop a,String name) throws ReflectiveOperationException {Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(a);}
 private void tick(){
  ActiveAirdrop a=plugin.getAirdropManager().getActive();
  if(a==null || a.getType()!=Airdrop.PEACEFUL){current=null;next.clear();slam.clear();return;}
  if(current!=a){current=a;next.clear();slam.clear();}
  long now=System.currentTimeMillis();Location chest;
  try{
   if((boolean)field(a,"unlocked") || (boolean)field(a,"looted") || ((boolean)field(a,"activated") && (long)field(a,"unlockTime")-now<=15000L)){next.clear();slam.clear();return;}
   chest=(Location)field(a,"chestLoc");
  }catch(ReflectiveOperationException | RuntimeException e){next.clear();slam.clear();return;}
  if(chest==null || chest.getWorld()==null)return;
  Set<UUID> near=new HashSet<>();
  for(Player p:chest.getWorld().getPlayers()){
   if(!p.isOnline() || p.isDead() || p.getGameMode()==GameMode.CREATIVE || p.getGameMode()==GameMode.SPECTATOR || p.isFlying() || p.isInsideVehicle())continue;
   Location loc=p.getLocation();double dx=loc.getX()-chest.getX(), dz=loc.getZ()-chest.getZ();
   if(dx*dx+dz*dz>400 || Math.abs(loc.getY()-chest.getY())>20)continue;
   UUID id=p.getUniqueId();near.add(id);
   Long impact=slam.get(id);
   if(impact!=null && now>=impact){
    slam.remove(id);
    if(!p.isOnGround()){Vector v=p.getVelocity();v.setY(-1.5);p.setVelocity(v);}
   }
   Long due=next.get(id);
   if(due==null){next.put(id,now+delay());continue;}
   if(now<due || !p.isOnGround())continue;
   next.put(id,now+delay());Vector v=p.getVelocity();v.setY(LaunchMath.upwardVelocity(ThreadLocalRandom.current()));p.setVelocity(v);slam.put(id,now+700L);
  }
  next.keySet().retainAll(near);slam.keySet().retainAll(near);
 }
 private static long delay(){return (5+ThreadLocalRandom.current().nextInt(6))*1000L;}
 /** Keep normal timeout from cutting short the post-loot display; the original guarded task stops it. */
 public static long endTime(ActiveAirdrop a){return isLooted(a)?System.currentTimeMillis()+300000L:a.getEndTime();}
 public static long lifetime(ActiveAirdrop a){return isLooted(a)?Long.MAX_VALUE:900000L;}
 private static boolean isLooted(ActiveAirdrop a){
  if(a==null || a.getType()!=Airdrop.PEACEFUL)return false;
  try{return (boolean)field(a,"looted");}catch(ReflectiveOperationException | RuntimeException e){return false;}
 }
}
