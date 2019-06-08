package com.mcf.davidee.nbteditpqb.packets;

import javax.xml.ws.handler.MessageContext;

import org.apache.logging.log4j.Level;

import com.mcf.davidee.nbteditpqb.NBTEdit;
import com.mcf.davidee.nbteditpqb.NBTHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.api.distmarker.Dist;
import thut.core.common.handlers.PlayerDataHandler;
import thut.core.common.handlers.PlayerDataHandler.PlayerData;

public class CustomNBTPacket implements IMessage
{
    /** The id of the entity being edited. */
    protected int            entityID;
    /** The nbt data of the entity. */
    protected CompoundNBT tag;
    /** The custom name tag */
    protected String         customName;

    /** Required default constructor. */
    public CustomNBTPacket()
    {
    }

    public CustomNBTPacket(int entityID, String customName, CompoundNBT tag)
    {
        this.entityID = entityID;
        this.customName = customName;
        this.tag = tag;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.entityID = buf.readInt();
        this.tag = NBTHelper.readNbtFromBuffer(buf);
        this.customName = new PacketBuffer(buf).readString(30);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.entityID);
        NBTHelper.writeToBuffer(this.tag, buf);
        new PacketBuffer(buf).writeString(customName);
    }

    public static class Handler implements IMessageHandler<CustomNBTPacket, IMessage>
    {

        @Override
        public IMessage onMessage(final CustomNBTPacket packet, MessageContext ctx)
        {
            if (ctx.side == Dist.DEDICATED_SERVER)
            {
                final ServerPlayerEntity player = ctx.getServerHandler().player;
                player.getServerWorld().addScheduledTask(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Entity entity = player.world.getEntityByID(packet.entityID);
                        if (entity != null && NBTEdit.proxy.checkPermission(player))
                        {
                            try
                            {
                                CompoundNBT tag = packet.tag;
                                PlayerData data = PlayerDataHandler.getInstance()
                                        .getPlayerData(entity.getCachedUniqueIdString()).getData(packet.customName);
                                data.readFromNBT(tag);
                                NBTEdit.log(Level.TRACE,
                                        player.getName() + " edited a tag -- Entity ID #" + packet.entityID);
                                NBTEdit.logTag(packet.tag);
                                NBTEdit.proxy.sendMessage(player, "Your changes have been saved", TextFormatting.WHITE);
                            }
                            catch (Throwable t)
                            {
                                NBTEdit.proxy.sendMessage(player, "Save Failed - Invalid NBT format for Entity",
                                        TextFormatting.RED);
                                NBTEdit.log(Level.WARN, player.getName() + " edited a tag and caused an exception");
                                NBTEdit.logTag(packet.tag);
                                NBTEdit.throwing("EntityNBTPacket", "Handler.onMessage", t);
                            }
                        }
                        else
                        {
                            NBTEdit.proxy.sendMessage(player, "Save Failed - Entity does not exist",
                                    TextFormatting.RED);
                        }
                    }
                });
            }
            else
            {
                System.out.println(packet.tag + " " + packet.entityID + " " + packet.customName);
                NBTEdit.proxy.openEditGUI(packet.entityID, packet.customName, packet.tag);
            }
            return null;
        }
    }
}
