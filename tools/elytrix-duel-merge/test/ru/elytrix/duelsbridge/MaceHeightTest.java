package ru.elytrix.duelsbridge;
import java.util.*;import org.bukkit.*;import org.bukkit.entity.Player;import org.bukkit.event.player.*;import org.bukkit.util.Vector;
public class MaceHeightTest {
 public static class Kit {final String name;Kit(String n){name=n;}public String getKitName(){return name;}}
 static class P implements Player {UUID id=UUID.randomUUID();Location loc;Vector velocity=new Vector(0,3,0);int tp,damage,warnings; double lastDamage;P(Location l){loc=l;}public UUID getUniqueId(){return id;}public Location getLocation(){return loc;}public boolean teleport(Location l){tp++;loc=l;return true;}public Vector getVelocity(){return velocity;}public void setVelocity(Vector v){velocity=v;}public boolean isDead(){return false;}public void damage(double amount){damage++;lastDamage=amount;} public void sendMessage(String msg){warnings++;}}
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
  check(MaceHeightDamage.step(id,"w",104,s)==0,"104 not above");
  check(MaceHeightDamage.step(id,"w",105,s)==0,"starts grace");
  check(MaceHeightDamage.step(id,"w",105,s+4_999_999_999L)==0,"no early warning");
  check(MaceHeightDamage.step(id,"w",105,s+5_000_000_000L)==-1,"warning at five seconds");
  check(MaceHeightDamage.step(id,"w",105,s+9_999_999_999L)==0,"warning grace");
  check(MaceHeightDamage.step(id,"w",105,s+10_000_000_000L)==1,"first damage");
  check(MaceHeightDamage.step(id,"w",105,s+15_000_000_000L)==2,"second damage");
  check(MaceHeightDamage.step(id,"w",105,s+20_000_000_000L)==3,"third damage");
  check(MaceHeightDamage.step(id,"w",104,s+21_000_000_000L)==0,"ground resets");
  check(MaceHeightDamage.step(id,"w",105,s+21_100_000_000L)==0,"new grace after reset");
  check(MaceHeightDamage.step(id,"new-world",105,s+40_000_000_000L)==0,"new fight resets");
  MaceHeightDamage.clear(p.id);p.loc=new Location(w,0,105,0);
  MaceHeightDamage.tick(r,List.of(p),s);MaceHeightDamage.tick(r,List.of(p),s+4_999_999_999L);
  check(p.damage==0 && p.warnings==0,"no effect before grace");
  MaceHeightDamage.tick(r,List.of(p),s+5_000_000_000L);check(p.damage==0 && p.warnings==1,"warning without damage");
  MaceHeightDamage.tick(r,List.of(p),s+10_000_000_000L);check(p.damage==1 && p.lastDamage==1,"first actual damage");
  MaceHeightDamage.tick(r,List.of(p),s+15_000_000_000L);check(p.damage==2 && p.lastDamage==2,"increasing actual damage");
  p.loc=new Location(w,0,104,0);MaceHeightDamage.tick(r,List.of(p),s+16_000_000_000L);
  p.loc=new Location(w,0,105,0);MaceHeightDamage.tick(r,List.of(p),s+17_000_000_000L);
  MaceHeightDamage.tick(r,List.of(p),s+22_000_000_000L);check(p.warnings==2 && p.damage==2,"new warning after descent");
  b.maxY=107;MaceHeightDamage.clear(p.id);
  MaceHeightDamage.tick(r,List.of(p),s+30_000_000_000L);
  MaceHeightDamage.tick(r,List.of(p),s+35_000_000_000L);check(p.warnings==3 && p.damage==2,"other kit gets warning");
  MaceHeightDamage.tick(r,List.of(p),s+40_000_000_000L);check(p.damage==3 && p.lastDamage==1,"other kit gets first damage");
  MaceHeightDamage.tick(r,List.of(p),s+45_000_000_000L);check(p.damage==4 && p.lastDamage==2,"other kit gets increasing damage");
  p.velocity=new Vector(0,3,0);move.to=new Location(w,0,200,0);ArenaBoundary.move(r,plugin,Map.of(),move);
  check(move.to.getY()<107 && p.velocity.getY()==0,"other kit retains upper barrier");
  r.bounds=null;MaceHeightDamage.tick(r,List.of(p),s+46_000_000_000L);r.bounds=b;
  MaceHeightDamage.tick(r,List.of(p),s+47_000_000_000L);check(p.warnings==3 && p.damage==4,"leaving battle resets timer");
  MaceHeightDamage.clear(p.id);MaceHeightDamage.step(p.id,w.getName(),105,s);
  PlayerTeleportEvent down=new PlayerTeleportEvent();down.player=p;down.to=new Location(w,0,104,0);MaceHeightDamage.onMovement(r,down);
  check(MaceHeightDamage.step(p.id,w.getName(),105,s+20_000_000_000L)==0,"successful downward teleport resets timer");
  System.out.println("Mace height and damage tests passed");
 }
}
