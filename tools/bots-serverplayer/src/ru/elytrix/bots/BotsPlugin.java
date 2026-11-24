package ru.elytrix.bots;

import com.mojang.authlib.GameProfile;
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
    private static final class Menu implements InventoryHolder {
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
        Inventory menu = getServer().createInventory(new Menu(),27,"§8Тренировочный бот");
        menu.setItem(11,item(Material.PLAYER_HEAD,"§aСоздать бота"));
        menu.setItem(15,item(Material.BARRIER,"§cУбрать бота")); p.openInventory(menu); return true;
    }
    private ItemStack item(Material material,String name) {
        ItemStack item = new ItemStack(material); ItemMeta meta=item.getItemMeta(); meta.setDisplayName(name); item.setItemMeta(meta); return item;
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if(event.getRawSlot()==11) { p.closeInventory(); spawn(p); }
        else if (event.getRawSlot()==15) { p.closeInventory(); remove(p.getUniqueId()); p.sendMessage("§aБот убран."); }
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
            bot=new TrainingPlayer(server,world,new GameProfile(uuid,"Bot_"+uuid.toString().replace("-","").substring(0,10)));
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
