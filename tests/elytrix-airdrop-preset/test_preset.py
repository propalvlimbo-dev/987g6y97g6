#!/usr/bin/env python3
"""Run LootPreset against minimal Bukkit stubs; no claim of a live server test."""
import os, pathlib, re, subprocess, tempfile
root = pathlib.Path(__file__).resolve().parents[2]
src = root/'tools/elytrix-airdrop/src/ru/rooyzee/elytrixairdrop/patch/LootPreset.java'
text = src.read_text()
materials = sorted(set(re.findall(r'Material\.([A-Z_]+)', text)))
potions = sorted(set(re.findall(r'PotionType\.([A-Z_]+)', text)))
enchants = sorted(set(re.findall(r'Enchantment\.([A-Z_]+)', text)))
files={
'org/bukkit/Material.java': 'package org.bukkit; public enum Material {'+','.join(materials)+'}',
'org/bukkit/ChatColor.java':'package org.bukkit; public enum ChatColor {RED,YELLOW,GREEN}',
'org/bukkit/potion/PotionType.java':'package org.bukkit.potion; public enum PotionType {'+','.join(potions)+'}',
'org/bukkit/enchantments/Enchantment.java':'package org.bukkit.enchantments; public enum Enchantment {'+','.join(enchants)+'}',
'org/bukkit/inventory/meta/PotionMeta.java':'package org.bukkit.inventory.meta; public class PotionMeta { public org.bukkit.potion.PotionType type; public void setBasePotionType(org.bukkit.potion.PotionType t){type=t;} }',
'org/bukkit/inventory/ItemStack.java':'''package org.bukkit.inventory; import java.util.*; import org.bukkit.*; import org.bukkit.enchantments.*; import org.bukkit.inventory.meta.*; public class ItemStack implements Cloneable {public Material type; public int amount=1; public PotionMeta meta=new PotionMeta(); public Map<Enchantment,Integer> ench=new HashMap<>(); public String id; public ItemStack(Material m){type=m;} public Material getType(){return type;} public int getMaxStackSize(){return type==Material.TOTEM_OF_UNDYING?1:64;} public void setAmount(int n){amount=n;} public PotionMeta getItemMeta(){return meta;} public void setItemMeta(PotionMeta m){meta=m;} public void addEnchantment(Enchantment e,int n){ench.put(e,n);} public ItemStack clone(){try{return (ItemStack)super.clone();}catch(Exception e){throw new AssertionError(e);}}}''',
'org/bukkit/plugin/Plugin.java':'package org.bukkit.plugin; public interface Plugin { boolean isEnabled(); }',
'org/bukkit/plugin/PluginManager.java':'package org.bukkit.plugin; public class PluginManager { public Plugin plugin; public Plugin getPlugin(String n){return plugin;} }',
'org/bukkit/scheduler/BukkitScheduler.java':'package org.bukkit.scheduler; public class BukkitScheduler { public void runTask(Object p,Runnable r){} }',
'org/bukkit/Bukkit.java':'package org.bukkit; import java.util.*; import org.bukkit.entity.Player; import org.bukkit.plugin.*; import org.bukkit.scheduler.*; public class Bukkit { public static PluginManager pm=new PluginManager(); public static PluginManager getPluginManager(){return pm;} public static BukkitScheduler getScheduler(){return new BukkitScheduler();} public static Collection<Player> getOnlinePlayers(){return List.of();} }',
'org/bukkit/command/Command.java':'package org.bukkit.command; public class Command {}',
'org/bukkit/command/CommandSender.java':'package org.bukkit.command; public interface CommandSender {boolean hasPermission(String s); void sendMessage(String s);}',
'org/bukkit/command/CommandExecutor.java':'package org.bukkit.command; public interface CommandExecutor {boolean onCommand(CommandSender s,Command c,String l,String[] a);}',
'org/bukkit/command/PluginCommand.java':'package org.bukkit.command; public class PluginCommand { public void setExecutor(CommandExecutor e){} }',
'org/bukkit/inventory/Inventory.java':'package org.bukkit.inventory; public class Inventory {public Object getHolder(){return null;} }',
'org/bukkit/inventory/InventoryView.java':'package org.bukkit.inventory; public class InventoryView {public Inventory getTopInventory(){return new Inventory();}}',
'org/bukkit/entity/Player.java':'package org.bukkit.entity; public interface Player {org.bukkit.inventory.InventoryView getOpenInventory();}',
'ru/rooyzee/elytrixairdrop/gui/holders/LootEditorHolder.java':'package ru.rooyzee.elytrixairdrop.gui.holders; public class LootEditorHolder {}',
'ru/rooyzee/elytrixairdrop/services/LootFileService.java':'package ru.rooyzee.elytrixairdrop.services; import java.util.*; import org.bukkit.inventory.ItemStack; public class LootFileService {public boolean hasAnyLootFiles(){return false;} public boolean isReadOnly(){return false;} public String installPreset(List<List<ItemStack>> l){return null;} }',
'ru/rooyzee/elytrixairdrop/Main.java':'package ru.rooyzee.elytrixairdrop; import java.util.logging.*; import org.bukkit.command.*; import ru.rooyzee.elytrixairdrop.services.*; public class Main {public Manager getAirdropManager(){return new Manager();} public PluginCommand getCommand(String s){return new PluginCommand();} public Logger getLogger(){return Logger.getAnonymousLogger();} public static class Manager {public LootFileService getLootFileService(){return new LootFileService();} public Object getActive(){return null;}}}',
'ru/rooyzee/elytrixitem/item/CustomItem.java':'package ru.rooyzee.elytrixitem.item; public interface CustomItem {org.bukkit.inventory.ItemStack createItem(); default boolean isStackable(){return true;}}',
'ru/rooyzee/elytrixitem/Main.java':'''package ru.rooyzee.elytrixitem; import org.bukkit.*; import org.bukkit.inventory.*; import org.bukkit.plugin.*; import ru.rooyzee.elytrixitem.item.*; public class Main implements Plugin {public boolean enabled=true; public boolean isEnabled(){return enabled;} public Registry getItemRegistry(){return new Registry();} public static class Registry {public CustomItem get(String id){if(id.equals("missing"))return null;return new CustomItem(){public ItemStack createItem(){ItemStack x=new ItemStack(Material.DIAMOND);x.id=id;return x;}};}}}''',
'TestPreset.java':'''import java.util.*; import org.bukkit.*; import org.bukkit.enchantments.*; import org.bukkit.inventory.*; import ru.rooyzee.elytrixairdrop.patch.LootPreset; public class TestPreset {public static void main(String[] args)throws Exception {Bukkit.pm.plugin=new ru.rooyzee.elytrixitem.Main(); java.lang.reflect.Constructor<LootPreset> c=LootPreset.class.getDeclaredConstructor(ru.rooyzee.elytrixairdrop.Main.class);c.setAccessible(true);LootPreset preset=c.newInstance(new ru.rooyzee.elytrixairdrop.Main()); List<List<ItemStack>> pages=preset.create();int[] sizes={26,24,20,18}; for(int i=0;i<4;i++)if(pages.get(i).size()!=sizes[i])throw new AssertionError("page "+i+" size "+pages.get(i).size()); for(Material m:new Material[]{Material.DIAMOND_HELMET,Material.DIAMOND_CHESTPLATE,Material.DIAMOND_LEGGINGS,Material.DIAMOND_BOOTS})if(pages.get(0).stream().noneMatch(x->x.type==m&&Integer.valueOf(4).equals(x.ench.get(Enchantment.PROTECTION))))throw new AssertionError("page 1 missing protection IV "+m); if(pages.get(0).stream().anyMatch(x->x.type.name().startsWith("IRON_")))throw new AssertionError("iron filler");for(Material m:new Material[]{Material.DIAMOND_HELMET,Material.DIAMOND_CHESTPLATE,Material.DIAMOND_LEGGINGS,Material.DIAMOND_BOOTS})if(pages.get(1).stream().noneMatch(x->x.type==m&&Integer.valueOf(4).equals(x.ench.get(Enchantment.PROTECTION))&&x.ench.containsKey(Enchantment.MENDING)))throw new AssertionError("upgrade armor");for(Material m:new Material[]{Material.NETHERITE_HELMET,Material.NETHERITE_CHESTPLATE,Material.NETHERITE_LEGGINGS,Material.NETHERITE_BOOTS})if(pages.get(2).stream().noneMatch(x->x.type==m&&Integer.valueOf(4).equals(x.ench.get(Enchantment.PROTECTION))))throw new AssertionError("netherite armor"); long custom=pages.stream().flatMap(List::stream).filter(x->x.id!=null).count();if(custom!=33)throw new AssertionError("custom count "+custom); Bukkit.pm.plugin=null;try{preset.create();throw new AssertionError("missing plugin accepted");}catch(IllegalStateException expected){} System.out.println("Preset stub test: 26/24/20/18 entries, 33 custom, protection IV, absent plugin OK");}}'''
}
java=os.environ['JAVA']; ecj=os.environ['ECJ']
with tempfile.TemporaryDirectory() as tmp:
 p=pathlib.Path(tmp)
 for name,content in files.items():
  f=p/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(content)
 classes=p/'classes';classes.mkdir()
 sources=[str(p/name) for name in files]+[str(src)]
 subprocess.run([java,'-jar',ecj,'-21','-proc:none','-nowarn','-d',str(classes),*sources],check=True)
 subprocess.run([java,'-Xverify:all','-cp',str(classes),'TestPreset'],check=True)
