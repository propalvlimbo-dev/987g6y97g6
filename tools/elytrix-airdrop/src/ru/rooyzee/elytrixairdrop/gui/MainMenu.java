package ru.rooyzee.elytrixairdrop.gui;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.gui.holders.MainMenuHolder;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
public class MainMenu {
 public static final String TITLE=ColorUtil.colorize("&#F8BEFBᴇʟʏᴛʀɪx &8» &fАирдропы");
 private final Main plugin;
 public MainMenu(Main plugin){this.plugin=plugin;}
 public void open(Player player){
  Inventory inv=Bukkit.createInventory(new MainMenuHolder(),27,TITLE);
  ItemStack pane=item(Material.BLACK_STAINED_GLASS_PANE," ","",null);
  for(int i=0;i<27;i++) inv.setItem(i,pane);
  inv.setItem(10,entry(Material.LIGHT_BLUE_WOOL,"&#208BFBМирный", "peaceful"));
  inv.setItem(13,entry(Material.RED_WOOL,"&cОгненный", "fire"));
  inv.setItem(16,entry(Material.LIGHT_BLUE_SHULKER_BOX,"&bВоздушный", "sky"));
  inv.setItem(19,item(Material.LIME_DYE,"&aЗапустить случайный","&7Запустить аирдроп", "action_start"));
  inv.setItem(22,item(Material.RED_DYE,"&cОстановить","&7Остановить текущий аирдроп", "action_stop"));
  inv.setItem(25,item(Material.ENDER_PEARL,"&bТелепорт","&7К текущему аирдропу", "action_tp"));
  player.openInventory(inv);
 }
 private ItemStack entry(Material material,String title,String id){
  String p="airdrops."+id+".";
  return item(material,title,"&7Статус: "+(plugin.getConfigManager().getConfig().getBoolean(p+"enabled")?"&aвключён":"&cвыключен"),id);
 }
 private ItemStack item(Material type,String name,String detail,String key){ItemStack s=new ItemStack(type);ItemMeta m=s.getItemMeta();m.setDisplayName(ColorUtil.colorize(name));if(key!=null)m.setLore(Arrays.asList(ColorUtil.colorize(detail),"",ColorUtil.colorize("&7Нажмите для настройки"),ColorUtil.colorize("&8key:"+key)));s.setItemMeta(m);return s;}
}
