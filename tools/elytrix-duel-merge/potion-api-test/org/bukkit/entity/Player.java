package org.bukkit.entity; import java.util.UUID; import org.bukkit.potion.PotionEffectType; public interface Player { UUID getUniqueId(); void removePotionEffect(PotionEffectType type); }
