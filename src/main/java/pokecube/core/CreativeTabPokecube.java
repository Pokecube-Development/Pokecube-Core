/**
 *
 */
package pokecube.core;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** @author Manchou */
public class CreativeTabPokecube extends CreativeTabs
{
    /** @param par1
     * @param par2Str */
    public CreativeTabPokecube(int par1, String par2Str)
    {
        super(par1, par2Str);
    }

    /** the itemID for the item to be displayed on the tab */
    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack getTabIconItem()
    {
        return PokecubeItems.getStack("pokedex");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getTabLabel()
    {
        return "Pok\u00e9cube";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getTranslatedTabLabel()
    {
        return getTabLabel();
    }
}
