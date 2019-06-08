/**
 * 
 */
package pokecube.core.entity.pokemobs.helper;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import pokecube.core.interfaces.PokecubeMod;

/** @author sebastien */
public abstract class EntityDropPokemob extends EntityMovesPokemob
{

    /** @param world */
    public EntityDropPokemob(World world)
    {
        super(world);
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable()
    {
        if (this.getEntityData().getBoolean("cloned")) return null;
        if (PokecubeMod.core.getConfig().pokemobsDropItems) return pokemobCap.getPokedexEntry().lootTable;
        else return null;
    }

    @Override
    /** Get the experience points the entity currently has. */
    protected int getExperiencePoints(PlayerEntity player)
    {
        float scale = PokecubeMod.core.getConfig().expFromDeathDropScale;
        int exp = (int) Math.max(1, pokemobCap.getBaseXP() * scale * 0.01 * Math.sqrt(pokemobCap.getLevel()));
        return exp;
    }
}
