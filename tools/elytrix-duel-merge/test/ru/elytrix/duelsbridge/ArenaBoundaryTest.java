package ru.elytrix.duelsbridge;
import java.util.*;import java.nio.file.*;import org.bukkit.*;import org.bukkit.entity.Player;import org.bukkit.util.Vector;import org.bukkit.event.player.*;
public class ArenaBoundaryTest {
 static class P implements Player{UUID id=UUID.randomUUID();Location loc;Vector v=new Vector(0,2,0);int teleports;P(Location l){loc=l;}public UUID getUniqueId(){return id;} public Location getLocation(){return loc;} public Vector getVelocity(){return v;}public void setVelocity(Vector v){this.v=v;}public boolean teleport(Location l){teleports++;loc=l;return true;}}
 static void ck(boolean x,String s){if(!x)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  World w=()->"arena_instance_test";P p=new P(new Location(w,0,100,0)); ArenaRules r=new ArenaRules();ArenaRules.Bounds b=new ArenaRules.Bounds(); b.world=w.getName();b.centerX=0;b.centerZ=0;b.radius=20;b.minY=75;b.maxY=115;r.bounds=b;
  var root=Files.createTempDirectory("arenaguard");Files.createDirectory(root.resolve("ElytrixDuel"));ElytrixDuelsBridge plugin=new ElytrixDuelsBridge(){public java.io.File getDataFolder(){return root.resolve("ElytrixDuel").toFile();} public java.io.InputStream getResource(String n){return new java.io.ByteArrayInputStream("a: 1".getBytes());}};
  PlayerTeleportEvent pearl=new PlayerTeleportEvent();pearl.player=p;pearl.from=p.loc;pearl.to=new Location(w,1,120,1);pearl.cause=PlayerTeleportEvent.TeleportCause.ENDER_PEARL;ArenaBoundary.teleport(r,plugin,pearl);
  ck(pearl.cancelled && p.teleports==0 && p.loc.getY()==100,"out-of-bounds pearl cancelled without spawn teleport");
  PlayerTeleportEvent ok=new PlayerTeleportEvent();ok.player=p;ok.from=p.loc;ok.to=new Location(w,1,110,1);ok.cause=PlayerTeleportEvent.TeleportCause.ENDER_PEARL;ArenaBoundary.teleport(r,plugin,ok);ck(!ok.cancelled,"valid pearl allowed");
  PlayerMoveEvent move=new PlayerMoveEvent();move.player=p;move.from=p.loc;move.to=new Location(w,1,120,1);ArenaBoundary.move(r,plugin,Map.of(),move);
  ck(move.to.getY()<115 && p.v.getY()==0 && p.teleports==0,"mace ceiling clamps without teleport and reduces upward velocity");
  p.v=new Vector(2,0,0);move.to=new Location(w,25,105,0);ArenaBoundary.move(r,plugin,Map.of(),move);ck(move.to.getX()<20 && p.v.getX()==0,"horizontal boundary and outward velocity clamped");
  p.loc=new Location(w,25,105,0);ArenaBoundary.recover(p,b);ck(p.teleports==1 && p.loc.getX()>19 && p.loc.getX()<20,"recovery at edge, not spawn");
  System.out.println("Arena boundary tests passed");
 }
}
