package ru.rooyzee.elytrixcasino.patch;
import java.util.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
public final class SpinRegistry {
    public interface Finishable {
        void finishNow();
        UUID spinPlayer();
        String spinMachine();
        Inventory spinInventory();
    }
    private static final Set<Finishable> SPINS=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<UUID,Finishable> BY_PLAYER=new HashMap<>();
    public static BukkitTask schedule(BukkitRunnable spin,Plugin plugin,long delay,long period) {
        BukkitTask task=spin.runTaskTimer(plugin,delay,period);
        Finishable entry=(Finishable)spin;
        SPINS.add(entry);
        BY_PLAYER.put(entry.spinPlayer(),entry);
        return task;
    }
    public static boolean reopen(Player player,String machine) {
        Finishable entry=BY_PLAYER.get(player.getUniqueId());
        if(entry==null || !Objects.equals(entry.spinMachine(),machine)) return false;
        player.openInventory(entry.spinInventory());
        return true;
    }
    public static void done(Finishable spin) {
        SPINS.remove(spin);
        BY_PLAYER.remove(spin.spinPlayer(),spin);
    }
    public static void finishAll() {
        for(Finishable spin:new ArrayList<>(SPINS)) { done(spin); spin.finishNow(); }
    }
}
