package ru.elytrix.bots;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import dev.by1337.virtualentity.api.entity.EntityAnimation;
import dev.by1337.virtualentity.api.virtual.player.VirtualPlayer;
import org.by1337.blib.geom.Vec3d;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class BotsPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Bot> bots = new HashMap<>();
    private static final class Menu implements InventoryHolder { public Inventory getInventory() { return null; } }
    private static final class Bot {
        final VirtualPlayer entity; final World world;
        double x,y,z,vx,vy,vz; long lastHit;
        Bot(VirtualPlayer e, Location l) { entity=e; world=l.getWorld(); x=l.getX(); y=l.getY(); z=l.getZ(); }
    }
    @Override public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
    }
    @Override public void onDisable() {
        for (Bot b : bots.values()) b.entity.tick(Collections.emptySet());
        bots.clear();
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cКоманда только для игроков."); return true; }
        Player p=(Player)sender;
        Inventory menu=getServer().createInventory(new Menu(), 27, "§8Тренировочный бот");
        menu.setItem(11, item(Material.PLAYER_HEAD,"§aСоздать бота"));
        menu.setItem(15, item(Material.BARRIER,"§cУбрать бота"));
        p.openInventory(menu); return true;
    }
    private ItemStack item(Material type,String name) {
        ItemStack stack=new ItemStack(type); ItemMeta meta=stack.getItemMeta(); meta.setDisplayName(name); stack.setItemMeta(meta); return stack;
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p=(Player)e.getWhoClicked();
        if (e.getRawSlot()==11) { p.closeInventory(); spawn(p); }
        else if (e.getRawSlot()==15) { p.closeInventory(); remove(p.getUniqueId()); p.sendMessage("§aБот убран."); }
    }
    @EventHandler public void drag(InventoryDragEvent e) { if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true); }
    private void spawn(Player p) {
        remove(p.getUniqueId());
        Location l=p.getLocation(); double dx=l.getDirection().getX(), dz=l.getDirection().getZ(); double len=Math.sqrt(dx*dx+dz*dz);
        if (len<0.001) { dx=0; dz=1; len=1; }
        double x=l.getX()+dx/len*2, z=l.getZ()+dz/len*2, y=l.getY();
        World w=p.getWorld();
        if (!clear(w,x,y,z) || !solid(w,x,y-0.05,z)) { p.sendMessage("§cНет свободного места перед вами. Встаньте на ровную площадку."); return; }
        VirtualPlayer v=VirtualPlayer.create(); v.setName("TrainingBot");
        Bot b=new Bot(v,new Location(w,x,y,z)); bots.put(p.getUniqueId(),b);
        v.setPos(new Vec3d(x,y,z)); v.setYaw((float)Math.toDegrees(Math.atan2(dx,-dz))); v.setOnGround(true);
        v.tick(Collections.singleton(p)); p.sendMessage("§aБот создан. Ударьте его для проверки.");
    }
    private void remove(UUID id) { Bot b=bots.remove(id); if (b!=null) b.entity.tick(Collections.emptySet()); }
    @EventHandler public void quit(PlayerQuitEvent e) { remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void hit(PlayerUseUnknownEntityEvent e) {
        if (!e.isAttack()) return;
        Player p=e.getPlayer(); Bot b=bots.get(p.getUniqueId());
        if (b==null || b.entity.getId()!=e.getEntityId() || !p.getWorld().equals(b.world)) return;
        Location eye=p.getEyeLocation(); double dx=b.x-eye.getX(), dy=b.y+0.9-eye.getY(), dz=b.z-eye.getZ();
        if (dx*dx+dy*dy+dz*dz>25.0 || System.nanoTime()-b.lastHit<250_000_000L) return;
        b.lastHit=System.nanoTime(); double horizontal=Math.sqrt(dx*dx+dz*dz);
        if (horizontal<0.01) { dx=eye.getDirection().getX(); dz=eye.getDirection().getZ(); horizontal=Math.sqrt(dx*dx+dz*dz); }
        if (horizontal>0.01) { double force=p.isSprinting()?0.60:0.40; b.vx=dx/horizontal*force; b.vz=dz/horizontal*force; b.vy=0.36; }
        b.entity.playAnimation(EntityAnimation.TAKE_DAMAGE); p.playSound(new Location(b.world,b.x,b.y,b.z),Sound.ENTITY_PLAYER_HURT,1f,1f);
    }
    private void tick() {
        Iterator<Map.Entry<UUID,Bot>> it=bots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID,Bot> entry=it.next(); Bot b=entry.getValue(); Player p=Bukkit.getPlayer(entry.getKey());
            if (p==null || !p.isOnline() || !p.getWorld().equals(b.world) || b.y < b.world.getMinHeight()-16) { b.entity.tick(Collections.emptySet()); it.remove(); continue; }
            if (clear(b.world,b.x+b.vx,b.y,b.z)) b.x+=b.vx; else b.vx=0;
            if (clear(b.world,b.x,b.y,b.z+b.vz)) b.z+=b.vz; else b.vz=0;
            // Small steps prevent falling through floors when velocity increases.
            double remaining=b.vy; int steps=Math.max(1,(int)Math.ceil(Math.abs(remaining)/0.2));
            for (int i=0;i<steps;i++) { double step=remaining/steps; if (clear(b.world,b.x,b.y+step,b.z)) b.y+=step; else { b.vy=0; break; } }
            boolean ground=solid(b.world,b.x,b.y-0.04,b.z);
            b.vx*=ground?0.60:0.91; b.vz*=ground?0.60:0.91; b.vy=(b.vy-0.08)*0.98;
            b.entity.setPos(new Vec3d(b.x,b.y,b.z)); b.entity.setOnGround(ground); b.entity.tick(Collections.singleton(p));
        }
    }
    private static int floor(double v) { return (int)Math.floor(v); }
    private static boolean solid(World w,double x,double y,double z) { return !w.getBlockAt(floor(x),floor(y),floor(z)).isPassable(); }
    private static boolean clear(World w,double x,double y,double z) {
        for (double ox : new double[]{-0.29,0.29}) for (double oz : new double[]{-0.29,0.29})
            for (double oy : new double[]{0.01,0.9,1.79}) if (solid(w,x+ox,y+oy,z+oz)) return false;
        return true;
    }
}
