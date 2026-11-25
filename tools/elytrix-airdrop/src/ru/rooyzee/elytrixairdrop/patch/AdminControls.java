package ru.rooyzee.elytrixairdrop.patch;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;
import ru.rooyzee.elytrixairdrop.gui.AirdropSettingsMenu;
import ru.rooyzee.elytrixairdrop.gui.holders.LootEditorHolder;
import ru.rooyzee.elytrixairdrop.gui.holders.MainMenuHolder;
import ru.rooyzee.elytrixairdrop.gui.holders.SettingsMenuHolder;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
public class AdminControls implements Listener {
 private final Main plugin;
 private AdminControls(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){Bukkit.getPluginManager().registerEvents(new AdminControls(plugin),plugin);}
 @EventHandler(priority=EventPriority.LOWEST) public void click(InventoryClickEvent e){
  if(!(e.getWhoClicked() instanceof Player) || e.getClickedInventory()!=e.getView().getTopInventory())return;
  Player player=(Player)e.getWhoClicked();InventoryHolder holder=e.getView().getTopInventory().getHolder();
  if(!(holder instanceof MainMenuHolder || holder instanceof SettingsMenuHolder))return;
  ItemStack item=e.getCurrentItem(); if(item==null || !item.hasItemMeta() || item.getItemMeta().getLore()==null)return;
  String key="";for(String line:item.getItemMeta().getLore()){String text=ChatColor.stripColor(line);if(text!=null && text.startsWith("key:"))key=text.substring(4);}
  if(!key.startsWith("action_")&&!key.startsWith("cfg_"))return;
  e.setCancelled(true); if(!player.hasPermission("elytrixairdrop.admin"))return;
  if(holder instanceof MainMenuHolder){
   if(key.equals("action_start")){boolean ok=plugin.getAirdropManager().canManualStart() && plugin.getAirdropManager().startRandom();player.sendMessage(ColorUtil.colorize(ok?"&aАирдроп запущен.":"&cНе удалось запустить аирдроп."));}
   else if(key.equals("action_stop")){if(plugin.getAirdropManager().getActive()!=null){plugin.getAirdropManager().stop(false);player.sendMessage(ColorUtil.colorize("&aАирдроп остановлен."));}else player.sendMessage(ColorUtil.colorize("&cАктивного аирдропа нет."));}
   else if(key.equals("action_tp")){ActiveAirdrop a=plugin.getAirdropManager().getActive();if(a!=null){player.closeInventory();player.teleport(a.getCenter().clone().add(0.5,2,0.5));}else player.sendMessage(ColorUtil.colorize("&cАктивного аирдропа нет."));}
  }else if(holder instanceof SettingsMenuHolder && key.equals("action_type_start")){
   Airdrop type=Airdrop.fromId(((SettingsMenuHolder)holder).getId());
   boolean ok=type!=null && plugin.getAirdropManager().canManualStart() && plugin.getAirdropManager().start(type);
   player.sendMessage(ColorUtil.colorize(ok?"&aАирдроп запущен.":"&cНе удалось запустить аирдроп."));
  }else if(holder instanceof SettingsMenuHolder && key.startsWith("cfg_")){
   String id=((SettingsMenuHolder)holder).getId(); String field=key.substring(4);
   if(!(id.equals("peaceful") && field.equals("activation-minutes")) && !(id.equals("sky") && (field.equals("min-above-ground")||field.equals("max-above-ground"))))return;
   int delta=e.isShiftClick()?10:1;if(e.isRightClick())delta=-delta;else if(!e.isLeftClick())return;
   String path="airdrops."+id+"."+field;int n=Math.max(0,Math.min(10000,plugin.getConfigManager().getConfig().getInt(path)+delta));
   if(field.equals("min-above-ground"))n=Math.min(n,plugin.getConfigManager().getConfig().getInt("airdrops.sky.max-above-ground"));
   if(field.equals("max-above-ground"))n=Math.max(n,plugin.getConfigManager().getConfig().getInt("airdrops.sky.min-above-ground"));
   plugin.getConfigManager().getConfig().set(path,n);plugin.getConfigManager().saveConfig();new AirdropSettingsMenu(plugin,id).open(player);
  }
 }
 @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){
  if(e.getView().getTopInventory().getHolder() instanceof LootEditorHolder && e.getRawSlots().stream().anyMatch(i->i>=45 && i<54))e.setCancelled(true);
 }
}
