package pokecube.core.moves.implementations.actions;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import pokecube.core.events.handlers.MoveEventsHandler;
import pokecube.core.interfaces.IMoveAction;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.world.terrain.PokecubeTerrainChecker;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;

public class ActionSecretPower implements IMoveAction
{
    public static Map<UUID, Vector4> pendingBaseLocations = Maps.newHashMap();

    public ActionSecretPower()
    {
    }

    @Override
    public boolean applyEffect(IPokemob attacker, Vector3 location)
    {
        if (attacker.getCombatState(CombatStates.ANGRY)) return false;
        if (!(attacker.getPokemonOwner() instanceof ServerPlayerEntity)) return false;
        if (!MoveEventsHandler.canEffectBlock(attacker, location)) return false;
        long time = attacker.getEntity().getEntityData().getLong("lastAttackTick");
        if (time + (20 * 3) > attacker.getEntity().getEntityWorld().getGameTime()) return false;
        ServerPlayerEntity owner = (ServerPlayerEntity) attacker.getPokemonOwner();
        BlockState state = location.getBlockState(owner.getEntityWorld());
        if (!(PokecubeTerrainChecker.isTerrain(state) || PokecubeTerrainChecker.isWood(state)))
        {
            TranslationTextComponent message = new TranslationTextComponent("pokemob.createbase.deny.wrongloc");
            owner.sendMessage(message);
            return false;
        }
        pendingBaseLocations.put(owner.getUniqueID(), new Vector4(location.x, location.y, location.z, owner.dimension));
        TranslationTextComponent message = new TranslationTextComponent("pokemob.createbase.confirm",
                location.set(location.getPos()));
        message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/pokebase confirm " + owner.posX + " " + owner.posY + " " + owner.posZ));
        owner.sendMessage(message);
        return true;
    }

    @Override
    public String getMoveName()
    {
        return "secretpower";
    }
}
