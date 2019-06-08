package pokecube.core.network.packets;

import java.io.IOException;

import javax.xml.ws.handler.MessageContext;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.api.distmarker.Dist;
import pokecube.core.PokecubeCore;
import pokecube.core.ai.properties.IGuardAICapability;
import pokecube.core.client.gui.helper.RouteEditHelper;
import pokecube.core.events.handlers.EventsHandler;
import pokecube.core.handlers.Config;
import pokecube.core.interfaces.PokecubeMod;

public class PacketSyncRoutes implements IMessage, IMessageHandler<PacketSyncRoutes, IMessage>
{
    public int            entityId;
    public CompoundNBT data = new CompoundNBT();

    public static void sendUpdateClientPacket(Entity mob, ServerPlayerEntity player, boolean gui)
    {
        IGuardAICapability guard = mob.getCapability(EventsHandler.GUARDAI_CAP, null);
        PacketSyncRoutes packet = new PacketSyncRoutes();
        packet.data.put("R", guard.serializeTasks());
        packet.data.putBoolean("O", gui);
        packet.entityId = mob.getEntityId();
        PokecubeMod.packetPipeline.sendTo(packet, player);
    }

    public static void sendServerPacket(Entity mob, INBT tag)
    {
        PacketSyncRoutes packet = new PacketSyncRoutes();
        packet.entityId = mob.getEntityId();
        if (tag instanceof CompoundNBT) packet.data = (CompoundNBT) tag;
        PokecubeMod.packetPipeline.sendToServer(packet);
    }

    public PacketSyncRoutes()
    {
    }

    @Override
    public IMessage onMessage(final PacketSyncRoutes message, final MessageContext ctx)
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

    void processMessage(MessageContext ctx, PacketSyncRoutes message)
    {
        PlayerEntity player;
        int id = message.entityId;
        CompoundNBT data = message.data;
        if (ctx.side == Dist.CLIENT)
        {
            player = PokecubeCore.getPlayer(null);
        }
        else
        {
            player = ctx.getServerHandler().player;
        }
        Entity e = PokecubeMod.core.getEntityProvider().getEntity(player.getEntityWorld(), id, true);
        if (e == null) return;
        IGuardAICapability guard = e.getCapability(EventsHandler.GUARDAI_CAP, null);

        if (guard != null)
        {
            if (ctx.side == Dist.CLIENT)
            {
                guard.loadTasks((ListNBT) data.getTag("R"));
                if (data.getBoolean("O"))
                {
                    sendServerPacket(e, null);
                }
            }
            else
            {
                if (data.hasNoTags())
                {
                    player.openGui(PokecubeMod.core, Config.GUIPOKEMOBROUTE_ID, e.getEntityWorld(), e.getEntityId(), 0,
                            0);
                }
                else RouteEditHelper.applyServerPacket(data.getTag("T"), e, guard);
            }
        }
    }
}
