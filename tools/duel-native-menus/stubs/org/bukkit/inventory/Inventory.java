package org.bukkit.inventory; public interface Inventory { InventoryHolder getHolder(); void setItem(int slot,ItemStack item); int getSize(); }
