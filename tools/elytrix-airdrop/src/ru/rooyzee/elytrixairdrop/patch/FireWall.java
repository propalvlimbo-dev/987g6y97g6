package ru.rooyzee.elytrixairdrop.patch;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.BoundingBox;
import ru.rooyzee.elytrixairdrop.Main;

/** Temporary physical blocks; originals are journalled before writing and recovered on restart. */
public final class FireWall implements Listener {
 private static final int HEIGHT=6, RISE_TICKS=36, STAGGER_TICKS=12, FRAME_TICKS=2, MAX_DISPLAYS=1536;
 private final Main plugin;private final World world;
 private final List<Part> parts=new ArrayList<>();private final List<Effect> effects=new ArrayList<>();private final Map<Location,Entry> owned=new LinkedHashMap<>();
 private int elapsed;private boolean completed,closed,journalOwner;
 private static final class Part {final Location target;final BlockData data;final int delay;BlockDisplay display;Part(Location t,BlockData d,int delay){target=t;data=d;this.delay=delay;}}
 private static final class Effect {final Location origin;final int delay;Effect(Location origin,int delay){this.origin=origin;this.delay=delay;}}
 private static final class Entry {final Location pos;final String original,placed;Entry(Location l,String o,String p){pos=l;original=o;placed=p;}}
 private FireWall(Main plugin,World world){this.plugin=plugin;this.world=world;}
 public static FireWall start(Main plugin,World world,List<Location> edges){
  FireWall wall=new FireWall(plugin,world);Bukkit.getPluginManager().registerEvents(wall,plugin);
  try{wall.prepare(edges);}catch(RuntimeException e){plugin.getLogger().warning("Fire wall could not animate: "+e.getMessage());wall.removeDisplays();}
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
  // Very large schematics still get collision blocks, without thousands of entities.
  if(parts.size()>MAX_DISPLAYS)return;
  for(Part p:parts){
   Location initial=p.target.clone().add(0,-HEIGHT,0);
   p.display=world.spawn(initial,BlockDisplay.class,d->{d.setBlock(p.data);d.setPersistent(false);d.setTeleportDuration(FRAME_TICKS);});
  }
 }
 public void tick(){
  if(closed || completed)return;
  elapsed+=FRAME_TICKS;
  for(Part p:parts)if(p.display!=null && p.display.isValid()){
   double t=Math.max(0.0,Math.min(1.0,(elapsed-p.delay)/(double)RISE_TICKS));
   double smooth=t*t*(3-2*t);
   p.display.teleport(p.target.clone().add(0,-HEIGHT*(1-smooth),0));
  }
  // A restrained rim of embers and smoke follows the rising wall, not the whole region.
  if(elapsed%6==0 && !effects.isEmpty()){
   int stride=Math.max(1,(effects.size()+17)/18);
   for(int i=(elapsed/6)%stride;i<effects.size();i+=stride){
    Effect effect=effects.get(i);double t=Math.max(0.0,Math.min(1.0,(elapsed-effect.delay)/(double)RISE_TICKS));
    Location p=effect.origin.clone().add(0,HEIGHT*t*t*(3-2*t),0);
    world.spawnParticle(Particle.FLAME,p,1,.12,.15,.12,.005);
    world.spawnParticle(Particle.SMOKE,p,1,.12,.10,.12,.005);
   }
  }
  if(elapsed<RISE_TICKS+STAGGER_TICKS)return;
  removeDisplays();completed=true;
  if(recoveryFile(plugin).exists()){plugin.getLogger().severe("Fire wall recovery file exists; refusing to overwrite it. The event boundary remains active.");return;}
  for(Part p:parts){
   Block b=p.target.getBlock();if(!b.getType().isAir() || occupied(p.target))continue;
   owned.put(p.target,new Entry(p.target,b.getBlockData().getAsString(),p.data.getAsString()));
  }
  if(owned.isEmpty())return;
  try{
   writeJournal(plugin,world,owned.values());journalOwner=true;
   for(Entry e:owned.values()){
    Block block=e.pos.getBlock();
    if(block.getType().isAir() && !occupied(e.pos))block.setBlockData(Bukkit.createBlockData(e.placed),false);
   }
  }catch(Exception e){plugin.getLogger().severe("Could not build fire wall safely: "+e.getMessage());restoreOwned();}
 }
 private boolean occupied(Location loc){
  BoundingBox cube=new BoundingBox(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),loc.getBlockX()+1,loc.getBlockY()+1,loc.getBlockZ()+1);
  Location center=loc.clone().add(.5,.5,.5);
  for(org.bukkit.entity.Entity entity:world.getNearbyEntities(center,1.5,1.5,1.5))if(entity instanceof LivingEntity && entity.getBoundingBox().overlaps(cube))return true;
  return false;
 }
 private void removeDisplays(){for(Part p:parts)if(p.display!=null){p.display.remove();p.display=null;}}
 public void close(){if(closed)return;closed=true;HandlerList.unregisterAll(this);removeDisplays();restoreOwned();}
 private void restoreOwned(){
  if(!journalOwner){owned.clear();return;}
  boolean ok=true;
  for(Entry e:owned.values())try{restore(e);}catch(RuntimeException ex){ok=false;plugin.getLogger().severe("Fire wall cleanup failed: "+ex.getMessage());}
  owned.clear();
  if(ok)try{Files.deleteIfExists(recoveryFile(plugin).toPath());journalOwner=false;}catch(IOException ex){plugin.getLogger().severe("Fire wall journal cleanup failed: "+ex.getMessage());}
 }
 private static void restore(Entry e){Block b=e.pos.getBlock();if(b.getBlockData().getAsString().equals(e.placed))b.setBlockData(Bukkit.createBlockData(e.original),false);}
 private static File recoveryFile(Main p){return new File(p.getDataFolder(),"fire-wall-recovery.bin");}
 private static void writeJournal(Main p,World w,Collection<Entry> entries)throws IOException{
  File file=recoveryFile(p);Files.createDirectories(file.toPath().getParent());Path tmp=file.toPath().resolveSibling(file.getName()+".tmp");
  try(FileOutputStream fos=new FileOutputStream(tmp.toFile());DataOutputStream out=new DataOutputStream(new BufferedOutputStream(fos))){
   out.writeUTF("ELYTRIX_FIRE_WALL_1");out.writeUTF(w.getName());out.writeInt(entries.size());
   for(Entry e:entries){out.writeInt(e.pos.getBlockX());out.writeInt(e.pos.getBlockY());out.writeInt(e.pos.getBlockZ());out.writeUTF(e.original);out.writeUTF(e.placed);}
   out.flush();fos.getFD().sync();
  }
  try{Files.move(tmp,file.toPath(),StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(tmp,file.toPath());}
 }
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
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBreak(BlockBreakEvent e){if(owned.containsKey(e.getBlock().getLocation()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBurn(BlockBurnEvent e){if(owned.containsKey(e.getBlock().getLocation()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPistonExtend(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(b->owned.containsKey(b.getLocation())))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPistonRetract(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(b->owned.containsKey(b.getLocation())))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onExplode(EntityExplodeEvent e){e.blockList().removeIf(b->owned.containsKey(b.getLocation()));}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBlockExplode(BlockExplodeEvent e){e.blockList().removeIf(b->owned.containsKey(b.getLocation()));}
}
