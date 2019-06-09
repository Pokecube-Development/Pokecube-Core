package pokecube.core.interfaces.entity.impl;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Potion;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import pokecube.core.interfaces.IPokemob.Stats;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.entity.IOngoingAffected;
import pokecube.core.interfaces.entity.IOngoingAffected.IOngoingEffect;

public class StatEffect extends BaseEffect
{
    public final static ResourceLocation ID = new ResourceLocation(PokecubeMod.ID, "stat_effect");

    Stats                                stat;
    byte                                 amount;

    public StatEffect()
    {
        super(ID);
        this.setDuration(5);
    }

    public StatEffect(Stats stat, byte amount)
    {
        this();
        this.stat = stat;
        this.amount = amount;
    }

    @Override
    public boolean onSavePersistant()
    {
        return true;
    }

    @Override
    public boolean allowMultiple()
    {
        return true;
    }

    @Override
    public AddType canAdd(IOngoingAffected affected, IOngoingEffect toAdd)
    {
        // Check if this is same stat, and if the stat in that direction is
        // capped.
        if (toAdd instanceof StatEffect)
        {
            StatEffect effect = (StatEffect) toAdd;
            if (effect.stat == stat)
            {
                if (this.amount > 4 || this.amount < 4) return AddType.DENY;
                this.amount += effect.amount;
                this.setDuration(Math.max(this.getDuration(), effect.getDuration()));
                return AddType.UPDATED;
            }
        }
        return AddType.ACCEPT;
    }

    @Override
    public void affectTarget(IOngoingAffected target)
    {
        if (amount == 0)
        {
            setDuration(0);
            return;
        }
        boolean up = amount > 0;
        LivingEntity entity = target.getEntity();
        int duration = PokecubeMod.core.getConfig().attackCooldown + 10;
        switch (stat)
        {
        case ACCURACY:
            break;
        case ATTACK:
            Potion atkD = Potion.getPotionFromResourceLocation("weakness");
            Potion atkU = Potion.getPotionFromResourceLocation("strength");
            if (up)
            {
                if (entity.isPotionActive(atkD))
                {
                    entity.removeEffectInstance(atkD);
                }
                entity.addEffectInstance(new EffectInstance(atkU, duration, amount));
            }
            else
            {
                if (entity.isPotionActive(atkU))
                {
                    entity.removeEffectInstance(atkU);
                }
                entity.addEffectInstance(new EffectInstance(atkD, duration, amount));
            }
            break;
        case DEFENSE:
            Potion defU = Potion.getPotionFromResourceLocation("resistance");
            if (up)
            {
                entity.addEffectInstance(new EffectInstance(defU, duration, amount));
            }
            break;
        case EVASION:
            break;
        case HP:
            break;
        case SPATTACK:
            break;
        case SPDEFENSE:
            break;
        case VIT:
            Potion vitD = Potion.getPotionFromResourceLocation("slowness");
            Potion vitU = Potion.getPotionFromResourceLocation("speed");
            if (up)
            {
                if (entity.isPotionActive(vitD))
                {
                    entity.removeEffectInstance(vitD);
                }
                entity.addEffectInstance(new EffectInstance(vitU, duration, amount));
            }
            else
            {
                if (entity.isPotionActive(vitU))
                {
                    entity.removeEffectInstance(vitU);
                }
                entity.addEffectInstance(new EffectInstance(vitD, duration, amount));
            }
            break;
        default:
            break;

        }
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {
        this.stat = Stats.values()[nbt.getByte("S")];
        this.amount = nbt.getByte("A");
        super.deserializeNBT(nbt);
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        CompoundNBT tag = super.serializeNBT();
        tag.setByte("S", (byte) stat.ordinal());
        tag.setByte("A", amount);
        return tag;
    }

}
