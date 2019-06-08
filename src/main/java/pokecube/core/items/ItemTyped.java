package pokecube.core.items;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.core.interfaces.PokecubeMod;

public class ItemTyped extends Item
{
    public final String type;

    public ItemTyped(String type, boolean reg)
    {
        this.type = type;
        if (reg)
        {
            this.setRegistryName(PokecubeMod.ID, type);
            this.setUnlocalizedName(this.getRegistryName().getResourcePath());
        }
        this.setCreativeTab(PokecubeMod.creativeTabPokecube);
    }

    public ItemTyped(String type)
    {
        this(type, true);
    }

    /** allows items to add custom lines of information to the mouseover
     * description */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World playerIn, List<String> list, ITooltipFlag advanced)
    {
        list.add(type);
    }
}
