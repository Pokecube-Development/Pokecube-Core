package com.mcf.davidee.nbteditpqb.packets;

import javax.xml.ws.handler.MessageContext;

import org.apache.logging.log4j.Level;

import com.mcf.davidee.nbteditpqb.NBTEdit;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;

public class EntityRequestPacket implements IMessage {
	/** The id of the entity being requested. */
	private int entityID;

	/** Required default constructor. */
	public EntityRequestPacket() {}

	public EntityRequestPacket(int entityID) {
		this.entityID = entityID;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.entityID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.entityID);
	}

	public static class Handler implements IMessageHandler<EntityRequestPacket, IMessage> {

		@Override
		public IMessage onMessage(EntityRequestPacket packet, MessageContext ctx) {
			ServerPlayerEntity player = ctx.getServerHandler().player;
			NBTEdit.log(Level.TRACE, player.getName() + " requested entity with Id #" + packet.entityID);
			NBTEdit.NETWORK.sendEntity(player, packet.entityID);
			return null;
		}
	}
}
