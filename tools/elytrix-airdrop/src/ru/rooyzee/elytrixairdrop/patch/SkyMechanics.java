package ru.rooyzee.elytrixairdrop.patch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

/** Vertical launch of players actually standing on the sky schematic. */
public final class SkyMechanics {
 private final Main plugin;
 private ActiveAirdrop current;
 private final Set<Location> floor=new HashSet<>();
 private final Map<UUID,Long> nextLaunch=new HashMap<>();
 private int minX,maxX,minY,maxY,minZ,maxZ;
 private SkyMechanics(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){SkyMechanics s=new SkyMechanics(plugin);Bukkit.getScheduler().runTaskTimer(plugin,s::tick,20L,5L);}
 public static boolean skyActive(Main plugin){ActiveAirdrop a=plugin.getAirdropManager().getActive();return a!=null && a.getType()==Airdrop.SKY;}
 private void tick(){
  ActiveAirdrop active=plugin.getAirdropManager().getActive();
  if(active==null || active.getType()!=Airdrop.SKY){current=null;floor.clear();nextLaunch.clear();return;}
  if(active!=current){current=active;indexBlocks(active);nextLaunch.clear();}
  if(floor.isEmpty() || active.getCenter().getWorld()==null)return;
  long now=System.currentTimeMillis();Set<UUID> nearby=new HashSet<>();
  for(Player player:active.getCenter().getWorld().getPlayers()){
   if(!eligible(player))continue;
   Location pos=player.getLocation();
   if(pos.getX()<minX-4 || pos.getX()>maxX+5 || pos.getZ()<minZ-4 || pos.getZ()>maxZ+5 || pos.getY()<minY-2 || pos.getY()>maxY+12)continue;
   UUID id=player.getUniqueId();nearby.add(id);
   if(!standingOnSchematic(player))continue;
   Long due=nextLaunch.get(id);
   if(due==null){nextLaunch.put(id,now+LaunchMath.delayMillis(ThreadLocalRandom.current()));continue;}
   if(now<due)continue;
   nextLaunch.put(id,now+LaunchMath.delayMillis(ThreadLocalRandom.current()));
   Vector velocity=player.getVelocity();
   velocity.setY(LaunchMath.upwardVelocity(ThreadLocalRandom.current()));
   player.setVelocity(velocity);
  }
  nextLaunch.keySet().retainAll(nearby);
 }
 private void indexBlocks(ActiveAirdrop event){
  floor.clear();boolean first=true;
  for(Location pos:event.getBackup().keySet()){
   if(pos.getWorld()==null || !pos.getBlock().getType().isSolid())continue;
   Location b=pos.getBlock().getLocation();floor.add(b);
   int x=b.getBlockX(),y=b.getBlockY(),z=b.getBlockZ();
   if(first){minX=maxX=x;minY=maxY=y;minZ=maxZ=z;first=false;}
   else{minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);}
  }
 }
 private boolean standingOnSchematic(Player player){
  if(!player.isOnGround())return false;
  Location feet=player.getLocation();
  Block at=feet.getBlock(),below=feet.clone().subtract(0,0.2,0).getBlock();
  return (floor.contains(at.getLocation()) && at.getType().isSolid()) || (floor.contains(below.getLocation()) && below.getType().isSolid());
 }
 private static boolean eligible(Player p){return p.isOnline() && !p.isDead() && p.getGameMode()!=GameMode.CREATIVE && p.getGameMode()!=GameMode.SPECTATOR && !p.isFlying() && !p.isInsideVehicle();}
}
