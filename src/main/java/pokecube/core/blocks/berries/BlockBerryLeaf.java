package pokecube.core.blocks.berries;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.core.blocks.berries.TileEntityBerries.Type;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.berries.BerryManager;
import thut.tech.common.blocks.lift.BlockLift.EnumType;

public class BlockBerryLeaf extends BlockLeaves implements ITileEntityProvider
{
    public BlockBerryLeaf()
    {
        super();
        setCreativeTab(PokecubeMod.creativeTabPokecubeBerries);
        this.setDefaultState(this.blockState.getBaseState().withProperty(BerryManager.type, "null")
                .withProperty(CHECK_DECAY, Boolean.valueOf(true)).withProperty(DECAYABLE, Boolean.valueOf(true)));
        this.setHardness(0.2F);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { BerryManager.type, CHECK_DECAY, DECAYABLE });
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityTickBerries(Type.LEAF);
    }

    @Override
    /** Get the actual Block state of this Block at the given position. This
     * applies properties not visible in the metadata, such as fence
     * connections. */
    public BlockState getActualState(BlockState state, IBlockReader worldIn, BlockPos pos)
    {
        return Blocks.LEAVES.getDefaultState();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer()
    {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public java.util.List<ItemStack> getDrops(IBlockReader world, BlockPos pos, BlockState state, int fortune)
    {
        java.util.List<ItemStack> ret = new java.util.ArrayList<ItemStack>();
        Random rand = world instanceof World ? ((World) world).rand : new Random();
        int chance = this.getSaplingDropChance(state);
        TileEntityBerries tile = (TileEntityBerries) world.getTileEntity(pos);

        if (tile == null) { return ret; }

        String berry = BerryManager.berryNames.get(tile.getBerryId());
        if (fortune > 0)
        {
            chance -= 2 << fortune;
            if (chance < 10) chance = 10;
        }

        if (rand.nextInt(chance) == 0) ret.add(BerryManager.getBerryItem(berry));

        chance = 200;
        if (fortune > 0)
        {
            chance -= 10 << fortune;
            if (chance < 40) chance = 40;
        }

        this.captureDrops(true);
        ret.addAll(this.captureDrops(false));
        return ret;
    }

    @Override
    /** Get the Item that this Block should drop when harvested. */
    public Item getItemDropped(BlockState state, Random rand, int fortune)
    {
        return null;
    }

    @Override
    /** Convert the BlockState into the correct metadata value */
    public int getMetaFromState(BlockState state)
    {
        int i = 0;

        if (!state.getValue(DECAYABLE).booleanValue())
        {
            i |= 4;
        }

        if (state.getValue(CHECK_DECAY).booleanValue())
        {
            i |= 8;
        }

        return i;
    }

    @Override
    /** Called when a user uses the creative pick block button on this block
     *
     * @param target
     *            The full target the player is looking at
     * @return A ItemStack to add to the player's inventory, Null if nothing
     *         should be added. */
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, World world, BlockPos pos,
            PlayerEntity player)
    {
        TileEntityBerries tile = (TileEntityBerries) world.getTileEntity(pos);
        return BerryManager.getBerryItem(tile.getBerryId());
    }

    @Override
    /** Convert the given metadata into a BlockState for this Block */
    public BlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(DECAYABLE, Boolean.valueOf((meta & 4) == 0))
                .withProperty(CHECK_DECAY, Boolean.valueOf((meta & 8) > 0));
    }

    @Override
    public EnumType getWoodType(int meta)
    {
        return EnumType.OAK;
    }

    @Override
    /** Used to determine ambient occlusion and culling when rebuilding chunks
     * for render */
    public boolean isOpaqueCube(BlockState state)
    {
        return false;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockReader world, BlockPos pos, int fortune)
    {
        List<ItemStack> ret = Lists.newArrayList();
        TileEntityBerries tile = (TileEntityBerries) world.getTileEntity(pos);
        String berry = BerryManager.berryNames.get(tile.getBerryId());
        ItemStack stack = BerryManager.getBerryItem(berry);
        ret.add(stack);
        return ret;
    }

}
