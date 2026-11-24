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

/** Mutes shared sound and particle broadcasts; permits our private hit cue. */
public final class QuietSounds {
    private static final Set<UUID> quiet=ConcurrentHashMap.newKeySet();
    public static final float CUE_VOLUME=0.913572f, CUE_PITCH=0.731248f;
    private QuietSounds() {}
    public static boolean isCue(Float volume,Float pitch) {
        return volume!=null && pitch!=null && Float.compare(volume,CUE_VOLUME)==0 && Float.compare(pitch,CUE_PITCH)==0;
    }
    public static void install(JavaPlugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,ListenerPriority.HIGHEST,
                PacketType.Play.Server.NAMED_SOUND_EFFECT,PacketType.Play.Server.ENTITY_SOUND,PacketType.Play.Server.WORLD_PARTICLES) {
            @Override public void onPacketSending(PacketEvent e) {
                if(e.getPlayer()==null || !quiet.contains(e.getPlayer().getUniqueId())) return;
                try {
                    if(e.getPacket()!=null && e.getPacketType()==PacketType.Play.Server.NAMED_SOUND_EFFECT &&
                       isCue(e.getPacket().getFloat().readSafely(0),e.getPacket().getFloat().readSafely(1))) return;
                } catch(RuntimeException ignored) { /* fail closed if the packet layout is incompatible */ }
                e.setCancelled(true);
            }
        });
    }
    public static void add(UUID id) { quiet.add(id); }
    public static void remove(UUID id) { quiet.remove(id); }
    public static void clear() { quiet.clear(); }
}
