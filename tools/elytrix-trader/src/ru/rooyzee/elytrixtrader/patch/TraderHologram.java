package ru.rooyzee.elytrixtrader.patch;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import ru.rooyzee.elytrixtrader.Main;
import ru.rooyzee.elytrixtrader.model.TraderInstance;
import ru.rooyzee.elytrixtrader.util.ColorUtil;
import ru.rooyzee.elytrixtrader.util.Text;
/** Two nonpersistent, transparent text displays matching the airdrop style. */
public final class TraderHologram implements Listener {
 private static final Map<TraderInstance,Entry> entries=new IdentityHashMap<>();
 private static final float TITLE=1.45f,BODY=1.15f;
 private final Main plugin;
 private static final class Entry {final Main plugin;TextDisplay title,body;long retry;boolean warned;String last="",lastTitle="";int count;Entry(Main p){plugin=p;}}
 private TraderHologram(Main plugin){this.plugin=plugin;}
 public static void install(Main p){Bukkit.getPluginManager().registerEvents(new TraderHologram(p),p);}
 public static void start(Main p,TraderInstance trader){
  if(trader==null || !p.config().hologramEnabled() || !trader.config().hologramEnabled())return;
  remove(trader);entries.put(trader,new Entry(p));update(trader);
 }
 public static void update(TraderInstance trader){
  Entry e=entries.get(trader);if(e==null || trader.location()==null || trader.location().getWorld()==null)return;
  Location base=trader.location().clone().add(0,Math.max(2.4,trader.config().hologramHeight()-0.3),0);
  World world=base.getWorld();
  List<String> configured=trader.config().hologramLines();
  List<String> lines=new ArrayList<>();
  for(String line:configured)lines.add(Text.apply(line==null?"":line,null,trader.placeholders()));
  if(lines.isEmpty())return;
  String title=ColorUtil.translate("&#F8BEFB&l"+ColorUtil.strip(lines.remove(0)));
  List<String> styled=new ArrayList<>();
  for(String line:lines)styled.add(line.replaceAll("(?i)§x§8§c§e§9§9§a", "§x§f§8§b§e§f§b"));
  String body=String.join("\n",styled);int count=styled.size();
  if(!ready(e)){
   clear(e);if(System.currentTimeMillis()<e.retry || !world.isChunkLoaded(base.getBlockX()>>4,base.getBlockZ()>>4))return;
   try{
    e.body=spawn(world,base,body,BODY);
    e.title=spawn(world,titleLocation(base,count),title,TITLE);
    e.last=body;e.lastTitle=title;e.count=count;e.warned=false;e.retry=0;
   }catch(RuntimeException ex){fail(e,ex);}
   return;
  }
  try{if(!body.equals(e.last)){e.body.setText(body);e.last=body;} if(!title.equals(e.lastTitle)){e.title.setText(title);e.lastTitle=title;} if(count!=e.count){if(!e.title.teleport(titleLocation(base,count)))throw new IllegalStateException("title teleport cancelled");e.count=count;}}
  catch(RuntimeException ex){fail(e,ex);}
 }
 private static Location titleLocation(Location base,int count){return base.clone().add(0,.25+count*.30*BODY,0);}
 private static boolean ready(Entry e){return e.title!=null && e.title.isValid() && e.body!=null && e.body.isValid();}
 private static TextDisplay spawn(World world,Location loc,String text,float scale){
  TextDisplay d=world.spawn(loc,TextDisplay.class);
  try{d.setPersistent(false);d.setInvulnerable(true);d.setBillboard(Display.Billboard.CENTER);d.setDefaultBackground(false);d.setBackgroundColor(Color.fromARGB(0,0,0,0));d.setShadowed(true);d.setSeeThrough(false);d.setTextOpacity((byte)255);d.setLineWidth(400);d.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale,scale,scale),new Quaternionf()));d.setText(text);if(!d.isValid())throw new IllegalStateException("display spawn cancelled");return d;}
  catch(RuntimeException ex){d.remove();throw ex;}
 }
 private static void clear(Entry e){if(e.title!=null)e.title.remove();if(e.body!=null)e.body.remove();e.title=null;e.body=null;}
 private static void fail(Entry e,RuntimeException ex){clear(e);e.retry=System.currentTimeMillis()+5000L;if(!e.warned){e.plugin.getLogger().warning("Cannot display trader hologram: "+ex.getMessage());e.warned=true;}}
 public static void remove(TraderInstance trader){Entry e=entries.remove(trader);if(e!=null)clear(e);}
 @EventHandler public void disable(PluginDisableEvent event){if(event.getPlugin()==plugin)for(TraderInstance t:new ArrayList<>(entries.keySet())){Entry e=entries.get(t);if(e!=null && e.plugin==plugin)remove(t);}}
}
