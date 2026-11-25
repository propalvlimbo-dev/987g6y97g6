package ru.rooyzee.elytrixairdrop.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixairdrop.Main;

/** Common loot for all event types. Legacy files are read, never modified. */
public class LootFileService {
    private static final int PAGES = 4;
    private static final int CAPACITY = 45;
    private static final String[] OLD_IDS = {"peaceful", "fire", "sky", "sky_common", "sky_rare", "sky_mythic", "sky_legendary"};
    private final Main plugin;
    private final File folder;
    private final Map<Integer, List<ItemStack>> pages = new LinkedHashMap<>();
    private boolean migrationBlocked;

    public LootFileService(Main plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "loot");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Cannot create loot folder: " + folder);
        }
        loadAll();
    }

    public void loadAll() {
        pages.clear();
        migrationBlocked = false;
        for (int i = 1; i <= PAGES; i++) pages.put(i, new ArrayList<>());
        File common = new File(folder, "common.yml");
        if (common.exists()) {
            read(common, pages);
            for (int i = 1; i <= PAGES; i++) {
                if (pages.get(i).size() > CAPACITY) {
                    migrationBlocked = true;
                    plugin.getLogger().severe("Common loot page " + i + " exceeds " + CAPACITY + " items; refusing to overwrite it.");
                }
            }
            return;
        }
        Map<Integer, List<ItemStack>> imported = new LinkedHashMap<>();
        for (int i = 1; i <= PAGES; i++) imported.put(i, new ArrayList<>());
        boolean found = false;
        for (String id : OLD_IDS) {
            File file = new File(folder, id + ".yml");
            if (file.isFile()) { found = true; read(file, imported); }
        }
        for (int i = 1; i <= PAGES; i++) {
            if (imported.get(i).size() > CAPACITY) {
                migrationBlocked = true;
                plugin.getLogger().severe("Cannot import legacy loot: page " + i + " exceeds " + CAPACITY + " items. Original files are unchanged; reduce or manually merge them into loot/common.yml.");
            }
        }
        if (!migrationBlocked && found) {
            pages.putAll(imported);
            savePages();
        }
    }

    private void read(File file, Map<Integer, List<ItemStack>> dest) {
        YamlConfiguration yaml = new YamlConfiguration();
        try { yaml.load(file); } catch (Exception e) {
            migrationBlocked = true;
            plugin.getLogger().severe("Cannot read loot file " + file + ": " + e.getMessage());
            return;
        }
        boolean hasPages = false;
        for (int i = 1; i <= PAGES; i++) if (yaml.contains("page_" + i)) hasPages = true;
        for (int i = 1; i <= PAGES; i++) {
            List<?> values = yaml.getList(hasPages ? "page_" + i : (i == 1 ? "items" : "page_" + i));
            if (values == null) continue;
            for (Object value : values) {
                if (value instanceof ItemStack) {
                    ItemStack item = (ItemStack) value;
                    if (item.getType() != Material.AIR) dest.get(i).add(item.clone());
                } else if (value != null) {
                    migrationBlocked = true;
                    plugin.getLogger().severe("Invalid item in " + file + "; refusing to overwrite shared loot.");
                }
            }
        }
    }

    private boolean savePages() {
        if (migrationBlocked) return false;
        YamlConfiguration yaml = new YamlConfiguration();
        for (int i = 1; i <= PAGES; i++) yaml.set("page_" + i, pages.get(i));
        File target = new File(folder, "common.yml");
        File temp = new File(folder, "common.yml.tmp");
        try {
            yaml.save(temp);
            try { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Cannot save common loot: " + e.getMessage());
            return false;
        }
    }
    public boolean isReadOnly() { return migrationBlocked; }
    public void loadType(String id) { loadAll(); }
    public void saveType(String id, Map<Integer, List<ItemStack>> data) {
        if (migrationBlocked) return;
        for (int i = 1; i <= PAGES; i++) {
            List<ItemStack> items = data.get(i);
            pages.put(i, items == null ? new ArrayList<>() : copy(items));
        }
        savePages();
    }
    public List<ItemStack> getItems(String id, int page) { return copy(pages.getOrDefault(page, Collections.emptyList())); }
    public void setItems(String id, int page, List<ItemStack> items) {
        if (migrationBlocked || page < 1 || page > PAGES || items.size() > CAPACITY) return;
        List<ItemStack> old = pages.put(page, copy(items));
        if (!savePages()) pages.put(page, old);
    }
    public List<ItemStack> load(String id) {
        List<ItemStack> result = new ArrayList<>();
        if (!migrationBlocked) for (int i = 1; i <= PAGES; i++) result.addAll(getItems(id, i));
        return result;
    }
    public void save(String id, List<ItemStack> items) { setItems(id, 1, items); }
    public void invalidate(String id) { loadAll(); }
    public void invalidateAll() { loadAll(); }
    public List<ItemStack> rollDrops(String id, int nearbyPlayers) {
        if (migrationBlocked) return Collections.emptyList();
        double[] chance = nearbyPlayers <= 3 ? new double[]{75,25,5,1} : nearbyPlayers <= 6 ? new double[]{90,50,20,5} : new double[]{100,75,45,15};
        int max = nearbyPlayers <= 3 ? 3 : nearbyPlayers <= 6 ? 5 : 8;
        List<ItemStack> result = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 1; i <= PAGES; i++) {
            List<ItemStack> list = pages.get(i);
            if (!list.isEmpty() && random.nextDouble(100) < chance[i - 1]) result.add(list.get(random.nextInt(list.size())).clone());
        }
        if (result.isEmpty()) {
            List<ItemStack> all = load(id);
            Collections.shuffle(all);
            result.addAll(all.subList(0, Math.min(max, all.size())));
        }
        return result;
    }
    private static List<ItemStack> copy(List<ItemStack> input) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : input) if (item != null && item.getType() != Material.AIR) result.add(item.clone());
        return result;
    }
}
