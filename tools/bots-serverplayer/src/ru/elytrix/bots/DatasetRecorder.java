package ru.elytrix.bots;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Short, explicitly started recordings. All methods run on the server thread. */
final class DatasetRecorder implements Listener {
    private static final long MAX_TICKS = 20L * 180;
    private static final int MAX_SESSIONS = 10;
    private final BotsPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private long tick;
    private static final class Session {
        final Player player;
        final String label;
        final long start;
        final StringBuilder records = new StringBuilder();
        Player target;
        int targetIndex;
        Session(Player player, String label, long start) { this.player=player; this.label=label; this.start=start; }
        long elapsed(long now) { return now-start; }
    }
    DatasetRecorder(BotsPlugin plugin) { this.plugin=plugin; }
    void start(Player player, String value) {
        String label=value.toLowerCase(Locale.ROOT);
        if (label.equals("creat")) label="crit";
        if (!Set.of("pro","noob","crit","combo").contains(label)) {
            player.sendMessage("§cИспользуй: /bots dataset <pro|noob|crit|combo|stop>"); return;
        }
        if (sessions.containsKey(player.getUniqueId())) { player.sendMessage("§cЗапись уже идёт. Сначала /bots dataset stop"); return; }
        if (sessions.size()>=MAX_SESSIONS) { player.sendMessage("§cСлишком много одновременных записей. Попробуй позже."); return; }
        Session session=new Session(player,label,tick);
        session.records.append("{\"type\":\"meta\",\"schema\":1,\"label\":\"").append(label).append("\",\"sample_ticks\":1,\"max_ticks\":").append(MAX_TICKS).append("}\n");
        sessions.put(player.getUniqueId(),session);
        player.sendMessage("§aЗапись «"+label+"» началась на 3 минуты. Остановить: /bots dataset stop.");
        player.sendMessage("§7Записываются движение, удары и состояние последнего PvP-противника без ников и координат. Получи согласие другого игрока.");
    }
    void tick() {
        tick++;
        for (Session s : new ArrayList<>(sessions.values())) {
            if (!s.player.isOnline() || s.elapsed(tick)>=MAX_TICKS) { stop(s.player.getUniqueId(),true); continue; }
            Player t=s.target;
            if (t!=null && (!t.isValid() || !t.getWorld().equals(s.player.getWorld()))) { s.target=null; t=null; }
            StringBuilder line=s.records;
            line.append("{\"type\":\"frame\",\"tick\":").append(s.elapsed(tick)).append(",\"self\":"); state(line,s.player);
            if (t!=null) {
                Location a=s.player.getLocation(), b=t.getLocation();
                line.append(",\"target_id\":").append(s.targetIndex).append(",\"target_is_bot\":").append(plugin.isBot(t));
                line.append(",\"relative\":"); vector(line,b.toVector().subtract(a.toVector()));
                line.append(",\"target\":"); state(line,t);
            }
            line.append("}\n");
        }
    }
    private static void number(StringBuilder s, double v) { if (Double.isFinite(v)) s.append(Math.round(v*10000.0)/10000.0); else s.append("null"); }
    private static void vector(StringBuilder s, Vector v) { s.append('['); number(s,v.getX()); s.append(','); number(s,v.getY()); s.append(','); number(s,v.getZ()); s.append(']'); }
    private static void state(StringBuilder s,Player p) {
        Location l=p.getLocation(); ItemStack item=p.getInventory().getItemInMainHand();
        s.append("{\"yaw\":"); number(s,l.getYaw()); s.append(",\"pitch\":");number(s,l.getPitch());
        s.append(",\"velocity\":");vector(s,p.getVelocity());s.append(",\"health\":");number(s,p.getHealth());
        s.append(",\"ground\":").append(p.isOnGround()).append(",\"sprint\":").append(p.isSprinting()).append(",\"sneak\":").append(p.isSneaking());
        s.append(",\"fall\":");number(s,p.getFallDistance());s.append(",\"cooldown\":");number(s,p.getAttackCooldown());
        s.append(",\"item\":\"").append(item.getType().name()).append("\"}");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false) public void damage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker) || !(e.getEntity() instanceof Player victim)) return;
        recordHit(sessions.get(attacker.getUniqueId()),victim,"out",e);
        recordHit(sessions.get(victim.getUniqueId()),attacker,"in",e);
    }
    private void recordHit(Session s,Player target,String direction,EntityDamageByEntityEvent e) {
        if(s==null || !s.player.getWorld().equals(target.getWorld())) return;
        if (s.target==null || !s.target.getUniqueId().equals(target.getUniqueId())) { s.target=target; s.targetIndex++; }
        s.records.append("{\"type\":\"hit\",\"tick\":").append(s.elapsed(tick)).append(",\"direction\":\"").append(direction).append("\",\"target_id\":").append(s.targetIndex).append(",\"cancelled\":").append(e.isCancelled()).append(",\"damage\":");
        number(s.records,e.getFinalDamage()); s.records.append("}\n");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void swing(PlayerAnimationEvent e) {
        Session s=sessions.get(e.getPlayer().getUniqueId());
        if(s!=null) s.records.append("{\"type\":\"swing\",\"tick\":").append(s.elapsed(tick)).append("}\n");
    }
    @EventHandler public void quit(PlayerQuitEvent e) { stop(e.getPlayer().getUniqueId(),false); }
    void stop(UUID id,boolean notify) {
        Session s=sessions.remove(id);
        if(s==null) { if(notify) { Player p=plugin.getServer().getPlayer(id); if(p!=null) p.sendMessage("§cЗапись не запущена."); } return; }
        s.records.append("{\"type\":\"end\",\"ticks\":").append(s.elapsed(tick)).append("}\n");
        String filename=s.label+"-"+UUID.randomUUID()+".jsonl";
        try {
            Path dir=plugin.getDataFolder().toPath().resolve("datasets"); Files.createDirectories(dir);
            Files.writeString(dir.resolve(filename),s.records,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            if(notify && s.player.isOnline()) s.player.sendMessage("§aЗапись сохранена: plugins/ElytrixBotsTrain/datasets/"+filename);
            plugin.getLogger().info("Датасет сохранён: datasets/"+filename);
        } catch(IOException failure) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,"Не удалось сохранить датасет",failure);
            if(s.player.isOnline()) s.player.sendMessage("§cОшибка сохранения датасета, проверь консоль.");
        }
    }
    void close() { for(UUID id:new ArrayList<>(sessions.keySet())) stop(id,false); }
}
