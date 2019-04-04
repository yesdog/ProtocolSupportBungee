package protocolsupport.injector.pe;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

import net.md_5.bungee.BungeeCord;

import protocolsupport.protocol.packet.middleimpl.readable.play.v_pe.CustomEventPacket;
import protocolsupport.protocol.packet.middleimpl.readable.play.v_pe.FromClientPlayerAction;

import java.util.ArrayList;

//lock outbound packet stream until we get a dim switch ack
public class PEDimSwitchLock extends ChannelDuplexHandler {

	public static final String NAME = "peproxy-dimlock";
	public static final String AWAIT_DIM_ACK_MESSAGE = "ps:dimlock";

	protected final ArrayList<ByteBuf> queue = new ArrayList<>(128);
	protected boolean isLocked = false;

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		queue.forEach(ReferenceCountUtil::safeRelease);
		queue.clear();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof ByteBuf) {
			if(isLocked && FromClientPlayerAction.isDimSwitchAck((ByteBuf) msg)) {
				final ArrayList<ByteBuf> qCopy = new ArrayList(queue);
				queue.clear();
				queue.trimToSize();
				isLocked = false;
				qCopy.stream().forEach(ctx::write);
				ctx.flush();
			}

		}
		super.channelRead(ctx, msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof ByteBuf) {
			if (isLocked) {
				queue.add((ByteBuf) msg);
				promise.trySuccess();
				if (queue.size() > 4096) {
					BungeeCord.getInstance().getLogger().warning(
							"PEDimSwitchLock: queue got too large, closing connection.");
					ctx.channel().close();
				}
				return;
			} else if (CustomEventPacket.isTag((ByteBuf) msg, AWAIT_DIM_ACK_MESSAGE)) {
				isLocked = true;
			}
		}
		super.write(ctx, msg, promise);
	}

}
