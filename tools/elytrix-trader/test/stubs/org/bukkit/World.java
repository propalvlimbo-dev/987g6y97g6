package org.bukkit; public interface World {boolean isChunkLoaded(int x,int z); <T extends org.bukkit.entity.Entity> T spawn(Location l,Class<T> t);}
