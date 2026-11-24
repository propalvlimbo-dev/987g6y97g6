package ru.elytrix.duelsbridge;
import java.util.*;
import org.bukkit.Bukkit; import org.bukkit.entity.Player; import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
public final class MenuTest {
 static final class P implements Player {
  UUID id=UUID.randomUUID(); List<String> cmds=new ArrayList<>(); Inventory opened; int closed;
  public UUID getUniqueId(){return id;} public InventoryView openInventory(Inventory i){opened=i;return view(i);} public void closeInventory(){closed++;opened=null;}
  public boolean isOnline(){return true;} public boolean performCommand(String c){cmds.add(c);return true;}
 }
 static InventoryView view(Inventory i){return new InventoryView(){public Inventory getTopInventory(){return i;}};}
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 public static void main(String[] args){
  int[] regs={0}; Bukkit.manager=(l,p)->regs[0]++; Bukkit.scheduler=(p,r)->{r.run();return null;};
  Menus m=new Menus(new ElytrixDuelsBridge()); P p=new P();
  check(!m.open(p,"elytrix:admin"),"only fixed menus may open");
  check(m.open(p,"elytrix:duel"),"main opens"); check(regs[0]==1,"one listener");
  Bukkit.TestInventory main=(Bukkit.TestInventory)p.opened;
  check(main.getSize()==54 && main.items[20].material.name.equals("NETHERITE_SWORD"),"reference layout");
  InventoryClickEvent click=new InventoryClickEvent(); click.view=view(main);click.who=p;click.slot=20; m.onClick(click);
  check(click.cancelled && p.cmds.equals(List.of("duelx pick")),"main button action");
  p.cmds.clear(); m.open(p,"elytrix:duel_queue"); Inventory queue=p.opened;
  click=new InventoryClickEvent();click.view=view(queue);click.who=p;click.slot=20; m.onClick(click);
  check(click.cancelled,"queue click cancelled");
  m.open(p,"elytrix:duel_stats",Map.of("wins","42","losses","3","score","4","winrate","93"));
  Bukkit.TestInventory stats=(Bukkit.TestInventory)p.opened;
  boolean has42=false; for(ItemStack stack: stats.items) if(stack!=null && stack.meta instanceof ItemStack.Meta) { List<String> l=((ItemStack.Meta)stack.meta).lore; if(l!=null && l.toString().contains("42"))has42=true; }
  check(has42,"stats placeholders");
  InventoryDragEvent drag=new InventoryDragEvent();drag.view=view(stats);m.onDrag(drag);check(drag.cancelled,"drag blocked");
  P other=new P(); click=new InventoryClickEvent();click.view=view(stats);click.who=other;click.slot=20;m.onClick(click);check(click.cancelled && other.cmds.isEmpty(),"owner enforced");
  System.out.println("native menu stub tests passed");
 }
}
