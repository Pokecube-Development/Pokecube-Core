package pokecube.core.blocks.pc;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;
import pokecube.core.blocks.TileEntityOwnable;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.network.packets.PacketPC;
import thut.core.common.blocks.BlockRotatable;

public class BlockPC extends BlockRotatable implements ITileEntityProvider
{
    public final boolean top;

    public BlockPC(boolean top)
    {
        super(Material.GLASS);
        this.top = top;
        this.setLightOpacity(0);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, Direction.NORTH));
        this.setHardness(500);
        this.setResistance(100);
        this.setLightLevel(1f);
        this.setRegistryName(PokecubeMod.ID, "pc_" + (top ? "top" : "base"));
        this.setCreativeTab(PokecubeMod.creativeTabPokecubeBlocks);
        this.setUnlocalizedName(getRegistryName().getResourcePath());
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING });
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2)
    {
        return new TileEntityPC();
    }

    @Override
    public BlockState getExtendedState(BlockState state, IBlockReader world, BlockPos pos)
    {
        return super.getExtendedState(state, world, pos);
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
        Direction Direction = Direction.getFront(meta & 7);

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
        this.setLightLevel(1f);
        if (!top) { return false; }
        BlockState down = worldIn.getBlockState(pos.down());
        Block idDown = down.getBlock();
        if (!(idDown instanceof BlockPC) || ((BlockPC) idDown).top
                || !(worldIn.getTileEntity(pos) instanceof TileEntityPC))
            return false;
        TileEntityPC pc = (TileEntityPC) worldIn.getTileEntity(pos);
        InventoryPC inventoryPC = pc.isBound() ? pc.getPC() : InventoryPC.getPC(playerIn.getUniqueID());
        if (inventoryPC != null)
        {
            if (worldIn.isRemote) { return true; }
            PacketPC.sendOpenPacket(playerIn, inventoryPC.owner, pos);
            return true;
        }
        return true;
    }

    @Override
    public BlockState getStateForPlacement(World world, BlockPos pos, Direction facing, float hitX, float hitY,
            float hitZ, int meta, LivingEntity placer, Hand hand)
    {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityOwnable)
        {
            TileEntityOwnable tile = (TileEntityOwnable) te;
            tile.setPlacer(placer);
        }
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }
}