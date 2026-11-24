package org.bukkit.event.inventory; import org.bukkit.entity.HumanEntity; import org.bukkit.inventory.InventoryView;
public class InventoryClickEvent { public InventoryView view; public HumanEntity who; public int slot; public boolean cancelled;
 public InventoryView getView(){return view;} public void setCancelled(boolean b){cancelled=b;} public HumanEntity getWhoClicked(){return who;} public int getRawSlot(){return slot;} }
