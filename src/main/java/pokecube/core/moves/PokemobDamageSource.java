/**
 * 
 */
package pokecube.core.moves;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.utils.PokeType;

/** This class extends {@link EntityDamageSource} and only modifies the death
 * message.
 * 
 * @author Manchou */
public class PokemobDamageSource extends DamageSource
{

    private LivingEntity damageSourceEntity;
    // TODO use this for damage stuff
    public Move_Base         move;
    /** This is the type of the used move, can be different from
     * move.getType() */
    private PokeType         moveType = null;
    public IPokemob          user;

    /** @param par1Str
     * @param par2Entity */
    public PokemobDamageSource(String par1Str, LivingEntity par2Entity, Move_Base type)
    {
        super(par1Str);
        damageSourceEntity = par2Entity;
        user = CapabilityPokemob.getPokemobFor(par2Entity);
        move = type;
    }

    public PokemobDamageSource setType(PokeType type)
    {
        this.moveType = type;
        return this;
    }

    public PokeType getType()
    {
        return moveType == null ? move.getType(user) : moveType;
    }

    @Override
    public ITextComponent getDeathMessage(LivingEntity par1PlayerEntity)
    {
        ItemStack localObject = (this.damageSourceEntity != null) ? user.getHeldItem() : null;
        if ((localObject != null) && (localObject.hasDisplayName()))
            return new TranslationTextComponent("death.attack." + this.damageType,
                    new Object[] { par1PlayerEntity.getDisplayName(), this.damageSourceEntity.getDisplayName(),
                            localObject.getTextComponent() });
        IPokemob sourceMob = CapabilityPokemob.getPokemobFor(this.damageSourceEntity);
        if (sourceMob != null && sourceMob.getPokemonOwner() != null)
        {
            TranslationTextComponent message = new TranslationTextComponent("pokemob.killed.tame",
                    par1PlayerEntity.getDisplayName(), sourceMob.getPokemonOwner().getDisplayName(),
                    this.damageSourceEntity.getDisplayName());
            return message;
        }
        else if (sourceMob != null && sourceMob.getPokemonOwner() == null
                && !sourceMob.getGeneralState(GeneralStates.TAMED))
        {
            TranslationTextComponent message = new TranslationTextComponent("pokemob.killed.wild",
                    par1PlayerEntity.getDisplayName(), this.damageSourceEntity.getDisplayName());
            return message;
        }
        return new TranslationTextComponent("death.attack." + this.damageType,
                new Object[] { par1PlayerEntity.getDisplayName(), this.damageSourceEntity.getDisplayName() });
    }

    @Override
    public Entity getTrueSource()
    {
        IPokemob sourceMob = CapabilityPokemob.getPokemobFor(this.damageSourceEntity);
        if (sourceMob != null && sourceMob.getOwner() != null) return sourceMob.getOwner();
        if (this.damageSourceEntity instanceof IEntityOwnable)
        {
            Entity owner = ((IEntityOwnable) this.damageSourceEntity).getOwner();
            return owner != null ? owner : this.damageSourceEntity;
        }
        return this.damageSourceEntity;
    }

    @Nullable
    @Override
    public Entity getImmediateSource()
    {
        return this.damageSourceEntity;
    }

    @Override
    /** Returns true if the damage is projectile based. */
    public boolean isProjectile()
    {
        return (move.getAttackCategory() & IMoveConstants.CATEGORY_DISTANCE) != 0;
    }

    public float getEffectiveness(IPokemob pokemobCap)
    {
        return PokeType.getAttackEfficiency(getType(), pokemobCap.getType1(), pokemobCap.getType2());
    }
}
