package org.bukkit.block; import org.bukkit.Material; public interface BlockState { Block getBlock(); Material getType(); boolean update(boolean force,boolean physics); }
