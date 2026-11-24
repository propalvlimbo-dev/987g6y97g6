package ru.elytrix.bots;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

/** One shared training ring; snapshots are written before changing player state. */
public final class TrainingArena implements Listener {
    private static TrainingArena instance;
    private final JavaPlugin plugin;
    private final Map<UUID,BossBar> active=new HashMap<>();
    private final Set<UUID> internal=new HashSet<>(), restoring=new HashSet<>(), waitingRespawn=new HashSet<>();
    private final Set<UUID> hints=new HashSet<>();
    private final File directory;
    private boolean quietReady;
    private static final String PINK="§x§F§8§B§E§F§B";
    private TrainingArena(JavaPlugin p) { plugin=p; directory=new File(p.getDataFolder(),"training-backups"); }
    public static void install(JavaPlugin p) {
        instance=new TrainingArena(p); Bukkit.getPluginManager().registerEvents(instance,p);
        if(Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            try { QuietSounds.install(p); instance.quietReady=true; }
            catch(LinkageError | RuntimeException ex) { p.getLogger().log(java.util.logging.Level.SEVERE,"ProtocolLib несовместим: тихая тренировка недоступна",ex); }
        } else p.getLogger().warning("ProtocolLib не запущен: вход на тихую тренировку отключён.");
        Bukkit.getScheduler().runTaskTimer(p,instance::tick,1,20);
        for(Player player:Bukkit.getOnlinePlayers()) if(instance.file(player).exists()) Bukkit.getScheduler().runTask(p,()->instance.restore(player));
    }
    public static void shutdown() {
        if(instance==null) return;
        for(UUID id:new ArrayList<>(instance.active.keySet())) { Player p=Bukkit.getPlayer(id); if(p!=null && !p.isDead()) instance.restore(p); }
        for(UUID id:new ArrayList<>(instance.active.keySet())) instance.unhide(id);
        for(BossBar bar:instance.active.values()) bar.removeAll();
        if(instance.quietReady) QuietSounds.clear();
    }
    public static boolean command(CommandSender sender,String[] args) {
        if(instance==null) return false;
        if(!(sender instanceof Player p)) { sender.sendMessage("§cКоманда только для игроков."); return true; }
        instance.runCommand(p,args); return true;
    }
    public static int menuPage(Player p,int page) { return instance!=null && instance.active.containsKey(p.getUniqueId()) && page==0 ? 1 : page; }
    private void runCommand(Player p,String[] args) {
        if(args.length==1 && args[0].equalsIgnoreCase("leave")) {
            if(active.containsKey(p.getUniqueId()) || file(p).exists()) restore(p); else p.sendMessage("§cВы не на тренировке."); return;
        }
        if(args.length!=0) { p.sendMessage("§cИспользуйте /bots или /bots leave."); return; }
        if(active.containsKey(p.getUniqueId())) {
            hints.add(p.getUniqueId()); updateHint(p); invoke("openMenu",new Class<?>[]{Player.class,int.class},p,1);
        } else if(file(p).exists()) restore(p); else openEntrance(p);
    }
    private static final class Entry implements InventoryHolder { public Inventory getInventory(){return null;} }
    private void openEntrance(Player p) {
        Inventory menu=Bukkit.createInventory(new Entry(),45,PINK+"Тренировка");
        ItemStack black=decoration(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack purple=decoration(Material.PURPLE_STAINED_GLASS_PANE);
        for(int slot=0;slot<45;slot++) {
            int row=slot/9,col=slot%9;
            if(row==0 || row==4 || col==0 || col==8) menu.setItem(slot,row<2?black:purple);
        }
        ItemStack item=new ItemStack(Material.LIME_CONCRETE); ItemMeta meta=item.getItemMeta();
        meta.setDisplayName("§7« §aВОЙТИ НА ТРЕНИРОВКУ §7»");
        meta.setLore(List.of(PINK+"┃ ",PINK+"┃ §fЛичный тренировочный бот",PINK+"┃ §fАлмазный меч и общая арена",PINK+"┃ §7Во время тренировки звуки отключены",PINK+"┃ ","§7● §fНажмите для входа"));
        item.setItemMeta(meta); menu.setItem(22,item); p.openInventory(menu);
        p.playSound(p.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.35f,1.4f);
    }
    private ItemStack decoration(Material material) {
        ItemStack item=new ItemStack(material); ItemMeta meta=item.getItemMeta(); meta.setDisplayName("§r"); item.setItemMeta(meta); return item;
    }
    @EventHandler public void menu(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Entry)) return;
        e.setCancelled(true); if(e.getRawSlot()==22 && e.getWhoClicked() instanceof Player p) { p.closeInventory(); enter(p); }
    }
    @EventHandler public void menuDrag(InventoryDragEvent e) { if(e.getView().getTopInventory().getHolder() instanceof Entry) e.setCancelled(true); }
    private File file(Player p) { return new File(directory,p.getUniqueId()+".yml"); }
    private boolean saveSnapshot(Player p) {
        if(!directory.isDirectory() && !directory.mkdirs()) return false;
        YamlConfiguration y=new YamlConfiguration();
        y.set("location",p.getLocation()); y.set("contents",Arrays.asList(p.getInventory().getContents()));
        y.set("cursor",p.getItemOnCursor()); y.set("held",p.getInventory().getHeldItemSlot());
        y.set("health",p.getHealth()); y.set("food",p.getFoodLevel()); y.set("saturation",p.getSaturation()); y.set("exhaustion",p.getExhaustion());
        y.set("level",p.getLevel()); y.set("exp",p.getExp()); y.set("totalExp",p.getTotalExperience());
        y.set("collidable",p.isCollidable()); y.set("gamemode",p.getGameMode().name()); y.set("allowFlight",p.getAllowFlight()); y.set("flying",p.isFlying());
        y.set("fire",p.getFireTicks()); y.set("fall",p.getFallDistance()); y.set("effects",new ArrayList<>(p.getActivePotionEffects()));
        File tmp=new File(directory,p.getUniqueId()+".tmp");
        try { y.save(tmp); try { Files.move(tmp.toPath(),file(p).toPath(),StandardCopyOption.ATOMIC_MOVE); }
              catch(AtomicMoveNotSupportedException ex) { Files.move(tmp.toPath(),file(p).toPath()); } return true;
        } catch(Exception ex) { plugin.getLogger().log(java.util.logging.Level.SEVERE,"Не удалось сохранить инвентарь тренировки",ex); return false; }
    }
    private Object invoke(String method,Class<?>[] types,Object... args) {
        try { var m=plugin.getClass().getDeclaredMethod(method,types); m.setAccessible(true); return m.invoke(plugin,args); }
        catch(Exception ex) { plugin.getLogger().log(java.util.logging.Level.SEVERE,"Ошибка вызова бота: "+method,ex); return null; }
    }
    private boolean botExists(UUID id) {
        try { var f=plugin.getClass().getDeclaredField("bots"); f.setAccessible(true); return ((Map<?,?>)f.get(plugin)).containsKey(id); }
        catch(Exception ex) { plugin.getLogger().log(java.util.logging.Level.SEVERE,"Не удалось проверить бота",ex); return false; }
    }
    private void enter(Player p) {
        if(file(p).exists()) { restore(p); return; }
        if(!quietReady || !Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) { p.sendMessage("§cТихая тренировка недоступна: необходим совместимый ProtocolLib. Сообщите администратору."); return; }
        World w=Bukkit.getWorld("duel");
        if(w==null || p.isDead() || p.getWorld().equals(w)) { p.sendMessage("§cВойти сейчас нельзя: проверьте мир duel и завершите текущую дуэль."); return; }
        Location destination=new Location(w,-400,101,0,p.getLocation().getYaw(),0);
        if(!destination.getBlock().isPassable() || !destination.clone().add(0,1,0).getBlock().isPassable() || destination.clone().add(0,-0.1,0).getBlock().isPassable()) { p.sendMessage("§cТочка тренировки небезопасна. Сообщите администратору."); return; }
        boolean room=false;
        for(int i=0;i<8;i++) {
            destination.setYaw(i*45f);
            Location near=destination.clone().add(destination.getDirection().setY(0).normalize().multiply(2));
            if(near.getBlock().isPassable() && near.clone().add(0,1,0).getBlock().isPassable() && !near.clone().add(0,-0.1,0).getBlock().isPassable()) { room=true; break; }
        }
        if(!room) { p.sendMessage("§cРядом с точкой тренировки нет места для бота. Сообщите администратору."); return; }
        if(!saveSnapshot(p)) { p.sendMessage("§cНе удалось сохранить ваши вещи. Вход отменён."); return; }
        BossBar bar=Bukkit.createBossBar("§a/bots §f— настроить бота",BarColor.GREEN,BarStyle.SOLID); active.put(p.getUniqueId(),bar); QuietSounds.add(p.getUniqueId()); bar.addPlayer(p);
        hideFromOtherTrainees(p);
        p.getInventory().clear(); p.setItemOnCursor(null); p.getInventory().setItem(0,new ItemStack(Material.DIAMOND_SWORD)); p.getInventory().setHeldItemSlot(0);
        for(PotionEffect effect:p.getActivePotionEffects()) p.removePotionEffect(effect.getType());
        p.setCollidable(false); p.setGameMode(GameMode.SURVIVAL); p.setFlying(false); p.setAllowFlight(false); p.setFoodLevel(20); p.setSaturation(20); p.setFireTicks(0);
        p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
        internal.add(p.getUniqueId()); boolean moved;
        try { moved=p.teleport(destination); } finally { internal.remove(p.getUniqueId()); }
        if(!moved) { p.sendMessage("§cНе удалось войти на арену. Возвращаем ваши вещи."); restore(p); return; }
        invoke("spawn",new Class<?>[]{Player.class},p);
        if(!botExists(p.getUniqueId())) { p.sendMessage("§cНе удалось создать бота. Возвращаем ваши вещи."); restore(p); return; }
        updateHint(p);
    }
    @SuppressWarnings("unchecked")
    private void restore(Player p) {
        UUID id=p.getUniqueId(); if(!restoring.add(id)) return;
        try {
            if(p.isDead()) { waitingRespawn.add(id); return; }
            File f=file(p); if(!f.exists()) { finish(id); return; }
            YamlConfiguration y=YamlConfiguration.loadConfiguration(f); Location loc=y.getLocation("location");
            if(loc==null || loc.getWorld()==null || !y.isList("contents")) { p.sendMessage("§cНе удалось загрузить сохранение тренировки. Позовите администратора."); return; }
            List<?> items=y.getList("contents"); ItemStack[] contents=new ItemStack[items.size()];
            if(contents.length!=p.getInventory().getContents().length) throw new IllegalStateException("Invalid saved inventory size");
            for(int i=0;i<items.size();i++) { Object item=items.get(i); if(item!=null && !(item instanceof ItemStack)) throw new IllegalStateException("Invalid saved item"); contents[i]=(ItemStack)item; }
            internal.add(id); boolean teleported; try { teleported=p.teleport(loc); } finally { internal.remove(id); }
            if(!teleported) { p.sendMessage("§cВозврат отменён другим плагином. Сохранение вещей оставлено; повторите /bots leave."); return; }
            p.getInventory().setContents(contents); p.setItemOnCursor(y.getItemStack("cursor")); p.getInventory().setHeldItemSlot(y.getInt("held",0));
            for(PotionEffect effect:p.getActivePotionEffects()) p.removePotionEffect(effect.getType());
            for(Object effect:y.getList("effects",List.of())) if(effect instanceof PotionEffect pe) p.addPotionEffect(pe);
            p.setCollidable(y.getBoolean("collidable",true));
            p.setGameMode(GameMode.valueOf(y.getString("gamemode","SURVIVAL"))); p.setAllowFlight(y.getBoolean("allowFlight")); if(p.getAllowFlight()) p.setFlying(y.getBoolean("flying"));
            p.setFoodLevel(y.getInt("food",20)); p.setSaturation((float)y.getDouble("saturation",5)); p.setExhaustion((float)y.getDouble("exhaustion"));
            p.setLevel(y.getInt("level")); p.setExp((float)y.getDouble("exp")); p.setTotalExperience(y.getInt("totalExp"));
            double max=p.getAttribute(Attribute.MAX_HEALTH).getValue(); p.setHealth(Math.max(0.1,Math.min(max,y.getDouble("health",max))));
            p.setFireTicks(y.getInt("fire")); p.setFallDistance((float)y.getDouble("fall"));
            p.saveData(); Files.delete(f.toPath()); finish(id); p.sendMessage("§aТренировка завершена. Ваши вещи и положение восстановлены.");
        } catch(Exception ex) { plugin.getLogger().log(java.util.logging.Level.SEVERE,"Не удалось восстановить игрока "+id+"; сохранение оставлено",ex); p.sendMessage("§cОшибка восстановления. Позовите администратора; сохранение не удалено."); }
        finally { restoring.remove(id); }
    }
    private void finish(UUID id) {
        unhide(id); if(quietReady) QuietSounds.remove(id);
        invoke("remove",new Class<?>[]{UUID.class},id); BossBar bar=active.remove(id); if(bar!=null) bar.removeAll(); hints.remove(id); waitingRespawn.remove(id);
    }
    private void hideFromOtherTrainees(Player player) {
        for(UUID id:active.keySet()) {
            if(id.equals(player.getUniqueId())) continue;
            Player other=Bukkit.getPlayer(id); if(other==null || !other.isOnline()) continue;
            player.hidePlayer(plugin,other); other.hidePlayer(plugin,player);
        }
    }
    private void unhide(UUID id) {
        Player player=Bukkit.getPlayer(id); if(player==null) return;
        for(UUID otherId:active.keySet()) {
            if(id.equals(otherId)) continue;
            Player other=Bukkit.getPlayer(otherId); if(other==null) continue;
            player.showPlayer(plugin,other); other.showPlayer(plugin,player);
        }
    }
    private void updateHint(Player p) {
        String msg=hints.contains(p.getUniqueId())?"§c/bots leave §f— выйти с арены":"§a/bots §f— настроить бота";
        BossBar b=active.get(p.getUniqueId()); if(b!=null) b.setTitle(msg); p.sendActionBar(msg);
    }
    private void tick() {
        for(UUID id:new ArrayList<>(active.keySet())) {
            Player p=Bukkit.getPlayer(id); if(p==null) continue;
            if(p.isDead()) { waitingRespawn.add(id); continue; }
            if(!p.getWorld().getName().equals("duel") || !botExists(id) || !Bukkit.getPluginManager().isPluginEnabled("GreatCombat") || !Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) { restore(p); continue; }
            p.setFoodLevel(20); p.setSaturation(20); p.setFireTicks(0);
            double max=p.getAttribute(Attribute.MAX_HEALTH).getValue(); if(p.getHealth()<max) p.setHealth(max);
            updateHint(p);
        }
    }
    private boolean training(Player p) { return active.containsKey(p.getUniqueId()) || file(p).exists(); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void commands(PlayerCommandPreprocessEvent e) {
        if(!training(e.getPlayer())) return;
        String s=e.getMessage().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
        if(!s.equals("/bots") && !s.equals("/bot") && !s.equals("/bots leave") && !s.equals("/bot leave")) { e.setCancelled(true); e.getPlayer().sendMessage("§cНа тренировке доступны только /bots и /bots leave."); }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void teleport(PlayerTeleportEvent e) { if(training(e.getPlayer()) && !internal.contains(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.MONITOR) public void changed(PlayerChangedWorldEvent e) { if(training(e.getPlayer()) && !internal.contains(e.getPlayer().getUniqueId())) Bukkit.getScheduler().runTask(plugin,()->restore(e.getPlayer())); }
    @EventHandler(priority=EventPriority.HIGHEST) public void blockBreak(BlockBreakEvent e) { if(training(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST) public void blockPlace(BlockPlaceEvent e) { if(training(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e) { if(training(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST) public void interactEntity(PlayerInteractEntityEvent e) { if(training(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST) public void drop(PlayerDropItemEvent e) { if(training(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST) public void pickup(EntityPickupItemEvent e) { if(e.getEntity() instanceof Player p && training(p)) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST) public void exp(com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent e) { if(training(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST) public void food(FoodLevelChangeEvent e) { if(e.getEntity() instanceof Player p && training(p)) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player p) || !training(p)) return;
        if(e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player attacker && botExists(p.getUniqueId()) && !attacker.getUniqueId().equals(p.getUniqueId())) {
            // Only this player's bot may land nonlethal hits; everyone else is blocked.
            String owner=attacker.getPersistentDataContainer().get(new NamespacedKey(plugin,"owner"),org.bukkit.persistence.PersistentDataType.STRING);
            if(p.getUniqueId().toString().equals(owner)) { if(e.getFinalDamage()>=p.getHealth()) e.setCancelled(true); return; }
        }
        e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void attack(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player p && training(p)) {
            String owner=e.getEntity().getPersistentDataContainer().get(new NamespacedKey(plugin,"owner"),org.bukkit.persistence.PersistentDataType.STRING);
            if(!p.getUniqueId().toString().equals(owner)) e.setCancelled(true);
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void quit(PlayerQuitEvent e) {
        Player p=e.getPlayer(); if(!training(p)) return;
        if(p.isDead()) finish(p.getUniqueId()); else restore(p);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void death(PlayerDeathEvent e) { if(training(e.getEntity())) { e.getDrops().clear(); e.setDroppedExp(0); waitingRespawn.add(e.getEntity().getUniqueId()); } }
    @EventHandler(priority=EventPriority.MONITOR) public void respawn(PlayerRespawnEvent e) { if(training(e.getPlayer())) Bukkit.getScheduler().runTask(plugin,()->restore(e.getPlayer())); }
    @EventHandler(priority=EventPriority.MONITOR) public void join(PlayerJoinEvent e) { if(file(e.getPlayer()).exists()) Bukkit.getScheduler().runTask(plugin,()->restore(e.getPlayer())); }
}
