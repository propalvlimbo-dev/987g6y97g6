package ru.rooyzee.elytrixairdrop.gui;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.gui.holders.SettingsMenuHolder;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
public class AirdropSettingsMenu {
 public static final String TITLE_PREFIX=ColorUtil.colorize("&#F8BEFBАирдроп &8» &f");
 private final Main plugin; private final String id;
 public AirdropSettingsMenu(Main plugin,String id){this.plugin=plugin;this.id=id;}
 public String getId(){return id;}
 public void open(Player player){
  String title=id.equals("peaceful")?"Мирный":id.equals("fire")?"Огненный":"Воздушный";
  Inventory inv=Bukkit.createInventory(new SettingsMenuHolder(id),45,TITLE_PREFIX+title);
  ItemStack pane=item(Material.BLACK_STAINED_GLASS_PANE," ","",null);
  for(int i=0;i<45;i++)inv.setItem(i,pane);
  String p="airdrops."+id+".";
  if(id.equals("peaceful")) inv.setItem(10,value(Material.CLOCK,"Активация, мин.",plugin.getConfigManager().getConfig().getInt(p+"activation-minutes"),"cfg_activation-minutes"));
  if(id.equals("sky")) { inv.setItem(28,value(Material.FEATHER,"Мин. высота",plugin.getConfigManager().getConfig().getInt(p+"min-above-ground"),"cfg_min-above-ground")); inv.setItem(34,value(Material.ELYTRA,"Макс. высота",plugin.getConfigManager().getConfig().getInt(p+"max-above-ground"),"cfg_max-above-ground")); }
  inv.setItem(12,value(Material.REPEATER,"Длительность, мин.",plugin.getConfigManager().getConfig().getInt(p+"duration-minutes"),"duration"));
  inv.setItem(14,value(Material.COMPASS,"Радиус региона",plugin.getConfigManager().getConfig().getInt(p+"region-radius"),"radius"));
  boolean on=plugin.getConfigManager().getConfig().getBoolean(p+"enabled");
  inv.setItem(16,item(on?Material.LIME_DYE:Material.GRAY_DYE,on?"&aВключён":"&cВыключен","&7Нажмите для переключения","toggle"));
  inv.setItem(22,item(Material.ENDER_CHEST,"&#F8BEFBОбщий лут","&7Один пул для всех аирдропов","loot_editor"));
  inv.setItem(31,item(Material.LIME_DYE,"&aЗапустить этот аирдроп","&7Нажмите для запуска", "action_type_start"));
  inv.setItem(40,item(Material.ARROW,"&fНазад","&7К выбору аирдропа","back"));
  player.openInventory(inv);
 }
 private ItemStack value(Material t,String n,int v,String key){return item(t,"&#F8BEFB"+n,"&7Значение: &f"+v+" &8| &7ЛКМ +1, ПКМ -1",key);}
 private ItemStack item(Material t,String n,String desc,String key){ItemStack s=new ItemStack(t);ItemMeta m=s.getItemMeta();m.setDisplayName(ColorUtil.colorize(n));if(key!=null)m.setLore(Arrays.asList(ColorUtil.colorize(desc),ColorUtil.colorize("&7Shift: изменение на 10"),ColorUtil.colorize("&8key:"+key)));s.setItemMeta(m);return s;}
}
