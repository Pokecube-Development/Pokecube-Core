package pokecube.core.network.pokemobs;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.xml.ws.handler.MessageContext;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.AICapWrapper;
import thut.api.entity.ai.IAIMob;
import thut.api.entity.ai.IAIRunnable;
import thut.api.entity.ai.ILogicRunnable;

public class PacketUpdateAI implements IMessage, IMessageHandler<PacketUpdateAI, IMessage>
{
    public int            entityId;
    public CompoundNBT data = new CompoundNBT();

    public static void sendUpdatePacket(IPokemob pokemob, @Nullable String ai, @Nullable String logic)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT savedAI = new CompoundNBT();
        CompoundNBT savedLogic = new CompoundNBT();
        if (ai != null) for (IAIRunnable runnable : pokemob.getAI().aiTasks)
        {
            if (runnable instanceof INBTSerializable && runnable.getIdentifier().equals(ai))
            {
                INBT base = INBTSerializable.class.cast(runnable).serializeNBT();
                savedAI.put(runnable.getIdentifier(), base);
                break;
            }
        }
        if (logic != null) for (ILogicRunnable runnable : pokemob.getAI().aiLogic)
        {
            if (runnable instanceof INBTSerializable && runnable.getIdentifier().equals(logic))
            {
                INBT base = INBTSerializable.class.cast(runnable).serializeNBT();
                savedLogic.put(runnable.getIdentifier(), base);
                break;
            }
        }
        tag.put("ai", savedAI);
        tag.put("logic", savedLogic);
        PacketUpdateAI packet = new PacketUpdateAI();
        packet.data = tag;
        packet.entityId = pokemob.getEntity().getEntityId();
        PokecubeMod.packetPipeline.sendToServer(packet);
    }

    public PacketUpdateAI()
    {
    }

    @Override
    public IMessage onMessage(final PacketUpdateAI message, final MessageContext ctx)
    {
        PokecubeCore.proxy.getMainThreadListener().addScheduledTask(new Runnable()
        {
            @Override
            public void run()
            {
                processMessage(ctx, message);
            }
        });
        return null;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        entityId = buffer.readInt();
        try
        {
            data = buffer.readCompoundTag();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(entityId);
        buffer.writeCompoundTag(data);
    }

    void processMessage(MessageContext ctx, PacketUpdateAI message)
    {
        PlayerEntity player = ctx.getServerHandler().player;
        int id = message.entityId;
        CompoundNBT data = message.data;
        Entity e = PokecubeMod.core.getEntityProvider().getEntity(player.getEntityWorld(), id, true);
        IAIMob ai = e.getCapability(IAIMob.THUTMOBAI, null);
        if (ai instanceof AICapWrapper)
        {
            AICapWrapper wrapper = (AICapWrapper) ai;
            wrapper.deserializeNBT(data);
            wrapper.init();
        }
    }
}
