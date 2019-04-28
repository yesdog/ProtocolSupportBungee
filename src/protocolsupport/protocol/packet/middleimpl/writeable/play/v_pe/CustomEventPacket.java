package protocolsupport.protocol.packet.middleimpl.writeable.play.v_pe;

import io.netty.buffer.ByteBuf;

import io.netty.buffer.Unpooled;
import net.md_5.bungee.protocol.packet.PluginMessage;

import protocolsupport.protocol.packet.id.PEPacketId;
import protocolsupport.protocol.packet.middleimpl.writeable.PESingleWriteablePacket;
import protocolsupport.protocol.serializer.PEPacketIdSerializer;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;

public class CustomEventPacket extends PESingleWriteablePacket<PluginMessage> {

    public CustomEventPacket() {
        super(PEPacketId.Dualbound.CUSTOM_EVENT);
    }

    @Override
    protected void write(ByteBuf data, PluginMessage packet) {
        write(data, packet.getTag(), packet.getData());
    }

    static void write(ByteBuf buf, String tag, byte[] data) {
        StringSerializer.writeVarIntUTF8String(buf, tag);
        VarNumberSerializer.writeVarInt(buf, data.length);
        buf.writeBytes(data);
    }

    static ByteBuf create(String tag) {
        final ByteBuf out = Unpooled.buffer();
        PEPacketIdSerializer.writePacketId(out, PEPacketId.Dualbound.CUSTOM_EVENT);
        write(out, tag, new byte[0]);
        return out;
    }
}
