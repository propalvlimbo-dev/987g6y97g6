package ru.rooyzee.elytrixtalisman.service;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.rooyzee.elytrixtalisman.util.ColorUtil;
import ru.rooyzee.elytrixtalisman.util.PlaceholderUtil;
/** Transparent Airdrop-style title/body displays with bounded recovery. */
public class HologramService {
 private static final float TITLE=1.45f,BODY=1.15f;
 private final Plugin plugin;private final NamespacedKey markerKey;
 private TextDisplay title,body;private Location base;private List<String> template;
 private Map<String,String> placeholders=Collections.emptyMap();private String lastTitle="",lastBody="";private int bodyLines;private long retry;private boolean warned;
 public HologramService(Plugin plugin){this.plugin=plugin;markerKey=new NamespacedKey(plugin,"talisman_hologram");}
 public void create(Location location,List<String> lines){
  destroy();if(location==null||location.getWorld()==null||lines==null||lines.isEmpty())return;
  base=location.clone().add(.5,0,.5);template=new ArrayList<>(lines);placeholders=Collections.emptyMap();retry=0;Map<String,String> initial=new HashMap<>();initial.put("%top1%","&8· · ·");initial.put("%top2%","&8· · ·");initial.put("%top3%","&8· · ·");initial.put("%points%","—");update(initial);
 }
 public void update(Map<String,String> values){
  if(base==null||template==null)return;
  placeholders=values==null?Collections.emptyMap():new HashMap<>(values);
  List<String> lines=new ArrayList<>();for(String s:template)lines.add(ColorUtil.colorize(PlaceholderUtil.replace(s==null?"":s,placeholders)));
  String newTitle=ColorUtil.colorize("&#F8BEFB&l"+ChatColor.stripColor(lines.remove(0)));
  String newBody=String.join("\n",lines);int count=lines.size();
  if(!isAlive()){
   clear();World w=base.getWorld();if(System.currentTimeMillis()<retry||w==null||!w.isChunkLoaded(base.getBlockX()>>4,base.getBlockZ()>>4))return;
   try{body=spawn(w,base,newBody,BODY);title=spawn(w,titleLocation(count),newTitle,TITLE);lastTitle=newTitle;lastBody=newBody;bodyLines=count;warned=false;retry=0;}
   catch(RuntimeException ex){fail(ex);}return;
  }
  try{if(!newTitle.equals(lastTitle)){title.setText(newTitle);lastTitle=newTitle;}if(!newBody.equals(lastBody)){body.setText(newBody);lastBody=newBody;}
   if(count!=bodyLines){if(!title.teleport(titleLocation(count)))throw new IllegalStateException("title teleport cancelled");bodyLines=count;}}
  catch(RuntimeException ex){fail(ex);}
 }
 private Location titleLocation(int count){return base.clone().add(0,.25+count*.30*BODY,0);}
 private TextDisplay spawn(World w,Location l,String text,float scale){
  TextDisplay d=w.spawn(l,TextDisplay.class);try{d.setPersistent(false);d.setInvulnerable(true);d.setBillboard(Display.Billboard.CENTER);d.setDefaultBackground(false);d.setBackgroundColor(Color.fromARGB(0,0,0,0));d.setShadowed(true);d.setSeeThrough(false);d.setTextOpacity((byte)255);d.setLineWidth(400);d.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale,scale,scale),new Quaternionf()));d.setText(text);if(!d.isValid())throw new IllegalStateException("display spawn cancelled");return d;}catch(RuntimeException ex){d.remove();throw ex;}
 }
 private void fail(RuntimeException ex){clear();retry=System.currentTimeMillis()+5000L;if(!warned){plugin.getLogger().warning("Не удалось показать голограмму талисмана: "+ex.getMessage());warned=true;}}
 private void clear(){if(title!=null)title.remove();if(body!=null)body.remove();title=null;body=null;}
 public void destroy(){clear();if(base!=null)sweep(base,8);template=null;base=null;placeholders=Collections.emptyMap();}
 /** Only remove legacy marked armor stands in already loaded chunks near this event. */
 public void sweep(Location center,int radius){
  if(center==null||center.getWorld()==null||radius<0)return;World w=center.getWorld();int cx=center.getBlockX()>>4,cz=center.getBlockZ()>>4,r=(radius+15)/16+1;
  for(int x=cx-r;x<=cx+r;x++)for(int z=cz-r;z<=cz+r;z++)if(w.isChunkLoaded(x,z))try{
   for(Entity e:w.getChunkAt(x,z).getEntities())if(e instanceof ArmorStand && e.getPersistentDataContainer().has(markerKey,PersistentDataType.BYTE) && e.getLocation().distanceSquared(center)<=(double)radius*radius)e.remove();
  }catch(RuntimeException ex){plugin.getLogger().warning("Не удалось убрать старую голограмму: "+ex.getMessage());}
 }
 public boolean isAlive(){return title!=null&&title.isValid()&&body!=null&&body.isValid();}
 public Location getBase(){return base==null?null:base.clone().add(-.5,0,-.5);}
}
