package protocolsupport.injector.pe;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

import io.netty.channel.unix.UnixChannelOption;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;

import raknetserver.RakNetServer;
import raknetserver.packet.ConnectionFailed;

import java.nio.channels.ClosedChannelException;
import java.util.UUID;
import java.util.logging.Level;

public class PEProxyServer {

    private final EventLoopGroup ioGroup = RakNetServer.DEFAULT_CHANNEL_EVENT_GROUP.get();
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
        .channelFactory(() -> new RakNetServer(RakNetServer.DEFAULT_CHANNEL_CLASS))
        .option(UnixChannelOption.SO_REUSEPORT, true)
        .option(RakNetServer.SERVER_ID, UUID.randomUUID().getLeastSignificantBits())
        .option(RakNetServer.METRICS, PERakNetMetrics.INSTANCE)
        .childOption(RakNetServer.USER_DATA_ID, 0xFE)
        .handler(new RakNetServer.DefaultIoInitializer(new ChannelInitializer() {
            protected void initChannel(Channel channel) {
                channel.pipeline()
                        .addLast(new PEProxyServerInfoHandler(bungee, listenerInfo))
                        .addLast(new PEQueryHandler(bungee, listenerInfo))
                        .addLast(new BungeeLogger(bungee, channel + " (PE UDP channel):", false));
            }
        }))
        .childHandler(new RakNetServer.DefaultChildInitializer(new ChannelInitializer() {
            protected void initChannel(Channel channel) {
                channel.pipeline()
                        .addLast(PECompressor.NAME, new PECompressor())
                        .addLast(PEDecompressor.NAME, new PEDecompressor())
                        .addLast(PEProxyNetworkManager.NAME, new PEProxyNetworkManager())
                        .addLast(new BungeeLogger(bungee, channel + " (PE child channel):", true));
            }
        }));
        channels.add(bootstrap.bind(listenerInfo.getHost()).channel());
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

    private class BungeeLogger extends ChannelOutboundHandlerAdapter {
        final BungeeCord bungee;
        final String desc;
        final boolean isChild;

        private BungeeLogger(BungeeCord bungee, String desc, boolean isChild) {
            this.bungee = bungee;
            this.desc = desc;
            this.isChild = isChild;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            final Level level;
            if (cause instanceof ClosedChannelException) {
                level = Level.FINE;
            } else {
                level = Level.WARNING;
            }
            if (isChild && ctx.channel().isOpen()) {
                ctx.writeAndFlush(new ConnectionFailed()).addListeners(
                        ChannelFutureListener.CLOSE, ChannelFutureListener.CLOSE_ON_FAILURE);
            }
            bungee.getLogger().log(level, "Error handling packet on " + desc, cause);
        }
    }

}
