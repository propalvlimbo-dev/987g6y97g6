package ru.rooyzee.elytrixairdrop.patch;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.scheduler.BukkitTask;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** Global event bar; actionbar is restricted to participants within 20 blocks. */
public final class EventBar implements Listener {
 private final Main plugin; private BossBar bar; private ActiveAirdrop shown; private BukkitTask task;
 private EventBar(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){EventBar tracker=new EventBar(plugin);Bukkit.getPluginManager().registerEvents(tracker,plugin);tracker.task=Bukkit.getScheduler().runTaskTimer(plugin,tracker::update,1L,20L);}
 private void update(){
  ActiveAirdrop current=plugin.getAirdropManager().getActive();
  if(current!=shown){clear();shown=current;if(current!=null){bar=Bukkit.createBossBar("",BarColor.PURPLE,BarStyle.SOLID);Bukkit.getOnlinePlayers().forEach(bar::addPlayer);}}
  if(current==null || bar==null)return;
  Location loc=current.getCenter(); String id=current.getType().getId();
  String name=plugin.getConfigManager().getConfig().getString("airdrops."+id+".display-name",id);
  bar.setTitle(ColorUtil.colorize("&#F8BEFBАирдроп &8» &f"+name+" &8│ &f"+loc.getBlockX()+" "+loc.getBlockY()+" "+loc.getBlockZ()+" &8│ "+stage(current)));
  int nearby=NearbyRewards.players(loc);
  String grade=nearby<=3?"Обычный":nearby<=6?"Средний":"Высокий";
  String action=ColorUtil.colorize("&#F8BEFBАирдроп &8» &fИгроки: &#F8BEFB"+nearby+" &8│ &fПотенциал лута: &#F8BEFB"+grade);
  if(loc.getWorld()!=null)for(Player p:loc.getWorld().getPlayers())if(p.getGameMode()!=org.bukkit.GameMode.SPECTATOR && p.getLocation().distanceSquared(loc)<=400.0)p.spigot().sendMessage(ChatMessageType.ACTION_BAR,TextComponent.fromLegacyText(action));
 }
 public static String stage(ActiveAirdrop a){
  try{
   String type=a.getType().getId();
   if(type.equals("peaceful")){
    if((boolean)field(a,"looted"))return "&7Лут забран";
    if((boolean)field(a,"unlocked"))return "&aОткрыт";
    if(!(boolean)field(a,"activated"))return "&7Ожидает активации";
    long ms=(long)field(a,"unlockTime")-System.currentTimeMillis();long s=Math.max(0,(ms+999)/1000);
    return "&fОткрытие через &#F8BEFB"+(s/60)+"м "+(s%60)+"с";
   }
   if(type.equals("fire")){Method m=a.getClass().getDeclaredMethod("statusLine");m.setAccessible(true);return ((String)m.invoke(a)).replace("●", "").trim();}
   if(type.equals("sky")){
    Map<?,?> boxes=(Map<?,?>)field(a,"shulkers");boolean active=false,open=false;
    for(Object b:boxes.values()){if((boolean)field(b,"activated"))active=true;if((boolean)field(b,"unlocked") && !(boolean)field(b,"looted"))open=true;}
    if(open)return "&aШалкеры открыты";
    return active?"&fШалкеры активируются":"&7Ожидает активации";
   }
  }catch(ReflectiveOperationException | RuntimeException e){return "&fИдёт событие";}
  return "&fИдёт событие";
 }
 private static Object field(Object object,String name)throws ReflectiveOperationException{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
 @EventHandler public void onJoin(PlayerJoinEvent event){if(bar!=null)bar.addPlayer(event.getPlayer());}
 @EventHandler public void onDisable(PluginDisableEvent event){if(event.getPlugin()==plugin){if(task!=null)task.cancel();clear();shown=null;}}
 private void clear(){if(bar!=null){bar.removeAll();bar=null;}}
}
