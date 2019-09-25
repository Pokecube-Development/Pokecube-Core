package pokecube.core.utils;

import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.Sets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;

public class AITools
{
    public static class AgroCheck implements Predicate<IPokemob>
    {
        @Override
        public boolean test(final IPokemob input)
        {
            final boolean tame = input.getGeneralState(GeneralStates.TAMED);
            boolean wildAgress = !tame;
            if (PokecubeCore.getConfig().mobAgroRate > 0) wildAgress = wildAgress && new Random().nextInt(PokecubeCore
                    .getConfig().mobAgroRate) == 0;
            else wildAgress = false;
            // Check if the mob should always be agressive.
            if (!tame && !wildAgress && input.getEntity().ticksExisted % 20 == 0) wildAgress = input.getEntity()
                    .getPersistentData().getBoolean("alwaysAgress");
            return wildAgress;
        }
    }

    private static class ValidCheck implements Predicate<Entity>
    {
        @Override
        public boolean test(final Entity input)
        {
            final ResourceLocation eid = input.getType().getRegistryName();
            if (AITools.invalidIDs.contains(eid)) return false;
            // Then check if disabled via class
            for (final Class<?> clas : AITools.invalidClasses)
                if (clas.isInstance(input)) return false;
            // Then check if is a spectating player.
            if (input instanceof ServerPlayerEntity)
            {
                final ServerPlayerEntity player = (ServerPlayerEntity) input;
                if (player.isSpectator()) return false;
            }
            return true;
        }
    }

    public static boolean handleDamagedTargets = true;

    public static int           DEAGROTIMER    = 50;
    public static Set<Class<?>> invalidClasses = Sets.newHashSet();
    public static Set<String>   invalidIDs     = Sets.newHashSet();

    /**
     * Checks the blacklists set via configs, to see whether the target is a
     * valid choice.
     */
    public static final Predicate<Entity> validTargets = new ValidCheck();

    /**
     * Checks to see if the wild pokemob should try to agro the nearest visible
     * player.
     */
    public static Predicate<IPokemob> shouldAgroNearestPlayer = new AgroCheck();

    public static void initIDs()
    {
        for (final String s : PokecubeCore.getConfig().guardBlacklistClass)
            try
            {
                final Class<?> c = Class.forName(s, false, PokecubeCore.getConfig().getClass().getClassLoader());
                AITools.invalidClasses.add(c);
            }
            catch (final ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        final Set<ResourceLocation> keys = ForgeRegistries.ENTITIES.getKeys();
        for (String s : PokecubeCore.getConfig().guardBlacklistId)
            if (s.endsWith("*"))
            {
                s = s.substring(0, s.length() - 1);
                for (final ResourceLocation res : keys)
                    if (res.toString().startsWith(s)) AITools.invalidIDs.add(res.toString());
            }
            else AITools.invalidIDs.add(s);
    }

}
