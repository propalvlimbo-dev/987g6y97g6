package ru.rooyzee.elytrixairdrop.gui;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
/** Uses the same generic 54-slot frame from Elytrix-RP as /menu. */
final class ElytrixStyle {
 private ElytrixStyle(){}
 static String title(String text){return ColorUtil.colorize("&fꈁꀀꈂꀁ&fꈃ"+text);}
 static ItemStack item(Material type,String name,String key,String... details){
  ItemStack stack=new ItemStack(type);ItemMeta meta=stack.getItemMeta();meta.setDisplayName(ColorUtil.colorize("&#FAD8FC"+name));
  List<String> lore=new ArrayList<>();lore.add("");for(String detail:details)lore.add(ColorUtil.colorize("&7"+detail));
  if(key!=null){lore.add("");lore.add(ColorUtil.colorize("&#F8BEFBНажми, чтобы открыть"));lore.add(ColorUtil.colorize("&8key:"+key));}
  meta.setLore(lore);stack.setItemMeta(meta);return stack;
 }
}
