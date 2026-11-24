package ru.elytrix.duelsbridge;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Native inventory menus for the existing bridge. No console commands or BMenu API. */
final class Menus implements Listener {
    static final int FAILED = 0, NO_ARGS = 1, WITH_ARGS = 2;
    private final ElytrixDuelsBridge plugin;
    private final Map<String, Layout> layouts = new HashMap<>();
    private boolean registered;

    Menus(ElytrixDuelsBridge plugin) {
        this.plugin = plugin;
        Layouts.load(this);
    }
    void layout(String id, int size, String title) { layouts.put(id, new Layout(size, title)); }
    void defaultArg(String id, String key, String value) { layouts.get(id).defaults.put(key, value); }
    void item(String id, int slot, String material, String name, String[] lore, String[] actions) {
        Layout layout = layouts.get(id);
        if (layout == null || slot < 0 || slot >= layout.size || layout.items.containsKey(slot))
            throw new IllegalArgumentException("Invalid menu slot " + id + ":" + slot);
        layout.items.put(slot, new Button(material, name, lore, actions));
    }
    boolean open(Player player, String id) { return open(player, id, null); }
    boolean open(Player player, String id, Map<String, String> args) { return openMenu(player, id, args) != FAILED; }
    int openMenu(Player player, String id, Map<String, String> args) {
        if (player == null || id == null) return FAILED;
        Layout layout = layouts.get(id.toLowerCase(Locale.ROOT));
        if (layout == null) return FAILED; // never open arbitrary BMenu IDs
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        Map<String, String> values = new HashMap<>(layout.defaults);
        if (args != null) values.putAll(args);
        MenuHolder holder = new MenuHolder(player.getUniqueId(), layout);
        Inventory inventory = Bukkit.createInventory(holder, layout.size, color(replace(layout.title, values))); 
        holder.inventory = inventory;
        for (Map.Entry<Integer, Button> entry : layout.items.entrySet()) {
            Button button = entry.getValue();
            Material material = Material.matchMaterial(button.material);
            if (material == null) continue;
            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(replace(button.name, values)));
                List<String> lore = new ArrayList<>();
                for (String line : button.lore) lore.add(color(replace(line, values)));
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inventory.setItem(entry.getKey(), stack);
        }
        player.openInventory(inventory);
        return WITH_ARGS;
    }
    private static String replace(String s, Map<String, String> args) {
        if (args == null) return s;
        for (Map.Entry<String, String> e : args.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        return s;
    }
    private static String color(String s) { return ElytrixDuelsBridge.colorize(s); }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder)) return;
        MenuHolder holder = (MenuHolder) top.getHolder();
        event.setCancelled(true); // includes shift-click, number keys, double-click and the bottom inventory
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!holder.owner.equals(player.getUniqueId())) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) return;
        Button button = holder.layout.items.get(slot);
        if (button == null || button.actions.length == 0) return;
        // Close first and defer navigation, so inventory changes aren't made inside the click event.
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            for (String action : button.actions) {
                if (action.startsWith("cmd:")) player.performCommand(action.substring(4));
                else if (action.startsWith("open:")) {
                    String target = action.substring(5);
                    if (target.equals("elytrix:menu")) player.performCommand("menu");
                    else open(player, target, plugin.menuArgsFor(player, target));
                }
                // [close] is already handled before the task; it must not close a new queue window.
            }
        });
    }
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }
    private static final class Layout {
        final int size; final String title; final Map<Integer, Button> items = new HashMap<>();
        final Map<String, String> defaults = new HashMap<>();
        Layout(int size, String title) { this.size = size; this.title = title; }
    }
    private static final class Button {
        final String material, name; final String[] lore, actions;
        Button(String material, String name, String[] lore, String[] actions) {
            this.material = material; this.name = name; this.lore = lore; this.actions = actions;
        }
    }
    private static final class MenuHolder implements InventoryHolder {
        final UUID owner; final Layout layout; Inventory inventory;
        MenuHolder(UUID owner, Layout layout) { this.owner = owner; this.layout = layout; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
