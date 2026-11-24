package ru.elytrix.bots;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
final class LocalPacketListener extends ServerGamePacketListenerImpl {
    private final TrainingPlayer bot;
    LocalPacketListener(MinecraftServer server, LocalConnection connection, TrainingPlayer player) {
        super(server, connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        bot = player;
    }
    @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket motion && motion.getId() == bot.getId()) bot.acceptKnockback(motion.getMovement());
        super.send(packet);
    }
    @Override public boolean hasClientLoaded() { return true; }
}
