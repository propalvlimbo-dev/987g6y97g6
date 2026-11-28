package org.bukkit.persistence;import org.bukkit.NamespacedKey;public interface PersistentDataContainer{<P,C> boolean has(NamespacedKey key,PersistentDataType<P,C> type);}
