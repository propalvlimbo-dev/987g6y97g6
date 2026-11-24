package ru.elytrix.duelsbridge;
import java.util.*;import org.bukkit.*;import org.bukkit.entity.*;import org.bukkit.event.player.*;import org.bukkit.event.entity.*;import org.bukkit.util.Vector;
public class SharedBattleWorldTest {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);}
 public static class Battle {UUID a=UUID.randomUUID(),b=UUID.randomUUID();boolean active=true;public boolean isActive(){return active;}public UUID getPlayer1(){return a;}public UUID getPlayer2(){return b;}}
 public static class Manager {Map<String,Battle> map=new HashMap<>();public Battle getDuelByWorld(String id){return map.get(id);} }
 public static class TeamBattle {UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),d=UUID.randomUUID();public Set<UUID> getAllParticipants(){return Set.of(a,b,c,d);}public Set<UUID> getTeamA(){return Set.of(a,b);}public boolean isActive(){return true;}}
 public static class TeamManager {Map<String,TeamBattle> map=new HashMap<>();public TeamBattle getByWorld(String id){return map.get(id);}}
 public static class Plugin extends ElytrixDuelsBridge {Arenas a=new Arenas();Manager m=new Manager();TeamManager t=new TeamManager();public java.io.File getDataFolder(){return new java.io.File(System.getProperty("java.io.tmpdir"),"shared-battle-tests/ElytrixDuel");} public java.io.InputStream getResource(String n){return new java.io.ByteArrayInputStream("x: 1".getBytes());} Arenas arenas(){return a;}public Manager getDuelManager(){return m;}public TeamManager getTeamDuelManager(){return t;}}
 public static class Arena {public String getWorldName(){return "duel";}}
 static class P implements Player {UUID id;Location l;String msg="";boolean allow=true;P(UUID id,World w){this.id=id;l=new Location(w,0,101,0);}public UUID getUniqueId(){return id;}public Location getLocation(){return l;}public boolean teleport(Location l){if(!allow)return false;this.l=l;return true;}public Vector getVelocity(){return new Vector(0,0,0);}public void setVelocity(Vector v){}public boolean isDead(){return false;}public void damage(double d){}public void sendMessage(String s){msg=s;}}
 public static class FinishManager {int finishes;private void finishReturn(Battle b){finishes++;}}
 public static void main(String[] args){
  Plugin plugin=new Plugin();World w=()->"duel";Bukkit.worlds.put("duel",w);List<Location> locations=new ArrayList<>();for(int i=0;i<8;i++)locations.add(new Location(w,100*i,101,0));plugin.a.stored=locations;
  Battle[] battles=new Battle[5];for(int i=0;i<5;i++){battles[i]=new Battle();plugin.m.map.put("duel"+i,battles[i]);Bukkit.players.put(battles[i].a,new P(battles[i].a,w));Bukkit.players.put(battles[i].b,new P(battles[i].b,w));}
  for(int i=0;i<4;i++)check(SharedBattleWorld.create(plugin,new Arena(),"duel"+i).join()==w,"reservation "+i);
  check(SharedBattleWorld.full(plugin),"full");check(SharedBattleWorld.create(plugin,new Arena(),"duel4").join()==null,"fifth refused");check(((P)Bukkit.players.get(battles[4].a)).msg.contains("заняты"),"notify");
  P p=(P)Bukkit.players.get(battles[2].a);check(SharedBattleWorld.points(plugin.a,w,p).get(0).getX()==400,"per-battle boundary");
  PlayerTeleportEvent e=new PlayerTeleportEvent();e.player=p;e.to=new Location(w,0,101,0);e.cause=PlayerTeleportEvent.TeleportCause.PLUGIN;SharedBattleWorld.teleport(plugin,e);check(e.to.getX()==400,"redirect player1");
  e.player=(P)Bukkit.players.get(battles[2].b);e.to=new Location(w,100,101,0);SharedBattleWorld.teleport(plugin,e);check(e.to.getX()==500,"redirect player2");
  PlayerRespawnEvent re=new PlayerRespawnEvent();re.player=e.player;SharedBattleWorld.respawn(re,new Location(w,0,101,0));check(re.loc.getX()==500,"respawn player2");
  FinishManager fin=new FinishManager();check(SharedBattleWorld.checkedTeleport(p,new Location(w,400,101,0),fin,battles[2]),"checked teleport success");p.allow=false;
  try{SharedBattleWorld.checkedTeleport(p,new Location(w,400,101,0),fin,battles[2]);throw new AssertionError("cancelled teleport allowed");}catch(IllegalStateException expected){check(fin.finishes==1,"restore path started");}p.allow=true;
  EntityDamageByEntityEvent damage=new EntityDamageByEntityEvent();damage.damager=p;damage.entity=(P)Bukkit.players.get(battles[1].a);SharedBattleWorld.isolate(damage);check(damage.cancelled,"other fight damage");
  damage=new EntityDamageByEntityEvent();damage.damager=p;damage.entity=(P)Bukkit.players.get(battles[2].b);SharedBattleWorld.isolate(damage);check(!damage.cancelled,"same fight damage");
  ThrownPotion potion=new ThrownPotion(){public World getWorld(){return w;}public org.bukkit.projectiles.ProjectileSource getShooter(){return p;}};
  PotionSplashEvent splash=new PotionSplashEvent();splash.potion=potion;splash.affected.add((P)Bukkit.players.get(battles[1].a));splash.affected.add((P)Bukkit.players.get(battles[2].b));SharedBattleWorld.splash(splash);check(splash.intensity.containsKey(splash.affected.get(0))&&!splash.intensity.containsKey(splash.affected.get(1)),"potion isolation");
  LingeringPotionSplashEvent linger=new LingeringPotionSplashEvent();linger.potion=potion;SharedBattleWorld.lingering(linger);check(linger.cancelled,"lingering blocked in shared world");
  SharedBattleWorld.release("duel1");check(!SharedBattleWorld.full(plugin),"released slot");check(SharedBattleWorld.create(plugin,new Arena(),"duel4").join()==w,"reuse slot");
  check(SharedBattleWorld.points(plugin.a,w,(P)Bukkit.players.get(battles[4].a)).get(0).getX()==200,"correct reused slot");
  SharedBattleWorld.release("duel1");check(SharedBattleWorld.full(plugin),"stale release");
  for(int i=0;i<5;i++)SharedBattleWorld.release("duel"+i);
  TeamBattle team=new TeamBattle();plugin.t.map.put("team",team);check(SharedBattleWorld.create(plugin,new Arena(),"team").join()==w,"team reservation");
  PlayerRespawnEvent teamRespawn=new PlayerRespawnEvent();teamRespawn.player=new P(team.c,w);SharedBattleWorld.respawn(teamRespawn,new Location(w,0,101,0));check(teamRespawn.loc.getX()==100,"team respawn");SharedBattleWorld.release("team");
  plugin.a.stored=List.of(new Location(w,0,101,0),new Location(w,20,101,0),new Location(w,10,101,0),new Location(w,30,101,0));
  check(SharedBattleWorld.create(plugin,new Arena(),"duel0").join()==w,"overlap first");check(SharedBattleWorld.full(plugin),"overlapping bounds not simultaneously allocated");SharedBattleWorld.release("duel0");
  Bukkit.worlds.clear();check(SharedBattleWorld.create(plugin,new Arena(),"duel0").join()==null,"missing world fails closed");
  System.out.println("Shared battle world tests passed");
 }
}
