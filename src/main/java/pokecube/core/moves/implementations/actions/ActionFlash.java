package pokecube.core.moves.implementations.actions;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.EffectInstance;
import pokecube.core.interfaces.IMoveAction;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import thut.api.entity.IHungrymob;
import thut.api.maths.Vector3;

public class ActionFlash implements IMoveAction
{
    public ActionFlash()
    {
    }

    @Override
    public boolean applyEffect(IPokemob user, Vector3 location)
    {
        if (user.getCombatState(CombatStates.ANGRY)) return false;
        LivingEntity owner = user.getPokemonOwner();
        if (owner == null) return false;
        IHungrymob mob = user;
        int count = 1;
        int level = user.getLevel();
        int hungerValue = PokecubeMod.core.getConfig().pokemobLifeSpan / 16;
        count = (int) Math.max(1, Math.ceil(count * Math.pow((100 - level) / 100d, 3))) * hungerValue;
        EffectInstance effect = new EffectInstance(Potion.getPotionFromResourceLocation("night_vision"), 5000);
        owner.addEffectInstance(effect);
        mob.setHungerTime(mob.getHungerTime() + count);
        return true;
    }

    @Override
    public String getMoveName()
    {
        return "flash";
    }
}
