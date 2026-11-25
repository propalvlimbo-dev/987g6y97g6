package ru.rooyzee.elytrixairdrop.patch;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** Shows the currently active event to every player, including new arrivals. */
public final class EventBar implements Listener {
    private final Main plugin;
    private BossBar bar;
    private ActiveAirdrop shown;
    private BukkitTask task;

    private EventBar(Main plugin) { this.plugin = plugin; }
    public static void install(Main plugin) {
        EventBar tracker = new EventBar(plugin);
        Bukkit.getPluginManager().registerEvents(tracker, plugin);
        tracker.task = Bukkit.getScheduler().runTaskTimer(plugin, tracker::update, 1L, 1L);
    }
    private void update() {
        ActiveAirdrop current = plugin.getAirdropManager().getActive();
        if (current == shown) return;
        clear();
        shown = current;
        if (current == null) return;
        Location loc = current.getCenter();
        String id = current.getType().getId();
        String name = plugin.getConfigManager().getConfig().getString("airdrops." + id + ".display-name", id);
        String world = loc.getWorld() == null ? "?" : loc.getWorld().getName();
        String title = ColorUtil.colorize("&#F8BEFBАирдроп &8» &f" + name + " &7| &f" + world + " &7| &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        bar = Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { if (bar != null) bar.addPlayer(event.getPlayer()); }
    @EventHandler public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) { if (task != null) task.cancel(); clear(); shown = null; }
    }
    private void clear() { if (bar != null) { bar.removeAll(); bar = null; } }
}
