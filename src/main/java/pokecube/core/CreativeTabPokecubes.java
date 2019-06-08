/**
 *
 */
package pokecube.core;

import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ObjectHolder;

public class CreativeTabPokecubes extends CreativeTabs
{
    @ObjectHolder(value = "pokecube:pokecube")
    public static final Item POKECUBE = null;
    @ObjectHolder(value = "pokecube:pokeseal")
    public static final Item POKESEAL = null;

    public ItemStack         stack;

    /** @param par1
     * @param par2Str */
    public CreativeTabPokecubes(int par1, String par2Str)
    {
        super(par1, par2Str);
    }

    /** the itemID for the item to be displayed on the tab */
    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack getTabIconItem()
    {
        if (stack == null) stack = new ItemStack(POKECUBE == null ? POKESEAL : POKECUBE);
        return stack;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getTabLabel()
    {
        return I18n.format("igwtab.entry.Pokecubes");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getTranslatedTabLabel()
    {
        return getTabLabel();
    }
}
