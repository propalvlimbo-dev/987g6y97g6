package ru.rooyzee.elytrixtrader.patch;
import org.bukkit.*;
import org.bukkit.entity.Entity;
public class BoundsTest {
 static class W implements World {public boolean isChunkLoaded(int x,int z){return true;}public <T extends Entity>T spawn(Location p,Class<T> type){return null;}public <T>void spawnParticle(Particle p,Location l,int n,double x,double y,double z,double s,T data){}}
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  W world=new W(),other=new W();Location c=new Location(world,-10.5,-20,50.5);
  for(int x=-11-9;x<=-11+9;x++)for(int y=-20-9;y<=-20+9;y++)for(int z=50-9;z<=50+9;z++){
   boolean inside=Math.abs(x+11)<=8 && Math.abs(y+20)<=8 && Math.abs(z-50)<=8;
   check(TraderBounds.containsBlock(c,8,x,y,z,world)==inside);
   check(TraderBounds.contains(c,8,new Location(world,x+.4,y+.4,z+.4))==inside);
  }
  check(!TraderBounds.containsBlock(c,8,-11,-20,50,other));check(!TraderBounds.contains(c,8,null));check(!TraderBounds.contains(null,8,c));
  check(!TraderBounds.containsBlock(c,8,Integer.MAX_VALUE,-20,50,world));
  check(TraderBounds.overlaps(c,8,new Location(world,5.5,-4,66.5),8));
  check(!TraderBounds.overlaps(c,8,new Location(world,6.5,-4,66.5),8));
  check(!TraderBounds.overlaps(c,8,new Location(world,5.5,-3,66.5),8));
  check(!TraderBounds.overlaps(c,8,new Location(other,-10.5,-20,50.5),8));
  System.out.println("8-block cuboid boundaries, corners, negative heights and worlds OK");
 }
}
