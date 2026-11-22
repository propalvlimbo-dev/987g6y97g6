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
 static class W implements World { List<Player> ps = new ArrayList<>(); Map<String,B> blocks = new HashMap<>(); public List<Player> getPlayers(){return ps;} public int getMinHeight(){return -64;} public int getMaxHeight(){return 320;} public Block getBlockAt(int x,int y,int z){ return blocks.computeIfAbsent(x+","+y+","+z, k -> new B(this,x,y,z)); } }
 static class B implements Block { final W w; final int x,y,z; Material type=Material.STONE; B(W w,int x,int y,int z){this.w=w;this.x=x;this.y=y;this.z=z;} public boolean isPassable(){return type==Material.AIR;} public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;} public World getWorld(){return w;} public void setType(Material m,boolean p){type=m;} public Material getType(){return type;} }
 static class S implements BlockState { B b; Material original; int updates; S(B b){this.b=b;original=b.type;} public Block getBlock(){return b;} public Material getType(){return original;} public boolean update(boolean f,boolean p){updates++;b.type=original;return true;} }
 static class Scheduler implements BukkitScheduler { List<Runnable> tasks=new ArrayList<>(); public BukkitTask runTaskLater(Plugin p,Runnable task,long delay){tasks.add(task);return null;} void run(){var copy=new ArrayList<>(tasks);tasks.clear();for(var t:copy)t.run();} }
 static void check(boolean value){ if(!value)throw new AssertionError(); }
 public static void main(String[] args){
   W w=new W(); P p=new P(); w.ps.add(p); Map<Block,BlockData> plan=new LinkedHashMap<>();
   B remote=(B)w.getBlockAt(2,10,2); plan.put(remote,()->Material.STONE);
   check(TrapSafety.prepare(null,p,w,-2,2,8,12,-2,2,plan));
   check(plan.get(w.getBlockAt(0,10,0)).getMaterial()==Material.AIR);
   check(plan.get(remote).getMaterial()==Material.STONE);
   check(!plan.containsKey(w.getBlockAt(-2,10,-2))); // schematic air left alone
   plan.put(w.getBlockAt(0,10,0),()->Material.STONE);
   check(!TrapSafety.prepare(null,p,w,-2,2,8,12,-2,2,plan));
   Scheduler scheduler=new Scheduler(); Bukkit.scheduler=scheduler; Bukkit.manager=name->()->true;
   B occupied=(B)w.getBlockAt(0,10,0); S snapshot=new S(occupied);
   check(TrapSafety.restore(snapshot,true,false)); check(occupied.type==Material.AIR && snapshot.updates==0);
   scheduler.run(); check(snapshot.updates==0 && scheduler.tasks.size()==1);
   w.ps.clear(); scheduler.run(); check(snapshot.updates==1 && occupied.type==Material.STONE);
   w.ps.add(p); S changed=new S(occupied); TrapSafety.restore(changed,true,false);
   occupied.type=Material.STONE; w.ps.clear(); scheduler.run(); check(changed.updates==0); // no overwrite
   System.out.println("TrapSafety tests passed");
 }
}
