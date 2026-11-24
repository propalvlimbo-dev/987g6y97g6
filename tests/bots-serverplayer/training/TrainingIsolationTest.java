import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import ru.elytrix.bots.TrainingArena;
import ru.elytrix.bots.QuietSounds;

public final class TrainingIsolationTest {
 static void check(boolean condition,String text) { if(!condition) throw new AssertionError(text); }
 static Object allocate(Class<?> type) throws Exception { Class<?> u=Class.forName("sun.misc.Unsafe"); Field f=u.getDeclaredField("theUnsafe"); f.setAccessible(true); return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type); }
 static void put(Class<?> type,Object o,String name,Object value) throws Exception { Field f=type.getDeclaredField(name); f.setAccessible(true); f.set(o,value); }
 static Player player(UUID id,List<String> calls) {
  return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(o,m,a)-> {
   switch(m.getName()) {
    case "getUniqueId": return id;
    case "isOnline": return true;
    case "hidePlayer": case "showPlayer": calls.add(id+":"+m.getName()+":"+((Player)a[1]).getUniqueId()); return null;
    case "hashCode": return System.identityHashCode(o);
    case "equals": return o==a[0];
   }
   if(m.getReturnType()==boolean.class) return false; if(m.getReturnType()==int.class) return 0; return null;
  });
 }
 public static void main(String[] args) throws Exception {
  TrainingArena arena=(TrainingArena)allocate(TrainingArena.class);
  UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(); List<String> calls=new ArrayList<>();
  Player pa=player(a,calls),pb=player(b,calls),pc=player(c,calls); Map<UUID,Player> players=Map.of(a,pa,b,pb,c,pc);
  Server server=(Server)Proxy.newProxyInstance(Server.class.getClassLoader(),new Class<?>[]{Server.class},(o,m,params)->m.getName().equals("getPlayer")?players.get(params[0]):null);
  put(Bukkit.class,null,"server",server); Map<UUID,Object> active=new HashMap<>(); active.put(a,null); active.put(b,null);
  put(TrainingArena.class,arena,"active",active); put(TrainingArena.class,arena,"directory",new File("/tmp/not-exists-"+a));
  Method hide=TrainingArena.class.getDeclaredMethod("hideFromOtherTrainees",Player.class); hide.setAccessible(true); hide.invoke(arena,pb);
  check(calls.size()==2 && calls.contains(a+":hidePlayer:"+b) && calls.contains(b+":hidePlayer:"+a),"mutual hiding, outsider unchanged");
  calls.clear(); Method unhide=TrainingArena.class.getDeclaredMethod("unhide",UUID.class); unhide.setAccessible(true); unhide.invoke(arena,b);
  check(calls.size()==2 && calls.contains(a+":showPlayer:"+b) && calls.contains(b+":showPlayer:"+a),"mutual restore");
  PacketAdapter listener=(PacketAdapter)allocate(Class.forName("ru.elytrix.bots.QuietSounds$1"));
  PacketEvent ea=new PacketEvent(new Object()); put(PacketEvent.class,ea,"playerReference",new WeakReference<>(pa));
  PacketEvent ec=new PacketEvent(new Object()); put(PacketEvent.class,ec,"playerReference",new WeakReference<>(pc));
  QuietSounds.add(a); listener.onPacketSending(ea); listener.onPacketSending(ec);
  check(ea.isCancelled() && !ec.isCancelled(),"only training listener sound muted");
  QuietSounds.remove(a); PacketEvent end=new PacketEvent(new Object()); put(PacketEvent.class,end,"playerReference",new WeakReference<>(pa)); listener.onPacketSending(end);
  check(!end.isCancelled(),"sound restored on leave");
  System.out.println("Mutual visibility and per-recipient sound filter passed");
 }
}
