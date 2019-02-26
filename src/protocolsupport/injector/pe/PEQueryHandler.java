package protocolsupport.injector.pe;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToByteEncoder;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.query.QueryHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class PEQueryHandler extends QueryHandler {

    public PEQueryHandler(ProxyServer bungee, ListenerInfo listener) {
        super(bungee, listener);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        final ByteBuf data = msg.content();
        //PE UDP channel gets some garbage sometimes, so lets filter it out here
        if (data.readableBytes() >= 2
                && data.getUnsignedByte(data.readerIndex()) == 0xFE
                && data.getUnsignedByte(data.readerIndex() + 1) == 0xFD) {
            super.channelRead0(ctx, msg);
        }
    }

}
