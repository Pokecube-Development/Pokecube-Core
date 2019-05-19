package pokecube.core.interfaces.pokemob;

import net.minecraft.entity.EntityLiving;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;

public interface IHasEntry extends IHasMobAIStates
{
    /** @return the minecraft entity associated with this pokemob */
    EntityLiving getEntity();

    /** @return the {@link PokedexEntry} of the species of this Pokemob */
    PokedexEntry getPokedexEntry();

    /** @return the int pokedex number */
    default Integer getPokedexNb()
    {
        return getPokedexEntry().getPokedexNb();
    }

    /** @return is this a shadow pokemob */
    default boolean isShadow()
    {
        return getPokedexEntry().isShadowForme;
    }

    /** @return is the pokemob shiny */
    boolean isShiny();

    /** @param entityIn
     *            Sets the vanilla entity for this pokemob */
    void setEntity(EntityLiving entityIn);

    /** @return the {@link PokedexEntry} of the species of this Pokemob */
    IPokemob setPokedexEntry(PokedexEntry newEntry);
}
