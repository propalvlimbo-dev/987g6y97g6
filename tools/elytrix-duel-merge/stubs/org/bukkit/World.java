package org.bukkit; import java.util.*;import org.bukkit.entity.Entity; public interface World { String getName(); default java.util.List<Entity> getEntities(){return List.of();} }
