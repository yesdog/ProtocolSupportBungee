package protocolsupport.injector.pe;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.query.QueryHandler;

public class PEQueryHandler extends QueryHandler {

    public PEQueryHandler(ProxyServer bungee, ListenerInfo listener) {
        super(bungee, listener);
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        //PE UDP channel gets some garbage sometimes, so lets filter it out here
        if (super.acceptInboundMessage(msg)) {
            final ByteBuf data = ((DatagramPacket) msg).content();
            return data.readableBytes() >= 2
                    && data.getUnsignedByte(data.readerIndex()) == 0xFE
                    && data.getUnsignedByte(data.readerIndex() + 1) == 0xFD;
        }
        return false;
    }

}
