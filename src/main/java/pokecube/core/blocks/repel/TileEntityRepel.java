package pokecube.core.blocks.repel;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.interfaces.PokecubeMod;

/** @author Manchou */
public class TileEntityRepel extends TileEntity
{
    public byte distance;
    boolean     enabled  = true;

    public TileEntityRepel()
    {
        distance = (byte) PokecubeMod.core.getConfig().repelRadius;
    }

    public boolean addForbiddenSpawningCoord()
    {
        if (getWorld() == null || getWorld().isRemote || !enabled) return false;
        return SpawnHandler.addForbiddenSpawningCoord(pos, getWorld().dimension.getDimension(), distance);
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        removeForbiddenSpawningCoord();
    }

    /** Reads a tile entity from NBT. */
    @Override
    public void readFromNBT(CompoundNBT nbt)
    {
        super.readFromNBT(nbt);
        distance = nbt.getByte("distance");
        enabled = nbt.getBoolean("enabled");
    }

    @Override
    public void onLoad()
    {
        if (enabled)
        {
            removeForbiddenSpawningCoord();
            addForbiddenSpawningCoord();
        }
    }

    public boolean removeForbiddenSpawningCoord()
    {
        if (getWorld() == null || getWorld().isRemote) return false;
        return SpawnHandler.removeForbiddenSpawningCoord(pos, getWorld().dimension.getDimension());
    }

    @Override
    public void validate()
    {
        super.validate();
        addForbiddenSpawningCoord();
    }

    /** Writes a tile entity to NBT.
     * 
     * @return */
    @Override
    public CompoundNBT writeToNBT(CompoundNBT nbt)
    {
        super.writeToNBT(nbt);
        nbt.setByte("distance", distance);
        nbt.putBoolean("enabled", enabled);
        return nbt;
    }
}
