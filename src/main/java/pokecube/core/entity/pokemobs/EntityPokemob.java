/**
 *
 */
package pokecube.core.entity.pokemobs;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.capabilities.DefaultPokemob;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;

public class EntityPokemob extends TameableEntity
{
    protected final DefaultPokemob pokemobCap;

    public EntityPokemob(final EntityType<? extends TameableEntity> type, final World world)
    {
        super(type, world);
        final DefaultPokemob cap = (DefaultPokemob) this.getCapability(CapabilityPokemob.POKEMOB_CAP, null).orElse(
                null);
        this.pokemobCap = cap == null ? new DefaultPokemob(this) : cap;
    }

    @Override
    public boolean canFitPassenger(final Entity passenger)
    {
        // TODO Auto-generated method stub
        return super.canFitPassenger(passenger);
    }

    @Override
    public AgeableEntity createChild(final AgeableEntity ageable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fall(final float distance, final float damageMultiplier)
    {
    }

    @Override
    public SitGoal getAISit()
    {
        // TODO custom sitting ai.
        return super.getAISit();
    }

    @Override
    /** Get the experience points the entity currently has. */
    protected int getExperiencePoints(final PlayerEntity player)
    {
        final float scale = (float) PokecubeCore.getConfig().expFromDeathDropScale;
        final int exp = (int) Math.max(1, this.pokemobCap.getBaseXP() * scale * 0.01 * Math.sqrt(this.pokemobCap
                .getLevel()));
        return exp;
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable()
    {
        if (this.getEntityData().getBoolean("cloned")) return null;
        if (PokecubeCore.getConfig().pokemobsDropItems) return this.pokemobCap.getPokedexEntry().lootTable;
        else return null;
    }

    @Override
    public ItemStack getPickedResult(final RayTraceResult target)
    {
        return ItemPokemobEgg.getEggStack(this.pokemobCap);
    }

    @Override
    public float getRenderScale()
    {
        return this.pokemobCap.getSize();
    }

    @Override
    public boolean isSitting()
    {
        return this.pokemobCap.getLogicState(LogicStates.SITTING);
    }

    @Override
    public void setSitting(final boolean sitting)
    {
        this.pokemobCap.setLogicState(LogicStates.SITTING, sitting);
        super.setSitting(sitting);
    }

    @Override
    protected void updateFallState(final double y, final boolean onGroundIn, final BlockState state, final BlockPos pos)
    {
    }
}
