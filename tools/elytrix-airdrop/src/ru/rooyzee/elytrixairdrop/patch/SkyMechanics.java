package ru.rooyzee.elytrixairdrop.patch;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;

/** A single gentle wind for players over the actual pasted sky structure. */
public final class SkyMechanics {
 private static final long DIRECTION_INTERVAL_MS=30_000L;
 private final Main plugin;
 private ActiveAirdrop current;
 private final Set<Location> floor=new HashSet<>();
 private int direction;
 private long nextChange;
 private SkyMechanics(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){SkyMechanics s=new SkyMechanics(plugin);Bukkit.getScheduler().runTaskTimer(plugin,s::tick,20L,5L);}
 public static boolean skyActive(Main plugin){ActiveAirdrop a=plugin.getAirdropManager().getActive();return a!=null && a.getType()==Airdrop.SKY;}
 private void tick(){
  ActiveAirdrop active=plugin.getAirdropManager().getActive();
  if(active==null || active.getType()!=Airdrop.SKY){current=null;floor.clear();return;}
  long now=System.currentTimeMillis();
  if(active!=current){current=active;indexBlocks(active);direction=ThreadLocalRandom.current().nextInt(8);nextChange=now+DIRECTION_INTERVAL_MS;}
  else if(now>=nextChange){direction=WindMath.nextDirection(direction,ThreadLocalRandom.current().nextInt(1,8));nextChange=now+DIRECTION_INTERVAL_MS;}
  if(floor.isEmpty()||active.getCenter().getWorld()==null)return;
  double dx=WindMath.directionX(direction),dz=WindMath.directionZ(direction);
  for(Player player:active.getCenter().getWorld().getPlayers()){
   if(!eligible(player) || !aboveSchematic(player))continue;
   Vector velocity=player.getVelocity();
   double impulse=WindMath.impulse(velocity.getX(),velocity.getZ(),dx,dz);
   if(impulse<=0)continue;
   velocity.setX(velocity.getX()+dx*impulse);
   velocity.setZ(velocity.getZ()+dz*impulse);
   player.setVelocity(velocity);
  }
 }
 private void indexBlocks(ActiveAirdrop event){
  floor.clear();
  // The backup includes cleared terrain. Only solid blocks that remain after the paste are indexed.
  for(Location pos:event.getBackup().keySet())if(pos.getWorld()!=null && pos.getBlock().getType().isSolid())floor.add(pos.getBlock().getLocation());
 }
 private boolean aboveSchematic(Player player){
  Location feet=player.getLocation();
  // Includes standing, slabs and short jumps, but not the entire WorldGuard region.
  for(int i=0;i<=6;i++){
   Block block=feet.clone().subtract(0,i*0.5,0).getBlock();
   if(floor.contains(block.getLocation()) && block.getType().isSolid())return true;
  }
  return false;
 }
 private static boolean eligible(Player p){return p.isOnline() && !p.isDead() && p.getGameMode()!=GameMode.CREATIVE && p.getGameMode()!=GameMode.SPECTATOR && !p.isFlying() && !p.isInsideVehicle();}
}
