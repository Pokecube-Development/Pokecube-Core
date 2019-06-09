package pokecube.core.items.pokecubes;

import java.util.ArrayList;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import pokecube.core.PokecubeItems;
import pokecube.core.interfaces.IPokecube.PokecubeBehavior;
import pokecube.core.utils.TagNames;
import thut.lib.CompatWrapper;
import thut.lib.IDefaultRecipe;

public class RecipePokeseals implements IDefaultRecipe
{
    private ItemStack toCraft = ItemStack.EMPTY;
    // private static final String __OBFID = "CL_00000083";

    /** Returns an Item that is the result of this recipe */
    @Override
    public ItemStack getCraftingResult(InventoryCrafting p_77572_1_)
    {
        return this.toCraft;
    }

    @Override
    public ItemStack getRecipeOutput()
    {
        return this.toCraft;
    }

    /** Used to check if a recipe matches current crafting inventory */
    @SuppressWarnings({ "unused", "rawtypes" })
    @Override
    public boolean matches(InventoryCrafting craft, World world)
    {
        this.toCraft = ItemStack.EMPTY;
        int cube = 0;
        int paper = 0;
        int gunpowder = 0;
        int dye = 0;
        int fireworkcharge = 0;
        int sparklystuff = 0;
        int boomboomstuff = 0;
        int addons = 0;

        for (int k1 = 0; k1 < craft.getSizeInventory(); ++k1)
        {
            ItemStack itemstack = craft.getStackInSlot(k1);

            if (CompatWrapper.isValid(itemstack))
            {
                if (itemstack.getItem() == PokecubeItems.getEmptyCube(PokecubeBehavior.POKESEAL)
                        && PokecubeManager.isFilled(itemstack) == false)
                {
                    ++cube;
                    toCraft = itemstack.copy();
                }
                /*
                 * if (itemstack.getItem() == Items.gunpowder) { ++j; } else if
                 * (itemstack.getItem() == Items.firework_charge) { ++l; }
                 */ else if (itemstack.getItem() == Items.DYE)
                {
                    ++addons;
                }
                /*
                 * else if (itemstack.getItem() == Items.paper) { ++paper; }
                 */ else if (itemstack.getItem() == Items.WATER_BUCKET)
                {
                    ++addons;
                }
                else if (itemstack.getItem() == Items.COAL)
                {
                    ++addons;
                }
                else if (itemstack.getItem() == Item.getItemFromBlock(Blocks.LEAVES))
                {
                    ++addons;
                }
                else if (itemstack.getItem() == Items.FEATHER)
                {
                    ++boomboomstuff;
                }
                else if (itemstack.getItem() == Items.GOLD_NUGGET)
                {
                    ++boomboomstuff;
                }
                else
                {
                    if (itemstack.getItem() != Items.SKULL) { return false; }

                    ++boomboomstuff;
                }
            }
        }

        sparklystuff += dye + boomboomstuff;

        CompoundNBT CompoundNBT;
        CompoundNBT CompoundNBT1;

        if (cube == 1 && addons > 0)
        {
            toCraft = new ItemStack(PokecubeItems.getEmptyCube(PokecubeBehavior.POKESEAL), 1);
            CompoundNBT = new CompoundNBT();
            CompoundNBT1 = new CompoundNBT();
            byte b0 = 0;
            ArrayList arraylist = new ArrayList();

            for (int l1 = 0; l1 < craft.getSizeInventory(); ++l1)
            {
                ItemStack itemstack2 = craft.getStackInSlot(l1);

                if (CompatWrapper.isValid(itemstack2))
                {
                    if (itemstack2.getItem() == Items.COAL)
                    {
                        CompoundNBT1.putBoolean("Flames", true);
                    }
                    if (itemstack2.getItem() == Items.WATER_BUCKET)
                    {
                        CompoundNBT1.putBoolean("Bubbles", true);
                    }
                    if (itemstack2.getItem() == Item.getItemFromBlock(Blocks.LEAVES))
                    {
                        CompoundNBT1.putBoolean("Leaves", true);
                    }
                    if (itemstack2.getItem() == Items.DYE)
                    {
                        CompoundNBT1.putInt("dye", itemstack2.getItemDamage());
                    }

                }
            }

            int[] aint1 = new int[arraylist.size()];

            for (int l2 = 0; l2 < aint1.length; ++l2)
            {
                aint1[l2] = ((Integer) arraylist.get(l2)).intValue();
            }
            CompoundNBT.put(TagNames.POKESEAL, CompoundNBT1);
            this.toCraft.put(CompoundNBT);
            return true;
        }
        return false;
    }

    public static ItemStack process(ItemStack cube, ItemStack seal)
    {
        if (!seal.hasTag()) return cube;
        CompoundNBT pokecubeTag = TagNames.getPokecubePokemobTag(cube.getTag())
                .getCompound(TagNames.VISUALSTAG).getCompound(TagNames.POKECUBE);
        if (!pokecubeTag.hasKey("tag")) pokecubeTag.put("tag", new CompoundNBT());
        CompoundNBT cubeTag = pokecubeTag.getCompound("tag");
        cubeTag.put(TagNames.POKESEAL, seal.getTag().getCompound(TagNames.POKESEAL));
        return cube;
    }

    ResourceLocation registryName;

    @Override
    public IRecipe setRegistryName(ResourceLocation name)
    {
        registryName = name;
        return this;
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return registryName;
    }

    @Override
    public Class<IRecipe> getRegistryType()
    {
        return IRecipe.class;
    }
}
