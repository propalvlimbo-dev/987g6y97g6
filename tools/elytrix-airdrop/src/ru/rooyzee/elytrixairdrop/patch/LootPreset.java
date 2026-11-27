package ru.rooyzee.elytrixairdrop.patch;

import java.lang.reflect.Method;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.gui.holders.LootEditorHolder;
import ru.rooyzee.elytrixairdrop.services.LootFileService;

/** Curated shared preset. Never overwrite a user's pool without explicit confirmation. */
public final class LootPreset implements CommandExecutor {
 private final Main plugin;
 private LootPreset(Main plugin){this.plugin=plugin;}
 public static void install(Main plugin){
  LootPreset preset=new LootPreset(plugin);
  Objects.requireNonNull(plugin.getCommand("airdroplootpreset")).setExecutor(preset);
  Bukkit.getScheduler().runTask(plugin,()->{
   LootFileService service=plugin.getAirdropManager().getLootFileService();
   if(!service.hasAnyLootFiles() && !service.isReadOnly() && plugin.getAirdropManager().getActive()==null && !editorOpen()){
    try{service.installPreset(preset.create());plugin.getLogger().info("Installed shared loot preset for a fresh setup.");}
    catch(Exception ex){plugin.getLogger().warning("Default loot not installed: "+ex.getMessage()+". Enable ElytrixItem, then use /airdroplootpreset confirm.");}
   }
  });
 }
 @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(!sender.hasPermission("elytrixairdrop.admin")){sender.sendMessage(ChatColor.RED+"Нет прав.");return true;}
  if(args.length!=1 || !args[0].equalsIgnoreCase("confirm")){
   sender.sendMessage(ChatColor.YELLOW+"/airdroplootpreset confirm — заменить общий лут подготовленным набором. Текущий common.yml будет сохранён в резервную копию.");return true;
  }
  if(plugin.getAirdropManager().getActive()!=null){sender.sendMessage(ChatColor.RED+"Сначала остановите активный аирдроп.");return true;}
  if(editorOpen()){sender.sendMessage(ChatColor.RED+"Сначала закройте редактор лута у всех игроков.");return true;}
  try{
   LootFileService service=plugin.getAirdropManager().getLootFileService();
   if(service.isReadOnly())throw new IllegalStateException("Текущий файл лута не прочитан; проверьте ошибки в консоли.");
   List<List<ItemStack>> data=create();String backup=service.installPreset(data);
   sender.sendMessage(ChatColor.GREEN+"Общий лут установлен: "+data.get(0).size()+" / "+data.get(1).size()+" / "+data.get(2).size()+" / "+data.get(3).size()+" предметов по страницам.");
   if(backup!=null)sender.sendMessage(ChatColor.YELLOW+"Копия прежнего лута: "+backup);
  }catch(Exception ex){sender.sendMessage(ChatColor.RED+"Лут не установлен: "+ex.getMessage());plugin.getLogger().warning("Loot preset was not installed: "+ex);}
  return true;
 }
 private static boolean editorOpen(){for(Player p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory().getHolder() instanceof LootEditorHolder)return true;return false;}
 public List<List<ItemStack>> create() throws Exception {
  Plugin itemPlugin=Bukkit.getPluginManager().getPlugin("ElytrixItem");
  if(itemPlugin==null || !itemPlugin.isEnabled())throw new IllegalStateException("ElytrixItem не включён");
  Object registry=itemPlugin.getClass().getMethod("getItemRegistry").invoke(itemPlugin);
  Method get=registry.getClass().getMethod("get",String.class);
  Class<?> iface=Class.forName("ru.rooyzee.elytrixitem.item.CustomItem",true,itemPlugin.getClass().getClassLoader());
  Method make=iface.getMethod("createItem"),stackable=iface.getMethod("isStackable");
  List<List<ItemStack>> p=new ArrayList<>();for(int i=0;i<4;i++)p.add(new ArrayList<>());
  // Page 1: PvP-ready diamond Protection IV, not starter iron gear.
  armor(p,1,Material.DIAMOND_HELMET,Material.DIAMOND_CHESTPLATE,Material.DIAMOND_LEGGINGS,Material.DIAMOND_BOOTS,false);
  gear(p,1,Material.DIAMOND_SWORD,Enchantment.SHARPNESS,5,Enchantment.UNBREAKING,3);
  gear(p,1,Material.DIAMOND_AXE,Enchantment.SHARPNESS,5,Enchantment.UNBREAKING,3);
  gear(p,1,Material.DIAMOND_PICKAXE,Enchantment.EFFICIENCY,5,Enchantment.UNBREAKING,3);
  gear(p,1,Material.BOW,Enchantment.POWER,5,Enchantment.UNBREAKING,3);
  add(p,1,Material.TOTEM_OF_UNDYING,1);add(p,1,Material.GOLDEN_APPLE,12);add(p,1,Material.ENDER_PEARL,12);
  add(p,1,Material.EXPERIENCE_BOTTLE,48);add(p,1,Material.END_CRYSTAL,12);add(p,1,Material.OBSIDIAN,32);
  add(p,1,Material.COBWEB,16);add(p,1,Material.GOLDEN_CARROT,32);add(p,1,Material.DIAMOND,12);add(p,1,Material.FIREWORK_ROCKET,32);
  potion(p,1,PotionType.LONG_FIRE_RESISTANCE,false);potion(p,1,PotionType.STRONG_SWIFTNESS,false);potion(p,1,PotionType.STRONG_STRENGTH,false);
  potion(p,1,PotionType.STRONG_HEALING,true);potion(p,1,PotionType.STRONG_REGENERATION,false);potion(p,1,PotionType.LONG_INVISIBILITY,false);
  custom(p,1,get,registry,make,stackable,"anti_pearl",1);custom(p,1,get,registry,make,stackable,"explosive_trap",1);
  // Page 2: upgraded Protection IV armour, fighting gear and custom potions.
  armor(p,2,Material.DIAMOND_HELMET,Material.DIAMOND_CHESTPLATE,Material.DIAMOND_LEGGINGS,Material.DIAMOND_BOOTS,true);
  gear(p,2,Material.DIAMOND_SWORD,Enchantment.SHARPNESS,5,Enchantment.FIRE_ASPECT,2);
  gear(p,2,Material.DIAMOND_PICKAXE,Enchantment.EFFICIENCY,5,Enchantment.FORTUNE,3);
  gear(p,2,Material.BOW,Enchantment.POWER,5,Enchantment.FLAME,1);
  add(p,2,Material.TOTEM_OF_UNDYING,1);add(p,2,Material.GOLDEN_APPLE,24);add(p,2,Material.ENDER_PEARL,16);
  add(p,2,Material.DIAMOND,24);add(p,2,Material.NETHERITE_SCRAP,4);add(p,2,Material.END_CRYSTAL,24);
  potion(p,2,PotionType.STRONG_SWIFTNESS,false);potion(p,2,PotionType.STRONG_STRENGTH,false);potion(p,2,PotionType.STRONG_REGENERATION,false);
  potion(p,2,PotionType.STRONG_HEALING,true);potion(p,2,PotionType.LONG_INVISIBILITY,false);
  for(String id:new String[]{"potion_healt","potion_lightness","potion_voina","potion_disappearances","potion_sportsmena","potion_salvation"})custom(p,2,get,registry,make,stackable,id,1);
  // Page 3: netherite Protection IV and unusual Elytrix ammunition and items.
  armor(p,3,Material.NETHERITE_HELMET,Material.NETHERITE_CHESTPLATE,Material.NETHERITE_LEGGINGS,Material.NETHERITE_BOOTS,true);
  gear(p,3,Material.NETHERITE_SWORD,Enchantment.SHARPNESS,5,Enchantment.UNBREAKING,3);
  gear(p,3,Material.NETHERITE_PICKAXE,Enchantment.EFFICIENCY,5,Enchantment.FORTUNE,3);
  gear(p,3,Material.BOW,Enchantment.POWER,5,Enchantment.INFINITY,1);
  add(p,3,Material.NETHERITE_INGOT,2);add(p,3,Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,1);add(p,3,Material.ENCHANTED_GOLDEN_APPLE,1);
  for(String id:new String[]{"arrow1","arrow2","arrow3","arrow5","arrow_levit"})custom(p,3,get,registry,make,stackable,id,8);
  for(String id:new String[]{"totem1","totem2","totem3","shar1","shar2"})custom(p,3,get,registry,make,stackable,id,1);
  // Page 4: valuable permanent shop items are deliberately in the rarest tier.
  for(String id:new String[]{"tyagun","collector_sword","infinite_firework","experience_pot","kirkaraskola","shalker1","shalker2","shalker3","shalker4","shalker5","elytra1","shar3","shar4","totem4","totem5"})custom(p,4,get,registry,make,stackable,id,1);
  gear(p,4,Material.NETHERITE_SWORD,Enchantment.SHARPNESS,5,Enchantment.UNBREAKING,3);
  add(p,4,Material.NETHERITE_INGOT,3);add(p,4,Material.ENCHANTED_GOLDEN_APPLE,2);
  for(List<ItemStack> page:p)if(page.isEmpty()||page.size()>45)throw new IllegalStateException("Invalid loot page size: "+page.size());
  return p;
 }
 private static void add(List<List<ItemStack>> p,int page,Material type,int amount){ItemStack item=new ItemStack(type);if(amount<1||amount>item.getMaxStackSize())throw new IllegalStateException("Invalid stack: "+type);item.setAmount(amount);p.get(page-1).add(item);}
 private static void gear(List<List<ItemStack>> p,int page,Material type,Enchantment first,int a,Enchantment second,int b){ItemStack item=new ItemStack(type);item.addEnchantment(first,a);item.addEnchantment(second,b);p.get(page-1).add(item);}
 private static void armor(List<List<ItemStack>> p,int page,Material helmet,Material chest,Material leggings,Material boots,boolean mending){
  for(Material m:new Material[]{helmet,chest,leggings,boots}){
   ItemStack item=new ItemStack(m);item.addEnchantment(Enchantment.PROTECTION,4);item.addEnchantment(Enchantment.UNBREAKING,3);
   if(mending)item.addEnchantment(Enchantment.MENDING,1);
   if(m==boots)item.addEnchantment(Enchantment.FEATHER_FALLING,4);
   p.get(page-1).add(item);
  }
 }
 private static void potion(List<List<ItemStack>> p,int page,PotionType type,boolean splash){ItemStack item=new ItemStack(splash?Material.SPLASH_POTION:Material.POTION);PotionMeta meta=(PotionMeta)item.getItemMeta();meta.setBasePotionType(type);item.setItemMeta(meta);p.get(page-1).add(item);}
 private static void custom(List<List<ItemStack>> p,int page,Method get,Object registry,Method make,Method stackable,String id,int count)throws Exception{
  Object entry=get.invoke(registry,id);if(entry==null)throw new IllegalStateException("ElytrixItem: неизвестный предмет "+id);
  ItemStack item=(ItemStack)make.invoke(entry);if(item==null || item.getType()==Material.AIR)throw new IllegalStateException("ElytrixItem вернул пустой предмет: "+id);
  if(count>1 && !Boolean.TRUE.equals(stackable.invoke(entry)))throw new IllegalStateException("ElytrixItem не позволяет складывать: "+id);
  if(count>item.getMaxStackSize())throw new IllegalStateException("Слишком большая пачка: "+id);
  item=item.clone();item.setAmount(count);p.get(page-1).add(item);
 }
}
