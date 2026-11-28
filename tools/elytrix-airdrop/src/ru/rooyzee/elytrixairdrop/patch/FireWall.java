package ru.rooyzee.elytrixairdrop.patch;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import ru.rooyzee.elytrixairdrop.Main;

/** Per-player visual wall; the actual boundary is enforced by FireArena. */
public final class FireWall {
 private static final int HEIGHT=6, RISE_TICKS=36, STAGGER_TICKS=12, FRAME_TICKS=2, MAX_DISPLAYS=1536, GLOBAL_DISPLAY_LIMIT=3000;
 private static int activeDisplays;
 private final Main plugin;private final World world;private final Player viewer;
 private final List<Part> parts=new ArrayList<>();private final List<Effect> effects=new ArrayList<>();
 private final Set<Location> sent=new HashSet<>();
 private int elapsed;private boolean completed,closed;
 private static final class Part {final Location target;final BlockData data;final int delay;BlockDisplay display;Part(Location t,BlockData d,int delay){target=t;data=d;this.delay=delay;}}
 private static final class Effect {final Location origin;final int delay;Effect(Location origin,int delay){this.origin=origin;this.delay=delay;}}
 private static final class Entry {final Location pos;final String original,placed;Entry(Location l,String o,String p){pos=l;original=o;placed=p;}}
 private FireWall(Main plugin,World world,Player viewer){this.plugin=plugin;this.world=world;this.viewer=viewer;}
 public static FireWall start(Main plugin,World world,List<Location> edges,Player viewer){
  FireWall wall=new FireWall(plugin,world,viewer);
  try{wall.prepare(edges);}catch(RuntimeException e){plugin.getLogger().warning("Personal fire wall could not animate: "+e.getMessage());wall.removeDisplays();}
  return wall;
 }
 private void prepare(List<Location> edges){
  double centerX=0,centerZ=0;
  for(Location edge:edges){centerX+=edge.getX()+.5;centerZ+=edge.getZ()+.5;}
  if(!edges.isEmpty()){centerX/=edges.size();centerZ/=edges.size();}
  for(Location edge:edges){
   Block source=edge.getBlock();if(!source.getType().isSolid())continue;
   BlockData data=source.getState() instanceof TileState?Material.BLACKSTONE.createBlockData():source.getBlockData().clone();
   double angle=Math.atan2(edge.getZ()+.5-centerZ,edge.getX()+.5-centerX);
   int delay=(int)Math.round(STAGGER_TICKS*(angle+Math.PI)/(2*Math.PI));
   effects.add(new Effect(edge.clone().add(.5,1.0,.5),delay));
   for(int y=1;y<=HEIGHT;y++){
    Location target=edge.clone().add(0,y,0).getBlock().getLocation();
    if(target.getBlockY()>=world.getMaxHeight() || !target.getBlock().getType().isAir())continue;
    parts.add(new Part(target,data,delay));
   }
  }
  // Bound the number of per-player display entities; fallback is instant private blocks.
  if(parts.size()>MAX_DISPLAYS || activeDisplays+parts.size()>GLOBAL_DISPLAY_LIMIT){completed=true;refresh();return;}
  for(Part p:parts){
   Location initial=p.target.clone().add(0,-HEIGHT,0);
   p.display=world.spawn(initial,BlockDisplay.class,d->{d.setVisibleByDefault(false);d.setBlock(p.data);d.setPersistent(false);d.setTeleportDuration(FRAME_TICKS);});
   activeDisplays++;viewer.showEntity(plugin,p.display);
  }
 }
 public void tick(){
  if(closed || !viewer.isOnline() || !world.equals(viewer.getWorld()))return;
  elapsed+=FRAME_TICKS;
  if(completed){if(elapsed%40==0)refresh();return;}
  for(Part p:parts)if(p.display!=null && p.display.isValid()){
   double t=Math.max(0.0,Math.min(1.0,(elapsed-p.delay)/(double)RISE_TICKS));double smooth=t*t*(3-2*t);
   p.display.teleport(p.target.clone().add(0,-HEIGHT*(1-smooth),0));
  }
  if(elapsed%6==0 && !effects.isEmpty()){
   int stride=Math.max(1,(effects.size()+17)/18);
   for(int i=(elapsed/6)%stride;i<effects.size();i+=stride){
    Effect effect=effects.get(i);double t=Math.max(0.0,Math.min(1.0,(elapsed-effect.delay)/(double)RISE_TICKS));
    Location p=effect.origin.clone().add(0,HEIGHT*t*t*(3-2*t),0);
    viewer.spawnParticle(Particle.FLAME,p,1,.12,.15,.12,.005);viewer.spawnParticle(Particle.SMOKE,p,1,.12,.10,.12,.005);
   }
  }
  if(elapsed<RISE_TICKS+STAGGER_TICKS)return;
  removeDisplays();completed=true;refresh();
 }
 private boolean occupied(Location loc){
  BoundingBox cube=new BoundingBox(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),loc.getBlockX()+1,loc.getBlockY()+1,loc.getBlockZ()+1);
  return viewer.getBoundingBox().overlaps(cube);
 }
 private void refresh(){
  if(!viewer.isOnline() || !world.equals(viewer.getWorld()))return;
  for(Part p:parts){
   Block real=p.target.getBlock();
   if(real.getType().isAir() && !occupied(p.target)){viewer.sendBlockChange(p.target,p.data);sent.add(p.target);}
   else if(sent.remove(p.target))viewer.sendBlockChange(p.target,real.getBlockData());
  }
 }
 private void removeDisplays(){for(Part p:parts)if(p.display!=null){p.display.remove();p.display=null;activeDisplays=Math.max(0,activeDisplays-1);}}
 public void close(){
  if(closed)return;closed=true;removeDisplays();
  if(viewer.isOnline() && world.equals(viewer.getWorld()))for(Location p:sent)viewer.sendBlockChange(p,p.getBlock().getBlockData());
  sent.clear();
 }
 private static void restore(Entry e){Block b=e.pos.getBlock();if(b.getBlockData().getAsString().equals(e.placed))b.setBlockData(Bukkit.createBlockData(e.original),false);}
 private static File recoveryFile(Main p){return new File(p.getDataFolder(),"fire-wall-recovery.bin");}
 /** Run once after plugin enable. Leave journal intact if the world or any block cannot be restored. */
 public static void recover(Main plugin){Bukkit.getScheduler().runTask(plugin,()->recoverNow(plugin));}
 private static void recoverNow(Main plugin){
  File f=recoveryFile(plugin);if(!f.exists())return;
  try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(f)))){
   if(!"ELYTRIX_FIRE_WALL_1".equals(in.readUTF()))throw new IOException("unknown journal version");
   String name=in.readUTF();World world=Bukkit.getWorld(name);if(world==null)throw new IOException("world not loaded: "+name);
   int n=in.readInt();if(n<0||n>100000)throw new IOException("invalid record count");List<Entry> records=new ArrayList<>();
   for(int i=0;i<n;i++){Location l=new Location(world,in.readInt(),in.readInt(),in.readInt());records.add(new Entry(l,in.readUTF(),in.readUTF()));}
   for(Entry e:records)restore(e);
   Files.delete(f.toPath());plugin.getLogger().info("Recovered temporary fire wall blocks after restart");
  }catch(Exception e){plugin.getLogger().severe("Could not recover temporary fire wall; keep journal and check world: "+e.getMessage());}
 }
}
