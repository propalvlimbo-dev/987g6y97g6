package org.bukkit.inventory;
import org.bukkit.Material; import org.bukkit.inventory.meta.ItemMeta; import java.util.List;
public class ItemStack {
 public final Material material; public ItemMeta meta=new Meta();
 public ItemStack(Material m){material=m;} public ItemMeta getItemMeta(){return meta;} public boolean setItemMeta(ItemMeta m){meta=m;return true;}
 public static class Meta implements ItemMeta { public String name; public List<String> lore; public void setDisplayName(String s){name=s;} public void setLore(List<String> l){lore=l;} }
}
