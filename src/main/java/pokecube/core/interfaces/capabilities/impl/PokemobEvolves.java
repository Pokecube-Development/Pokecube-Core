package pokecube.core.interfaces.capabilities.impl;

import net.minecraft.item.ItemStack;

public abstract class PokemobEvolves extends PokemobHungry
{

    @Override
    public void setEvolutionStack(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public ItemStack getEvolutionStack()
    {
        return stack;
    }

    /** @return the evolutionTicks */
    @Override
    public int getEvolutionTicks()
    {
        return dataSync().get(params.EVOLTICKDW);
    }

    /** @param evolutionTicks
     *            the evolutionTicks to set */
    @Override
    public void setEvolutionTicks(int evolutionTicks)
    {
        dataSync().set(params.EVOLTICKDW, new Integer(evolutionTicks));
    }
}
