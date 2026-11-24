package ru.elytrix.duelsbridge;
import java.util.*;import org.bukkit.*;import org.bukkit.entity.Player;import org.bukkit.event.player.*;import org.bukkit.util.Vector;
public class MaceHeightTest {
 public static class Kit {final String name;Kit(String n){name=n;}public String getKitName(){return name;}}
 static class P implements Player {UUID id=UUID.randomUUID();Location loc;Vector velocity=new Vector(0,3,0);int tp,damage;P(Location l){loc=l;}public UUID getUniqueId(){return id;}public Location getLocation(){return loc;}public boolean teleport(Location l){tp++;loc=l;return true;}public Vector getVelocity(){return velocity;}public void setVelocity(Vector v){velocity=v;}public boolean isDead(){return false;}public void damage(double amount){damage++;}}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  check(Double.isInfinite(ArenaBoundary.effectiveHeight(15,new Kit("Булава"))),"mace unlimited");
  check(Double.isInfinite(ArenaBoundary.effectiveHeight(15,new Kit("mace"))),"english mace unlimited");
  check(ArenaBoundary.effectiveHeight(7,new Kit("Классика"))==7,"other kit unchanged");
  World w=()->"arena_instance_x"; P p=new P(new Location(w,0,100,0));ArenaRules r=new ArenaRules();ArenaRules.Bounds b=new ArenaRules.Bounds();b.world=w.getName();b.radius=20;b.maxY=Double.POSITIVE_INFINITY;b.minY=75;r.bounds=b;
  java.nio.file.Path path=java.nio.file.Files.createTempDirectory("mace");java.nio.file.Files.createDirectory(path.resolve("ElytrixDuel"));
  ElytrixDuelsBridge plugin=new ElytrixDuelsBridge(){public java.io.File getDataFolder(){return path.resolve("ElytrixDuel").toFile();}public java.io.InputStream getResource(String n){return new java.io.ByteArrayInputStream("a: 1".getBytes());}};
  PlayerMoveEvent move=new PlayerMoveEvent();move.player=p;move.from=p.loc;move.to=new Location(w,0,200,0);ArenaBoundary.move(r,plugin,Map.of(),move);
  check(move.to.getY()==200 && p.velocity.getY()==3,"no mace ceiling or upward speed change");
  move.to=new Location(w,25,200,0);p.velocity=new Vector(2,3,0);ArenaBoundary.move(r,plugin,Map.of(),move);
  check(move.to.getX()<20 && move.to.getY()==200 && p.velocity.getX()==0 && p.velocity.getY()==3,"sides only at height");
  PlayerTeleportEvent pearl=new PlayerTeleportEvent();pearl.player=p;pearl.to=new Location(w,0,200,0);pearl.cause=PlayerTeleportEvent.TeleportCause.ENDER_PEARL;ArenaBoundary.teleport(r,plugin,pearl);check(!pearl.cancelled,"high pearl allowed in mace");
  pearl.to=new Location(w,25,200,0);ArenaBoundary.teleport(r,plugin,pearl);check(pearl.cancelled,"side pearl blocked in mace");
  UUID id=UUID.randomUUID();long s=1_000_000_000L;
  check(!MaceHeightDamage.due(id,"w",104,s),"104 not above");
  check(!MaceHeightDamage.due(id,"w",105,s),"starts grace");
  check(!MaceHeightDamage.due(id,"w",105,s+4_999_999_999L),"no early damage");
  check(MaceHeightDamage.due(id,"w",105,s+5_000_000_000L),"damage after five seconds");
  check(!MaceHeightDamage.due(id,"w",105,s+5_500_000_000L),"no rapid repeat");
  check(MaceHeightDamage.due(id,"w",105,s+6_000_000_000L),"repeat after one second");
  check(!MaceHeightDamage.due(id,"w",104,s+7_000_000_000L),"ground resets");
  check(!MaceHeightDamage.due(id,"w",105,s+7_100_000_000L),"new grace after reset");
  check(!MaceHeightDamage.due(id,"new-world",105,s+20_000_000_000L),"new fight resets");
  MaceHeightDamage.clear(p.id);p.loc=new Location(w,0,105,0);
  MaceHeightDamage.tick(r,List.of(p),s);MaceHeightDamage.tick(r,List.of(p),s+4_999_999_999L);
  check(p.damage==0,"no actual damage before grace");
  MaceHeightDamage.tick(r,List.of(p),s+5_000_000_000L);check(p.damage==1,"actual damage after grace");
  b.maxY=107;MaceHeightDamage.tick(r,List.of(p),s+8_000_000_000L);check(p.damage==1,"other kit does not receive damage");
  System.out.println("Mace height and damage tests passed");
 }
}
