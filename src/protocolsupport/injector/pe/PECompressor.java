package protocolsupport.injector.pe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import protocolsupport.protocol.serializer.MiscSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.utils.netty.Allocator;
import protocolsupport.utils.netty.Compressor;

import raknetserver.RakNetServer;

public class PECompressor extends ChannelOutboundHandlerAdapter {

	public static final String NAME = "peproxy-comp";

	private static final int MAX_POOL_BYTES = 2048;

	private final ByteBuf stashBuffer = Allocator.allocateBuffer();
	private final Compressor compressor = Compressor.create();

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		super.handlerRemoved(ctx);
		stashBuffer.release();
		compressor.recycle();
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof ByteBuf) {
			final ByteBuf buf = (ByteBuf) msg;
			try {
				VarNumberSerializer.writeVarInt(stashBuffer, buf.readableBytes());
				stashBuffer.writeBytes(buf);
			} finally {
				buf.release();
			}
			if (stashBuffer.readableBytes() > MAX_POOL_BYTES) {
				flushPacket(ctx);
			}
			promise.trySuccess();
			PERakNetMetrics.INSTANCE.outPrePacket.inc(1);
			return;
		}
		if (msg instanceof RakNetServer.Tick) {
			flushPacket(ctx);
		}
		super.write(ctx, msg, promise);
	}

	protected void flushPacket(ChannelHandlerContext ctx) {
		if (stashBuffer.readableBytes() == 0) {
			return;
		}
		PERakNetMetrics.INSTANCE.outPreComp.inc(stashBuffer.readableBytes());
		final byte[] out = compressor.compress(MiscSerializer.readAllBytes(stashBuffer));
		PERakNetMetrics.INSTANCE.outPostComp.inc(out.length);
		ctx.write(Unpooled.wrappedBuffer(out)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		stashBuffer.clear();
	}

}
