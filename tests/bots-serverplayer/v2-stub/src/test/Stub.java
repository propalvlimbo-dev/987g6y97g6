package test;
import org.bukkit.plugin.java.JavaPlugin;import org.bukkit.event.*;import org.bukkit.event.entity.EntityDamageByEntityEvent;import org.bukkit.entity.Player;import dev.enco.greatcombat.api.events.CombatPreStartEvent;import java.util.*;
public class Stub extends JavaPlugin implements Listener {
 public final Set<UUID> combat=new HashSet<>(); public void onEnable(){getServer().getPluginManager().registerEvents(this,this);}
 public void start(Player a,Player b){CombatPreStartEvent e=new CombatPreStartEvent(a,b);getServer().getPluginManager().callEvent(e);if(!e.isCancelled()){combat.add(a.getUniqueId());combat.add(b.getUniqueId());}getLogger().info("STUB EVENT cancel="+e.isCancelled()+" a="+a.getName()+" b="+b.getName()+" ownerTag="+combat.contains(a.getUniqueId()));}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void hit(EntityDamageByEntityEvent e){if(e.getDamager() instanceof Player a && e.getEntity() instanceof Player b)start(a,b);}
}
