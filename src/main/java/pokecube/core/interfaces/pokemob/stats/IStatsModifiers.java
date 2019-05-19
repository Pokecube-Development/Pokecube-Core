package pokecube.core.interfaces.pokemob.stats;

import pokecube.core.interfaces.IPokemob.Stats;

public interface IStatsModifiers
{

    /** Is the result of getModifier a percantage or a flat value?
     * 
     * @return */
    boolean isFlat();

    /** Priority of application of these stats modifiers, higher numbers go
     * later, the default modifiers (such as from growl) will be given priority
     * of 100, so set yours accordingly.
     * 
     * @return */
    int getPriority();

    /** Returns the effective value of the modifier, either a percantage, or a
     * flat amount, based on isFlat
     * 
     * @param stat
     * @return */
    float getModifier(Stats stat);

    /** Returns the raw value for the modifier, this should match whatever is
     * set in setModifier.
     * 
     * @param stat
     * @return */
    float getModifierRaw(Stats stat);

    void setModifier(Stats stat, float value);

    /** Is this modifier saved with the pokemob, and persists outside of battle
     * 
     * @return */
    boolean persistant();

    default void reset()
    {
        for (Stats stat : Stats.values())
            setModifier(stat, 0);
    }
}
