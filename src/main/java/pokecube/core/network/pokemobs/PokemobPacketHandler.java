package pokecube.core.network.pokemobs;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.server.ServerWorld;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import thut.core.common.network.Packet;

/**
 * This class handles the packets sent for the IPokemob Entities.
 *
 * @author Thutmose
 */
public class PokemobPacketHandler
{
    public static class MessageServer extends Packet
    {

        public static final byte RETURN       = 0;
        public static final byte CANCELEVOLVE = 12;

        PacketBuffer buffer;;

        public MessageServer()
        {
        }

        public MessageServer(byte messageid, int entityId)
        {
            this.buffer = new PacketBuffer(Unpooled.buffer(9));
            this.buffer.writeByte(messageid);
            this.buffer.writeInt(entityId);
        }

        public MessageServer(byte channel, int id, CompoundNBT nbt)
        {
            this.buffer = new PacketBuffer(Unpooled.buffer(9));
            this.buffer.writeByte(channel);
            this.buffer.writeInt(id);
            this.buffer.writeCompoundTag(nbt);
        }

        public MessageServer(byte[] data)
        {
            this.buffer = new PacketBuffer(Unpooled.copiedBuffer(data));
        }

        public MessageServer(PacketBuffer buffer)
        {
            this.buffer = buffer;
        }

        @Override
        public void handleServer(ServerPlayerEntity player)
        {
            final byte channel = this.buffer.readByte();
            final int id = this.buffer.readInt();
            final ServerWorld world = player.getServerWorld();
            final Entity entity = PokecubeCore.getEntityProvider().getEntity(world, id, true);
            final IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
            if (pokemob == null) return;
            if (channel == MessageServer.RETURN) pokemob.onRecall();
            else if (channel == MessageServer.CANCELEVOLVE) pokemob.cancelEvolve();
        }

        @Override
        public void write(PacketBuffer buf)
        {
            if (this.buffer == null) this.buffer = new PacketBuffer(Unpooled.buffer());
            buf.writeBytes(this.buffer);
        }
    }

    public static MessageServer makeServerPacket(byte channel, byte[] data)
    {
        final byte[] packetData = new byte[data.length + 1];
        packetData[0] = channel;

        for (int i = 1; i < packetData.length; i++)
            packetData[i] = data[i - 1];
        return new MessageServer(packetData);
    }
}
