package org.bukkit.entity; import org.bukkit.World; public interface Entity { World getWorld(); default void remove(){} }
