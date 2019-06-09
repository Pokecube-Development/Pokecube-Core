package pokecube.core.blocks.tradingTable;

import java.util.Random;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokecubes.PokecubeManager;
import thut.core.common.blocks.BlockRotatable;
import thut.lib.CompatWrapper;

public class BlockTradingTable extends BlockRotatable implements ITileEntityProvider
{
    public final boolean tradingTable;

    public BlockTradingTable(boolean tradingtable)
    {
        super(Material.CLOTH);
        this.tradingTable = tradingtable;
        // this.setBlockBounds(0, 0, 0, 1, 0.75f, 1);
        this.setCreativeTab(PokecubeMod.creativeTabPokecube);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, Direction.NORTH));
        this.setHardness(100);
        this.setResistance(100);
        this.setLightOpacity(0);
        this.setRegistryName("pokecube", tradingtable ? "trading_table" : "tm_machine");
        this.setCreativeTab(PokecubeMod.creativeTabPokecubeBlocks);
        this.setUnlocalizedName(getRegistryName().getResourcePath());
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, BlockState state)
    {
        dropItems(worldIn, pos);
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING });
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2)
    {
        return tradingTable ? new TileEntityTradingTable() : new TileEntityTMMachine();
    }

    private void dropItems(World world, BlockPos pos)
    {
        Random rand = new Random();
        TileEntity tile_entity = world.getTileEntity(pos);

        if (!(tile_entity instanceof IInventory)) { return; }

        if (tile_entity instanceof TileEntityTradingTable)
        {
            TileEntityTradingTable table = (TileEntityTradingTable) tile_entity;
            if (table.player1 != null) table.player1.closeScreen();
            if (table.player2 != null) table.player2.closeScreen();
        }
        IInventory inventory = (IInventory) tile_entity;

        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack item = inventory.getStackInSlot(i);
            if (CompatWrapper.isValid(item))
            {
                float rx = rand.nextFloat() * 0.6F + 0.1F;
                float ry = rand.nextFloat() * 0.6F + 0.1F;
                float rz = rand.nextFloat() * 0.6F + 0.1F;
                ItemEntity entity_item = new ItemEntity(world, pos.getX() + rx, pos.getY() + ry, pos.getZ() + rz,
                        new ItemStack(item.getItem(), item.getCount(), item.getItemDamage()));
                if (item.hasTag())
                {
                    entity_item.getItem().put(item.getTag().copy());
                }
                if (PokecubeManager.isFilled(item))
                {
                    ItemTossEvent toss = new ItemTossEvent(entity_item, PokecubeMod.getFakePlayer(world));
                    MinecraftForge.EVENT_BUS.post(toss);
                    boolean toPC = toss.isCanceled();
                    if (toPC)
                    {
                        continue;
                    }
                }
                float factor = 0.5F;
                entity_item.motionX = rand.nextGaussian() * factor;
                entity_item.motionY = rand.nextGaussian() * factor + 0.2F;
                entity_item.motionZ = rand.nextGaussian() * factor;
                world.spawnEntity(entity_item);
                item.setCount(0);
            }
        }
    }

    @Override
    /** Convert the BlockState into the correct metadata value */
    public int getMetaFromState(BlockState state)
    {
        int ret = state.getValue(FACING).getIndex();
        return ret;
    }

    @Override
    /** Convert the given metadata into a BlockState for this Block */
    public BlockState getStateFromMeta(int meta)
    {
        Direction Direction = Direction.getFront(meta);
        if (Direction.getAxis() == Direction.Axis.Y)
        {
            Direction = Direction.NORTH;
        }
        return this.getDefaultState().withProperty(FACING, Direction);
    }

    @Override
    public boolean isFullCube(BlockState state)
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube(BlockState state)
    {
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, BlockState state, PlayerEntity playerIn,
            Hand hand, Direction side, float hitX, float hitY, float hitZ)
    {

        if (tradingTable)
        {
            TileEntityTradingTable table = (TileEntityTradingTable) worldIn.getTileEntity(pos);
            table.openGUI(playerIn);
            return true;
        }
        else 
        {
            TileEntityTMMachine table = (TileEntityTMMachine) worldIn.getTileEntity(pos);
            table.openGUI(playerIn);
            return true;
        }
    }

    @Override
    public BlockState getStateForPlacement(World worldIn, BlockPos pos, Direction facing, float hitX, float hitY,
            float hitZ, int meta, LivingEntity placer)
    {
        return this.getStateFromMeta(meta).withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }
}
