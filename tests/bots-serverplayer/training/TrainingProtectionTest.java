import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.*;
import ru.elytrix.bots.TrainingArena;

public final class TrainingProtectionTest {
 static void check(boolean b,String msg) { if(!b) throw new AssertionError(msg); }
 static Object fake(Class<?> cls, UUID id) {
  return Proxy.newProxyInstance(cls.getClassLoader(),new Class<?>[]{cls},(proxy,m,a)->{
   if(m.getName().equals("getUniqueId")) return id;
   if(m.getName().equals("getServer")) return fake(Server.class,id);
   if(m.getName().equals("getPluginManager")) return fake(org.bukkit.plugin.PluginManager.class,id);
   if(m.getName().equals("isPluginEnabled")) return true;
   if(m.getName().equals("getOnlinePlayers")) return List.of();
   if(m.getName().equals("hashCode")) return System.identityHashCode(proxy);
   if(m.getName().equals("equals")) return proxy==a[0];
   if(m.getReturnType()==boolean.class) return false;
   if(m.getReturnType()==int.class) return 0;
   if(m.getReturnType()==long.class) return 0L;
   if(m.getReturnType()==float.class) return 0f;
   if(m.getReturnType()==double.class) return 0d;
   return null;
  });
 }
 static void field(Object obj,String name,Object value) throws Exception { Field f=TrainingArena.class.getDeclaredField(name); f.setAccessible(true); f.set(obj,value); }
 public static void main(String[] args) throws Exception {
  Class<?> u=Class.forName("sun.misc.Unsafe"); Field uf=u.getDeclaredField("theUnsafe"); uf.setAccessible(true); Object unsafe=uf.get(null);
  TrainingArena arena=(TrainingArena)u.getMethod("allocateInstance",Class.class).invoke(unsafe,TrainingArena.class);
  UUID id=UUID.randomUUID(); Player p=(Player)fake(Player.class,id); Player outsider=(Player)fake(Player.class,UUID.randomUUID());
  Map<UUID,Object> active=new HashMap<>(); active.put(id,null); field(arena,"active",active); field(arena,"internal",new HashSet<UUID>()); field(arena,"directory",new File("/tmp/test-no-such-backups-"+id));
  for(String cmd:List.of("/bots","/bot","/bots leave","/bot leave","/BOTS LEAVE")) { PlayerCommandPreprocessEvent e=new PlayerCommandPreprocessEvent(p,cmd); arena.commands(e); check(!e.isCancelled(),"allowed "+cmd); }
  for(String cmd:List.of("/home","/spawn","/bots dataset","/bots:bot leave","/bot leave now","/minecraft:tp a b")) { PlayerCommandPreprocessEvent e=new PlayerCommandPreprocessEvent(p,cmd); arena.commands(e); check(e.isCancelled(),"blocked "+cmd); }
  PlayerCommandPreprocessEvent other=new PlayerCommandPreprocessEvent(outsider,"/home"); arena.commands(other); check(!other.isCancelled(),"other player not restricted");
  Location l=new Location(null,0,0,0); PlayerTeleportEvent tp=new PlayerTeleportEvent(p,l,l,PlayerTeleportEvent.TeleportCause.PLUGIN); arena.teleport(tp); check(tp.isCancelled(),"external teleport blocked");
  ((Set<UUID>)get(arena,"internal")).add(id); PlayerTeleportEvent own=new PlayerTeleportEvent(p,l,l,PlayerTeleportEvent.TeleportCause.PLUGIN); arena.teleport(own); check(!own.isCancelled(),"internal teleport allowed");
  Block block=(Block)fake(Block.class,id); BlockBreakEvent be=new BlockBreakEvent(block,p); arena.blockBreak(be); check(be.isCancelled(),"break blocked");
  BlockBreakEvent normal=new BlockBreakEvent(block,outsider); arena.blockBreak(normal); check(!normal.isCancelled(),"other player break unchanged");
  Field serverField=Bukkit.class.getDeclaredField("server"); serverField.setAccessible(true); serverField.set(null,fake(Server.class,id));
  List<String> messages=new ArrayList<>(); UUID entrantId=UUID.randomUUID();
  Player entrant=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(proxy,m,a)-> {
   if(m.getName().equals("getUniqueId")) return entrantId;
   if(m.getName().equals("sendMessage") && a!=null && a.length==1 && a[0] instanceof String s) messages.add(s);
   if(m.getReturnType()==boolean.class) return false; if(m.getReturnType()==int.class) return 0; return null;
  });
  field(arena,"quietReady",true);
  Method enter=TrainingArena.class.getDeclaredMethod("enter",Player.class); enter.setAccessible(true); enter.invoke(arena,entrant);
  check(messages.size()==1 && messages.get(0).contains("мир duel"),"another active session must not trigger a capacity rejection");
  messages.clear(); field(arena,"quietReady",false); enter.invoke(arena,entrant);
  check(messages.size()==1 && messages.get(0).contains("ProtocolLib"),"missing sound filter must refuse entry");
  System.out.println("Training command, teleport, block protection, capacity and quiet-mode gate passed");
 }
 static Object get(Object o,String name) throws Exception { Field f=TrainingArena.class.getDeclaredField(name); f.setAccessible(true); return f.get(o); }
}
