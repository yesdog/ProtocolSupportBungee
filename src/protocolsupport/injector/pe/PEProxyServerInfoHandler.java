package protocolsupport.injector.pe;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;

import protocolsupport.protocol.utils.ProtocolVersionsHelper;

import raknetserver.pipeline.PingHandler;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public class PEProxyServerInfoHandler extends PingHandler {

    public static final int PACKET_ID = 3;

    protected final ListenerInfo listenerInfo;
    protected final BungeeCord bungee;

    public PEProxyServerInfoHandler(BungeeCord bungee, ListenerInfo listenerInfo) {
        this.listenerInfo = listenerInfo;
        this.bungee = bungee;
    }

    protected void handlePing(InetSocketAddress sender, long serverId, Consumer<String> respond) {
        final ServerPing.Protocol protocol = new ServerPing.Protocol(
                "", //leave version blank, we do multi-version.
                101 //bare minimum protocol version
        );
        final ServerPing.Players players = new ServerPing.Players(
                listenerInfo.getMaxPlayers(), bungee.getOnlineCount(), new ServerPing.PlayerInfo[0]
        );
        final BaseComponent desc = new TextComponent(TextComponent.fromLegacyText(listenerInfo.getMotd()));
        final ServerPing serverPing = new ServerPing(protocol, players, desc, null);
        final ProxyPingEvent ev = new ProxyPingEvent(new PingConnection(sender), serverPing, (event, throwable) -> {
            if (throwable != null) {
                bungee.getLogger().log(Level.WARNING, "Failed processing PE ping:", throwable);
                respond.accept("");
            } else {
                final ServerPing result = event.getResponse();
                respond.accept(String.join(";",
                        "MCPE",
                        result.getDescriptionComponent().toLegacyText().replace(";", "\\;"),
                        String.valueOf(result.getVersion().getProtocol()),
                        result.getVersion().getName(),
                        String.valueOf(result.getPlayers().getOnline()),
                        String.valueOf(result.getPlayers().getMax()),
                        String.valueOf(serverId)
                ));
            }
        });
        bungee.getPluginManager().callEvent(ev);
    }

    protected class PingConnection implements PendingConnection {

        final InetSocketAddress remoteAddress;

        PingConnection(InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public String getName() {
            return null;
        }

        public int getVersion() {
            return ProtocolVersionsHelper.LATEST_PE.getId();
        }

        public InetSocketAddress getVirtualHost() {
            return null;
        }

        public ListenerInfo getListener() {
            return listenerInfo;
        }

        public String getUUID() {
            return null;
        }

        public UUID getUniqueId() {
            return null;
        }

        public void setUniqueId(UUID uuid) {}

        public boolean isOnlineMode() {
            return bungee.config.isOnlineMode();
        }

        public void setOnlineMode(boolean b) {}

        public boolean isLegacy() {
            return true;
        }

        public InetSocketAddress getAddress() {
            return remoteAddress;
        }

        public void disconnect(String s) {}

        public void disconnect(BaseComponent... baseComponents) {}

        public void disconnect(BaseComponent baseComponent) {}

        public boolean isConnected() {
            return false;
        }

        public Unsafe unsafe() {
            return x -> {};
        }

    }

}
