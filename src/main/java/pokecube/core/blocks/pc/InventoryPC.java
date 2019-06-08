package pokecube.core.blocks.pc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.common.FMLCommonHandler;
import pokecube.core.PokecubeCore;
import pokecube.core.handlers.playerdata.PlayerPokemobCache;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.utils.PCSaveHandler;
import thut.core.common.handlers.PlayerDataHandler;
import thut.lib.CompatWrapper;

public class InventoryPC implements IInventory, INBTSerializable<CompoundNBT>
{
    static HashMap<UUID, InventoryPC> map_server = new HashMap<UUID, InventoryPC>();
    static HashMap<UUID, InventoryPC> map_client = new HashMap<UUID, InventoryPC>();

    public static HashMap<UUID, InventoryPC> getMap()
    {
        return FMLCommonHandler.instance().getEffectiveSide() == Dist.CLIENT ? map_client : map_server;
    }

    // blank PC for client use.
    public static InventoryPC blank;
    public static UUID        defaultId = new UUID(1234, 4321);
    public static int         PAGECOUNT = 32;

    public static void addPokecubeToPC(ItemStack mob, World world)
    {
        if (!(PokecubeManager.isFilled(mob))) return;
        String player = PokecubeManager.getOwner(mob);
        UUID id;
        try
        {
            id = UUID.fromString(player);
            addStackToPC(id, mob);
        }
        catch (Exception e)
        {

        }
    }

    public static void addStackToPC(UUID uuid, ItemStack mob)
    {
        if (uuid == null || !CompatWrapper.isValid(mob))
        {
            System.err.println("Could not find the owner of this item " + mob + " " + uuid);
            return;
        }
        InventoryPC pc = getPC(uuid);

        if (pc == null) { return; }

        if (PokecubeManager.isFilled(mob))
        {
            ItemStack stack = mob;
            PokecubeManager.heal(stack);
            PlayerPokemobCache.UpdateCache(mob, true, false);
            if (PokecubeCore.proxy.getPlayer(uuid) != null) PokecubeCore.proxy.getPlayer(uuid)
                    .sendMessage(new TranslationTextComponent("tile.pc.sentto", mob.getDisplayName()));
        }
        pc.addItem(mob.copy());
        PCSaveHandler.getInstance().savePC(uuid);
    }

    public static void clearPC()
    {
        getMap().clear();
    }

    public static InventoryPC getPC(Entity player)
    {// TODO Sync box names/numbers to blank
        if (player == null || player.getEntityWorld().isRemote)
            return blank == null ? blank = new InventoryPC(defaultId) : blank;
        return getPC(player.getUniqueID());
    }

    public static InventoryPC getPC(UUID uuid)
    {
        if (uuid != null)
        {
            if (!getMap().containsKey(uuid))
            {
                PCSaveHandler.getInstance().loadPC(uuid);
            }
            if (getMap().containsKey(uuid)) { return getMap().get(uuid); }
            return new InventoryPC(uuid);
        }
        return null;
    }

    public static void loadFromNBT(ListNBT nbt)
    {
        loadFromNBT(nbt, true);
    }

    public static void loadFromNBT(ListNBT nbt, boolean replace)
    {
        int i;
        tags:
        for (i = 0; i < nbt.size(); i++)
        {
            CompoundNBT items = nbt.getCompound(i);
            InventoryPC loaded = new InventoryPC();
            loaded.deserializeNBT(items);
            if (!replace && getMap().containsKey(loaded.owner)) continue;
            if (PokecubeMod.debug) PokecubeMod.log("Loading PC for " + loaded.owner);
            InventoryPC load = null;
            load = replace ? loaded : getPC(loaded.owner);
            if (load == null)
            {
                if (PokecubeMod.debug) PokecubeMod.log("Skipping " + loaded.owner);
                continue tags;
            }
            load.autoToPC = loaded.autoToPC;
            load.seenOwner = loaded.seenOwner;
            load.setPage(loaded.getPage());
            if (load != loaded)
            {
                load.contents.clear();
                load.contents.putAll(loaded.contents);
            }
            getMap().put(loaded.owner, load);
        }
    }

    public static ListNBT saveToNBT(UUID uuid)
    {
        if (PokecubeMod.debug) PokecubeMod.log("Saving PC for " + uuid);
        ListNBT nbttag = new ListNBT();
        CompoundNBT items = getMap().get(uuid).serializeNBT();
        nbttag.appendTag(items);
        return nbttag;
    }

    private int                              page      = 0;

    public boolean                           autoToPC  = false;

    public boolean[]                         opened    = new boolean[PAGECOUNT];

    public String[]                          boxes     = new String[PAGECOUNT];
    private Int2ObjectOpenHashMap<ItemStack> contents  = new Int2ObjectOpenHashMap<>();

    public UUID                              owner;

    public boolean                           seenOwner = false;

    public InventoryPC()
    {
        opened = new boolean[PAGECOUNT];
        boxes = new String[PAGECOUNT];
        for (int i = 0; i < PAGECOUNT; i++)
        {
            boxes[i] = "Box " + String.valueOf(i + 1);
        }
    }

    public InventoryPC(UUID player)
    {
        if (!getMap().containsKey(player)) getMap().put(player, this);
        opened = new boolean[PAGECOUNT];
        boxes = new String[PAGECOUNT];
        owner = player;
        for (int i = 0; i < PAGECOUNT; i++)
        {
            boxes[i] = "Box " + String.valueOf(i + 1);
        }
    }

    public void addItem(ItemStack stack)
    {
        for (int i = page * 54; i < getSizeInventory(); i++)
        {
            if (!CompatWrapper.isValid(this.getStackInSlot(i)))
            {
                this.setInventorySlotContents(i, stack);
                return;
            }
        }
        for (int i = 0; i < page * 54; i++)
        {
            if (!CompatWrapper.isValid(this.getStackInSlot(i)))
            {
                this.setInventorySlotContents(i, stack);
                return;
            }
        }
    }

    @Override
    public void clear()
    {
        this.contents.clear();
    }

    @Override
    public void closeInventory(PlayerEntity player)
    {
        PCSaveHandler.getInstance().savePC(owner);
    }

    @Override
    public ItemStack decrStackSize(int i, int j)
    {
        if (CompatWrapper.isValid(contents.get(i)))
        {
            ItemStack itemstack = contents.get(i).splitStack(j);
            if (!CompatWrapper.isValid(contents.get(i)))
            {
                contents.remove(i);
            }
            return itemstack;
        }
        return ItemStack.EMPTY;
    }

    public HashSet<ItemStack> getContents()
    {
        HashSet<ItemStack> ret = new HashSet<ItemStack>();
        for (int i : contents.keySet())
        {
            if (CompatWrapper.isValid(contents.get(i))) ret.add(contents.get(i));
        }
        return ret;
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
        return ContainerPC.STACKLIMIT;
    }

    @Override
    public String getName()
    {
        PlayerEntity player = PokecubeCore.getPlayer(owner.toString());
        String name = "Public";
        if (!owner.equals(defaultId))
        {
            name = "Private bound";
        }
        if (player != null)
        {
            name = player.getName() + "'s";
        }
        else if (name.equals("Public")) { return "tile.pc.public"; }
        return "tile.pc.title";
    }

    public int getPage()
    {
        return page;
    }

    @Override
    public int getSizeInventory()
    {
        return PAGECOUNT * 54;
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        ItemStack stack = contents.get(i);
        if (stack == null) stack = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public boolean hasCustomName()
    {
        return false;
    }

    /** Returns true if automation is allowed to insert the given stack
     * (ignoring stack size) into the given slot. */
    @Override
    public boolean isItemValidForSlot(int par1, ItemStack stack)
    {
        return ContainerPC.isItemValid(stack);
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity PlayerEntity)
    {
        return true;
    }

    @Override
    public void markDirty()
    {
    }

    @Override
    public void openInventory(PlayerEntity player)
    {
    }

    @Override
    public ItemStack removeStackFromSlot(int i)
    {
        ItemStack stack = contents.remove(i);
        if (stack == null) stack = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setField(int id, int value)
    {

    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack)
    {
        if (CompatWrapper.isValid(itemstack)) contents.put(i, itemstack);
        else contents.remove(i);
    }

    public void setPage(int page)
    {
        this.page = page;
    }

    @Override
    public String toString()
    {
        String ret = "Owner: " + owner + ", Current Page, " + (getPage() + 1) + ": Auto Move, " + autoToPC + ": ";
        String eol = System.getProperty("line.separator");
        ret += eol;
        for (int i : contents.keySet())
        {
            if (CompatWrapper.isValid(this.getStackInSlot(i)))
            {
                ret += "Slot " + i + ", " + this.getStackInSlot(i).getDisplayName() + "; ";
            }
        }
        ret += eol;
        for (int i = 0; i < boxes.length; i++)
        {
            ret += "Box " + (i + 1) + ", " + boxes[i] + "; ";
        }
        ret += eol;
        return ret;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    public CompoundNBT serializeBox(int box)
    {
        CompoundNBT items = new CompoundNBT();
        items.setInteger("box", box);
        int start = box * 54;
        for (int i = start; i < start + 54; i++)
        {
            ItemStack itemstack = getStackInSlot(i);
            CompoundNBT CompoundNBT = new CompoundNBT();
            if (!itemstack.isEmpty())
            {
                CompoundNBT.setShort("Slot", (short) i);
                itemstack.writeToNBT(CompoundNBT);
                items.put("item" + i, CompoundNBT);
            }
        }
        return items;
    }

    public void deserializeBox(CompoundNBT nbt)
    {
        int start = nbt.getInteger("box") * 54;
        for (int i = start; i < start + 54; i++)
        {
            this.setInventorySlotContents(i, ItemStack.EMPTY);
            if (!nbt.hasKey("item" + i)) continue;
            CompoundNBT CompoundNBT = nbt.getCompound("item" + i);
            int j = CompoundNBT.getShort("Slot");
            if (j >= start && j < start + 54)
            {
                ItemStack itemstack = new ItemStack(CompoundNBT);
                this.setInventorySlotContents(j, itemstack);
            }
        }
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        CompoundNBT items = new CompoundNBT();
        CompoundNBT boxes = new CompoundNBT();
        boxes.putString("UUID", owner.toString());
        boxes.putBoolean("seenOwner", seenOwner);
        boxes.putBoolean("autoSend", autoToPC);
        boxes.setInteger("page", page);
        for (int i = 0; i < PAGECOUNT; i++)
        {
            boxes.putString("name" + i, this.boxes[i]);
        }
        items.setInteger("page", getPage());
        for (int i = 0; i < getSizeInventory(); i++)
        {
            ItemStack itemstack = getStackInSlot(i);
            CompoundNBT CompoundNBT = new CompoundNBT();
            if (!itemstack.isEmpty())
            {
                CompoundNBT.setShort("Slot", (short) i);
                itemstack.writeToNBT(CompoundNBT);
                items.put("item" + i, CompoundNBT);
            }
        }
        items.put("boxes", boxes);
        return items;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {
        CompoundNBT boxes = nbt.getCompound("boxes");
        String id = boxes.getString("UUID");
        this.owner = UUID.fromString(id);
        PlayerPokemobCache cache = PlayerDataHandler.getInstance().getPlayerData(owner)
                .getData(PlayerPokemobCache.class);
        for (int k = 0; k < PAGECOUNT; k++)
        {
            if (k == 0)
            {
                this.autoToPC = boxes.getBoolean("autoSend");
                this.seenOwner = boxes.getBoolean("seenOwner");
                this.setPage(boxes.getInteger("page"));
            }
            if (boxes.getString("name" + k) != null)
            {
                this.boxes[k] = boxes.getString("name" + k);
            }
        }
        contents.clear();
        for (int k = 0; k < this.getSizeInventory(); k++)
        {
            if (!nbt.hasKey("item" + k)) continue;
            CompoundNBT CompoundNBT = nbt.getCompound("item" + k);
            int j = CompoundNBT.getShort("Slot");
            if (j >= 0 && j < this.getSizeInventory())
            {
                if (this.contents.containsKey(j)) continue;
                ItemStack itemstack = new ItemStack(CompoundNBT);
                this.setInventorySlotContents(j, itemstack);
                cache.addPokemob(id, itemstack, true, false);
            }
        }
    }

}
