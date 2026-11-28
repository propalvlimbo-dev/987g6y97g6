package ru.rooyzee.elytrixtrader.patch;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import ru.rooyzee.elytrixtrader.region.TraderRegion;
public class RegionTest {
 static class W implements World {public boolean isChunkLoaded(int x,int z){return true;}public <T extends Entity>T spawn(Location p,Class<T> type){return null;}public <T>void spawnParticle(Particle p,Location l,int n,double x,double y,double z,double s,T data){}}
 public static void main(String[] args){
  W w=new W();Location center=new Location(w,-10.5,-20,50.5);TraderRegion r=new TraderRegion("test",center,8,"experienced",1);
  for(int x=-20;x<=-2;x++)for(int y=-29;y<=-11;y++)for(int z=41;z<=59;z++){
   boolean expected=Math.abs(x+11)<=8 && Math.abs(y+20)<=8 && Math.abs(z-50)<=8;
   if(r.containsBlock(x,y,z,w)!=expected || r.contains(new Location(w,x+.4,y+.4,z+.4))!=expected)throw new AssertionError("Mismatch "+x+" "+y+" "+z);
  }
  System.out.println("Patched TraderRegion matches WorldGuard cuboid coordinates OK");
 }
}
