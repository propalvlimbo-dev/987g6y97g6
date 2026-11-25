package ru.rooyzee.elytrixairdrop.gui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.gui.holders.MainMenuHolder;
public class MainMenu {
 public static final String TITLE=ElytrixStyle.title("АИРДРОПЫ");
 private final Main plugin;
 public MainMenu(Main plugin){this.plugin=plugin;}
 public void open(Player player){
  Inventory inv=Bukkit.createInventory(new MainMenuHolder(),54,TITLE);
  inv.setItem(20,entry(Material.BEACON,"МИРНЫЙ", "peaceful"));
  inv.setItem(22,entry(Material.MAGMA_BLOCK,"ОГНЕННЫЙ", "fire"));
  inv.setItem(24,entry(Material.SHULKER_BOX,"ВОЗДУШНЫЙ", "sky"));
  inv.setItem(39,ElytrixStyle.item(Material.LIME_DYE,"ЗАПУСТИТЬ","action_start","Запустить случайный аирдроп"));
  inv.setItem(40,ElytrixStyle.item(Material.RED_DYE,"ОСТАНОВИТЬ","action_stop","Остановить текущий аирдроп"));
  inv.setItem(41,ElytrixStyle.item(Material.ENDER_PEARL,"ТЕЛЕПОРТ","action_tp","Переместиться к событию"));
  player.openInventory(inv);
 }
 private org.bukkit.inventory.ItemStack entry(Material mat,String title,String id){
  boolean on=plugin.getConfigManager().getConfig().getBoolean("airdrops."+id+".enabled");
  return ElytrixStyle.item(mat,title,id,"Настройки и общий лут","Статус: "+(on?"&aвключён":"&cвыключен"));
 }
}
