package protocolsupport.injector.pe;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

import net.md_5.bungee.BungeeCord;

import protocolsupport.protocol.packet.id.PEPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.play.v_pe.CustomEventPacket;
import protocolsupport.protocol.serializer.PEPacketIdSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;

import java.util.ArrayList;

//lock outbound packet stream until we get a dim switch ack
public class PEDimSwitchLock extends ChannelDuplexHandler {

	public static final String NAME = "peproxy-dimlock";
	public static final String AWAIT_DIM_ACK_MESSAGE = "ps:dimlock";

	protected static final int DIMENSION_CHANGE_ACK = 14;

	protected static int MAX_QUEUE_SIZE = 4096;

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
			if(isLocked && isDimSwitchAck((ByteBuf) msg)) {
				final ArrayList<ByteBuf> qCopy = new ArrayList(queue);
				queue.clear();
				queue.trimToSize();
				isLocked = false;
				for (ByteBuf data : qCopy) {
					write(ctx, data, ctx.voidPromise());
				}
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
				if (queue.size() > MAX_QUEUE_SIZE) {
					BungeeCord.getInstance().getLogger().warning(
							"PEDimSwitchLock: queue got too large, closing connection.");
					ctx.channel().close();
				}
				return;
			} else if (CustomEventPacket.isTag((ByteBuf) msg, AWAIT_DIM_ACK_MESSAGE) ||
					PEPacketIdSerializer.peekPacketId((ByteBuf) msg)
							== PEPacketId.Clientbound.EXT_PS_AWAIT_DIM_SWITCH_ACK) {
				isLocked = true;
				return;
			}
		}
		super.write(ctx, msg, promise);
	}

	public static boolean isDimSwitchAck(ByteBuf data) {
		if (PEPacketIdSerializer.peekPacketId(data) == PEPacketId.Serverbound.PLAY_PLAYER_ACTION) {
			final ByteBuf copy = data.duplicate();
			PEPacketIdSerializer.readPacketId(copy);
			VarNumberSerializer.readVarLong(copy); // entity id
			return VarNumberSerializer.readSVarInt(copy) == DIMENSION_CHANGE_ACK;
		}
		return false;
	}

}
