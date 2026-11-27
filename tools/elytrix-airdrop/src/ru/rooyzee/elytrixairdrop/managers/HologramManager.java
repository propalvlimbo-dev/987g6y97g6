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
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.types.ActiveFireAirdrop;
import ru.rooyzee.elytrixairdrop.patch.EventBar;
import ru.rooyzee.elytrixairdrop.patch.HologramText;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** Plugin-owned non-persistent transparent text displays, including sky shulkers. */
public final class HologramManager implements Listener {
 private final Main plugin;
 private final Map<String,Entry> entries=new HashMap<>();
 private static final class Entry { Location position;String text;TextDisplay entity;boolean warned;long retryAfter;Entry(Location p,String t){position=p;text=t;} }
 public HologramManager(Main plugin){this.plugin=plugin;Bukkit.getPluginManager().registerEvents(this,plugin);}
 public String createHologram(Location location,List<String> lines){String name="airdrop_"+UUID.randomUUID();createNamed(name,location,lines);return name;}
 public static void createSky(String name,Location location,List<String> lines){Main.getInstance().getHologramManager().createNamed(name,location,lines);}
 private void createNamed(String name,Location location,List<String> lines){
  if(name==null||location==null||location.getWorld()==null)throw new IllegalArgumentException("Invalid hologram location");
  if(entries.containsKey(name))throw new IllegalArgumentException("Duplicate hologram: "+name);
  Entry entry=new Entry(location.clone(),format(lines));entries.put(name,entry);ensure(name,entry);
 }
 public void updateHologram(String name,List<String> lines){
  Entry entry=entries.get(name);if(entry==null)return;
  ActiveAirdrop active=plugin.getAirdropManager()==null?null:plugin.getAirdropManager().getActive();
  if(active!=null && name.equals(active.getHologramName()) && !plugin.getConfigManager().getConfig().getBoolean("holograms.use-custom-lines",false)){
   String phase=active instanceof ActiveFireAirdrop?((ActiveFireAirdrop)active).hologramPhase():"";
   List<String> modern=HologramText.main(active.getType().getId(),EventBar.stage(active),phase);
   if(!modern.isEmpty())lines=modern;
  }
  String formatted=format(lines);boolean changed=!formatted.equals(entry.text);entry.text=formatted;
  boolean created=ensure(name,entry);
  if(!created && changed && entry.entity!=null && entry.entity.isValid())try{entry.entity.setText(entry.text);}catch(RuntimeException ex){
   plugin.getLogger().warning("Cannot update airdrop hologram "+name+": "+ex.getMessage());
   entry.entity.remove();entry.entity=null;entry.retryAfter=System.currentTimeMillis()+5000L;
  }
 }
 public void moveHologram(String name,Location location){
  Entry entry=entries.get(name);if(entry==null||location==null||location.getWorld()==null)return;
  entry.position=location.clone();
  if(entry.entity!=null && entry.entity.isValid()){
   boolean moved=false;try{if(location.getWorld().isChunkLoaded(location.getBlockX()>>4,location.getBlockZ()>>4))moved=entry.entity.teleport(location);}catch(RuntimeException ex){plugin.getLogger().warning("Cannot move airdrop hologram "+name+": "+ex.getMessage());}
   if(!moved){entry.entity.remove();entry.entity=null;}
  }
  ensure(name,entry);
 }
 public void removeHologram(String name){Entry entry=entries.remove(name);if(entry!=null && entry.entity!=null)entry.entity.remove();}
 @EventHandler public void disable(PluginDisableEvent event){if(event.getPlugin()==plugin){for(String name:new ArrayList<>(entries.keySet()))removeHologram(name);}}
 /** Return true if newly created; do not force-load chunks solely for a hologram. */
 private boolean ensure(String name,Entry entry){
  if(entry.entity!=null && entry.entity.isValid())return false;
  if(System.currentTimeMillis()<entry.retryAfter)return false;
  World world=entry.position.getWorld();if(world==null || !world.isChunkLoaded(entry.position.getBlockX()>>4,entry.position.getBlockZ()>>4))return false;
  TextDisplay display=null;
  try{
   display=world.spawn(entry.position,TextDisplay.class);display.setPersistent(false);display.setInvulnerable(true);
   display.setBillboard(Display.Billboard.CENTER);display.setDefaultBackground(false);
   display.setBackgroundColor(Color.fromARGB(0,0,0,0));display.setShadowed(false);display.setSeeThrough(false);
   display.setTextOpacity((byte)255);display.setLineWidth(400);display.setText(entry.text);if(!display.isValid())throw new IllegalStateException("display spawn was cancelled");entry.entity=display;entry.warned=false;entry.retryAfter=0;return true;
  }catch(RuntimeException ex){if(display!=null)display.remove();entry.retryAfter=System.currentTimeMillis()+5000L;if(!entry.warned){plugin.getLogger().warning("Cannot create airdrop hologram "+name+": "+ex.getMessage());entry.warned=true;}return false;}
 }
 private static String format(List<String> lines){List<String> colored=new ArrayList<>();if(lines!=null)for(String s:lines)colored.add(ColorUtil.colorize(s==null?"":s));return String.join("\n",colored);}
}
