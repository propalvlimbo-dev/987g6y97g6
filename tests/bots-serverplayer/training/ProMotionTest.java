import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import ru.elytrix.bots.ProMotion;
public final class ProMotionTest {
 static void ck(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  ck(ProMotion.forward(1f,"pro",9,100,110)==0,"stop first tick after hit");
  ck(ProMotion.forward(1f,"pro",9,101,110)==0,"stop second tick after hit");
  ck(ProMotion.forward(1f,"pro",9,102,110)==1,"resume chase");
  ck(!ProMotion.sprint(true,"pro",9,100,110) && ProMotion.sprint(true,"pro",9,102,110),"stop and restart sprint");
  ck(ProMotion.sprint(true,"noob",9,100,110) && !ProMotion.sprint(false,"pro",9,102,110),"other modes and original sprint gate retained");
  ck(ProMotion.forward(1f,"pro",13,100,110)==1,"no pause when far");
  for(String mode:List.of("noob","combo","crit","custom")) ck(ProMotion.forward(.65f,mode,9,100,110)==.65f,"other modes unchanged");
  World w=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(o,m,a)-> { if(m.getName().equals("equals"))return o==a[0];if(m.getName().equals("hashCode"))return System.identityHashCode(o);return null; });
  UUID id=UUID.randomUUID();List<Float> rotations=new ArrayList<>();
  Player owner=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(o,m,a)->switch(m.getName()) {case "getUniqueId"->id;case "isDead"->false;case "getWorld"->w;case "getLocation"->new Location(w,0,101,0);default->null;});
  Player bot=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(o,m,a)->switch(m.getName()) {case "isDead"->false;case "getWorld"->w;case "getLocation"->new Location(w,1,101,0);case "setRotation"->{rotations.add((Float)a[0]);yield null;}default->null;});
  ProMotion.spin(bot,owner,"noob",0,0,false);ck(rotations.isEmpty(),"noob never spins");
  ProMotion.spin(bot,owner,"pro",0,0,false);
  Field states=ProMotion.class.getDeclaredField("states");states.setAccessible(true);Map<?,?> map=(Map<?,?>)states.get(null);Object state=map.get(id);
  Field next=state.getClass().getDeclaredField("nextSpin");next.setAccessible(true);next.setLong(state,100);
  for(long tick=100;tick<108;tick++)ProMotion.spin(bot,owner,"pro",tick,110,false);
  ck(rotations.size()==8 && rotations.get(0)==45f && rotations.get(7)==360f,"complete smooth turn over eight ticks");
  rotations.clear();ProMotion.spin(bot,owner,"pro",108,110,false);ProMotion.spin(bot,owner,"pro",115,121,false);ck(rotations.isEmpty(),"spin stops and respects cooldown");
  ProMotion.clear(id);ck(map.isEmpty(),"state cleaned on removal");
  System.out.println("Pro pause, resume, rare 360 and mode isolation passed");
 }
}
