package pokecube.core.items.pokecubes;

import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraftforge.common.util.FakePlayer;
import pokecube.core.PokecubeItems;
import pokecube.core.interfaces.IPokecube;
import pokecube.core.interfaces.PokecubeMod;
import thut.api.maths.Vector3;

public class DispenserBehaviorPokecube implements IBehaviorDispenseItem
{

    @Override
    public ItemStack dispense(IBlockSource source, ItemStack stack)
    {
        Direction dir = null;
        IBlockState state = source.getBlockState();
        for (IProperty<?> prop : state.getPropertyKeys())
        {
            if (prop.getValueClass() == Direction.class)
            {
                dir = (Direction) state.getValue(prop);
                break;
            }
        }
        if (dir == null) return stack;

        FakePlayer player = PokecubeMod.getFakePlayer(source.getWorld());
        player.posX = source.getX();
        player.posY = source.getY() - player.getEyeHeight();
        player.posZ = source.getZ();

        // Defaults are for south.
        player.rotationPitch = 0;
        player.rotationYaw = 0;

        if (dir == Direction.EAST)
        {
            player.rotationYaw = -90;
        }
        else if (dir == Direction.WEST)
        {
            player.rotationYaw = 90;
        }
        else if (dir == Direction.NORTH)
        {
            player.rotationYaw = 180;
        }
        else if (dir == Direction.UP)
        {
            player.rotationPitch = -90;
        }
        else if (dir == Direction.DOWN)
        {
            player.rotationPitch = 90;
        }

        if (stack.getItem() == PokecubeItems.pokemobEgg)
        {
            player.setHeldItem(Hand.MAIN_HAND, stack);
            stack.onItemUse(player, source.getWorld(), source.getBlockPos().offset(dir), Hand.MAIN_HAND,
                    Direction.UP, 0.5f, 0.5f, 0.5f);
            player.inventory.clear();
        }
        else if (stack.getItem() instanceof IPokecube)
        {
            IPokecube cube = (IPokecube) stack.getItem();
            Vector3 direction = Vector3.getNewVector().set(dir);
            if (cube.throwPokecube(source.getWorld(), player, stack, direction, 0.25f)) stack.splitStack(1);
        }
        return stack;
    }

}