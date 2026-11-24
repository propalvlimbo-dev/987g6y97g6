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
    private static final String PINK = "§x§F§8§B§E§F§B";
    private static final class Menu implements InventoryHolder {
        final boolean settings;
        Menu(boolean settings) { this.settings = settings; }
        public Inventory getInventory() { return null; }
    }
    @Override public void onEnable() {
        marker = new NamespacedKey(this,"training_bot"); ownerKey = new NamespacedKey(this,"owner");
        getServer().getPluginManager().registerEvents(this,this);
        getServer().getScheduler().runTaskTimer(this,this::maintain,1,1);
    }
    @Override public void onDisable() { for (UUID id : new ArrayList<>(bots.keySet())) remove(id); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cКоманда только для игроков."); return true; }
        openMenu(p, false); return true;
    }
    private String settingPath(Player player, String key) { return "players." + player.getUniqueId() + "." + key; }
    private boolean enabled(Player player, String key) { return getConfig().getBoolean(settingPath(player,key), false); }
    private void toggle(Player player, String key) {
        getConfig().set(settingPath(player,key), !enabled(player,key)); saveConfig(); openMenu(player,true);
    }
    private void openMenu(Player p, boolean settings) {
        Inventory menu = getServer().createInventory(new Menu(settings),54,PINK+"§l"+(settings?"Настройка бота":"Тренировочный бот"));
        ItemStack black=item(Material.BLACK_STAINED_GLASS_PANE,"§r");
        ItemStack purple=item(Material.PURPLE_STAINED_GLASS_PANE,"§r");
        for(int i=0;i<54;i++) {
            int row=i/9,col=i%9;
            if(row==0 || row==5 || col==0 || col==8) menu.setItem(i,row<3?black:purple);
        }
        if(settings) {
            menu.setItem(20,option(Material.FEATHER,"Бот бегает за вами","Бот будет бежать к вам",enabled(p,"follow")));
            menu.setItem(22,option(Material.IRON_SWORD,"Бот бьёт вас","Бот будет атаковать вас",enabled(p,"attack")));
            menu.setItem(24,option(Material.ENDER_EYE,"Бот смотрит на вас","Бот будет следить за вами взглядом",enabled(p,"look")));
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
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p)) return;
        int slot=event.getRawSlot();
        if(menu.settings) {
            if(slot==20) toggle(p,"follow"); else if(slot==22) toggle(p,"attack"); else if(slot==24) toggle(p,"look"); else if(slot==49) openMenu(p,false);
        } else {
            if(slot==20) { p.closeInventory(); spawn(p); }
            else if(slot==22) openMenu(p,true);
            else if(slot==24) { p.closeInventory(); remove(p.getUniqueId()); p.sendMessage(PINK+"Бот убран."); }
            else if(slot==49) p.closeInventory();
        }
    }
    @EventHandler public void drag(InventoryDragEvent event) { if(event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true); }
    private void spawn(Player owner) {
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
        boolean follow=enabled(owner,"follow"), attack=enabled(owner,"attack"), look=enabled(owner,"look");
        Player fake=bot.getBukkitEntity();
        double distance=fake.getLocation().distanceSquared(owner.getLocation());
        boolean targetValid=!owner.isDead() && owner.getGameMode()!=GameMode.SPECTATOR;
        boolean chasing=follow && targetValid && distance>5.76 && distance<4096;
        if (look && targetValid) face(bot,owner,true);
        else if (chasing) face(bot,owner,false);
        bot.zza=chasing?1.0f:0.0f;
        bot.xxa=0; bot.setSprinting(chasing);
        bot.setJumping(chasing && bot.horizontalCollision && bot.onGround());
        long now=bot.level().getServer().getTickCount();
        if(attack && targetValid && distance<=9.0 && fake.hasLineOfSight(owner) && now>=bot.nextAttackTick) {
            face(bot,owner,true);
            bot.nextAttackTick=now+20;
            bot.attack(((CraftPlayer)owner).getHandle());
            bot.swing(InteractionHand.MAIN_HAND);
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
