package ru.rooyzee.elytrixcasino.patch;
import java.util.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
public final class SpinRegistry {
    public interface Finishable { void finishNow(); }
    private static final Set<Finishable> SPINS=Collections.newSetFromMap(new IdentityHashMap<>());
    public static BukkitTask schedule(BukkitRunnable spin,Plugin plugin,long delay,long period) {
        BukkitTask task=spin.runTaskTimer(plugin,delay,period);
        SPINS.add((Finishable)spin); return task;
    }
    public static void done(Finishable spin) { SPINS.remove(spin); }
    public static void finishAll() {
        for(Finishable spin:new ArrayList<>(SPINS)) { SPINS.remove(spin); spin.finishNow(); }
    }
}
