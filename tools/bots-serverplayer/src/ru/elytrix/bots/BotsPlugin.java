package ru.elytrix.bots;

import net.minecraft.world.InteractionHand;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class BotsPlugin extends JavaPlugin implements Listener {
    private final Map<UUID,TrainingPlayer> bots = new HashMap<>();
    private final Map<UUID,UUID> owners = new HashMap<>();
    private NamespacedKey marker, ownerKey;
    private boolean combatReady;
    private static final String PINK = "§x§F§8§B§E§F§B";
    private static final class Menu implements InventoryHolder {
        final int page;
        Menu(int page) { this.page = page; }
        public Inventory getInventory() { return null; }
    }
    @Override public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("GreatCombat")) {
            getLogger().severe("GreatCombat не запущен: плагин ботов отключён, чтобы не нарушать антирелог.");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        marker = new NamespacedKey(this,"training_bot"); ownerKey = new NamespacedKey(this,"owner");
        getServer().getPluginManager().registerEvents(this,this);
        registerGreatCombat();
        getServer().getScheduler().runTaskTimer(this,this::maintain,1,1);
    }
    @Override public void onDisable() { for (UUID id : new ArrayList<>(bots.keySet())) remove(id); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cКоманда только для игроков."); return true; }
        openMenu(p, 0); return true;
    }
    private String settingPath(Player player, String key) { return "players." + player.getUniqueId() + "." + key; }
    private boolean enabled(Player player, String key) { return getConfig().getBoolean(settingPath(player,key), false); }
    private void toggle(Player player, String key) {
        getConfig().set(settingPath(player,key), !enabled(player,key)); saveConfig(); openMenu(player,1);
    }
    private String mode(Player p) {
        String value=getConfig().getString(settingPath(p,"mode"),"custom");
        return Set.of("custom","crit","combo","pro","noob").contains(value) ? value : "custom";
    }
    private void setMode(Player p,String mode) {
        getConfig().set(settingPath(p,"mode"),mode); saveConfig();
        TrainingPlayer bot=bots.get(p.getUniqueId());
        if(bot!=null) { bot.nextAttackTick=0; bot.attacksMade=0; }
        openMenu(p,2);
    }
    private void openMenu(Player p, int page) {
        Inventory menu = getServer().createInventory(new Menu(page),54,PINK+"§l"+(page==2?"Режим боя":page==1?"Настройка бота":"Тренировочный бот"));
        ItemStack black=item(Material.BLACK_STAINED_GLASS_PANE,"§r");
        ItemStack purple=item(Material.PURPLE_STAINED_GLASS_PANE,"§r");
        for(int i=0;i<54;i++) {
            int row=i/9,col=i%9;
            if(row==0 || row==5 || col==0 || col==8) menu.setItem(i,row<3?black:purple);
        }
        if(page==2) {
            String current=mode(p);
            menu.setItem(19,modeItem(Material.DIAMOND_SWORD,"Криты","Криты с прыжка",current.equals("crit")));
            menu.setItem(21,modeItem(Material.IRON_SWORD,"Комба","Быстрые атаки с разбега",current.equals("combo")));
            menu.setItem(23,modeItem(Material.NETHERITE_SWORD,"Профи","Криты, комбо и движение",current.equals("pro")));
            menu.setItem(25,modeItem(Material.WOODEN_SWORD,"Нуб","Медленнее и иногда промахивается",current.equals("noob")));
            menu.setItem(31,modeItem(Material.COMPARATOR,"Свои настройки","Использовать ваши переключатели",current.equals("custom")));
            menu.setItem(49,item(Material.ARROW,"§7« "+PINK+"Назад §7»",PINK+"§l┃ ","§7● §fВернуться к настройкам"));
        } else if(page==1) {
            menu.setItem(20,option(Material.FEATHER,"Бот бегает за вами","Бот будет бежать к вам",enabled(p,"follow")));
            menu.setItem(22,option(Material.IRON_SWORD,"Бот бьёт вас","Бот будет атаковать вас",enabled(p,"attack")));
            menu.setItem(24,option(Material.ENDER_EYE,"Бот смотрит на вас","Бот будет следить за вами взглядом",enabled(p,"look")));
            menu.setItem(40,item(Material.NETHERITE_SWORD,"§7« "+PINK+"Режимы боя §7»",PINK+"§l┃ ",PINK+"§l┃ §fВыберите стиль бота",PINK+"§l┃ §fРежимы заменяют переключатели выше",PINK+"§l┃ ","§7● §fНажмите для выбора"));
            menu.setItem(49,item(Material.ARROW,"§7« "+PINK+"Назад §7»",PINK+"§l┃ ","§7● §fВернуться в меню бота"));
        } else {
            menu.setItem(20,item(Material.PLAYER_HEAD,"§7« "+PINK+"Создать бота §7»",PINK+"§l┃ ",PINK+"§l┃ §fЛичный бессмертный бот",PINK+"§l┃ ","§7● §fНажмите для создания"));
            menu.setItem(22,item(Material.COMPARATOR,"§7« "+PINK+"Настроить бота §7»",PINK+"§l┃ ",PINK+"§l┃ §fДвижение, атака и взгляд",PINK+"§l┃ ","§7● §fНажмите для настройки"));
            menu.setItem(24,item(Material.BARRIER,"§7« "+PINK+"Убрать бота §7»",PINK+"§l┃ ",PINK+"§l┃ §fУдалить вашего бота",PINK+"§l┃ ","§7● §fНажмите для удаления"));
            menu.setItem(49,item(Material.RED_DYE,"§7« §cЗакрыть меню §7»",PINK+"§l┃ ","§7● §fНажмите для закрытия"));
        }
        p.openInventory(menu);
        p.playSound(p.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.35f,1.4f);
    }
    private ItemStack item(Material material,String name,String... lore) {
        ItemStack item = new ItemStack(material); ItemMeta meta=item.getItemMeta(); meta.setDisplayName(name);
        if (lore.length>0) meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); return item;
    }
    private ItemStack option(Material material,String name,String description,boolean on) {
        return item(material,"§7« "+PINK+name+" §7»",PINK+"§l┃ ",PINK+"§l┃ §f"+description,PINK+"§l┃ §fСостояние: "+(on?"§aВключено":"§cВыключено"),PINK+"§l┃ ","§7● §fНажмите для переключения");
    }
    private ItemStack modeItem(Material material,String title,String description,boolean active) {
        return item(material,"§7« "+PINK+title+" §7»",PINK+"§l┃ ",PINK+"§l┃ §f"+description,PINK+"§l┃ §fРежим: "+(active?"§aВыбран":"§7Не выбран"),PINK+"§l┃ ","§7● §fНажмите для выбора");
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p)) return;
        int slot=event.getRawSlot();
        if(menu.page==2) {
            if(slot==19) setMode(p,"crit"); else if(slot==21) setMode(p,"combo"); else if(slot==23) setMode(p,"pro"); else if(slot==25) setMode(p,"noob"); else if(slot==31) setMode(p,"custom"); else if(slot==49) openMenu(p,1);
        } else if(menu.page==1) {
            if(slot==20) toggle(p,"follow"); else if(slot==22) toggle(p,"attack"); else if(slot==24) toggle(p,"look"); else if(slot==40) openMenu(p,2); else if(slot==49) openMenu(p,0);
        } else {
            if(slot==20) { p.closeInventory(); spawn(p); }
            else if(slot==22) openMenu(p,1);
            else if(slot==24) { p.closeInventory(); remove(p.getUniqueId()); p.sendMessage(PINK+"Бот убран."); }
            else if(slot==49) p.closeInventory();
        }
    }
    @EventHandler public void drag(InventoryDragEvent event) { if(event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true); }
    private void spawn(Player owner) {
        if (!combatReady || !getServer().getPluginManager().isPluginEnabled("GreatCombat")) {
            owner.sendMessage("§cБот недоступен: не удалось подключить API GreatCombat. Сообщите администратору."); return;
        }
        remove(owner.getUniqueId());
        Location base=owner.getLocation(); org.bukkit.util.Vector direction=base.getDirection().setY(0);
        if (direction.lengthSquared() < 0.001) direction.setZ(1); direction.normalize().multiply(2);
        Location at=base.clone().add(direction);
        if (!at.getBlock().isPassable() || !at.clone().add(0,1,0).getBlock().isPassable() || at.clone().add(0,-0.1,0).getBlock().isPassable()) {
            owner.sendMessage("§cПеред вами нет подходящего места. Встаньте на ровную площадку."); return;
        }
        MinecraftServer server=((CraftServer)getServer()).getServer();
        ServerLevel world=((CraftWorld)owner.getWorld()).getHandle();
        UUID uuid=UUID.randomUUID();
        TrainingPlayer bot=null;
        try {
            String name;
            do {
                String random = Integer.toString(ThreadLocalRandom.current().nextInt(36*36*36*36),36);
                name = "ElytrixBots_" + "0000".substring(random.length()) + random;
            } while (nameInUse(name));
            bot=new TrainingPlayer(server,world,BotSkin.profile(uuid,name));
            bot.snapTo(at.getX(),at.getY(),at.getZ(),base.getYaw()+180,0);
            LocalConnection connection=new LocalConnection();
            bot.connection = new LocalPacketListener(server,connection,bot);
            Player fake=bot.getBukkitEntity();
            fake.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);
            fake.getPersistentDataContainer().set(ownerKey,PersistentDataType.STRING,owner.getUniqueId().toString());
            fake.setVisibleByDefault(false); fake.setPersistent(false); fake.setCollidable(false);
            AttributeInstance health=fake.getAttribute(Attribute.MAX_HEALTH); if (health!=null) health.setBaseValue(2048);
            fake.setHealth(fake.getMaxHealth());
            bots.put(owner.getUniqueId(),bot); owners.put(uuid,owner.getUniqueId());
            owner.showEntity(this,fake);
            owner.unlistPlayer(fake);
            world.addDuringTeleport(bot);
            if (!fake.isValid()) throw new IllegalStateException("Bot was not added to world");
            owner.sendMessage("§aСерверный бот создан.");
        } catch (Throwable failure) {
            getLogger().log(java.util.logging.Level.SEVERE,"Не удалось создать ServerPlayer-бота",failure);
            if (bot != null) {
                try { if (bots.containsKey(owner.getUniqueId())) remove(owner.getUniqueId()); else bot.remove(RemovalReason.DISCARDED); }
                catch (Throwable cleanupFailure) { getLogger().log(java.util.logging.Level.WARNING,"Не удалось полностью убрать бота",cleanupFailure); }
            }
            owner.sendMessage("§cНе удалось создать бота. Проверьте ошибку в консоли.");
        }
    }
    private boolean nameInUse(String name) {
        if (Bukkit.getPlayerExact(name)!=null) return true;
        for (TrainingPlayer bot : bots.values()) if (bot.getGameProfile().name().equalsIgnoreCase(name)) return true;
        return false;
    }
    private void remove(UUID id) {
        TrainingPlayer bot=bots.remove(id); if(bot==null) return;
        owners.remove(bot.getUUID());
        Player p=Bukkit.getPlayer(id);
        if (p!=null && p.isOnline()) {
            if (!p.isListed(bot.getBukkitEntity()) && p.canSee(bot.getBukkitEntity())) p.listPlayer(bot.getBukkitEntity());
            if (isEnabled()) p.hideEntity(this,bot.getBukkitEntity());
            ((CraftPlayer)p).getHandle().connection.send(new ClientboundPlayerInfoRemovePacket(List.of(bot.getUUID())));
        }
        bot.remove(RemovalReason.DISCARDED);
    }
    private void maintain() {
        if (!getServer().getPluginManager().isPluginEnabled("GreatCombat")) {
            combatReady = false;
            for (UUID id : new ArrayList<>(bots.keySet())) remove(id);
            return;
        }
        for (UUID id : new ArrayList<>(bots.keySet())) {
            TrainingPlayer bot=bots.get(id); Player owner=Bukkit.getPlayer(id);
            if(owner==null || !owner.isOnline() || !owner.getWorld().equals(bot.getBukkitEntity().getWorld()) || bot.isRemoved() || bot.getY()<bot.level().getMinY()-16) { remove(id); continue; }
            Player fake=bot.getBukkitEntity(); if (!fake.isDead()) fake.setHealth(fake.getMaxHealth());
            behave(bot, owner);
        }
    }
    private void face(TrainingPlayer bot, Player owner, boolean pitch) {
        Location target=owner.getEyeLocation(), from=bot.getBukkitEntity().getEyeLocation();
        double dx=target.getX()-from.getX(), dy=target.getY()-from.getY(), dz=target.getZ()-from.getZ();
        float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
        bot.setYRot(yaw); bot.setYHeadRot(yaw); bot.setYBodyRot(yaw);
        if(pitch) bot.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
    }
    private void behave(TrainingPlayer bot, Player owner) {
        String mode=mode(owner); boolean custom=mode.equals("custom");
        boolean follow=!custom || enabled(owner,"follow");
        boolean attack=!custom || enabled(owner,"attack");
        boolean look=!custom || enabled(owner,"look");
        Player fake=bot.getBukkitEntity();
        double distance=fake.getLocation().distanceSquared(owner.getLocation());
        boolean valid=!owner.isDead() && owner.getGameMode()!=GameMode.SPECTATOR;
        long now=bot.level().getServer().getTickCount();
        boolean criticalMode=mode.equals("crit") || (mode.equals("pro") && bot.attacksMade%3==0);
        boolean inRange=distance<=9.0 && fake.hasLineOfSight(owner);
        boolean prepareCritical=attack && valid && criticalMode && inRange && now>=bot.nextAttackTick;
        boolean chasing=follow && valid && distance>5.76 && distance<4096;
        if (look && valid) face(bot,owner,true); else if (chasing) face(bot,owner,false);
        bot.zza=chasing?(mode.equals("noob")?0.65f:1.0f):0.0f;
        bot.xxa=mode.equals("pro") && valid && distance<16 && !prepareCritical ? ((now/24)%2==0?0.55f:-0.55f):0;
        // A genuine vanilla critical requires falling and not sprinting.
        bot.setSprinting(chasing && !prepareCritical);
        if (valid && bot.onGround() && (prepareCritical || (chasing && bot.horizontalCollision))) bot.jumpForCombat();
        if (!attack || !valid || !inRange || now<bot.nextAttackTick || bot.getAttackStrengthScale(0.5f)<0.9f) return;
        if (criticalMode && !(bot.fallDistance>0 && !bot.onGround() && !bot.isSprinting() && !bot.onClimbable() && !bot.isInWater() && !bot.isMobilityRestricted() && !bot.isPassenger())) return;
        face(bot,owner,true);
        bot.nextAttackTick=now+(mode.equals("noob")?26:mode.equals("combo")?12:mode.equals("pro")?11:20);
        bot.attacksMade++;
        if (mode.equals("noob") && ThreadLocalRandom.current().nextDouble()<0.35) {
            bot.swing(InteractionHand.MAIN_HAND); return;
        }
        bot.attack(((CraftPlayer)owner).getHandle());
        bot.swing(InteractionHand.MAIN_HAND);
    }
    // Load from GreatCombat itself: some server plugin class loaders do not expose its API to us.
    @SuppressWarnings("unchecked")
    private void registerGreatCombat() {
        try {
            org.bukkit.plugin.Plugin plugin = getServer().getPluginManager().getPlugin("GreatCombat");
            Class<?> type;
            try {
                type = Class.forName("dev.enco.greatcombat.api.events.CombatPreStartEvent", true, plugin.getClass().getClassLoader());
            } catch (ClassNotFoundException missingV2) {
                type = Class.forName("dev.enco.greatcombat.api.CombatPreStartEvent", true, plugin.getClass().getClassLoader());
            }
            if (!Event.class.isAssignableFrom(type) || !Cancellable.class.isAssignableFrom(type)) throw new IllegalStateException("Event is not cancellable");
            java.lang.reflect.Method damager = type.getMethod("getDamager"), target = type.getMethod("getTarget");
            if (!Player.class.isAssignableFrom(damager.getReturnType()) || !Player.class.isAssignableFrom(target.getReturnType())) throw new IllegalStateException("Unexpected GreatCombat API");
            getServer().getPluginManager().registerEvent((Class<? extends Event>)type, this, EventPriority.HIGHEST, (listener, event) -> {
                try {
                    Player attacker = (Player) damager.invoke(event), victim = (Player) target.invoke(event);
                    if (attacker != null && victim != null && (Objects.equals(owners.get(attacker.getUniqueId()), victim.getUniqueId()) || Objects.equals(owners.get(victim.getUniqueId()), attacker.getUniqueId()))) ((Cancellable) event).setCancelled(true);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    ((Cancellable) event).setCancelled(true);
                    combatReady = false;
                    for (UUID id : new ArrayList<>(bots.keySet())) remove(id);
                    getLogger().log(java.util.logging.Level.SEVERE, "Ошибка API GreatCombat: боты удалены и отключены", failure);
                }
            }, this, true);
            combatReady = true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            getLogger().log(java.util.logging.Level.SEVERE, "Не удалось подключить API GreatCombat; создание ботов отключено. Проверьте версию GreatCombat.", failure);
        }
    }
    @EventHandler public void interact(PlayerInteractEntityEvent e) { if (owners.containsKey(e.getRightClicked().getUniqueId())) e.setCancelled(true); }
    @EventHandler public void quit(PlayerQuitEvent e) { remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { remove(e.getPlayer().getUniqueId()); }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void damage(EntityDamageEvent event) {
        UUID owner=owners.get(event.getEntity().getUniqueId()); if (owner==null) return;
        if (!(event instanceof EntityDamageByEntityEvent attack) || !(attack.getDamager() instanceof Player attacker) || !attacker.getUniqueId().equals(owner)) { event.setCancelled(true); return; }
        if (event.getDamage()>100) event.setDamage(100);
    }
}
