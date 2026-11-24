package ru.elytrix.bots;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Suppresses positional/entity sound packets to training participants only. */
public final class QuietSounds {
    private static final Set<UUID> quiet=ConcurrentHashMap.newKeySet();
    private QuietSounds() {}
    public static void install(JavaPlugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,ListenerPriority.HIGHEST,
                PacketType.Play.Server.NAMED_SOUND_EFFECT,PacketType.Play.Server.ENTITY_SOUND) {
            @Override public void onPacketSending(PacketEvent e) {
                if(e.getPlayer()!=null && quiet.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
            }
        });
    }
    public static void add(UUID id) { quiet.add(id); }
    public static void remove(UUID id) { quiet.remove(id); }
    public static void clear() { quiet.clear(); }
}
