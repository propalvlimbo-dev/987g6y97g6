package ru.rooyzee.elytrixairdrop.patch;
import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
public final class PeacefulChest implements Listener {
 private final Main plugin;
 private PeacefulChest(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){Bukkit.getPluginManager().registerEvents(new PeacefulChest(plugin),plugin);}
 @EventHandler public void close(InventoryCloseEvent e){
  if(!(e.getPlayer() instanceof Player))return;
  ActiveAirdrop active=plugin.getAirdropManager().getActive();
  if(active==null || active.getType()!=Airdrop.PEACEFUL)return;
  Inventory inv=e.getInventory();Player player=(Player)e.getPlayer();
  if(!shouldReopen(active,inv,player))return;
  Bukkit.getScheduler().runTask(plugin,()->{
   if(plugin.isEnabled() && plugin.getAirdropManager().getActive()==active && shouldReopen(active,inv,player)){
    player.openInventory(inv);
    player.sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &fЗаберите все волны лута, чтобы закрыть сундук."));
   }
  });
 }
 private boolean shouldReopen(ActiveAirdrop active,Inventory inv,Player player){
  if(!player.isOnline()||player.isDead()||player.getHealth()<=0)return false;
  try{
   if(field(active,"lootInventory")!=inv || (boolean)field(active,"looted"))return false;
   Location chest=(Location)field(active,"chestLoc"), pos=player.getLocation();
   return chest!=null && chest.getWorld()!=null && chest.getWorld().equals(pos.getWorld()) && chest.distanceSquared(pos)<=64;
  }catch(ReflectiveOperationException | RuntimeException ex){return false;}
 }
 private static Object field(Object obj,String name)throws ReflectiveOperationException{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
}
