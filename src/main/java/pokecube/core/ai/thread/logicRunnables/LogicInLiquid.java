package pokecube.core.ai.thread.logicRunnables;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import thut.api.TickHandler;
import thut.api.maths.Matrix3;
import thut.api.maths.Vector3;

/** This checks if the pokemob is in lava or water. The checks are done on a
 * seperate thread via doLogic() for performance reasons. */
public class LogicInLiquid extends LogicBase
{
    Matrix3 box = new Matrix3();

    public LogicInLiquid(IPokemob pokemob_)
    {
        super(pokemob_);
    }

    @Override
    public void doLogic()
    {
        world = TickHandler.getInstance().getWorldCache(entity.dimension);
        if (world == null) return;
        boolean lava = false;
        boolean water = false;
        if (entity.getParts() != null)
        {
            for (Entity e : entity.getParts())
            {
                box.set(e.getEntityBoundingBox());
                if (!lava) lava = lava || box.isInMaterial(world, Vector3.empty, Vector3.empty, Material.LAVA);
                if (!water) water = water || box.isInMaterial(world, Vector3.empty, Vector3.empty, Material.WATER);
            }
        }
        else
        {
            box.set(entity.getEntityBoundingBox());
            if (!lava) lava = lava || box.isInMaterial(world, Vector3.empty, Vector3.empty, Material.LAVA);
            if (!water) water = water || box.isInMaterial(world, Vector3.empty, Vector3.empty, Material.WATER);
        }
        pokemob.setLogicState(LogicStates.INLAVA, lava);
        pokemob.setLogicState(LogicStates.INWATER, water);
    }
}
