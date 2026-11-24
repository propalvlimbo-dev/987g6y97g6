package ru.elytrix.bots;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import java.net.InetSocketAddress;

/** In-memory, non-network connection: outbound packets to this player are discarded. */
final class LocalConnection extends Connection {
    private PacketListener inbound;
    LocalConnection() {
        super(PacketFlow.SERVERBOUND);
        this.channel = new EmbeddedChannel();
        this.address = new InetSocketAddress("127.0.0.1", 0);
        this.channel.pipeline().addLast("decoder", new ChannelInboundHandlerAdapter());
        this.channel.pipeline().addLast("encoder", new ChannelOutboundHandlerAdapter() {
            @Override public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) { promise.setSuccess(); }
        });
    }
    @Override public void setReadOnly() { }
    @Override public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
        if (listener != null) {
            try { listener.operationComplete(channel.newSucceededFuture()); }
            catch (Exception ex) { throw new IllegalStateException("Bot packet callback failed", ex); }
        }
    }
    @Override public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocol, T listener) { inbound = listener; }
    @Override public PacketListener getPacketListener() { PacketListener p = super.getPacketListener(); return p == null ? inbound : p; }
}
