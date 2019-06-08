package pokecube.core.ai.pokemob;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.PlayerEntity;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;

public class PokemobSitShoulder extends EntityAIBase
{
    private final MobEntity entity;
    private final IPokemob     pokemob;
    private PlayerEntity       owner;
    private int                cooldownTicks = 100;
    private boolean            isSittingOnShoulder;

    public PokemobSitShoulder(MobEntity entityIn)
    {
        this.entity = entityIn;
        this.pokemob = CapabilityPokemob.getPokemobFor(entityIn);
    }

    /** Returns whether the EntityAIBase should begin execution. */
    @Override
    public boolean shouldExecute()
    {
        LivingEntity LivingEntity = (LivingEntity) this.pokemob.getOwner();
        if (!(LivingEntity instanceof PlayerEntity) || pokemob.getGeneralState(GeneralStates.STAYING)) return false;
        boolean flag = LivingEntity != null && !((PlayerEntity) LivingEntity).isSpectator()
                && !((PlayerEntity) LivingEntity).capabilities.isFlying && !LivingEntity.isInWater()
                && !this.pokemob.getLogicState(LogicStates.SITTING);
        if (!flag) cooldownTicks = 100;
        if (cooldownTicks < -100) cooldownTicks = 100;
        return flag && cooldownTicks-- <= 0;
    }

    /** Determine if this AI Task is interruptible by a higher (= lower value)
     * priority task. All vanilla AITask have this value set to true. */
    @Override
    public boolean isInterruptible()
    {
        return !this.isSittingOnShoulder;
    }

    /** Execute a one shot task or start executing a continuous task */
    @Override
    public void startExecuting()
    {
        this.owner = (PlayerEntity) this.pokemob.getOwner();
        this.isSittingOnShoulder = false;
    }

    /** Keep ticking a continuous task that has already been started */
    @Override
    public void updateTask()
    {
        if (!this.isSittingOnShoulder && !this.pokemob.getLogicState(LogicStates.SITTING) && !this.entity.getLeashed())
        {
            if (this.entity.getEntityBoundingBox().intersects(this.owner.getEntityBoundingBox()))
            {
                this.isSittingOnShoulder = this.pokemob.moveToShoulder(this.owner);
            }
        }
    }
}