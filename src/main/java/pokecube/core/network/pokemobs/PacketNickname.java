package pokecube.core.network.pokemobs;

import javax.xml.ws.handler.MessageContext;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;

public class PacketNickname implements IMessage, IMessageHandler<PacketNickname, IMessage>
{
    public static void sendPacket(Entity mob, String name)
    {
        PacketNickname packet = new PacketNickname();
        packet.entityId = mob.getEntityId();
        packet.name = name;
        PokecubeMod.packetPipeline.sendToServer(packet);
    }

    int    entityId;
    String name;

    public PacketNickname()
    {
    }

    @Override
    public IMessage onMessage(final PacketNickname message, final MessageContext ctx)
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
        name = buffer.readString(20);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(entityId);
        buffer.writeString(name);
    }

    static void processMessage(MessageContext ctx, PacketNickname message)
    {
        PlayerEntity player;
        player = ctx.getServerHandler().player;
        Entity mob = PokecubeMod.core.getEntityProvider().getEntity(player.getEntityWorld(), message.entityId, true);
        IPokemob pokemob = CapabilityPokemob.getPokemobFor(mob);
        if (pokemob == null) return;
        String name = ChatAllowedCharacters.filterAllowedCharacters(new String(message.name));
        if (pokemob.getPokemonDisplayName().getFormattedText().equals(name)) return;
        boolean OT = pokemob.getPokemonOwnerID() == null || pokemob.getOriginalOwnerUUID() == null
                || (pokemob.getPokemonOwnerID().equals(pokemob.getOriginalOwnerUUID()));
        if (!OT && pokemob.getPokemonOwner() != null)
        {
            OT = pokemob.getPokemonOwner().getUniqueID().equals(pokemob.getOriginalOwnerUUID());
        }
        if (!OT)
        {
            if (pokemob.getPokemonOwner() != null)
            {
                pokemob.getPokemonOwner().sendMessage(new TranslationTextComponent("pokemob.rename.deny"));
            }
        }
        else
        {
            pokemob.getPokemonOwner().sendMessage(new TranslationTextComponent("pokemob.rename.success",
                    pokemob.getPokemonDisplayName().getFormattedText(), name));
            pokemob.setPokemonNickname(name);
        }

    }
}