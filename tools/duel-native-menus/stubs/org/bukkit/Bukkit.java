package org.bukkit;
import org.bukkit.inventory.*; import org.bukkit.plugin.*; import org.bukkit.scheduler.*;
public final class Bukkit {
  public static PluginManager manager; public static BukkitScheduler scheduler; public static Inventory last;
  public static PluginManager getPluginManager(){return manager;}
  public static BukkitScheduler getScheduler(){return scheduler;}
  public static Inventory createInventory(InventoryHolder h,int s,String t){return last=new TestInventory(h,s,t);}
  public static class TestInventory implements Inventory {
    public InventoryHolder holder; public ItemStack[] items; public String title;
    public TestInventory(InventoryHolder h,int s,String t){holder=h;items=new ItemStack[s];title=t;}
    public InventoryHolder getHolder(){return holder;} public void setItem(int slot,ItemStack item){items[slot]=item;} public int getSize(){return items.length;}
  }
}
