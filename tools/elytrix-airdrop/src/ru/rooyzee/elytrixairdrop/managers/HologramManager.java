package ru.rooyzee.elytrixairdrop.managers;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.types.ActiveFireAirdrop;
import ru.rooyzee.elytrixairdrop.patch.EventBar;
import ru.rooyzee.elytrixairdrop.patch.HologramText;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** Transparent title + subtitle display; all spawned entities belong to this plugin. */
public final class HologramManager implements Listener {
 private static final float TITLE_SCALE=1.45f, BODY_SCALE=1.15f;
 private final Main plugin;
 private final Map<String,Entry> entries=new HashMap<>();
 private static final class Entry {
  Location position; final boolean main;String title="",body="";int bodyLines;
  TextDisplay titleEntity,bodyEntity;boolean warned;long retryAfter;
  Entry(Location p,boolean main){position=p;this.main=main;}
 }
 public HologramManager(Main plugin){this.plugin=plugin;Bukkit.getPluginManager().registerEvents(this,plugin);}
 public String createHologram(Location location,List<String> lines){String name="airdrop_"+UUID.randomUUID();createNamed(name,location,lines,true);return name;}
 public static void createSky(String name,Location location,List<String> lines){Main.getInstance().getHologramManager().createNamed(name,location,lines,false);}
 private void createNamed(String name,Location location,List<String> lines,boolean main){
  if(name==null||location==null||location.getWorld()==null)throw new IllegalArgumentException("Invalid hologram location");
  if(entries.containsKey(name))throw new IllegalArgumentException("Duplicate hologram: "+name);
  Entry entry=new Entry(location.clone(),main);setLines(entry,lines);entries.put(name,entry);ensure(name,entry);
 }
 public void updateHologram(String name,List<String> lines){
  Entry entry=entries.get(name);if(entry==null)return;
  ActiveAirdrop active=plugin.getAirdropManager()==null?null:plugin.getAirdropManager().getActive();
  if(active!=null && name.equals(active.getHologramName()) && !plugin.getConfigManager().getConfig().getBoolean("holograms.use-custom-lines",false)){
   String phase=active instanceof ActiveFireAirdrop?((ActiveFireAirdrop)active).hologramPhase():"";
   List<String> modern=HologramText.main(active.getType().getId(),EventBar.stage(active),phase);
   if(!modern.isEmpty())lines=modern;
  }
  String oldTitle=entry.title,oldBody=entry.body;int oldCount=entry.bodyLines;setLines(entry,lines);
  boolean created=ensure(name,entry);
  if(!created && ready(entry))try{
   if(entry.main && !entry.title.equals(oldTitle))entry.titleEntity.setText(entry.title);
   if(!entry.body.equals(oldBody))entry.bodyEntity.setText(entry.body);
   if(entry.main && oldCount!=entry.bodyLines && !entry.titleEntity.teleport(titleLocation(entry)))throw new IllegalStateException("title teleport was cancelled");
  }catch(RuntimeException ex){fail(name,entry,ex);}
 }
 public void moveHologram(String name,Location location){
  Entry entry=entries.get(name);if(entry==null||location==null||location.getWorld()==null)return;
  entry.position=location.clone();
  if(ready(entry))try{
   if(!location.getWorld().isChunkLoaded(location.getBlockX()>>4,location.getBlockZ()>>4) || !entry.bodyEntity.teleport(location)
      || (entry.main && !entry.titleEntity.teleport(titleLocation(entry))))clear(entry);
  }catch(RuntimeException ex){fail(name,entry,ex);}
  ensure(name,entry);
 }
 public void removeHologram(String name){Entry entry=entries.remove(name);if(entry!=null)clear(entry);}
 @EventHandler public void disable(PluginDisableEvent event){if(event.getPlugin()==plugin){for(String name:new ArrayList<>(entries.keySet()))removeHologram(name);}}
 private static Location titleLocation(Entry entry){return entry.position.clone().add(0,0.25+entry.bodyLines*0.30*BODY_SCALE,0);}
 private static boolean ready(Entry e){return e.bodyEntity!=null && e.bodyEntity.isValid() && (!e.main || (e.titleEntity!=null && e.titleEntity.isValid()));}
 private static void clear(Entry e){if(e.titleEntity!=null)e.titleEntity.remove();if(e.bodyEntity!=null)e.bodyEntity.remove();e.titleEntity=null;e.bodyEntity=null;}
 /** Do not force-load chunks solely for holograms; retry cancelled spawns at a bounded rate. */
 private boolean ensure(String name,Entry entry){
  if(ready(entry))return false;
  clear(entry);
  if(System.currentTimeMillis()<entry.retryAfter)return false;
  World world=entry.position.getWorld();if(world==null || !world.isChunkLoaded(entry.position.getBlockX()>>4,entry.position.getBlockZ()>>4))return false;
  try{
   entry.bodyEntity=spawn(world,entry.position,entry.body,BODY_SCALE);
   if(entry.main)entry.titleEntity=spawn(world,titleLocation(entry),entry.title,TITLE_SCALE);
   entry.warned=false;entry.retryAfter=0;return true;
  }catch(RuntimeException ex){fail(name,entry,ex);return false;}
 }
 private TextDisplay spawn(World world,Location position,String text,float scale){
  TextDisplay display=world.spawn(position,TextDisplay.class);
  try{
   display.setPersistent(false);display.setInvulnerable(true);display.setBillboard(Display.Billboard.CENTER);
   display.setDefaultBackground(false);display.setBackgroundColor(Color.fromARGB(0,0,0,0));
   display.setShadowed(true);display.setSeeThrough(false);display.setTextOpacity((byte)255);display.setLineWidth(400);
   display.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale,scale,scale),new Quaternionf()));
   display.setText(text);if(!display.isValid())throw new IllegalStateException("display spawn was cancelled");return display;
  }catch(RuntimeException ex){display.remove();throw ex;}
 }
 private void fail(String name,Entry entry,RuntimeException ex){
  clear(entry);entry.retryAfter=System.currentTimeMillis()+5000L;
  if(!entry.warned){plugin.getLogger().warning("Cannot display airdrop hologram "+name+": "+ex.getMessage());entry.warned=true;}
 }
 private static void setLines(Entry entry,List<String> lines){
  List<String> colored=new ArrayList<>();if(lines!=null)for(String s:lines)colored.add(ColorUtil.colorize(s==null?"":s));
  if(entry.main && !colored.isEmpty())entry.title=colored.remove(0);else entry.title="";
  entry.bodyLines=colored.size();entry.body=String.join("\n",colored);
 }
}
