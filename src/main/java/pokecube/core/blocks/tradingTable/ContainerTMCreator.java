package pokecube.core.blocks.tradingTable;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import thut.lib.CompatWrapper;

public class ContainerTMCreator extends Container
{
    TileEntityTMMachine tile;

    public ContainerTMCreator(TileEntityTMMachine tile, InventoryPlayer playerInv)
    {
        this.tile = tile;
        if (tile != null) tile.moves(playerInv.player);
        bindInventories(playerInv);
    }

    public void bindInventories(InventoryPlayer playerInv)
    {
        addSlotToContainer(new SlotTMCreator(tile == null ? new InventoryBasic("", false, 1) : tile, 0, 15, 12));
        bindPlayerInventory(playerInv);
    }

    private void bindPlayerInventory(InventoryPlayer playerInventory)
    {
        // Action Bar
        for (int x = 0; x < 9; x++)
            addSlotToContainer(new Slot(playerInventory, x, 8 + x * 18, 142));

        // Inventory
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                addSlotToContainer(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
    }

    @Override
    public boolean canInteractWith(PlayerEntity PlayerEntity)
    {
        return tile.isUsableByPlayer(PlayerEntity);
    }

    protected void clearSlots()
    {
        this.inventorySlots.clear();
    }

    public TileEntityTMMachine getTile()
    {
        return tile;
    }

    @Override
    public void onContainerClosed(PlayerEntity playerIn)
    {
        super.onContainerClosed(playerIn);
        tile.closeInventory(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < 1)
            {
                if (!this.mergeItemStack(itemstack1, 1, this.inventorySlots.size(), false)) { return ItemStack.EMPTY; }
            }
            else if (!this.mergeItemStack(itemstack1, 0, 1, false)) { return ItemStack.EMPTY; }
            if (!CompatWrapper.isValid(itemstack1))
            {
                slot.putStack(ItemStack.EMPTY);
            }
            else
            {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }

}
