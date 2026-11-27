package ru.rooyzee.elytrixairdrop.placeholders;

import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.managers.AirdropManager;
import ru.rooyzee.elytrixairdrop.patch.EventBar;
import ru.rooyzee.elytrixairdrop.patch.NearbyRewards;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** Stable PlaceholderAPI values for the BMenu event screen. No loot contents are exposed. */
public class AirdropPlaceholder extends PlaceholderExpansion {
 private static final int MIN_ONLINE=5;
 private final Main plugin;
 public AirdropPlaceholder(Main plugin){this.plugin=plugin;}
 @Override public String getIdentifier(){return "elytrixairdrop";}
 @Override public String getAuthor(){return "rooyzee";}
 @Override public String getVersion(){return "1.0.1";}
 @Override public boolean persist(){return true;}
 @Override public String onPlaceholderRequest(Player ignored,String params){
  if(params==null)return null;
  AirdropManager manager=plugin.getAirdropManager();if(manager==null)return "—";
  ActiveAirdrop active=manager.getActive();int online=Bukkit.getOnlinePlayers().size();
  switch(params.toLowerCase(Locale.ROOT)){
   case "status": return active!=null?"Активен":manager.getNextSpawnSeconds()<=0 && online<MIN_ONLINE?"Ожидание игроков":"Ожидание";
   case "active": return active!=null?"true":"false";
   case "time":
    if(active!=null)return "Идёт событие";
    long seconds=manager.getNextSpawnSeconds();
    if(seconds>0)return manager.formatTime(seconds);
    return online<MIN_ONLINE?"Ожидание игроков":"Ожидается запуск";
   case "online":return online+"/"+MIN_ONLINE;
   case "type":
    if(active==null)return "—";
    String id=active.getType().getId();
    return ColorUtil.colorize(plugin.getConfigManager().getConfig().getString("airdrops."+id+".display-name",id));
   case "stage":return active==null?"—":ColorUtil.colorize(EventBar.stage(active));
   case "nearby":return active==null?"0":String.valueOf(NearbyRewards.players(active.getCenter()));
   case "location":
    if(active==null || active.getCenter()==null)return "Появится после старта";
    Location loc=active.getCenter();return "X: "+loc.getBlockX()+"  Y: "+loc.getBlockY()+"  Z: "+loc.getBlockZ();
   case "world":return active==null||active.getCenter()==null||active.getCenter().getWorld()==null?"—":active.getCenter().getWorld().getName();
   case "x":return coordinate(active,'x');
   case "y":return coordinate(active,'y');
   case "z":return coordinate(active,'z');
   default:return null;
  }
 }
 private static String coordinate(ActiveAirdrop active,char axis){
  if(active==null || active.getCenter()==null)return "—";
  Location loc=active.getCenter();return String.valueOf(axis=='x'?loc.getBlockX():axis=='y'?loc.getBlockY():loc.getBlockZ());
 }
}
