package ru.rooyzee.elytrixairdrop.patch;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;

/** Runtime hooks; the actual items remain in the original event's server-side maps. */
public final class LootRuntime implements Listener {
    private static final Set<Inventory> inventories = Collections.newSetFromMap(new WeakHashMap<Inventory, Boolean>());
    private LootRuntime() {}
    public static void install(Main plugin) { Bukkit.getPluginManager().registerEvents(new LootRuntime(), plugin); }
    public static void track(Inventory inventory) { inventories.add(inventory); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent e) {
        if (inventories.contains(e.getView().getTopInventory()) && e.getRawSlots().stream().anyMatch(slot -> slot < e.getView().getTopInventory().getSize())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent e) {
        if (!inventories.contains(e.getView().getTopInventory())) return;
        if (e.getClickedInventory() == e.getView().getTopInventory() || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) e.setCancelled(true);
    }
    public static int perContainer(ActiveAirdrop event) {
        int n = NearbyRewards.players(event.getCenter());
        return n <= 3 ? 2 : n <= 6 ? 3 : 5;
    }
    public static void preparePeaceful(ActiveAirdrop event, List<ItemStack> pending) {
        pending.clear();
        int nearby = NearbyRewards.players(event.getCenter());
        List<ItemStack> pool = Main.getInstance().getAirdropManager().getLootFileService().rollDrops("common", nearby);
        if (pool.isEmpty()) return;
        int amount = 5 * perContainer(event);
        Random random = new Random();
        for (int i = 0; i < amount; i++) pending.add(pool.get(random.nextInt(pool.size())).clone());
    }
}
