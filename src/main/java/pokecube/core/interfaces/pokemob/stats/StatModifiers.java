package pokecube.core.interfaces.pokemob.stats;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import pokecube.core.interfaces.IPokemob.Stats;
import pokecube.core.interfaces.pokemob.IHasStats;
import pokecube.core.utils.PokeType;

public class StatModifiers
{

    public static final String                                  DEFAULTMODIFIERS = "default";
    public static Map<String, Class<? extends IStatsModifiers>> modifierClasses  = Maps.newHashMap();

    static
    {
        modifierClasses.put(DEFAULTMODIFIERS, DefaultModifiers.class);
    }

    public static void registerModifier(String name, Class<? extends IStatsModifiers> modclass)
    {
        if (!modifierClasses.containsKey(name)) modifierClasses.put(name, modclass);
        else throw new IllegalArgumentException(name + " is already registered as a modifier.");
    }

    final Map<String, IStatsModifiers> modifiers       = Maps.newHashMap();
    public List<IStatsModifiers>       sortedModifiers = Lists.newArrayList();
    public Map<String, Integer>        indecies        = Maps.newHashMap();
    /** This are types which may be modified via abilities or moves. */
    public PokeType                    type1, type2;
    DefaultModifiers                   defaultmods;

    public StatModifiers()
    {
        for (String s : modifierClasses.keySet())
        {
            try
            {
                modifiers.put(s, modifierClasses.get(s).newInstance());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        defaultmods = getModifiers(DEFAULTMODIFIERS, DefaultModifiers.class);
        sortedModifiers.addAll(modifiers.values());
        Collections.sort(sortedModifiers, new Comparator<IStatsModifiers>()
        {
            @Override
            public int compare(IStatsModifiers o1, IStatsModifiers o2)
            {
                int comp = o1.getPriority() - o2.getPriority();
                if (comp == 0)
                {
                    comp = o1.getClass().getName().compareTo(o2.getClass().getName());
                }
                return comp;
            }
        });
        outer:
        for (int i = 0; i < sortedModifiers.size(); i++)
        {
            for (String key : modifiers.keySet())
            {
                if (modifiers.get(key) == sortedModifiers.get(i))
                {
                    indecies.put(key, i);
                    continue outer;
                }
            }
        }
    }

    public float getStat(IHasStats pokemob, Stats stat, boolean modified)
    {
        if (modified && stat == Stats.HP) { return pokemob.getHealth(); }
        int index = stat.ordinal();
        byte nature = 0;
        if (index < 6) nature = pokemob.getNature().stats[index];
        float natureMod = (nature * 10f + 100) / 100f;
        int baseStat = pokemob.getBaseStat(stat);
        float actualStat = 1;
        if (index < 6)
        {
            int IV = pokemob.getIVs()[stat.ordinal()];
            int EV = pokemob.getEVs()[stat.ordinal()] - Byte.MIN_VALUE;
            int level = pokemob.getLevel();
            if (stat == Stats.HP)
            {
                if (baseStat != 1)
                {
                    actualStat = level + 10 + (2 * baseStat + IV + EV / 4) * level / 100;
                }
                else actualStat = 1;
            }
            else
            {
                actualStat = 5 + level * (2 * baseStat + IV + EV / 4) / 100;
                actualStat *= natureMod;
            }
        }
        if (modified) for (IStatsModifiers mods : sortedModifiers)
        {
            if (mods.isFlat()) actualStat += mods.getModifier(stat);
            else actualStat *= mods.getModifier(stat);
        }
        return actualStat;
    }

    public IStatsModifiers getModifiers(String name)
    {
        return modifiers.get(name);
    }

    public <T extends IStatsModifiers> T getModifiers(String name, Class<T> type)
    {
        return type.cast(modifiers.get(name));
    }

    public DefaultModifiers getDefaultMods()
    {
        return defaultmods;
    }

    public void outOfCombatReset()
    {
        defaultmods.reset();
        for (IStatsModifiers mods : sortedModifiers)
        {
            if (!mods.persistant()) mods.reset();
        }
    }
}
