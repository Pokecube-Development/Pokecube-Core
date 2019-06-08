package pokecube.core.blocks.pc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import pokecube.core.blocks.TileEntityOwnable;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.network.packets.PacketPC;
import thut.api.network.PacketHandler;

public class TileEntityPC extends TileEntityOwnable implements IInventory
{
    private boolean     bound     = false;
    private UUID        boundId   = null;
    private String      boundName = "";
    public List<String> visible   = new ArrayList<String>();

    public TileEntityPC()
    {
        super();
    }

    @Override
    public void clear()
    {
    }

    @Override
    public void closeInventory(PlayerEntity player)
    {
    }

    @Override
    public ItemStack decrStackSize(int i, int j)
    {
        if (getPC() != null) { return getPC().decrStackSize(i, j); }
        return null;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return null;
    }

    @Override
    public int getField(int id)
    {
        return 0;
    }

    @Override
    public int getFieldCount()
    {
        return 0;
    }

    @Override
    public int getInventoryStackLimit()
    {
        if (getPC() != null) return getPC().getInventoryStackLimit();
        return 0;
    }

    @Override
    public String getName()
    {
        if (getPC() != null) { return boundName; }
        return null;
    }

    public InventoryPC getPC()
    {
        if (bound) { return InventoryPC.getPC(boundId); }
        return null;
    }

    @Override
    public int getSizeInventory()
    {
        if (getPC() != null) return getPC().getSizeInventory();
        return 0;
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        if (getPC() != null) { return getPC().getStackInSlot(i); }
        return null;
    }

    /** Overriden in a sign to provide the text. */
    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        CompoundNBT CompoundNBT = new CompoundNBT();
        this.writeToNBT(CompoundNBT);
        return new SPacketUpdateTileEntity(this.getPos(), 3, CompoundNBT);
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        CompoundNBT nbt = new CompoundNBT();
        return writeToNBT(nbt);
    }

    @Override
    public boolean hasCustomName()
    {
        if (getPC() != null) return getPC().hasCustomName();
        return false;
    }

    public boolean isBound()
    {
        return bound;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack)
    {
        if (getPC() != null) { return getPC().isItemValidForSlot(i, itemstack); }
        return false;
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity PlayerEntity)
    {
        if (getPC() != null) return getPC().isUsableByPlayer(PlayerEntity);
        return false;
    }

    /** Called when you receive a TileEntityData packet for the location this
     * TileEntity is currently in. On the client, the NetworkManager will always
     * be the remote server. On the server, it will be whomever is responsible
     * for sending the packet.
     *
     * @param net
     *            The NetworkManager the packet originated from
     * @param pkt
     *            The data packet */
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    {
        CompoundNBT nbt = pkt.getNbtCompound();
        this.readFromNBT(nbt);
    }

    @Override
    public void openInventory(PlayerEntity player)
    {
    }

    /** Reads a tile entity from NBT. */
    @Override
    public void readFromNBT(CompoundNBT par1CompoundNBT)
    {
        super.readFromNBT(par1CompoundNBT);
        this.bound = par1CompoundNBT.getBoolean("bound");
        try
        {
            if (par1CompoundNBT.hasKey("boundID"))
                boundId = UUID.fromString(par1CompoundNBT.getString("boundID"));
        }
        catch (Exception e)
        {
        }
        boundName = par1CompoundNBT.getString("boundName");
        if (boundId == null)
        {
            boundId = InventoryPC.defaultId;
            boundName = "Public Box";
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int i)
    {
        if (getPC() != null) { return getPC().removeStackFromSlot(i); }
        return null;
    }

    public void setBoundOwner(PlayerEntity player)
    {
        if (!canEdit(player)) return;
        if (this.world.isRemote)
        {
            PacketPC packet = new PacketPC(PacketPC.BIND);
            packet.data.putBoolean("O", false);
            PokecubeMod.packetPipeline.sendToServer(packet);
            return;
        }
        TileEntity te = world.getTileEntity(getPos().down());
        if (te != null && te instanceof TileEntityPC) ((TileEntityPC) te).setBoundOwner(player);
        boundId = player.getUniqueID();
        boundName = player.getDisplayNameString();
        if (!world.isRemote)
        {
            PacketHandler.sendTileUpdate(this);
        }
    }

    @Override
    public void setField(int id, int value)
    {
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack)
    {
        if (getPC() != null)
        {
            getPC().setInventorySlotContents(i, itemstack);
        }
    }

    public void toggleBound()
    {
        if (this.world.isRemote)
        {
            PacketPC packet = new PacketPC(PacketPC.BIND);
            packet.data.putBoolean("O", true);
            PokecubeMod.packetPipeline.sendToServer(packet);
            return;
        }

        TileEntity te = world.getTileEntity(getPos().down());
        this.bound = !this.bound;

        if (te != null && te instanceof TileEntityPC) ((TileEntityPC) te).toggleBound();

        if (bound)
        {
            boundId = InventoryPC.defaultId;
            boundName = "Public";
        }
        else
        {
            boundId = null;
            boundName = "";
        }
        if (!world.isRemote)
        {
            PacketHandler.sendTileUpdate(this);
        }
    }

    /** Writes a tile entity to NBT.
     * 
     * @return */
    @Override
    public CompoundNBT writeToNBT(CompoundNBT par1CompoundNBT)
    {
        super.writeToNBT(par1CompoundNBT);
        par1CompoundNBT.putBoolean("bound", bound);
        if (boundId != null) par1CompoundNBT.putString("boundID", boundId.toString());
        par1CompoundNBT.putString("boundName", boundName);
        return par1CompoundNBT;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }
}
