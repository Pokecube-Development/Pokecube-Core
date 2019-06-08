package pokecube.core.interfaces.pokemob.commandhandlers;

import java.util.logging.Level;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import pokecube.core.events.pokemob.combat.CommandAttackEvent;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.moves.MovesUtils;
import pokecube.core.network.pokemobs.PacketCommand.DefaultHandler;

public class AttackEntityHandler extends DefaultHandler
{
    public int targetId;

    public AttackEntityHandler()
    {
    }

    public AttackEntityHandler(Integer targetId)
    {
        this.targetId = targetId;
    }

    @Override
    public void handleCommand(IPokemob pokemob)
    {
        World world = pokemob.getEntity().getEntityWorld();
        Entity target = PokecubeMod.core.getEntityProvider().getEntity(world, targetId, true);
        if (target == null || !(target instanceof LivingEntity))
        {
            if (PokecubeMod.debug)
            {
                if (target == null) PokecubeMod.log(Level.WARNING, "Target Mob cannot be null!",
                        new IllegalArgumentException(pokemob.getEntity().toString()));
                else PokecubeMod.log(Level.WARNING, "Invalid Target!",
                        new IllegalArgumentException(pokemob.getEntity() + " " + target));
            }
            return;
        }
        int currentMove = pokemob.getMoveIndex();
        CommandAttackEvent event = new CommandAttackEvent(pokemob.getEntity(), target);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled() && currentMove != 5 && MovesUtils.canUseMove(pokemob))
        {
            Move_Base move = MovesUtils.getMoveFromName(pokemob.getMoves()[currentMove]);
            if (move.isSelfMove())
            {
                pokemob.executeMove(pokemob.getEntity(), null, 0);
            }
            else
            {
                ITextComponent mess = new TranslationTextComponent("pokemob.command.attack",
                        pokemob.getPokemonDisplayName(), target.getDisplayName(),
                        new TranslationTextComponent(MovesUtils.getUnlocalizedMove(move.getName())));
                if (fromOwner()) pokemob.displayMessageToOwner(mess);
                pokemob.getEntity().setAttackTarget((LivingEntity) target);
                pokemob.setCombatState(CombatStates.ANGRY, true);
                if (target instanceof MobEntity) ((MobEntity) target).setAttackTarget(pokemob.getEntity());
                IPokemob targ = CapabilityPokemob.getPokemobFor(target);
                if (targ != null) targ.setCombatState(CombatStates.ANGRY, true);
            }
        }
    }

    @Override
    public void writeToBuf(ByteBuf buf)
    {
        super.writeToBuf(buf);
        buf.writeInt(targetId);
    }

    @Override
    public void readFromBuf(ByteBuf buf)
    {
        super.readFromBuf(buf);
        targetId = buf.readInt();
    }
}
