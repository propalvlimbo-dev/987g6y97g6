package test;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
public final class Probe extends JavaPlugin implements Listener {
 public void onEnable(){getServer().getPluginManager().registerEvents(this,this);}
 @EventHandler public void join(PlayerJoinEvent e) {Player p=e.getPlayer(); if(!p.getName().equals("TestOwner"))return;
  getServer().getScheduler().runTaskLater(this,()->{try{
   World w=p.getWorld();for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){w.getBlockAt(x,99,z).setType(Material.STONE);for(int y=100;y<104;y++)w.getBlockAt(x,y,z).setType(Material.AIR);}
   p.teleport(new Location(w,0.5,100,0.5,0,0));
   JavaPlugin plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("ElytrixBotsTrain");String path="players."+p.getUniqueId()+".";
   plugin.getConfig().set(path+"follow",true);plugin.getConfig().set(path+"look",true);plugin.getConfig().set(path+"attack",false);
   Method spawn=plugin.getClass().getDeclaredMethod("spawn",Player.class);spawn.setAccessible(true);spawn.invoke(plugin,p);
   Field field=plugin.getClass().getDeclaredField("bots");field.setAccessible(true);ServerPlayer b=(ServerPlayer)((Map<?,?>)field.get(plugin)).get(p.getUniqueId());
   if(b==null){getLogger().severe("PROBE BOT NULL");return;}
   p.teleport(new Location(w,0.5,100,8.5,0,0));
   getLogger().info("PROBE START "+b.position()+" yaw="+b.getYRot()+" name="+b.getScoreboardName());
   getServer().getScheduler().runTaskLater(this,()->getLogger().info("PROBE FOLLOW "+b.position()+" yaw="+b.getYRot()),60L);
   getServer().getScheduler().runTaskLater(this,()->{plugin.getConfig().set(path+"attack",true);getLogger().info("PROBE ATTACK ON bot="+b.position()+" target="+p.getLocation());},70L);
   getServer().getScheduler().runTaskLater(this,()->getLogger().info("PROBE END bot="+b.position()+" targetHP="+p.getHealth()),150L);
  }catch(Throwable ex){getLogger().log(java.util.logging.Level.SEVERE,"PROBE FAIL",ex);}},30L); }
 @EventHandler(priority=EventPriority.MONITOR) public void damage(EntityDamageByEntityEvent e){if(e.getDamager().getName().startsWith("ElytrixBots_"))getLogger().info("PROBE BOT HIT damage="+e.getFinalDamage()+" cancelled="+e.isCancelled());}
}
