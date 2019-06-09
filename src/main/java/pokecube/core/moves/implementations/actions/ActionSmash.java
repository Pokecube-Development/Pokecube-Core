package pokecube.core.moves.implementations.actions;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import pokecube.core.events.handlers.MoveEventsHandler;
import pokecube.core.events.pokemob.combat.MoveUse;
import pokecube.core.interfaces.IMoveAction;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.moves.MovesUtils;
import pokecube.core.moves.templates.Move_Basic;
import pokecube.core.world.terrain.PokecubeTerrainChecker;
import thut.api.maths.Vector3;

public class ActionSmash implements IMoveAction
{
    public ActionSmash()
    {
    }

    @Override
    public boolean applyEffect(IPokemob user, Vector3 location)
    {
        if (user.getCombatState(CombatStates.ANGRY)) return false;
        boolean used = false;
        int count = 10;
        int level = user.getLevel();
        int hungerValue = PokecubeMod.core.getConfig().pokemobLifeSpan / 4;
        if (!MoveEventsHandler.canEffectBlock(user, location)) return false;
        level = Math.min(99, level);
        int rocks = smashRock(user, location, true);
        count = (int) Math.max(0, Math.ceil(rocks * Math.pow((100 - level) / 100d, 3))) * hungerValue;
        if (rocks > 0)
        {
            smashRock(user, location, false);
            used = true;
            user.setHungerTime(user.getHungerTime() + count);
        }
        if (!used)
        {
            World world = user.getEntity().getEntityWorld();
            List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class, location.getAABB().grow(1));
            if (!items.isEmpty())
            {
                Move_Base move = MovesUtils.getMoveFromName(getMoveName());
                return PokecubeMod.MOVE_BUS.post(new MoveUse.MoveWorldAction.AffectItem(move, user, location, items));
            }
        }
        return used;
    }

    @Override
    public String getMoveName()
    {
        return "rocksmash";
    }

    private int smashRock(IPokemob digger, Vector3 v, boolean count)
    {
        int ret = 0;
        LivingEntity owner = digger.getPokemonOwner();
        PlayerEntity player = null;
        if (owner instanceof PlayerEntity)
        {
            player = (PlayerEntity) owner;
            BreakEvent evt = new BreakEvent(player.getEntityWorld(), v.getPos(),
                    v.getBlockState(player.getEntityWorld()), player);

            MinecraftForge.EVENT_BUS.post(evt);
            if (evt.isCanceled()) return 0;
        }
        int fortune = digger.getLevel() / 30;
        boolean silky = Move_Basic.shouldSilk(digger) && player != null;
        World world = digger.getEntity().getEntityWorld();
        Vector3 temp = Vector3.getNewVector();
        temp.set(v);
        int range = 0;
        for (int i = -range; i <= range; i++)
            for (int j = -range; j <= range; j++)
                for (int k = -range; k <= range; k++)
                {
                    temp.set(v);
                    BlockState state = temp.addTo(i, j, k).getBlockState(world);
                    if (PokecubeTerrainChecker.isRock(state))
                    {
                        if (!count)
                        {
                            if (!silky) doFortuneDrop(temp, world, fortune);
                            else
                            {
                                Block block = state.getBlock();
                                if (block.canSilkHarvest(world, temp.getPos(), state, player))
                                {
                                    Move_Basic.silkHarvest(state, temp.getPos(), world, player);
                                    temp.breakBlock(world, false);
                                }
                                else
                                {
                                    doFortuneDrop(temp, world, fortune);
                                }
                            }
                        }
                        ret++;
                    }
                }
        return ret;
    }

    private void doFortuneDrop(Vector3 location, World world, int fortune)
    {
        location.getBlock(world).dropBlockAsItem(world, location.getPos(), location.getBlockState(world), fortune);
        location.breakBlock(world, false);
    }
}
