package ru.rooyzee.elytrixairdrop.gui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.gui.holders.SettingsMenuHolder;
public class AirdropSettingsMenu {
 public static final String TITLE_PREFIX=ElytrixStyle.title("");
 private final Main plugin; private final String id;
 public AirdropSettingsMenu(Main plugin,String id){this.plugin=plugin;this.id=id;}
 public String getId(){return id;}
 public void open(Player player){
  String title=id.equals("peaceful")?"МИРНЫЙ":id.equals("fire")?"ОГНЕННЫЙ":"ВОЗДУШНЫЙ";
  Inventory inv=Bukkit.createInventory(new SettingsMenuHolder(id),54,ElytrixStyle.title(title));
  String p="airdrops."+id+".";
  if(id.equals("peaceful"))inv.setItem(19,value(Material.CLOCK,"АКТИВАЦИЯ, МИН.",p+"activation-minutes","cfg_activation-minutes"));
  if(id.equals("sky")){inv.setItem(19,value(Material.FEATHER,"МИН. ВЫСОТА",p+"min-above-ground","cfg_min-above-ground"));inv.setItem(25,value(Material.ELYTRA,"МАКС. ВЫСОТА",p+"max-above-ground","cfg_max-above-ground"));}
  inv.setItem(21,value(Material.REPEATER,"ДЛИТЕЛЬНОСТЬ, МИН.",p+"duration-minutes","duration"));
  inv.setItem(23,value(Material.COMPASS,"РАДИУС РЕГИОНА",p+"region-radius","radius"));
  boolean on=plugin.getConfigManager().getConfig().getBoolean(p+"enabled");
  inv.setItem(31,ElytrixStyle.item(on?Material.LIME_DYE:Material.GRAY_DYE,on?"ВКЛЮЧЁН":"ВЫКЛЮЧЕН","toggle","Нажми для переключения"));
  inv.setItem(29,ElytrixStyle.item(Material.ENDER_CHEST,"ОБЩИЙ ЛУТ","loot_editor","Один пул для всех трёх событий","Четыре категории редкости"));
  inv.setItem(33,ElytrixStyle.item(Material.FIREWORK_ROCKET,"ЗАПУСТИТЬ","action_type_start","Запустить этот аирдроп"));
  inv.setItem(49,ElytrixStyle.item(Material.ARROW,"НАЗАД","back","К выбору аирдропа"));
  player.openInventory(inv);
 }
 private org.bukkit.inventory.ItemStack value(Material t,String n,String path,String key){return ElytrixStyle.item(t,n,key,"Значение: &f"+plugin.getConfigManager().getConfig().getInt(path),"ЛКМ +1 / ПКМ -1","Shift изменяет на 10");}
}
