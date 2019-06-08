package pokecube.core.network.packets;

import java.io.IOException;
import java.util.UUID;

import javax.xml.ws.handler.MessageContext;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import pokecube.core.PokecubeCore;
import pokecube.core.blocks.pc.ContainerPC;
import pokecube.core.blocks.pc.InventoryPC;
import pokecube.core.handlers.Config;
import pokecube.core.interfaces.PokecubeMod;

public class PacketPC implements IMessage, IMessageHandler<PacketPC, IMessage>
{
    public static final byte   SETPAGE    = 0;
    public static final byte   RENAME     = 1;
    public static final byte   PCINIT     = 2;
    public static final byte   RELEASE    = 3;
    public static final byte   TOGGLEAUTO = 4;
    public static final byte   BIND       = 5;
    public static final byte   PCOPEN     = 6;

    public static final String OWNER      = "_owner_";

    public static void sendInitialSyncMessage(PlayerEntity sendTo)
    {
        InventoryPC inv = InventoryPC.getPC(sendTo.getUniqueID());
        PacketPC packet = new PacketPC(PacketPC.PCINIT, sendTo.getUniqueID());
        packet.data.setInteger("N", inv.boxes.length);
        packet.data.putBoolean("A", inv.autoToPC);
        packet.data.putBoolean("O", inv.seenOwner);
        packet.data.setInteger("C", inv.getPage());
        for (int i = 0; i < inv.boxes.length; i++)
        {
            packet.data.putString("N" + i, inv.boxes[i]);
        }
        PokecubeMod.packetPipeline.sendTo(packet, (ServerPlayerEntity) sendTo);
    }

    public static void sendOpenPacket(PlayerEntity sendTo, UUID owner, BlockPos pcPos)
    {
        InventoryPC inv = InventoryPC.getPC(owner);
        for (int i = 0; i < inv.boxes.length; i++)
        {
            PacketPC packet = new PacketPC(PacketPC.PCOPEN, owner);
            packet.data = inv.serializeBox(i);
            packet.data.setUniqueId(OWNER, owner);
            PokecubeMod.packetPipeline.sendTo(packet, (ServerPlayerEntity) sendTo);
        }
        sendTo.openGui(PokecubeMod.core, Config.GUIPC_ID, sendTo.getEntityWorld(), pcPos.getX(), pcPos.getY(),
                pcPos.getZ());
    }

    byte                  message;
    public CompoundNBT data = new CompoundNBT();

    public PacketPC()
    {
    }

    public PacketPC(byte message)
    {
        this.message = message;
    }

    public PacketPC(byte message, UUID owner)
    {
        this(message);
        this.data.setUniqueId(OWNER, owner);
    }

    @Override
    public IMessage onMessage(final PacketPC message, final MessageContext ctx)
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
        message = buf.readByte();
        PacketBuffer buffer = new PacketBuffer(buf);
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
        buf.writeByte(message);
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeCompoundTag(data);
    }

    void processMessage(MessageContext ctx, PacketPC message)
    {
        PlayerEntity player;
        if (ctx.side == Dist.CLIENT)
        {
            player = PokecubeCore.getPlayer(null);
        }
        else
        {
            player = ctx.getServerHandler().player;
        }
        ContainerPC container = null;
        if (player.openContainer instanceof ContainerPC) container = (ContainerPC) player.openContainer;
        InventoryPC pc;
        switch (message.message)
        {
        case BIND:
            if (container != null && container.pcTile != null)
            {
                boolean owned = message.data.getBoolean("O");
                if (PokecubeMod.debug) PokecubeMod.log("Bind PC Packet: " + owned + " " + player);
                if (owned)
                {
                    container.pcTile.toggleBound();
                }
                else
                {
                    container.pcTile.setBoundOwner(player);
                }
            }
            break;
        case SETPAGE:
            if (container != null)
            {
                container.gotoInventoryPage(message.data.getInteger("P"));
            }
            break;
        case RENAME:
            if (container != null)
            {
                String name = message.data.getString("N");
                container.changeName(name);
            }
            break;
        case PCINIT:
            InventoryPC.blank = new InventoryPC(InventoryPC.defaultId);
            pc = InventoryPC.getPC(message.data.getUniqueId(OWNER));
            pc.seenOwner = message.data.getBoolean("O");
            pc.autoToPC = message.data.getBoolean("A");
            if (message.data.hasKey("C")) pc.setPage(message.data.getInteger("C"));
            if (message.data.hasKey("N"))
            {
                int num = message.data.getInteger("N");
                pc.boxes = new String[num];
                for (int i = 0; i < pc.boxes.length; i++)
                {
                    pc.boxes[i] = message.data.getString("N" + i);
                }
            }
            break;
        case RELEASE:
            boolean toggle = message.data.getBoolean("T");
            if (toggle)
            {
                container.setRelease(message.data.getBoolean("R"));
            }
            else
            {
                int page = message.data.getInteger("page");
                pc = InventoryPC.getPC(message.data.getUniqueId(OWNER));
                for (int i = 0; i < 54; i++)
                {
                    if (message.data.getBoolean("val" + i))
                    {
                        int j = i + page * 54;
                        pc.setInventorySlotContents(j, ItemStack.EMPTY);
                    }
                }
            }
            break;
        case TOGGLEAUTO:
            pc = InventoryPC.getPC(message.data.getUniqueId(OWNER));
            pc.autoToPC = message.data.getBoolean("A");
            break;
        case PCOPEN:
            if (ctx.side == Dist.CLIENT)
            {
                pc = InventoryPC.getPC(message.data.getUniqueId(OWNER));
                pc.deserializeBox(message.data);
            }
            break;
        default:
            break;
        }
    }

}
