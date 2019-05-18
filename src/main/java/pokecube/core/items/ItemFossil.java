package pokecube.core.items;

import pokecube.core.interfaces.PokecubeMod;

public class ItemFossil extends ItemTyped
{
    public ItemFossil(String type)
    {
        super(type, false);
        this.setRegistryName(PokecubeMod.ID, "fossil_" + type);
        this.setUnlocalizedName(this.getRegistryName().getResourcePath());
    }
}
