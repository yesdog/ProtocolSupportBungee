package protocolsupport;

import java.util.logging.Level;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import protocolsupport.injector.BungeeNettyChannelInjector;
import protocolsupport.injector.pe.PEProxyServer;

public class ProtocolSupport extends Plugin {

	private PEProxyServer peserver;

	@Override
	public void onLoad() {
		try {
			getProxy().getPluginManager().registerCommand(this, new CommandHandler());
			BungeeNettyChannelInjector.inject();
		} catch (Throwable t) {
			t.printStackTrace();
			ProxyServer.getInstance().stop();
		}
	}

	@Override
	public void onEnable() {
		getLogger().log(Level.INFO, "Enabling MCPE support");
		(peserver = new PEProxyServer(getLogger())).start();
	}

	@Override
	public void onDisable() {
		if (peserver != null) {
			peserver.stop();
		}
	}

}
