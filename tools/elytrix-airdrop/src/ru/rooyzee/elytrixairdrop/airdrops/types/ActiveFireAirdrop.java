package ru.rooyzee.elytrixairdrop.airdrops.types;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.airdrops.ActiveAirdrop;
import ru.rooyzee.elytrixairdrop.airdrops.Airdrop;
import ru.rooyzee.elytrixairdrop.managers.SchematicManager;
import ru.rooyzee.elytrixairdrop.patch.FireSchedule;
import ru.rooyzee.elytrixairdrop.patch.LootRuntime;
import ru.rooyzee.elytrixairdrop.patch.NearbyRewards;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** One central meteor and a bounded, dynamically paced shower. */
public final class ActiveFireAirdrop extends ActiveAirdrop {
 private static final long COOLING_MS=180_000L;
 private final List<Location> surface=new ArrayList<>();
 private final List<Flight> flights=new ArrayList<>();
 private final List<BlockDisplay> landedDisplays=new ArrayList<>();
 private final Map<Integer,ItemStack> rewards=new HashMap<>();
 private final Map<UUID,Long> cooldowns=new HashMap<>();
 private Main plugin;
 private BukkitTask animationTask;
 private Inventory loot;
 private Location centerLanding,chestLocation;
 private long enteredAt,landedAt,unlockAt,lastSmallStart;
 private int smallStarted;
 private Phase phase=Phase.WAITING;
 private boolean looted;
 private enum Phase { WAITING, FALLING, COOLING, OPEN }

 public ActiveFireAirdrop(Location center,String region,String hologram,Map<Location,SchematicManager.BlockData> backup,long endTime){super(Airdrop.FIRE,center,region,hologram,backup,endTime);}
 @Override public void onStart(Main plugin,ConfigurationSection config){
  this.plugin=plugin;findSurface();updateHologram(config);
  if(centerLanding==null){plugin.getLogger().severe("Fire airdrop: no solid schematic surface found");Bukkit.getScheduler().runTask(plugin,()->{if(plugin.getAirdropManager().getActive()==this)plugin.getAirdropManager().stop(false);});return;}
  animationTask=Bukkit.getScheduler().runTaskTimer(plugin,this::animate,1L,1L);
 }
 @Override public void onTick(Main plugin,ConfigurationSection config){
  long now=System.currentTimeMillis();
  if(phase==Phase.WAITING && centerLanding!=null && hasRegionPlayer()){
   enteredAt=now;phase=Phase.FALLING;startFlight(centerLanding,true);
  }
  if(phase==Phase.COOLING){
   if(now>=unlockAt){phase=Phase.OPEN;createLoot();}
   else if(now>=enteredAt+10_000L){
    int nearby=NearbyRewards.players(getCenter());
    int limit=FireSchedule.target(nearby);
    long interval=FireSchedule.intervalMillis(nearby,smallStarted,unlockAt-now);
    if(smallStarted<limit && now+1500L<unlockAt && (smallStarted==0 || now-lastSmallStart>=interval)){
     Location target=surface.get(ThreadLocalRandom.current().nextInt(surface.size()));
     startFlight(target,false);smallStarted++;lastSmallStart=now;
    }
   }
  }
  updateHologram(config);
 }
 @Override public void onStop(Main plugin,ConfigurationSection config){
  if(animationTask!=null){animationTask.cancel();animationTask=null;}
  for(Flight f:flights)f.remove();flights.clear();
  for(BlockDisplay d:landedDisplays)d.remove();landedDisplays.clear();
  if(loot!=null)for(org.bukkit.entity.HumanEntity viewer:new ArrayList<>(loot.getViewers()))viewer.closeInventory();
  rewards.clear();cooldowns.clear();
 }
 private boolean hasRegionPlayer(){
  World world=getCenter().getWorld();if(world==null||getRegionId()==null)return false;
  for(Player player:world.getPlayers())if(player.getGameMode()!=GameMode.SPECTATOR && plugin.getRegionManager().isInRegion(player.getLocation(),getRegionId()))return true;
  return false;
 }
 private void findSurface(){
  World world=getCenter().getWorld();if(world==null)return;
  Map<Long,Location> columns=new HashMap<>();
  for(Location key:getBackup().keySet()){
   if(!world.equals(key.getWorld()) || !key.getBlock().getType().isSolid())continue;
   int x=key.getBlockX(),z=key.getBlockZ();long id=((long)x<<32)^(z&0xffffffffL);
   Location old=columns.get(id);if(old==null || key.getBlockY()>old.getBlockY())columns.put(id,key.getBlock().getLocation());
  }
  surface.addAll(columns.values());double best=Double.MAX_VALUE;
  for(Location loc:surface){double dx=loc.getBlockX()-getCenter().getBlockX(),dz=loc.getBlockZ()-getCenter().getBlockZ(),d=dx*dx+dz*dz;if(d<best){best=d;centerLanding=loc;}}
 }
 private void startFlight(Location surfaceBlock,boolean large){
  World w=surfaceBlock.getWorld();if(w==null)return;
  Location end=surfaceBlock.clone().add(0.5,1.0,0.5);
  double angle=ThreadLocalRandom.current().nextDouble(Math.PI*2);
  double side=large?25:12,height=large?50:32;
  Location start=end.clone().add(Math.cos(angle)*side,height,Math.sin(angle)*side);
  try{flights.add(new Flight(start,end,large));}
  catch(RuntimeException ex){plugin.getLogger().severe("Fire meteor could not be spawned: "+ex.getMessage());Bukkit.getScheduler().runTask(plugin,()->{if(plugin.getAirdropManager().getActive()==this)plugin.getAirdropManager().stop(false);});return;}
  w.playSound(end,large?Sound.ENTITY_ENDER_DRAGON_GROWL:Sound.ENTITY_BLAZE_SHOOT,large?0.65f:0.35f,large?0.65f:0.8f);
 }
 private void animate(){
  if(plugin.getAirdropManager().getActive()!=this)return;
  Iterator<Flight> it=flights.iterator();
  while(it.hasNext()){
   Flight f=it.next();if(!f.advance())continue;
   it.remove();
   if(f.large){landedDisplays.addAll(f.displays);landMain(f.end);}
   else{f.remove();impactSmall(f.end);}
  }
 }
 private void landMain(Location impact){
  if(phase!=Phase.FALLING)return;
  World w=impact.getWorld();if(w==null)return;
  landedAt=System.currentTimeMillis();unlockAt=landedAt+COOLING_MS;phase=Phase.COOLING;
  // Only write into air: never replace a schematic block or a tile entity.
  for(int dy=3;dy<=8;dy++){
   Block b=impact.clone().add(0,dy,0).getBlock();
   if(!b.getType().isAir())continue;
   Location key=b.getLocation();getBackup().putIfAbsent(key,new SchematicManager.BlockData(b.getType(),b.getBlockData().getAsString()));
   b.setType(Material.CHEST,false);chestLocation=key;break;
  }
  if(chestLocation==null){plugin.getLogger().severe("Fire airdrop: no empty place for meteor chest");Bukkit.getScheduler().runTask(plugin,()->{if(plugin.getAirdropManager().getActive()==this)plugin.getAirdropManager().stop(false);});return;}
  w.playSound(impact,Sound.ENTITY_GENERIC_EXPLODE,0.9f,0.65f);
  w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,impact,8,1.0,0.3,1.0,0.02);
 }
 private void impactSmall(Location impact){
  World world=impact.getWorld();if(world==null)return;
  world.playSound(impact,Sound.BLOCK_ANVIL_LAND,0.45f,0.7f);
  world.spawnParticle(Particle.SMOKE,impact,5,0.4,0.2,0.4,0.02);
  for(Player player:world.getPlayers()){
   if(player.getGameMode()==GameMode.CREATIVE||player.getGameMode()==GameMode.SPECTATOR||player.isDead())continue;
   if(player.getLocation().distanceSquared(impact)<=16.0)player.damage(5.0);
  }
  int nearby=NearbyRewards.players(getCenter());
  List<ItemStack> drops=plugin.getAirdropManager().getLootFileService().rollRewards(nearby,1);
  if(!drops.isEmpty())world.dropItemNaturally(impact.clone().add(0,0.5,0),drops.get(0));
 }
 private void createLoot(){
  int nearby=NearbyRewards.players(getCenter());int count=5*LootRuntime.perContainer(this);
  List<ItemStack> selected=plugin.getAirdropManager().getLootFileService().rollRewards(nearby,count);
  loot=Bukkit.createInventory(null,54,ColorUtil.colorize("&f☁ &#F8BEFBАирдроп &7» &cОгненный"));
  LootRuntime.track(loot);List<Integer> slots=new ArrayList<>();for(int i=0;i<54;i++)slots.add(i);Collections.shuffle(slots);
  ItemStack mask=new ItemStack(Material.BLACK_DYE);ItemMeta meta=mask.getItemMeta();meta.setDisplayName(ColorUtil.colorize("&#F8BEFB???"));mask.setItemMeta(meta);
  for(int i=0;i<selected.size()&&i<slots.size();i++){int slot=slots.get(i);rewards.put(slot,selected.get(i));loot.setItem(slot,mask.clone());}
 }
 @Override public boolean handleChestClick(Main plugin,Player player,Location block){
  if(chestLocation==null||!sameBlock(block,chestLocation))return false;
  if(player.getLocation().distanceSquared(chestLocation)>64){player.sendMessage(ColorUtil.colorize("&cСлишком далеко от сундука."));return true;}
  if(phase!=Phase.OPEN){player.sendMessage(ColorUtil.colorize("&cМетеорит ещё остывает."));return true;}
  if(looted||loot==null||rewards.isEmpty()){player.sendMessage(ColorUtil.colorize("&7Лут уже забрали."));return true;}
  player.openInventory(loot);return true;
 }
 @Override public boolean handleLootClick(Main plugin,Player player,InventoryClickEvent event){
  if(loot==null||event.getInventory()!=loot)return false;
  if(event.getClickedInventory()!=loot){if(event.getAction()==InventoryAction.MOVE_TO_OTHER_INVENTORY||event.getAction()==InventoryAction.COLLECT_TO_CURSOR)event.setCancelled(true);return true;}
  event.setCancelled(true);long now=System.currentTimeMillis();
  if(now-cooldowns.getOrDefault(player.getUniqueId(),0L)<500L)return true;
  int slot=event.getSlot();ItemStack item=rewards.remove(slot);if(item==null)return true;
  loot.setItem(slot,null);cooldowns.put(player.getUniqueId(),now);
  for(ItemStack overflow:player.getInventory().addItem(item.clone()).values())player.getWorld().dropItemNaturally(player.getLocation(),overflow);
  player.playSound(player.getLocation(),Sound.ENTITY_ITEM_PICKUP,0.5f,1.4f);
  if(rewards.isEmpty()){
   looted=true;for(org.bukkit.entity.HumanEntity viewer:new ArrayList<>(loot.getViewers()))viewer.closeInventory();
   Bukkit.getScheduler().runTaskLater(plugin,()->{if(plugin.getAirdropManager().getActive()==this)plugin.getAirdropManager().stop(true);},200L);
  }
  return true;
 }
 @Override public boolean isProtectedBlock(Location location){return super.isProtectedBlock(location)||(chestLocation!=null&&sameBlock(chestLocation,location));}
 public boolean isBreakable(Location location){return false;}
 public void onBreakableBroken(Main plugin,Location location){}
 public boolean isMeteorArea(Location location){return chestLocation!=null&&location!=null&&location.getWorld()!=null&&location.getWorld().equals(chestLocation.getWorld())&&location.distanceSquared(chestLocation)<=16;}
 private String statusLine(){
  if(phase==Phase.WAITING)return "&7Ожидание игроков";
  if(phase==Phase.FALLING)return "&cМетеорит падает";
  if(phase==Phase.COOLING){long s=Math.max(0,(unlockAt-System.currentTimeMillis()+999)/1000);return "&fОстывание: &#F8BEFB"+(s/60)+"м "+(s%60)+"с";}
  return looted?"&7Лут забран":"&aСундук открыт";
 }
 private void updateHologram(ConfigurationSection config){
  if(config==null)return;List<String> lines=new ArrayList<>();
  for(String line:config.getStringList("hologram"))lines.add(line.replace("%status%",statusLine()));
  plugin.getHologramManager().updateHologram(getHologramName(),lines);
 }
 private final class Flight {
  final Location start,end;final boolean large;final List<BlockDisplay> displays=new ArrayList<>();final List<Vector> offsets=new ArrayList<>();int age;
  Flight(Location start,Location end,boolean large){
   this.start=start;this.end=end;this.large=large;
   World world=start.getWorld();if(world==null)return;
   int radius=large?2:1;
   try { for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
    if(x*x+y*y+z*z>radius*radius+1)continue;
    if(!large && x*x+y*y+z*z>1)continue;
    Vector offset=new Vector(x,y,z);Material material=ThreadLocalRandom.current().nextInt(4)==0?Material.MAGMA_BLOCK:Material.BLACKSTONE;
    BlockDisplay display=world.spawn(start.clone().add(offset),BlockDisplay.class,d->{d.setBlock(material.createBlockData());d.setPersistent(false);d.setInterpolationDuration(2);d.setTeleportDuration(2);});
    offsets.add(offset);displays.add(display);
   }} catch(RuntimeException ex){remove();throw ex;}
  }
  boolean advance(){
   age++;int duration=large?40:30;double t=Math.min(1.0,age/(double)duration);double progress=t*t;
   if(age%2==0||age>=duration){
    Location loc=start.clone().add(end.toVector().subtract(start.toVector()).multiply(progress));
    for(int i=0;i<displays.size();i++)displays.get(i).teleport(loc.clone().add(offsets.get(i)));
    if(age%6==0&&loc.getWorld()!=null)loc.getWorld().spawnParticle(Particle.SMOKE,loc,large?3:1,0.25,0.25,0.25,0.01);
   }
   return age>=duration;
  }
  void remove(){for(BlockDisplay d:displays)d.remove();displays.clear();}
 }
}
