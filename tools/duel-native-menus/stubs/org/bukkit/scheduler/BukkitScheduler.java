package org.bukkit.scheduler; import org.bukkit.plugin.Plugin; public interface BukkitScheduler { BukkitTask runTask(Plugin p, Runnable r); }
