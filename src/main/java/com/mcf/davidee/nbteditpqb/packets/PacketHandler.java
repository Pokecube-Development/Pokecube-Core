package com.mcf.davidee.nbteditpqb.packets;

import org.apache.logging.log4j.Level;

import com.mcf.davidee.nbteditpqb.NBTEdit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.api.distmarker.Dist;
import thut.core.common.handlers.PlayerDataHandler;
import thut.core.common.handlers.PlayerDataHandler.PlayerData;

/** Created by Jay113355 on 6/28/2016. */
public class PacketHandler
{
    public SimpleNetworkWrapper INSTANCE;
    private static int          ID = 0;

    public void initialize()
    {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(NBTEdit.MODID);
        registerPackets();
    }

    public void registerPackets()
    {
        INSTANCE.registerMessage(TileRequestPacket.Handler.class, TileRequestPacket.class, ID++, Dist.DEDICATED_SERVER);
        INSTANCE.registerMessage(TileNBTPacket.Handler.class, TileNBTPacket.class, ID++, Dist.CLIENT);
        INSTANCE.registerMessage(TileNBTPacket.Handler.class, TileNBTPacket.class, ID++, Dist.DEDICATED_SERVER);
        INSTANCE.registerMessage(EntityRequestPacket.Handler.class, EntityRequestPacket.class, ID++, Dist.DEDICATED_SERVER);
        INSTANCE.registerMessage(EntityNBTPacket.Handler.class, EntityNBTPacket.class, ID++, Dist.CLIENT);
        INSTANCE.registerMessage(EntityNBTPacket.Handler.class, EntityNBTPacket.class, ID++, Dist.DEDICATED_SERVER);
        INSTANCE.registerMessage(MouseOverPacket.Handler.class, MouseOverPacket.class, ID++, Dist.CLIENT);
        INSTANCE.registerMessage(CustomNBTPacket.Handler.class, CustomNBTPacket.class, ID++, Dist.CLIENT);
        INSTANCE.registerMessage(CustomNBTPacket.Handler.class, CustomNBTPacket.class, ID++, Dist.DEDICATED_SERVER);
    }

    /** Sends a TileEntity's nbt data to the player for editing.
     * 
     * @param player
     *            The player to send the TileEntity to.
     * @param pos
     *            The block containing the TileEntity. */
    public void sendTile(final ServerPlayerEntity player, final BlockPos pos)
    {
        if (NBTEdit.proxy.checkPermission(player))
        {
            player.getServerWorld().addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    TileEntity te = player.getServerWorld().getTileEntity(pos);
                    if (te != null)
                    {
                        CompoundNBT tag = new CompoundNBT();
                        te.writeToNBT(tag);
                        INSTANCE.sendTo(new TileNBTPacket(pos, tag), player);
                    }
                    else
                    {
                        NBTEdit.proxy.sendMessage(player, "Error - There is no TileEntity at " + pos.getX() + ", "
                                + pos.getY() + ", " + pos.getZ(), TextFormatting.RED);
                    }
                }
            });
        }
    }

    /** Sends a Entity's nbt data to the player for editing.
     * 
     * @param player
     *            The player to send the Entity data to.
     * @param entityId
     *            The id of the Entity. */
    public void sendEntity(final ServerPlayerEntity player, final int entityId)
    {
        if (NBTEdit.proxy.checkPermission(player))
        {
            player.getServerWorld().addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    Entity entity = player.getServerWorld().getEntityByID(entityId);
                    if (entity instanceof PlayerEntity && entity != player)
                    {
                        NBTEdit.proxy.sendMessage(player, "Error - You may not use NBTEdit on other Players",
                                TextFormatting.RED);
                        NBTEdit.log(Level.WARN,
                                player.getName() + " tried to use NBTEdit on another player, " + entity.getName());
                    }
                    if (entity != null)
                    {
                        CompoundNBT tag = new CompoundNBT();
                        entity.writeToNBT(tag);
                        INSTANCE.sendTo(new EntityNBTPacket(entityId, tag), player);
                    }
                    else
                    {
                        NBTEdit.proxy.sendMessage(player, "\"Error - Unknown EntityID #" + entityId,
                                TextFormatting.RED);
                    }
                }
            });
        }
    }

    /** Sends a Entity's nbt data to the player for editing.
     * 
     * @param player
     *            The player to send the Entity data to.
     * @param entityId
     *            The id of the Entity. */
    public void sendCustomTag(final ServerPlayerEntity player, final int entityId, final String customType)
    {
        if (NBTEdit.proxy.checkPermission(player))
        {
            player.getServerWorld().addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    Entity entity = player.getServerWorld().getEntityByID(entityId);

                    if (entity != null && !(entity instanceof PlayerEntity))
                    {
                        NBTEdit.proxy.sendMessage(player, "\"Error- Target must be a player", TextFormatting.RED);
                    }
                    else if (entity != null)
                    {
                        CompoundNBT tag = new CompoundNBT();
                        PlayerData data = PlayerDataHandler.getInstance()
                                .getPlayerData(entity.getCachedUniqueIdString()).getData(customType);
                        if (data == null)
                        {
                            NBTEdit.proxy.sendMessage(player, "\"Error - Unknown DataType " + customType,
                                    TextFormatting.RED);
                        }
                        else
                        {
                            data.writeToNBT(tag);
                            INSTANCE.sendTo(new CustomNBTPacket(entityId, customType, tag), player);
                        }
                    }
                    else
                    {
                        NBTEdit.proxy.sendMessage(player, "\"Error - Unknown EntityID #" + entityId,
                                TextFormatting.RED);
                    }
                }
            });
        }
    }
}
