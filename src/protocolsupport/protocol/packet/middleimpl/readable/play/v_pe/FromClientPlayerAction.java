package protocolsupport.protocol.packet.middleimpl.readable.play.v_pe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.protocol.PacketWrapper;
import protocolsupport.protocol.packet.id.PEPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.PEDefinedReadableMiddlePacket;
import protocolsupport.protocol.pipeline.version.v_pe.NoopDefinedPacket;
import protocolsupport.protocol.serializer.PEPacketIdSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;

import java.util.Collection;
import java.util.Collections;

public class FromClientPlayerAction extends PEDefinedReadableMiddlePacket {

    protected static final int DIMENSION_CHANGE_ACK = 14;

    protected int action;

    public FromClientPlayerAction() {
        super(PEPacketId.Serverbound.PLAY_PLAYER_ACTION);
    }

    @Override
    protected void read0(ByteBuf from) {
        from.skipBytes(from.readableBytes());
    }

    @Override
    public Collection<PacketWrapper> toNative() {
        return Collections.singletonList(new PacketWrapper(new NoopDefinedPacket(), Unpooled.wrappedBuffer(readbytes)));
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
