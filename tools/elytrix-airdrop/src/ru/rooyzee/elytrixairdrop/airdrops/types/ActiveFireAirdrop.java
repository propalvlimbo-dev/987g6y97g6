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
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
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
import ru.rooyzee.elytrixairdrop.patch.MeteorGeometry;
import ru.rooyzee.elytrixairdrop.patch.LootRuntime;
import ru.rooyzee.elytrixairdrop.patch.NearbyRewards;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;

/** One central meteor and a bounded, dynamically paced shower. */
public final class ActiveFireAirdrop extends ActiveAirdrop {
 private static final long COOLING_MS=180_000L;
 private final List<Location> surface=new ArrayList<>();
 private final List<Flight> flights=new ArrayList<>();
 private final List<Debris> debris=new ArrayList<>();
 private static final int RADIUS=MeteorGeometry.RADIUS;
 private final List<Location> meteorBlocks=new ArrayList<>();
 private final List<Location> coolingBlocks=new ArrayList<>();
 private final Map<Integer,ItemStack> rewards=new HashMap<>();
 private final Map<UUID,Long> cooldowns=new HashMap<>();
 private Main plugin;
 private BukkitTask animationTask;
 private Inventory loot;
 private Location centerLanding,meteorCenter,chestLocation;
 private long enteredAt,landedAt,unlockAt,lastSmallStart;
 private int smallStarted,cooled;
 private boolean firstInPairReward;
 private Phase phase=Phase.WAITING;
 private boolean looted;
 private enum Phase { WAITING, FALLING, COOLING, OPEN }

 public ActiveFireAirdrop(Location center,String region,String hologram,Map<Location,SchematicManager.BlockData> backup,long endTime){super(Airdrop.FIRE,center,region,hologram,backup,endTime);}
 @Override public void onStart(Main plugin,ConfigurationSection config){
  this.plugin=plugin;findSurface();findMeteorSpace();
  if(meteorCenter!=null)plugin.getHologramManager().moveHologram(getHologramName(),meteorCenter.clone().add(0.5,MeteorGeometry.hologramAboveCenter(),0.5));
  updateHologram(config);
  if(centerLanding==null || meteorCenter==null){plugin.getLogger().severe("Fire airdrop: no safe air space for the meteor above the schematic");Bukkit.getScheduler().runTask(plugin,()->{if(plugin.getAirdropManager().getActive()==this)plugin.getAirdropManager().stop(false);});return;}
  animationTask=Bukkit.getScheduler().runTaskTimer(plugin,this::animate,1L,1L);
 }
 @Override public void onTick(Main plugin,ConfigurationSection config){
  long now=System.currentTimeMillis();
  if(phase==Phase.WAITING && centerLanding!=null && hasRegionPlayer()){
   enteredAt=now;phase=Phase.FALLING;startFlight(centerLanding,true,false);
  }
  if(phase==Phase.COOLING){
   coolMeteor(now);
   if(now>=unlockAt){phase=Phase.OPEN;createLoot();}
   else if(now>=enteredAt+10_000L){
    int nearby=NearbyRewards.players(getCenter());
    int limit=FireSchedule.target(nearby);
    long interval=FireSchedule.intervalMillis(nearby,smallStarted,unlockAt-now);
    if(smallStarted<limit && now+3500L<unlockAt && (smallStarted==0 || now-lastSmallStart>=interval)){
     Location target=chooseSmallTarget();
     if(target==null)return;
     if(smallStarted%2==0)firstInPairReward=ThreadLocalRandom.current().nextBoolean();
     boolean award=FireSchedule.carriesLoot(smallStarted,firstInPairReward);
     startFlight(target,false,award);smallStarted++;lastSmallStart=now;
    }
   }
  }
  updateHologram(config);
 }
 @Override public void onStop(Main plugin,ConfigurationSection config){
  if(animationTask!=null){animationTask.cancel();animationTask=null;}
  for(Flight f:flights)f.remove();flights.clear();
  for(Debris d:debris)d.display.remove();debris.clear();
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
 private void findMeteorSpace(){
  if(centerLanding==null)return;
  // Try the most central surface first. The sphere intersects three layers of that surface.
  List<Location> candidates=new ArrayList<>(surface);
  candidates.sort(Comparator.comparingDouble(loc->{double x=loc.getX()-getCenter().getBlockX(),z=loc.getZ()-getCenter().getBlockZ();return x*x+z*z;}));
  for(Location landing:candidates){
   double dx=landing.getX()-getCenter().getBlockX(),dz=landing.getZ()-getCenter().getBlockZ();
   if(dx*dx+dz*dz>144)break;
   Location candidate=landing.clone().add(0,MeteorGeometry.centerAboveSurface(),0).getBlock().getLocation();
   if(candidate.getBlockY()+RADIUS>=candidate.getWorld().getMaxHeight() || candidate.getBlockY()-RADIUS<candidate.getWorld().getMinHeight())continue;
   if(canPlaceMeteor(candidate,landing.getBlockY())){centerLanding=landing;meteorCenter=candidate;return;}
  }
 }
 private boolean canPlaceMeteor(Location center,int surfaceY){
  int embeddedBlocks=0,belowSurface=0;
  for(int x=-RADIUS;x<=RADIUS;x++)for(int y=-RADIUS;y<=RADIUS;y++)for(int z=-RADIUS;z<=RADIUS;z++){
   if(!MeteorGeometry.contains(x,y,z))continue;
   Location loc=center.clone().add(x,y,z).getBlock().getLocation();Block block=loc.getBlock();
   if(block.getType().isAir())continue;
   // Only replace known backed-up, solid blocks in the three upper impact layers.
   // Never touch inventories or other tile entities, liquids, or blocks above the surface.
   if(loc.getBlockY()>surfaceY || loc.getBlockY()<surfaceY-2 || !getBackup().containsKey(loc)
       || !block.getType().isSolid() || block.getState() instanceof TileState)return false;
   embeddedBlocks++;if(loc.getBlockY()<surfaceY)belowSurface++;
  }
  // A thin pillar or a hollow space is not a convincing embedded impact.
  if(embeddedBlocks<8 || belowSurface<1)return false;
  Block anchor=centerLandingFor(center,surfaceY).getBlock();
  return anchor.getType().isSolid() && !(anchor.getState() instanceof TileState) && getBackup().containsKey(anchor.getLocation());
 }
 private Location centerLandingFor(Location center,int surfaceY){return center.clone().add(0,surfaceY-center.getBlockY(),0);}
 private Location chooseSmallTarget(){
  World world=getCenter().getWorld();List<Player> targets=new ArrayList<>();
  for(Player p:world.getPlayers())if(p.getGameMode()!=GameMode.CREATIVE && p.getGameMode()!=GameMode.SPECTATOR && !p.isDead() && p.getLocation().distanceSquared(getCenter())<=400)targets.add(p);
  if(!targets.isEmpty()){
   Player chosen=targets.get(ThreadLocalRandom.current().nextInt(targets.size()));Location loc=chosen.getLocation();
   Location under=loc.clone().subtract(0,0.2,0).getBlock().getLocation();
   if(meteorBlocks.contains(under) && under.clone().add(0,1,0).getBlock().getType().isAir())return under;
   double tx=loc.getX()+ThreadLocalRandom.current().nextDouble(-1.5,1.5),tz=loc.getZ()+ThreadLocalRandom.current().nextDouble(-1.5,1.5);
   Location best=null;double score=Double.MAX_VALUE;
   for(Location candidate:surface){
    if(!candidate.clone().add(0,1,0).getBlock().getType().isAir())continue;
    double dx=candidate.getX()+0.5-tx,dz=candidate.getZ()+0.5-tz,dy=Math.abs(candidate.getY()+1-loc.getY());double d=dx*dx+dz*dz+dy*dy*2;
    if(d<score){score=d;best=candidate;}}
   if(best!=null)return best;
  }
  List<Location> open=new ArrayList<>();for(Location candidate:surface)if(candidate.clone().add(0,1,0).getBlock().getType().isAir())open.add(candidate);
  return open.isEmpty()?null:open.get(ThreadLocalRandom.current().nextInt(open.size()));
 }
 private void startFlight(Location surfaceBlock,boolean large,boolean award){
  World w=surfaceBlock.getWorld();if(w==null)return;
  Location end=large?meteorCenter.clone():surfaceBlock.clone().add(0.5,1.0,0.5);
  double angle=ThreadLocalRandom.current().nextDouble(Math.PI*2);
  double side=large?20:12,height=large?45:32;
  Location start=end.clone().add(Math.cos(angle)*side,height,Math.sin(angle)*side);
  try{flights.add(new Flight(start,end,large,award));}
  catch(RuntimeException ex){plugin.getLogger().severe("Fire meteor could not be spawned: "+ex.getMessage());Bukkit.getScheduler().runTask(plugin,()->{if(plugin.getAirdropManager().getActive()==this)plugin.getAirdropManager().stop(false);});return;}
  w.playSound(end,large?Sound.ENTITY_ENDER_DRAGON_GROWL:Sound.ENTITY_BLAZE_SHOOT,large?0.65f:0.35f,large?0.65f:0.8f);
 }
 private void animate(){
  if(plugin.getAirdropManager().getActive()!=this)return;
  Iterator<Debris> fragments=debris.iterator();
  while(fragments.hasNext()){Debris d=fragments.next();if(d.advance()){d.display.remove();fragments.remove();}}
  Iterator<Flight> it=flights.iterator();
  while(it.hasNext()){
   Flight f=it.next();if(!f.advance())continue;
   it.remove();
   if(f.large){f.remove();landMain(f.end);}
   else {f.shatter();impactSmall(f.end,f.award);}
  }
 }
 private void landMain(Location impact){
  if(phase!=Phase.FALLING || meteorCenter==null)return;
  World w=meteorCenter.getWorld();if(w==null)return;
  // Re-check immediately before impact: another plugin or player may have changed it.
  if(!canPlaceMeteor(meteorCenter,centerLanding.getBlockY())){
   plugin.getLogger().severe("Fire meteor impact space changed or contains unsafe blocks; cancelling without overwriting them.");
   Bukkit.getScheduler().runTask(plugin,()->{if(plugin.getAirdropManager().getActive()==this)plugin.getAirdropManager().stop(false);});return;
  }
  List<Location> planned=new ArrayList<>();
  for(int x=-RADIUS;x<=RADIUS;x++)for(int y=-RADIUS;y<=RADIUS;y++)for(int z=-RADIUS;z<=RADIUS;z++){
   if(MeteorGeometry.contains(x,y,z))planned.add(meteorCenter.clone().add(x,y,z).getBlock().getLocation());
  }
  // Back up the complete sphere before modifying any block. Never overwrite a tile entity.
  for(Location loc:planned){Block b=loc.getBlock();getBackup().putIfAbsent(loc,new SchematicManager.BlockData(b.getType(),b.getBlockData().getAsString()));}
  try {
   carveImpact(planned);
   chestLocation=meteorCenter.clone();
   for(Location loc:planned){
    if(loc.equals(chestLocation)){loc.getBlock().setType(Material.CHEST,false);continue;}
    loc.getBlock().setType(Material.MAGMA_BLOCK,false);meteorBlocks.add(loc);coolingBlocks.add(loc);
   }
  }catch(RuntimeException ex){plugin.getLogger().severe("Fire meteor placement failed: "+ex.getMessage());Bukkit.getScheduler().runTask(plugin,()->{if(plugin.getAirdropManager().getActive()==this)plugin.getAirdropManager().stop(false);});return;}
  Collections.shuffle(coolingBlocks);
  landedAt=System.currentTimeMillis();unlockAt=landedAt+COOLING_MS;phase=Phase.COOLING;
  w.playSound(impact,Sound.ENTITY_GENERIC_EXPLODE,0.9f,0.65f);
  w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,impact,8,1.0,0.3,1.0,0.02);
 }
 /** Carve a shallow impact crater only in solid blocks already covered by the event backup. */
 private void carveImpact(List<Location> meteor){
  Set<Location> occupied=new HashSet<>(meteor);
  int surfaceY=centerLanding.getBlockY();
  for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
   int distance=dx*dx+dz*dz;if(distance<16||distance>36)continue;
   for(int depth=0;depth<=1;depth++){
    if(depth==1&&distance>25)continue;
    Location loc=centerLanding.clone().add(dx,-depth,dz).getBlock().getLocation();
    Block block=loc.getBlock();
    if(loc.getBlockY()!=surfaceY-depth || occupied.contains(loc) || !getBackup().containsKey(loc)
       || !block.getType().isSolid() || block.getState() instanceof TileState)continue;
    block.setType(Material.AIR,false);
   }
  }
 }
 /** Fake flying fragments: visual displays only, never remove or drop the original blocks. */
 private void spawnDebris(Location impact){
  World world=impact.getWorld();if(world==null)return;
  List<Location> sources=new ArrayList<>();
  Location base=impact.clone().subtract(0,0.1,0).getBlock().getLocation();
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
   Location loc=base.clone().add(dx,0,dz);Block block=loc.getBlock();
   if((getBackup().containsKey(loc)||meteorBlocks.contains(loc)) && block.getType().isSolid() && !(block.getState() instanceof TileState))sources.add(loc);
  }
  Collections.shuffle(sources);
  for(int i=0;i<Math.min(6,sources.size());i++){
   Location source=sources.get(i);double angle=ThreadLocalRandom.current().nextDouble(Math.PI*2);
   double speed=ThreadLocalRandom.current().nextDouble(0.08,0.18);
   try{
    BlockDisplay display=world.spawn(source.clone(),BlockDisplay.class,d->{d.setBlock(source.getBlock().getBlockData());d.setPersistent(false);d.setTeleportDuration(1);});
    debris.add(new Debris(display,source.clone(),new Vector(Math.cos(angle)*speed,ThreadLocalRandom.current().nextDouble(0.18,0.29),Math.sin(angle)*speed)));
   }catch(RuntimeException ex){plugin.getLogger().warning("Unable to show meteor debris: "+ex.getMessage());}
  }
 }
 private final class Debris{
  final BlockDisplay display;final Location position;final Vector velocity;int age;
  Debris(BlockDisplay display,Location position,Vector velocity){this.display=display;this.position=position;this.velocity=velocity;}
  boolean advance(){
   if(++age>=12 || !display.isValid())return true;
   position.add(velocity);velocity.setY(velocity.getY()-0.04);display.teleport(position);return false;
  }
 }
 private void coolMeteor(long now){
  int total=coolingBlocks.size();long elapsed=now-landedAt;
  int goal=FireSchedule.cooledBlocks(total,elapsed,COOLING_MS);
  while(cooled<goal){Location loc=coolingBlocks.get(cooled++);if(loc.getBlock().getType()==Material.MAGMA_BLOCK)loc.getBlock().setType(Material.OBSIDIAN,false);}
 }
 private void impactSmall(Location impact,boolean award){
  World world=impact.getWorld();if(world==null)return;
  world.playSound(impact,Sound.ENTITY_GENERIC_EXPLODE,0.45f,1.25f);
  world.spawnParticle(Particle.EXPLOSION,impact,1,0,0,0,0);
  world.spawnParticle(Particle.SMOKE,impact,5,0.4,0.2,0.4,0.02);
  spawnDebris(impact);
  for(Player player:world.getPlayers()){
   if(player.getGameMode()==GameMode.CREATIVE||player.getGameMode()==GameMode.SPECTATOR||player.isDead())continue;
   if(player.getLocation().distanceSquared(impact)<=16.0)player.damage(9.0);
  }
  if(!award)return;
  int nearby=NearbyRewards.players(getCenter());
  List<ItemStack> drops=plugin.getAirdropManager().getLootFileService().rollRewards(nearby,1);
  if(!drops.isEmpty()){Item drop=world.dropItemNaturally(impact.clone().add(0,0.5,0),drops.get(0));drop.setGlowing(true);drop.setInvulnerable(true);}
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
 public boolean isBreakable(Location location){return phase==Phase.OPEN && location!=null && meteorBlocks.contains(blockKey(location)) && location.getBlock().getType()==Material.OBSIDIAN;}
 public void onBreakableBroken(Main plugin,Location location){}
 public boolean isMeteorArea(Location location){return location!=null && meteorCenter!=null && location.getWorld()!=null && location.getWorld().equals(meteorCenter.getWorld()) && location.distanceSquared(meteorCenter)<=25;}
 private String statusLine(){
  if(phase==Phase.WAITING)return "&7Ожидание игроков";
  if(phase==Phase.FALLING)return "&cМетеорит падает";
  if(phase==Phase.COOLING){long s=Math.max(0,(unlockAt-System.currentTimeMillis()+999)/1000);return "&fОстывание: &#F8BEFB"+(s/60)+"м "+(s%60)+"с";}
  return looted?"&7Лут забран":"&aРазбейте обсидиан — сундук внутри";
 }
 public String hologramPhase(){return phase.name();}
 private void updateHologram(ConfigurationSection config){
  if(config==null)return;List<String> lines=new ArrayList<>();
  for(String line:config.getStringList("hologram"))lines.add(line.replace("%status%",statusLine()));
  plugin.getHologramManager().updateHologram(getHologramName(),lines);
 }
 private final class Flight {
  final Location start,end;final boolean large,award;final List<BlockDisplay> displays=new ArrayList<>();final List<Vector> offsets=new ArrayList<>();int age;
  Flight(Location start,Location end,boolean large,boolean award){
   this.start=start;this.end=end;this.large=large;this.award=award;
   World world=start.getWorld();if(world==null)return;
   int radius=large?RADIUS:1;
   try { for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
    if(large ? !MeteorGeometry.contains(x,y,z) : x*x+y*y+z*z>1)continue;
    Vector offset=new Vector(x,y,z);Material material=large?Material.MAGMA_BLOCK:(ThreadLocalRandom.current().nextInt(3)==0?Material.MAGMA_BLOCK:Material.BLACKSTONE);
    BlockDisplay display=world.spawn(start.clone().add(offset),BlockDisplay.class,d->{d.setBlock(material.createBlockData());d.setPersistent(false);d.setInterpolationDuration(2);d.setTeleportDuration(2);});
    offsets.add(offset);displays.add(display);
   }} catch(RuntimeException ex){remove();throw ex;}
  }
  boolean advance(){
   age++;int duration=large?35:50;double progress=MeteorGeometry.flightProgress(age,duration);
   if(age%2==0||age>=duration){
    Location loc=start.clone().add(end.toVector().subtract(start.toVector()).multiply(progress));
    for(int i=0;i<displays.size();i++)displays.get(i).teleport(loc.clone().add(offsets.get(i)));
    if(age%8==0&&loc.getWorld()!=null)loc.getWorld().spawnParticle(Particle.SMOKE,loc,large?3:1,0.25,0.25,0.25,0.01);
   }
   return age>=duration;
  }
  void shatter(){
   // Keep the actual meteor displays: every fragment retains its magma/blackstone appearance.
   for(int i=0;i<displays.size();i++){
    BlockDisplay display=displays.get(i);Vector offset=offsets.get(i);
    Location position=end.clone().add(offset);display.setTeleportDuration(1);display.teleport(position);
    double angle=ThreadLocalRandom.current().nextDouble(Math.PI*2);
    double speed=ThreadLocalRandom.current().nextDouble(0.18,0.35);
    debris.add(new Debris(display,position,new Vector(Math.cos(angle)*speed,ThreadLocalRandom.current().nextDouble(0.18,0.33),Math.sin(angle)*speed)));
   }
   displays.clear();offsets.clear();
  }
  void remove(){for(BlockDisplay d:displays)d.remove();displays.clear();}
 }
}
