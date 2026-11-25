package ru.rooyzee.elytrixairdrop.patch;
import java.lang.reflect.Field;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
/** No inventory re-open: TEST3's re-open path appeared in the watchdog dump. */
public final class PeacefulChest implements Listener {
 private final Main plugin;
 private PeacefulChest(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){Bukkit.getPluginManager().registerEvents(new PeacefulChest(plugin),plugin);}
 @EventHandler public void close(InventoryCloseEvent e){
  if(!(e.getPlayer() instanceof Player))return;
  ActiveAirdrop active=plugin.getAirdropManager().getActive();
  if(active==null || active.getType()!=Airdrop.PEACEFUL)return;
  try{
   Inventory loot=(Inventory)field(active,"lootInventory");
   if(e.getInventory()!=loot || (boolean)field(active,"looted"))return;
   List<?> pending=(List<?>)field(active,"pendingLoot");
   if(!pending.isEmpty())e.getPlayer().sendMessage(ColorUtil.colorize("&#F8BEFBАирдроп &8» &fПосле этой волны появится ещё лут — можно открыть сундук снова."));
  }catch(ReflectiveOperationException | RuntimeException ex){plugin.getLogger().warning("Could not check peaceful loot waves: "+ex.getMessage());}
 }
 private static Object field(Object obj,String name)throws ReflectiveOperationException{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
}
