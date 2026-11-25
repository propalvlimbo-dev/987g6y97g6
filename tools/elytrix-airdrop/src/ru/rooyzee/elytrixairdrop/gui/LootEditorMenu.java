package ru.rooyzee.elytrixairdrop.gui;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.gui.holders.LootEditorHolder;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
public class LootEditorMenu {
 private final Main plugin;
 public LootEditorMenu(Main plugin,String ignored){this.plugin=plugin;}
 public LootEditorMenu(Main plugin,String ignored,String title){this.plugin=plugin;}
 public void open(Player player,int page){
  if(plugin.getAirdropManager().getLootFileService().isReadOnly()){player.sendMessage(ColorUtil.colorize("&cОбщий лут не загружен: проверьте ошибки в консоли."));return;}
  page=Math.max(1,Math.min(4,page));
  LootEditorHolder holder=new LootEditorHolder("common","Общий лут",page);
  Inventory inv=Bukkit.createInventory(holder,54,ColorUtil.colorize("&#F8BEFBОбщий лут &8» &f"+page+"/4"));holder.setInventory(inv);
  List<ItemStack> items=plugin.getAirdropManager().getLootFileService().getItems("common",page);
  for(int i=0;i<items.size() && i<45;i++)inv.setItem(i,items.get(i).clone());
  ItemStack pane=button(Material.BLACK_STAINED_GLASS_PANE," ");for(int i=45;i<54;i++)inv.setItem(i,pane);
  inv.setItem(45,button(page>1?Material.ARROW:Material.BARRIER,"&fПредыдущая"));
  inv.setItem(47,button(Material.BOOK,"&7Категория "+page+" / 4"));
  inv.setItem(49,button(Material.NETHER_STAR,"&aСохранить"));
  inv.setItem(53,button(page<4?Material.ARROW:Material.BARRIER,"&fСледующая"));
  player.openInventory(inv);
 }
 public void saveFromInventory(Inventory inv,int page){List<ItemStack> items=new ArrayList<>();for(int i=0;i<45;i++){ItemStack item=inv.getItem(i);if(item!=null&&item.getType()!=Material.AIR)items.add(item.clone());}plugin.getAirdropManager().getLootFileService().setItems("common",page,items);}
 private ItemStack button(Material t,String name){ItemStack s=new ItemStack(t);ItemMeta m=s.getItemMeta();m.setDisplayName(ColorUtil.colorize(name));m.setLore(Arrays.asList(ColorUtil.colorize("&7Изменения сохраняются при закрытии")));s.setItemMeta(m);return s;}
}
