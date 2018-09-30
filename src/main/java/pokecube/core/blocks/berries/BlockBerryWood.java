package pokecube.core.blocks.berries;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import pokecube.core.interfaces.PokecubeMod;

public class BlockBerryWood extends Block
{
    public final String type;

    public BlockBerryWood(String name)
    {
        super(Material.WOOD);
        this.type = name;
        this.setRegistryName(PokecubeMod.ID, name);
        this.setCreativeTab(PokecubeMod.creativeTabPokecubeBerries);
        this.setUnlocalizedName(this.getRegistryName().getResourcePath());
        this.setSoundType(SoundType.WOOD).setHardness(2.0F).setResistance(5.0F);
    }
}