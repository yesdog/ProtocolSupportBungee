package protocolsupport.injector.pe;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;

import raknetserver.RakNetServer;
import raknetserver.udp.UdpServerChannel;

import java.nio.channels.ClosedChannelException;
import java.util.logging.Level;

public class PEProxyServer {

    private final EventLoopGroup ioGroup = UdpServerChannel.NEW_EVENT_GROUP.apply(0);
    private final EventLoopGroup childGroup = new DefaultEventLoopGroup();
    private final ChannelGroup channels = new DefaultChannelGroup(childGroup.next());

    private void newListener(ListenerInfo listenerInfo) {
        if (listenerInfo.isQueryEnabled() && listenerInfo.getQueryPort() == listenerInfo.getHost().getPort()) {
            throw new IllegalArgumentException(
                    "[MCPE] Listener has query port enabled on the same port as " +
                    "the server connection. PE port will handle queries just fine.");
        }

        final BungeeCord bungee = BungeeCord.getInstance();
        final ServerBootstrap bootstrap = new ServerBootstrap()
        .group(ioGroup, childGroup)
        .channelFactory(() -> new UdpServerChannel())
        .handler(new RakNetServer.DefaultIoInitializer(new ChannelInitializer() {
            protected void initChannel(Channel channel) {
                channel.pipeline()
                        .addFirst(new BungeeLogger(bungee, channel + " (PE UDP channel):"))
                        .addLast(new PEProxyServerInfoHandler(bungee, listenerInfo))
                        .addLast(new PEQueryHandler(bungee, listenerInfo));
            }
        }))
        .childHandler(new RakNetServer.DefaultChildInitializer(new ChannelInitializer() {
            protected void initChannel(Channel channel) {
                channel.attr(RakNetServer.USER_DATA_ID).set(0xFE);
                channel.attr(RakNetServer.RN_METRICS).set(PERakNetMetrics.INSTANCE);
                channel.pipeline()
                        .addFirst(new BungeeLogger(bungee, channel + " (PE child channel):"))
                        .addLast(PECompressor.NAME, new PECompressor())
                        .addLast(PEDecompressor.NAME, new PEDecompressor())
                        .addLast(PEProxyNetworkManager.NAME, new PEProxyNetworkManager());
            }
        }));
        channels.add(bootstrap.bind(listenerInfo.getHost()).channel());
    }

    private class BungeeLogger extends ChannelOutboundHandlerAdapter {
        final BungeeCord bungee;
        final String desc;

        private BungeeLogger(BungeeCord bungee, String desc) {
            this.bungee = bungee;
            this.desc = desc;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            final Level level;
            if (cause instanceof ClosedChannelException) {
                level = Level.FINE;
            } else {
                level = Level.WARNING;
            }
            bungee.getLogger().log(level, "Error handling packet on " + desc, cause);
        }
    }

    public void start() {
        stop();
        try {
            BungeeCord.getInstance().getConfig().getListeners().forEach(this::newListener);
        } catch (Exception e) {
            stop();
            throw e;
        }
    }

    public void stop() {
        channels.close().syncUninterruptibly();
    }

}
