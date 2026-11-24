package org.bukkit.scheduler;import org.bukkit.plugin.Plugin;public interface BukkitScheduler { BukkitTask runTaskTimer(Plugin plugin,Runnable r,long delay,long period); }
