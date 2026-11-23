package org.bukkit.entity; import org.bukkit.inventory.InventoryView; public interface Player { boolean isOnline(); void closeInventory(); InventoryView getOpenInventory(); }
