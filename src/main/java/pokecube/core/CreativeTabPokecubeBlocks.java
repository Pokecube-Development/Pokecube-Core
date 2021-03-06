/**
 *
 */
package pokecube.core;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author Manchou */
public class CreativeTabPokecubeBlocks extends CreativeTabs
{
    /** @param par1
     * @param par2Str */
    public CreativeTabPokecubeBlocks(int par1, String par2Str)
    {
        super(par1, par2Str);
    }

    /** the itemID for the item to be displayed on the tab */
    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack getTabIconItem()
    {
        return PokecubeItems.getStack("pokecenter");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getTabLabel()
    {
        return "Pok\u00e9cube Blocks";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getTranslatedTabLabel()
    {
        return getTabLabel();
    }
}
