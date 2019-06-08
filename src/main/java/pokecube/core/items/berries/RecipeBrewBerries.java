package pokecube.core.items.berries;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import pokecube.core.PokecubeItems;

public class RecipeBrewBerries implements IBrewingRecipe
{

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient)
    {
        if (isIngredient(ingredient)) return makeOutput(input, ingredient);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isIngredient(ItemStack ingredient)
    {
        return ingredient.getItem() instanceof ItemBerry;
    }

    @Override
    public boolean isInput(ItemStack input)
    {
        CompoundNBT tag = input.getTag();
        if ((tag != null && tag.hasKey("pokebloc"))) return true;
        return input.getItem() == Items.GLASS_BOTTLE;
    }

    private ItemStack makeOutput(ItemStack input, ItemStack ingredient)
    {

        CompoundNBT pokebloc = new CompoundNBT();
        ItemStack stack = PokecubeItems.getStack("revive");

        if (ingredient.getItem() instanceof ItemBerry)
        {
            ItemBerry berry = (ItemBerry) ingredient.getItem();
            int[] flav = BerryManager.berryFlavours.get(berry.index);
            int[] old = null;
            if (input.hasTag() && input.getTag().hasKey("pokebloc"))
                old = input.getTag().getIntArray("pokebloc");
            if (flav != null)
            {
                flav = flav.clone();
                if (old != null) for (int i = 0; i < (Math.min(old.length, flav.length)); i++)
                {
                    flav[i] += old[i];
                }
                pokebloc.putIntArray("pokebloc", flav);
                CompoundNBT tag = input.hasTag() ? input.getTag().copy() : new CompoundNBT();
                tag.put("pokebloc", pokebloc);
                stack.put(tag);
            }
        }
        return stack;
    }

}
