package protocolsupport.injector.pe;

import io.github.waterfallmc.waterfall.QueryResult;
import io.github.waterfallmc.waterfall.event.ProxyQueryEvent;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.query.QueryHandler;
import protocolsupport.protocol.utils.ProtocolVersionsHelper;
import raknetserver.RakNetServer;
import raknetserver.udp.UdpServerChannel;

import java.util.ArrayList;
import java.util.logging.Level;

public class PEProxyServer {

    private final EventLoopGroup ioGroup = UdpServerChannel.NEW_EVENT_GROUP.apply(0);
    private final EventLoopGroup childGroup = new DefaultEventLoopGroup();
    private final ChannelGroup channels = new DefaultChannelGroup(childGroup.next());

    private static String generatePingInfo(BungeeCord bungee, ListenerInfo listenerInfo) {
        final ProxyQueryEvent event = new ProxyQueryEvent(listenerInfo, new QueryResult(listenerInfo.getMotd(),
                "SMP", "ProtocolSupportBungee", bungee.getOnlineCount(),
                listenerInfo.getMaxPlayers(), listenerInfo.getHost().getPort(), listenerInfo.getHost().getHostString(),
                "MCPE", new ArrayList<>(), bungee.getGameVersion()));
        final QueryResult result = bungee.getPluginManager().callEvent(event).getResult();
        return String.join(";",
                "MCPE",
                result.getMotd().replace(";", ":"),
                String.valueOf(result.getVersion()),
                ProtocolVersionsHelper.LATEST_PE.getName().replaceFirst("PE-", ""),
                String.valueOf(result.getOnlinePlayers()),
                String.valueOf(result.getMaxPlayers())
        );
    }

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
                channel.attr(RakNetServer.PING_SUPPLIER).set(() -> generatePingInfo(bungee, listenerInfo));
                channel.pipeline()
                        .addFirst(new UDPBungeeLogger(bungee))
                        .addLast(new QueryHandler(bungee, listenerInfo));
            }
        }))
        .childHandler(new RakNetServer.DefaultChildInitializer(new ChannelInitializer() {
            protected void initChannel(Channel channel) {
                channel.attr(RakNetServer.USER_DATA_ID).set(0xFE);
                channel.attr(RakNetServer.RN_METRICS).set(PERakNetMetrics.INSTANCE);
                channel.pipeline()
                        .addLast(PECompressor.NAME, new PECompressor())
                        .addLast(PEDecompressor.NAME, new PEDecompressor())
                        .addLast(PEProxyNetworkManager.NAME, new PEProxyNetworkManager());
            }
        }));
        channels.add(bootstrap.bind(listenerInfo.getHost()).channel());
    }

    private class UDPBungeeLogger extends ChannelOutboundHandlerAdapter {
        final BungeeCord bungee;

        private UDPBungeeLogger(BungeeCord bungee) {
            this.bungee = bungee;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            bungee.getLogger().log(Level.WARNING, "Error whilst handling UDP packet from PE channel.", cause);
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
