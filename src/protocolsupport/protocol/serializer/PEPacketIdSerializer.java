package protocolsupport.protocol.serializer;

import io.netty.buffer.ByteBuf;

public class PEPacketIdSerializer {

    public static int readPacketId(ByteBuf from) {
        return VarNumberSerializer.readVarInt(from);
    }

    public static void writePacketId(ByteBuf data, int packetId) {
        VarNumberSerializer.writeVarInt(data, packetId);
    }

    public static int peekPacketId(ByteBuf from) {
        try {
            return readPacketId(from.markReaderIndex());
        } finally {
            from.resetReaderIndex();
        }
    }

}
