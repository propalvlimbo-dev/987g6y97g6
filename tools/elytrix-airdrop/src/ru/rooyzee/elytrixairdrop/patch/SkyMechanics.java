package ru.rooyzee.elytrixairdrop.patch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;

/** Per-player random launch only while standing on a block of the pasted sky structure. */
public final class SkyMechanics {
 private final Main plugin;
 private ActiveAirdrop current;
 private final Set<Location> floor=new HashSet<>();
 private final Map<UUID,Long> deadlines=new HashMap<>();
 private int minX,maxX,minY,maxY,minZ,maxZ;
 private SkyMechanics(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){SkyMechanics s=new SkyMechanics(plugin);Bukkit.getScheduler().runTaskTimer(plugin,s::tick,20L,5L);}
 /** Only bypass this plugin's pearl/chorus restriction for a sky event. */
 public static boolean skyActive(Main plugin){ActiveAirdrop a=plugin.getAirdropManager().getActive();return a!=null && a.getType()==Airdrop.SKY;}
 private void tick(){
  ActiveAirdrop active=plugin.getAirdropManager().getActive();
  if(active==null || active.getType()!=Airdrop.SKY){current=null;floor.clear();deadlines.clear();return;}
  if(active!=current){current=active;indexBlocks(active);deadlines.clear();}
  if(floor.isEmpty()||active.getCenter().getWorld()==null)return;
  World world=active.getCenter().getWorld();long now=System.currentTimeMillis();Set<UUID> nearby=new HashSet<>();
  for(Player p:world.getPlayers()){
   if(!eligible(p))continue;
   Location loc=p.getLocation();
   if(loc.getX()<minX-4||loc.getX()>maxX+5||loc.getZ()<minZ-4||loc.getZ()>maxZ+5||loc.getY()<minY-2||loc.getY()>maxY+10)continue;
   UUID id=p.getUniqueId();nearby.add(id);
   if(!standingOnSchematic(p))continue;
   Long due=deadlines.get(id);
   if(due==null){deadlines.put(id,next(now));continue;}
   if(now>=due){deadlines.put(id,next(now));launch(p,active);}
  }
  deadlines.keySet().retainAll(nearby);
 }
 private void indexBlocks(ActiveAirdrop event){
  floor.clear();boolean first=true;
  for(Location pos:event.getBackup().keySet()){
   if(pos.getWorld()==null||!pos.getBlock().getType().isSolid())continue;
   Location b=pos.getBlock().getLocation();floor.add(b);
   int x=b.getBlockX(),y=b.getBlockY(),z=b.getBlockZ();
   if(first){minX=maxX=x;minY=maxY=y;minZ=maxZ=z;first=false;}
   else {minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);}
  }
 }
 private boolean standingOnSchematic(Player p){
  if(!p.isOnGround())return false;
  Location feet=p.getLocation();
  // Check the block at the feet as well as just underneath (slabs and carpets).
  Block at=feet.getBlock(), below=feet.clone().subtract(0,0.2,0).getBlock();
  return (floor.contains(at.getLocation()) && at.getType().isSolid()) || (floor.contains(below.getLocation()) && below.getType().isSolid());
 }
 private static boolean eligible(Player p){return p.isOnline() && !p.isDead() && p.getGameMode()!=GameMode.CREATIVE && p.getGameMode()!=GameMode.SPECTATOR && !p.isFlying() && !p.isInsideVehicle();}
 private static long next(long now){return now+ThreadLocalRandom.current().nextInt(5,16)*1000L;}
 private void launch(Player player,ActiveAirdrop active){
  Location start=player.getLocation().clone();
  Vector initial=player.getVelocity();initial.setY(ThreadLocalRandom.current().nextDouble(0.92,1.12));player.setVelocity(initial);
  Bukkit.getScheduler().runTaskLater(plugin,()->{
   if(plugin.getAirdropManager().getActive()!=active || !eligible(player))return;
   Location loc=player.getLocation();
   if(loc.getWorld()!=start.getWorld()||loc.distanceSquared(start)>144)return;
   Vector throwVector=outward(loc.getX(),loc.getZ(),minX,maxX,minZ,maxZ);
   player.setVelocity(throwVector);
  },10L);
 }
 /** Aim towards the nearest boundary; let the server's collision physics apply. */
 static Vector outward(double x,double z,int minX,int maxX,int minZ,int maxZ){
  double[] d={Math.max(0,x-minX),Math.max(0,maxX+1-x),Math.max(0,z-minZ),Math.max(0,maxZ+1-z)};
  int k=0;for(int i=1;i<4;i++)if(d[i]<d[k])k=i;
  double speed=Math.max(0.8,Math.min(3.5,(d[k]+2)/5.0));
  return new Vector(k==0?-speed:k==1?speed:0,-0.55,k==2?-speed:k==3?speed:0);
 }
}
