package pokecube.core.blocks.nests;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;
import pokecube.core.database.PokedexEntry;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import thut.api.block.PropertyInteger;

public class BlockNest extends Block implements ITileEntityProvider
{
    public static ArrayList<String> types = new ArrayList<String>();

    static
    {
        types.add("nest");
        types.add("secretPortal");
    }

    public PropertyInteger TYPE;

    public BlockNest()
    {
        super(Material.ROCK);
        setLightOpacity(2);
        this.setHardness(10);
        this.setDefaultState(this.blockState.getBaseState().withProperty(TYPE, Integer.valueOf(0)));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        if (meta == 0) return new TileEntityNest();
        else if (meta == 1) return new TileEntityBasePortal();
        else return null;
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        if (TYPE == null)
        {
            TYPE = PropertyInteger.create("type", 0, types.size() - 1);
        }
        return new BlockStateContainer(this, new IProperty[] { TYPE });
    }

    @Override
    @Deprecated
    public BlockState getActualState(BlockState state, IBlockReader worldIn, BlockPos pos)
    {
        if (state.getValue(TYPE) == 1) return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT,
                BlockStoneBrick.EnumType.CHISELED);
        if (pos.getY() > 0) state = worldIn.getBlockState(pos.down()).getActualState(worldIn, pos.down());
        if (!state.isBlockNormalCube()) state = Blocks.STONE.getDefaultState();
        return state;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, BlockState state, PlayerEntity playerIn,
            Hand hand, Direction side, float hitX, float hitY, float hitZ)
    {
        TileEntity tile_entity = worldIn.getTileEntity(pos);
        if (tile_entity instanceof TileEntityNest && playerIn.capabilities.isCreativeMode
                && playerIn.getHeldItemMainhand() != null
                && playerIn.getHeldItemMainhand().getItem() instanceof ItemPokemobEgg
                && !playerIn.getEntityWorld().isRemote)
        {
            TileEntityNest nest = (TileEntityNest) tile_entity;
            PokedexEntry entry = ItemPokemobEgg.getEntry(playerIn.getHeldItemMainhand());
            if (entry != null) nest.spawns.add(entry);
            playerIn.sendMessage(new StringTextComponent("Added " + entry));
            return true;
        }
        if (state.getValue(TYPE) == 1)
        {
            if (!worldIn.isRemote)
            {
                TileEntityBasePortal portal = (TileEntityBasePortal) tile_entity;
                portal.transferPlayer(playerIn);
            }
        }
        return true;
    }

    @Override
    /** Convert the BlockState into the correct metadata value */
    public int getMetaFromState(BlockState state)
    {
        if (state.getBlock() == this) return state.getValue(TYPE).intValue();
        return state.getBlock().getMetaFromState(state);
    }

    @Override
    /** Convert the given metadata into a BlockState for this Block */
    public BlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, Integer.valueOf(meta));
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        return state.getValue(TYPE).intValue() < 2;
    }

    /** Get the Item that this Block should drop when harvested. */
    @Override
    public List<ItemStack> getDrops(IBlockReader world, BlockPos pos, BlockState state, int fortune)
    {
        List<ItemStack> ret = new java.util.ArrayList<ItemStack>();

        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        int count = 1;
        for (int i = 0; i < count; i++)
        {
            Item item = Blocks.STONE.getItemDropped(Blocks.STONE.getDefaultState(), rand, fortune);
            if (item != null)
            {
                ret.add(new ItemStack(item, 1, Blocks.STONE.damageDropped(Blocks.STONE.getDefaultState())));
            }
        }
        return ret;
    }
}
