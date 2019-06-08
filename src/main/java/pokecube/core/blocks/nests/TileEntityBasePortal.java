package pokecube.core.blocks.nests;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import pokecube.core.blocks.TileEntityOwnable;
import pokecube.core.world.dimensions.PokecubeDimensionManager;
import pokecube.core.world.dimensions.secretpower.SecretBaseManager;
import pokecube.core.world.dimensions.secretpower.SecretBaseManager.Coordinate;

public class TileEntityBasePortal extends TileEntityOwnable
{
    public boolean exit        = false;
    public boolean sendToUsers = false;
    public int     exitDim     = 0;

    public void transferPlayer(PlayerEntity playerIn)
    {
        if (!sendToUsers && placer == null) return;
        String owner = sendToUsers ? playerIn.getCachedUniqueIdString() : placer.toString();
        BlockPos exitLoc = PokecubeDimensionManager.getBaseEntrance(owner, world.dimension.getDimension());
        if (exitLoc == null)
        {
            PokecubeDimensionManager.setBaseEntrance(owner, world.dimension.getDimension(), pos);
            exitLoc = pos;
        }
        double dist = exitLoc.distanceSq(pos);
        if (dist > 36)
        {
            world.setBlockState(pos, Blocks.STONE.getDefaultState());
            playerIn.sendMessage(new TranslationTextComponent("pokemob.removebase.stale"));
        }
        else PokecubeDimensionManager.sendToBase(owner, playerIn, exitLoc.getX(), exitLoc.getY(), exitLoc.getZ(),
                exitDim);
    }

    @Override
    public boolean shouldBreak()
    {
        return false;
    }

    @Override
    public void readFromNBT(CompoundNBT tagCompound)
    {
        super.readFromNBT(tagCompound);
        this.sendToUsers = tagCompound.getBoolean("allPlayer");
    }

    @Override
    public CompoundNBT writeToNBT(CompoundNBT tagCompound)
    {
        super.writeToNBT(tagCompound);
        tagCompound.putBoolean("allPlayer", sendToUsers);
        return tagCompound;
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        Coordinate c = new Coordinate(getPos().getX(), getPos().getY(), getPos().getZ(),
                getWorld().dimension.getDimension());
        SecretBaseManager.removeBase(c);
    }

    @Override
    public void validate()
    {
        super.validate();
        Coordinate c = new Coordinate(getPos().getX(), getPos().getY(), getPos().getZ(),
                getWorld().dimension.getDimension());
        SecretBaseManager.addBase(c);
    }
}
