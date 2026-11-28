package ru.rooyzee.elytrixtrader.patch;
import org.bukkit.Location;
import org.bukkit.World;
/** Block-aligned cuboid matching the WorldGuard region, inclusive on each side. */
public final class TraderBounds {
 private TraderBounds(){}
 public static boolean contains(Location center,int radius,Location pos){
  return pos!=null && containsBlock(center,radius,pos.getBlockX(),pos.getBlockY(),pos.getBlockZ(),pos.getWorld());
 }
 public static boolean containsBlock(Location center,int radius,int x,int y,int z,World world){
  if(center==null || world==null || center.getWorld()==null || !world.equals(center.getWorld()) || radius<0)return false;
  return Math.abs((long)x-center.getBlockX())<=radius && Math.abs((long)y-center.getBlockY())<=radius && Math.abs((long)z-center.getBlockZ())<=radius;
 }
 public static boolean overlaps(Location a,int ar,Location b,int br){
  if(a==null||b==null||a.getWorld()==null||!a.getWorld().equals(b.getWorld())||ar<0||br<0)return false;
  long span=(long)ar+br;
  return Math.abs((long)a.getBlockX()-b.getBlockX())<=span && Math.abs((long)a.getBlockY()-b.getBlockY())<=span && Math.abs((long)a.getBlockZ()-b.getBlockZ())<=span;
 }
}
