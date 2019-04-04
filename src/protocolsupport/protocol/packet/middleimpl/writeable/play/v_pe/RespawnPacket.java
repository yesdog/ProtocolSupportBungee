package protocolsupport.protocol.packet.middleimpl.writeable.play.v_pe;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.md_5.bungee.protocol.packet.Respawn;

import protocolsupport.injector.pe.PEDimSwitchLock;
import protocolsupport.protocol.packet.id.PEPacketId;
import protocolsupport.protocol.packet.middleimpl.writeable.PESingleWriteablePacket;
import protocolsupport.protocol.packet.middleimpl.writeable.login.v_pe.LoginSuccessPacket;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.MiscSerializer;
import protocolsupport.protocol.serializer.PEPacketIdSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;

public class RespawnPacket extends PESingleWriteablePacket<Respawn> {

    public RespawnPacket() {
        super(PEPacketId.Clientbound.PLAY_RESPAWN);
    }

    @Override
    protected void write(ByteBuf data, Respawn packet) {
        VarNumberSerializer.writeSVarInt(data, getPeDimensionId(packet.getDimension()));
        data.writeFloatLE(0); //x
        data.writeFloatLE(0); //y
        data.writeFloatLE(0); //z
        data.writeBoolean(true); //respawn
    }

    @Override
    public Collection<ByteBuf> toData(Respawn packet) {
        final ArrayList<ByteBuf> packets = new ArrayList<>();
        packets.add(super.toData(packet).iterator().next());
        if (packet.getDimension() != cache.getLoginDimension()) { //fake dim switch
            ByteBuf publisherUpdate = Unpooled.buffer();
            PEPacketIdSerializer.writePacketId(publisherUpdate, PEPacketId.Clientbound.CHUNK_PUBLISHER_UPDATE_PACKET);
            VarNumberSerializer.writeSVarInt(publisherUpdate, 0);
            VarNumberSerializer.writeVarInt(publisherUpdate, 0);
            VarNumberSerializer.writeSVarInt(publisherUpdate, 0);
            VarNumberSerializer.writeVarInt(publisherUpdate, 300); //10 chunks.
            packets.add(publisherUpdate);
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    ByteBuf buffer = Unpooled.buffer();
                    PEPacketIdSerializer.writePacketId(buffer, PEPacketId.Clientbound.PLAY_CHUNK_DATA);
                    VarNumberSerializer.writeSVarInt(buffer, x);
                    VarNumberSerializer.writeSVarInt(buffer, z);
                    buffer.writeBytes(getPEChunkData());
                    packets.add(buffer);
                }
            }
            ByteBuf loginSuccess = Unpooled.buffer();
            PEPacketIdSerializer.writePacketId(loginSuccess, PEPacketId.Clientbound.PLAY_PLAY_STATUS);
            loginSuccess.writeInt(LoginSuccessPacket.PLAYER_SPAWN);
            packets.add(loginSuccess);
            packets.add(CustomEventPacket.create(PEDimSwitchLock.AWAIT_DIM_ACK_MESSAGE));
        }
        return packets;
    }

    public static int getPeDimensionId(int dimId) {
        switch (dimId) {
            case -1: {
                return 1;
            }
            case 1: {
                return 2;
            }
            case 0: {
                return 0;
            }
            default: {
                throw new IllegalArgumentException(MessageFormat.format("Unknown dim id {0}", dimId));
            }
        }
    }

    private static byte[] fakePEChunkData;
    static {
        ByteBuf serializer = Unpooled.buffer();
        ArraySerializer.writeVarIntByteArray(serializer, chunkdata -> {
            chunkdata.writeByte(1); //1 section
            chunkdata.writeByte(8); //New subchunk version!
            chunkdata.writeByte(1); //Zero blockstorages :O
            chunkdata.writeByte((1 << 1) | 1);  //Runtimeflag and palette id.
            chunkdata.writeZero(512);
            VarNumberSerializer.writeSVarInt(chunkdata, 1); //Palette size
            VarNumberSerializer.writeSVarInt(chunkdata, 0); //Air
            chunkdata.writeZero(512); //heightmap.
            chunkdata.writeZero(256); //Biomedata.
            chunkdata.writeByte(0); //borders
        });
        fakePEChunkData = MiscSerializer.readAllBytes(serializer);
    }
    public static byte[] getPEChunkData() {
        return fakePEChunkData;
    }
}
