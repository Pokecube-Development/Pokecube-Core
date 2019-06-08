package com.mcf.davidee.nbteditpqb.packets;

import javax.xml.ws.handler.MessageContext;

import com.mcf.davidee.nbteditpqb.NBTEdit;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;

public class MouseOverPacket implements IMessage {

	/** Required default constructor. */
	public MouseOverPacket() {}

	@Override
	public void fromBytes(ByteBuf buf) {}

	@Override
	public void toBytes(ByteBuf buf) {}

	public static class Handler implements IMessageHandler<MouseOverPacket, IMessage> {

		@Override
		public IMessage onMessage(MouseOverPacket message, MessageContext ctx) {
			RayTraceResult pos = Minecraft.getInstance().objectMouseOver;
			if (pos != null) {
				if (pos.entityHit != null) {
					return new EntityRequestPacket(pos.entityHit.getEntityId());
				} else if (pos.typeOfHit == RayTraceResult.Type.BLOCK) {
					return new TileRequestPacket(pos.getBlockPos());
				} else {
					NBTEdit.proxy.sendMessage(null, "Error - No tile or entity selected", TextFormatting.RED);
				}
			}
			return null;
		}
	}
}
