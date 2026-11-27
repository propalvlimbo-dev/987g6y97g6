package ru.rooyzee.elytrixairdrop.patch;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryType;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
/** One deferred, guarded reopen per voluntary close, never during the close handler itself. */
public final class PeacefulChest implements Listener {
 private final Main plugin;
 private final ReopenGuard guard=new ReopenGuard();
 private PeacefulChest(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){Bukkit.getPluginManager().registerEvents(new PeacefulChest(plugin),plugin);}
 @EventHandler(priority=EventPriority.MONITOR) public void close(InventoryCloseEvent e){
  if(!(e.getPlayer() instanceof Player) || e.getReason()!=InventoryCloseEvent.Reason.PLAYER)return;
  Player player=(Player)e.getPlayer();ActiveAirdrop active=plugin.getAirdropManager().getActive();
  if(active==null || active.getType()!=Airdrop.PEACEFUL)return;
  try{
   Inventory loot=(Inventory)field(active,"lootInventory");
   if(e.getInventory()!=loot || !unfinished(active))return;
   UUID id=player.getUniqueId();
   if(!guard.queue(id,System.currentTimeMillis())){
    player.sendMessage(ColorUtil.colorize("&eЕщё есть волны. Откройте сундук снова, чтобы забрать оставшийся лут."));return;
   }
   try{Bukkit.getScheduler().runTaskLater(plugin,()->reopen(player,active,loot,id),2L);}
   catch(RuntimeException ex){guard.cancel(id);throw ex;}
  }catch(ReflectiveOperationException | RuntimeException ex){plugin.getLogger().warning("Could not check peaceful loot waves: "+ex.getMessage());}
 }
 private void reopen(Player player,ActiveAirdrop active,Inventory loot,UUID id){
  try{
   if(plugin.getAirdropManager().getActive()!=active || !player.isOnline() || player.isDead() || !unfinished(active) || field(active,"lootInventory")!=loot){guard.cancel(id);return;}
   Location chest=(Location)field(active,"chestLoc");
   if(chest==null || !chest.getWorld().equals(player.getWorld()) || player.getLocation().distanceSquared(chest)>36 || player.getOpenInventory().getTopInventory().getType()!=InventoryType.CRAFTING){guard.cancel(id);return;}
   guard.start(id);
   try{
    if(player.openInventory(loot)==null)player.sendMessage(ColorUtil.colorize("&eОткройте сундук снова: ожидаются новые волны."));
    else player.sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &fЕщё будут волны лута!"));
   }finally{guard.finish(id);}
  }catch(ReflectiveOperationException | RuntimeException ex){guard.cancel(id);plugin.getLogger().warning("Could not reopen peaceful chest: "+ex.getMessage());}
 }
 @EventHandler public void quit(PlayerQuitEvent event){guard.clear(event.getPlayer().getUniqueId());}
 private static boolean unfinished(Object obj)throws ReflectiveOperationException{
  return !(boolean)field(obj,"looted") && !((List<?>)field(obj,"pendingLoot")).isEmpty();
 }
 private static Object field(Object obj,String name)throws ReflectiveOperationException{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
}
