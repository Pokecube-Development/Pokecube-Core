package pokecube.core.interfaces.pokemob.stats;

import pokecube.core.interfaces.IPokemob.Stats;

public class DefaultModifiers implements IStatsModifiers
{
    public DefaultModifiers()
    {
    }

    public float[] values = new float[Stats.values().length];

    @Override
    public boolean persistant()
    {
        return false;
    }

    @Override
    public boolean isFlat()
    {
        return false;
    }

    @Override
    public int getPriority()
    {
        return 100;
    }

    @Override
    public float getModifier(Stats stat)
    {
        return modifierToRatio((byte) values[stat.ordinal()], stat.ordinal() > 5);
    }

    @Override
    public void setModifier(Stats stat, float value)
    {
        values[stat.ordinal()] = value;
    }

    @Override
    public float getModifierRaw(Stats stat)
    {
        return values[stat.ordinal()];
    }

    public float modifierToRatio(byte mod, boolean accuracy)
    {
        float modifier = 1;
        if (mod == 0) modifier = 1;
        else if (mod == 1) modifier = !accuracy ? 1.5f : 4 / 3f;
        else if (mod == 2) modifier = !accuracy ? 2 : 5 / 3f;
        else if (mod == 3) modifier = !accuracy ? 2.5f : 2;
        else if (mod == 4) modifier = !accuracy ? 3 : 7 / 3f;
        else if (mod == 5) modifier = !accuracy ? 3.5f : 8 / 3f;
        else if (mod == 6) modifier = !accuracy ? 4 : 3;
        else if (mod == -1) modifier = !accuracy ? 2 / 3f : 3 / 4f;
        else if (mod == -2) modifier = !accuracy ? 1 / 2f : 3 / 5f;
        else if (mod == -3) modifier = !accuracy ? 2 / 5f : 3 / 6f;
        else if (mod == -4) modifier = !accuracy ? 1 / 3f : 3 / 7f;
        else if (mod == -5) modifier = !accuracy ? 2 / 7f : 3 / 8f;
        else if (mod == -6) modifier = !accuracy ? 1 / 4f : 3 / 9f;
        return modifier;
    }

}
