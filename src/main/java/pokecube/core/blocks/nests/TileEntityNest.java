package pokecube.core.blocks.nests;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.common.MinecraftForge;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.SpawnData;
import pokecube.core.database.SpawnBiomeMatcher;
import pokecube.core.events.EggEvent;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.events.handlers.SpawnHandler.ForbidReason;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.items.pokemobeggs.EntityPokemobEgg;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import thut.api.maths.Vector3;

public class TileEntityNest extends TileEntity implements ITickable
{
    public static int  NESTSPAWNTYPES = 1;

    HashSet<IPokemob>  residents      = new HashSet<IPokemob>();
    int                time           = 0;
    List<PokedexEntry> spawns         = Lists.newArrayList();

    public TileEntityNest()
    {
    }

    public boolean addForbiddenSpawningCoord()
    {
        BlockPos pos = getPos();
        return SpawnHandler.addForbiddenSpawningCoord(pos.getX(), pos.getY(), pos.getZ(), world.dimension.getDimension(),
                16, ForbidReason.NEST);
    }

    public boolean removeForbiddenSpawningCoord()
    {
        return SpawnHandler.removeForbiddenSpawningCoord(getPos(), world.dimension.getDimension());
    }

    public void addResident(IPokemob resident)
    {
        residents.add(resident);
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return null;
    }

    public void init()
    {
        Vector3 pos = Vector3.getNewVector().set(this);
        // TODO init spawn for nest here.
        for (int i = 0; i < NESTSPAWNTYPES; i++)
        {
            int tries = 0;
            PokedexEntry entry = SpawnHandler.getSpawnForLoc(getWorld(), pos);
            while (entry == null && tries++ < 10)
                entry = SpawnHandler.getSpawnForLoc(getWorld(), pos);
            if (entry != null) spawns.add(entry);
        }
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        removeForbiddenSpawningCoord();
    }

    /** Reads a tile entity from NBT. */
    @Override
    public void readFromNBT(CompoundNBT nbt)
    {
        super.readFromNBT(nbt);
        spawns.clear();
        if (nbt.hasKey("spawns"))
        {
            ListNBT spawnsTag = (ListNBT) nbt.getTag("spawns");
            for (int i = 0; i < spawnsTag.size(); i++)
            {
                String name = spawnsTag.getStringTagAt(i);
                PokedexEntry entry = Database.getEntry(name);
                if (entry != null) spawns.add(entry);
            }
        }
        time = nbt.getInteger("time");
    }

    public void removeResident(IPokemob resident)
    {
        residents.remove(resident);
    }

    @Override
    public void update()
    {
        time++;
        int power = world.getRedstonePower(getPos(), Direction.DOWN);
        if (world.isRemote || (world.getDifficulty() == EnumDifficulty.PEACEFUL && power == 0)) return;
        if (spawns.isEmpty() && time >= 200)
        {
            time = 0;
            init();
        }
        if (spawns.isEmpty() || time < 200 + world.rand.nextInt(2000)) return;
        time = 0;
        int num = 3;
        PokedexEntry entry = spawns.get(world.rand.nextInt(spawns.size()));
        SpawnData data = entry.getSpawnData();
        if (data != null)
        {
            Vector3 here = Vector3.getNewVector().set(this);
            SpawnBiomeMatcher matcher = data.getMatcher(world, here);
            int min = data.getMin(matcher);
            int max = data.getMax(matcher);
            int diff = Math.max(1, max - min);
            num = min + world.rand.nextInt(diff);
        }
        if (residents.size() < num)
        {
            ItemStack eggItem = ItemPokemobEgg.getEggStack(entry);
            CompoundNBT nbt = eggItem.getTag();
            nbt.putIntArray("nestLocation", new int[] { getPos().getX(), getPos().getY(), getPos().getZ() });
            eggItem.put(nbt);
            Random rand = new Random();
            EntityPokemobEgg egg = new EntityPokemobEgg(world, getPos().getX() + rand.nextGaussian(),
                    getPos().getY() + 1, getPos().getZ() + rand.nextGaussian(), eggItem, null);
            EggEvent.Lay event = new EggEvent.Lay(egg);
            MinecraftForge.EVENT_BUS.post(event);
            if (!event.isCanceled())
            {
                world.spawnEntity(egg);
            }
        }
    }

    @Override
    public void validate()
    {
        super.validate();
        addForbiddenSpawningCoord();
    }

    /** Writes a tile entity to NBT.
     * 
     * @return */
    @Override
    public CompoundNBT writeToNBT(CompoundNBT nbt)
    {
        super.writeToNBT(nbt);
        ListNBT spawnsTag = new ListNBT();
        for (PokedexEntry entry : spawns)
        {
            spawnsTag.appendTag(new StringNBT(entry.getTrimmedName()));
        }
        nbt.put("spawns", spawnsTag);
        nbt.setInteger("time", time);
        return nbt;
    }
}
