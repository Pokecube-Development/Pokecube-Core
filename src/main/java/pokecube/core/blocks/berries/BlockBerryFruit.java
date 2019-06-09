package pokecube.core.blocks.berries;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockBush;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraftforge.common.EnumPlantType;
import pokecube.core.PokecubeItems;
import pokecube.core.blocks.berries.TileEntityBerries.Type;
import pokecube.core.interfaces.IBerryFruitBlock;
import pokecube.core.items.berries.BerryManager;
import thut.lib.CompatWrapper;

/** @author Oracion
 * @author Manchou */
public class BlockBerryFruit extends BlockBush implements IBerryFruitBlock, ITileEntityProvider
{
    protected static final AxisAlignedBB BUSH2_AABB = new AxisAlignedBB(0.35D, 0.55D, 0.35D, 0.65D, 1D, 0.65D);

    public BlockBerryFruit()
    {
        super();
        this.setCreativeTab(null);
        this.setTickRandomly(true);
        this.setDefaultState(this.blockState.getBaseState().withProperty(BerryManager.type, "null"));
    }

    /** Can this block stay at this position. Similar to canPlaceBlockAt except
     * gets checked often with plants. */
    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, BlockState state)
    {
        return worldIn.getBlockState(pos.down()).getBlock() instanceof BlockBerryCrop || worldIn.getBlockState(pos.up())
                .getBlock().isLeaves(worldIn.getBlockState(pos.up()), worldIn, pos.up());
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { BerryManager.type });
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityBerries(Type.FRUIT);
    }

    @Override
    /** Spawns ItemEntity in the world for the given ItemStack if the world is
     * not remote. */
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, BlockState state, float chance, int fortune)
    {
        if (!worldIn.isRemote && !worldIn.restoringBlockSnapshots)
        {
            NonNullList<ItemStack> items = NonNullList.create();
            getDrops(items, worldIn, pos, state, fortune);
            chance = net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, state, fortune,
                    chance, false, harvesters.get());

            if (worldIn.rand.nextFloat() <= chance)
            {
                ItemStack stack = getBerryStack(worldIn, pos);
                if (worldIn.getGameRules().getBoolean("doTileDrops") && CompatWrapper.isValid(stack))
                {
                    if (captureDrops.get())
                    {
                        capturedDrops.get().add(stack);
                        return;
                    }
                    float f = 0.5F;
                    double d0 = worldIn.rand.nextFloat() * f + (1.0F - f) * 0.5D;
                    double d1 = worldIn.rand.nextFloat() * f + (1.0F - f) * 0.5D;
                    double d2 = worldIn.rand.nextFloat() * f + (1.0F - f) * 0.5D;
                    ItemEntity ItemEntity = new ItemEntity(worldIn, pos.getX() + d0, pos.getY() + d1, pos.getZ() + d2,
                            stack);
                    ItemEntity.setDefaultPickupDelay();
                    worldIn.spawnEntity(ItemEntity);
                }
            }
        }
    }

    @Override
    /** Get the actual Block state of this Block at the given position. This
     * applies properties not visible in the metadata, such as fence
     * connections. */
    public BlockState getActualState(BlockState state, IBlockReader worldIn, BlockPos pos)
    {
        if (!(worldIn.getTileEntity(pos) instanceof TileEntityBerries))
            return state.withProperty(BerryManager.type, "null");
        TileEntityBerries tile = (TileEntityBerries) worldIn.getTileEntity(pos);
        if (tile == null) { return state.withProperty(BerryManager.type, "null"); }
        String name = BerryManager.berryNames.get(tile.getBerryId());
        if (name == null) name = "cheri";
        return state.withProperty(BerryManager.type, name);
    }

    @Override
    public ItemStack getBerryStack(IBlockReader world, BlockPos pos)
    {
        if (!(world.getTileEntity(pos) instanceof TileEntityBerries)) return ItemStack.EMPTY;
        TileEntityBerries tile = (TileEntityBerries) world.getTileEntity(pos);
        if (tile == null) return ItemStack.EMPTY;
        return BerryManager.getBerryItem(BerryManager.berryNames.get(tile.getBerryId()));
    }

    @Override
    public AxisAlignedBB getBoundingBox(BlockState state, IBlockReader source, BlockPos pos)
    {
        if (!(source.getTileEntity(pos) instanceof TileEntityBerries)) return BUSH_AABB;
        TileEntityBerries tile = (TileEntityBerries) source.getTileEntity(pos);
        if (TileEntityBerries.trees.containsKey(tile.getBerryId())) { return BUSH2_AABB; }
        return BUSH_AABB;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockReader world, BlockPos pos, BlockState state, int fortune)
    {
        List<ItemStack> ret = drops;
        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        int count = quantityDropped(state, fortune, rand);
        ItemStack stack = getBerryStack(world, pos);
        if (stack.isEmpty()) return;
        stack.setCount(count);
        ret.add(stack);
    }

    /** Returns the ID of the items to drop on destruction. */
    @Override
    public Item getItemDropped(BlockState state, Random p_149650_2_, int p_149650_3_)
    {
        return PokecubeItems.nullberry;
    }

    /** Convert the BlockState into the correct metadata value */
    @Override
    public int getMetaFromState(BlockState state)
    {
        return 0;
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
    public boolean onBlockActivated(World worldIn, BlockPos pos, BlockState state, PlayerEntity playerIn,
            Hand hand, Direction side, float hitX, float hitY, float hitZ)
    {
        this.onBlockHarvested(worldIn, pos, state, playerIn);
        worldIn.setBlockToAir(pos);
        return true;
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player)
    {
        Random rand = worldIn != null ? worldIn.rand : RANDOM;

        int count = 1 + rand.nextInt(2);
        for (int i = 0; i < count; i++)
        {
            Item item = this.getItemDropped(state, rand, 0);
            if (item != null)
            {
                dropBlockAsItemWithChance(worldIn, pos, state, 1, 0);
            }
        }
    }

    /** Returns the quantity of items to drop on block destruction. */
    @Override
    public int quantityDropped(Random par1Random)
    {
        int i = par1Random.nextInt(2) + 1;
        return i;
    }

    @Override
    public EnumPlantType getPlantType(IBlockReader world, BlockPos pos)
    {
        return EnumPlantType.Crop;
    }
}
