package protocolsupport.injector.pe;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.ReferenceCountUtil;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;

import network.ycc.raknet.utils.DataSerializer;
import network.ycc.raknet.server.RakNetServer;
import network.ycc.raknet.server.channel.RakNetChildChannel;

import network.ycc.raknet.RakNet;
import network.ycc.raknet.packet.ConnectionFailed;
import network.ycc.raknet.server.channel.RakNetServerChannel;

import java.nio.channels.ClosedChannelException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PEProxyServer {

    private final Logger logger;
    private final EventLoopGroup ioGroup = RakNetServer.DEFAULT_CHANNEL_EVENT_GROUP.get();
    private final EventLoopGroup childGroup = new DefaultEventLoopGroup();
    private final ChannelGroup channels = new DefaultChannelGroup(childGroup.next());

    public PEProxyServer(Logger logger) {
        this.logger = logger;
    }

    private void newListener(ListenerInfo listenerInfo) {
        if (listenerInfo.isQueryEnabled() && listenerInfo.getQueryPort() == listenerInfo.getHost().getPort()) {
            throw new IllegalArgumentException(
                    "[MCPE] Listener has query port enabled on the same port as the server " +
                    "connection. Disable it, the PE port will handle queries just fine.");
        }

        final BungeeCord bungee = BungeeCord.getInstance();
        final ServerBootstrap bootstrap = new ServerBootstrap()
        .group(ioGroup, childGroup)
        .channelFactory(() -> new RakNetServer(RakNetServer.DEFAULT_CHANNEL_CLASS))
        .option(UnixChannelOption.SO_REUSEPORT, true)
        .option(RakNet.SERVER_ID, UUID.randomUUID().getMostSignificantBits())
        .option(RakNet.METRICS, PERakNetMetrics.INSTANCE)
        .childOption(RakNet.USER_DATA_ID, 0xFE)
        .handler(new RakNetServer.DefaultIoInitializer(new ChannelInitializer() {
            protected void initChannel(Channel channel) {
                channel.pipeline()
                        .addLast(new PEProxyServerInfoHandler(bungee, listenerInfo))
                        .addLast(new PEQueryHandler(bungee, listenerInfo))
                        .addLast(new PluginLoggerInitializer());
            }
        }))
        .childHandler(new RakNetServer.DefaultChildInitializer(new ChannelInitializer() {
            protected void initChannel(Channel channel) {
                channel.pipeline() //TODO: add read timeout here, not raknet
                        .addLast(PECompressor.NAME, new PECompressor())
                        .addLast(PEDecompressor.NAME, new PEDecompressor())
                        .addLast(PEDimSwitchLock.NAME, new PEDimSwitchLock())
                        .addLast(PEProxyNetworkManager.NAME, new PEProxyNetworkManager())
                        .addLast(new PluginLoggerInitializer());
            }
        }));
        final Channel channel = bootstrap.bind(listenerInfo.getHost()).channel();
        channel.closeFuture().addListener(v -> logger.warning("Server channel closed: " + channel));
        channels.add(channel);
    }

    public void start() {
        logger.info("Starting PE server, using " + RakNetServer.DEFAULT_CHANNEL_CLASS.getSimpleName());
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

    private class PluginLoggerInitializer extends ChannelInitializer {
        //TODO: make this better somehow. place last, and move it afterwards?
        protected void initChannel(Channel channel) {
            //delay so we know its last
            channel.eventLoop().execute(() ->
                channel.pipeline().addLast(new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        if (msg instanceof DatagramPacket && ctx.channel() instanceof RakNetServerChannel) {
                            logger.log(Level.FINER, "Stray datagram sent to server channel handler");
                            ReferenceCountUtil.safeRelease(msg);
                        } else {
                            ctx.fireChannelRead(msg);
                        }
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        final Level level;
                        final String prefix;
                        if (!ctx.channel().isActive()) {
                            return;
                        } else if (cause instanceof ReadTimeoutException) {
                            prefix = "Read timeout on";
                            level = Level.INFO;
                        } else if (cause instanceof ClosedChannelException) {
                            prefix = "Closed channel";
                            level = Level.FINE;
                        } else if (cause instanceof DataSerializer.MagicDecodeException) {
                            prefix = "Bad RakNet magic";
                            level = Level.FINE;
                        } else {
                            prefix = "Pipeline error on";
                            level = Level.WARNING;
                        }
                        if (channel instanceof RakNetChildChannel && channel.isOpen()) {
                            channel.writeAndFlush(new ConnectionFailed()).addListeners(
                                    ChannelFutureListener.CLOSE, ChannelFutureListener.CLOSE_ON_FAILURE);
                        }
                        if (level == Level.INFO) {
                            logger.log(level, prefix + " " + channel);
                        } else {
                            logger.log(level, prefix + " " + channel, cause);
                        }
                    }
                })
            );
        }
    }

}
