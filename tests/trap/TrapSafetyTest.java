import java.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.*;
import org.bukkit.scheduler.*;
import org.bukkit.util.*;
import ru.rooyzee.elytrixitem.item.impl.TrapSafety;

public class TrapSafetyTest {
 static class P implements Player { BoundingBox box = new BoundingBox(0.2,10,0.2,0.8,11.8,0.8); public BoundingBox getBoundingBox(){return box;} }
 static class W implements World { List<Player> ps = new ArrayList<>(); Map<String,B> blocks = new HashMap<>(); public List<Player> getPlayers(){return ps;} public int getMinHeight(){return -64;} public int getMaxHeight(){return 320;} public Block getBlockAt(int x,int y,int z){ return blocks.computeIfAbsent(x+","+y+","+z, k -> new B(this,x,y,z)); } B at(int x,int y,int z,Material m){B b=(B)getBlockAt(x,y,z);b.type=m;return b;} }
 static class B implements Block { final W w; final int x,y,z; Material type=Material.AIR; B(W w,int x,int y,int z){this.w=w;this.x=x;this.y=y;this.z=z;} public boolean isPassable(){return type==Material.AIR||type==Material.WATER;} public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;} public World getWorld(){return w;} public void setType(Material m,boolean p){type=m;} public Material getType(){return type;} }
 static class S implements BlockState { B b; Material original; int updates; S(B b){this.b=b;original=b.type;} public Block getBlock(){return b;} public Material getType(){return original;} public boolean update(boolean f,boolean p){updates++;b.type=original;return true;} }
 static class Scheduler implements BukkitScheduler { List<Runnable> tasks=new ArrayList<>(); public BukkitTask runTaskLater(Plugin p,Runnable task,long delay){tasks.add(task);return null;} void run(){var copy=new ArrayList<>(tasks);tasks.clear();for(var t:copy)t.run();} }
 static void check(boolean value,String message){ if(!value)throw new AssertionError(message); }
 static BlockData data(Material type){return ()->type;}
 public static void main(String[] args){
   W w=new W(); P p=new P(); w.ps.add(p); Map<Block,BlockData> plan=new LinkedHashMap<>();
   // A schematic floor with one omitted outside corner and a wall.
   for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=-2||z!=-2)plan.put(w.getBlockAt(x,8,z),data(Material.STONE));
   Block wall=w.getBlockAt(2,10,2);plan.put(wall,data(Material.STONE));
   Block center=w.at(0,10,0,Material.STONE), distant=w.at(2,11,-1,Material.STONE);
   Block canopy=w.at(1,40,1,Material.STONE), ceiling=w.at(-1,319,1,Material.STONE), fluid=w.at(1,13,0,Material.WATER);
   Block outside=w.at(-2,10,-2,Material.STONE), below=w.at(0,7,0,Material.STONE), empty=w.getBlockAt(1,100,0);
   check(TrapSafety.prepare(null,p,w,-2,2,8,12,-2,2,plan),"valid plan");
   for(Block b:new Block[]{center,distant,canopy,ceiling,fluid})check(plan.get(b).getMaterial()==Material.AIR,"clear interior and overhead");
   check(plan.get(wall).getMaterial()==Material.STONE,"preserve schematic wall");
   check(plan.get(w.getBlockAt(0,8,0)).getMaterial()==Material.STONE,"preserve floor");
   check(!plan.containsKey(outside)&&!plan.containsKey(below),"preserve outside and below");
   check(!plan.containsKey(empty),"do not snapshot empty air");
   check(TrapSafety.clearanceLimit(null)==Integer.MAX_VALUE-1,"protected overhead");
   // A roof at any height is not silently broken through.
   B bedrock=w.at(1,250,1,Material.BEDROCK);
   check(!TrapSafety.prepare(null,p,w,-2,2,8,12,-2,2,new LinkedHashMap<>(plan)),"bedrock safety");
   bedrock.type=Material.AIR;
   // Snapshot and restore cleared overhead blocks using the same existing restore path.
   S canopyState=new S((B)canopy); ((B)canopy).type=Material.AIR;
   check(TrapSafety.restore(canopyState,true,false) && canopy.getType()==Material.STONE,"restore canopy");
   plan.put(center,data(Material.STONE));
   check(!TrapSafety.prepare(null,p,w,-2,2,8,12,-2,2,plan),"reject player collision");
   Scheduler scheduler=new Scheduler(); Bukkit.scheduler=scheduler; Bukkit.manager=name->()->true;
   B occupied=(B)center; S snapshot=new S(occupied);
   check(TrapSafety.restore(snapshot,true,false),"defer occupied restoration"); check(occupied.type==Material.AIR && snapshot.updates==0,"do not trap player");
   scheduler.run(); check(snapshot.updates==0 && scheduler.tasks.size()==1,"retry");
   w.ps.clear(); scheduler.run(); check(snapshot.updates==1 && occupied.type==Material.STONE,"restore after exit");
   w.ps.add(p); S changed=new S(occupied); TrapSafety.restore(changed,true,false);
   occupied.type=Material.STONE; w.ps.clear(); scheduler.run(); check(changed.updates==0,"no overwrite after deferred restore");
   System.out.println("TrapSafety tests passed");
 }
}
