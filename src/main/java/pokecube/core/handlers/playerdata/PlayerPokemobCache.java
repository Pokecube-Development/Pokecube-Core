package pokecube.core.handlers.playerdata;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.items.pokecubes.PokecubeManager;
import thut.core.common.handlers.PlayerDataHandler;
import thut.core.common.handlers.PlayerDataHandler.PlayerData;

/** This is a backup cache of the pokemobs owned by the player. */
public class PlayerPokemobCache extends PlayerData
{
    public static void UpdateCache(IPokemob mob)
    {
        if (!mob.isPlayerOwned() || mob.getOwnerId() == null) return;
        final ItemStack stack = PokecubeManager.pokemobToItem(mob);
        // Schedule this to run at some point, as it takes a while.
        PokecubeCore.proxy.getMainThreadListener().addScheduledTask(new Runnable()
        {
            @Override
            public void run()
            {
                UpdateCache(stack, false, false);
            }
        });
    }

    public static void UpdateCache(ItemStack stack, boolean pc, boolean deleted)
    {
        String owner = PokecubeManager.getOwner(stack);
        if (owner.isEmpty()) return;
        Integer uid = PokecubeManager.getUID(stack);
        if (uid == null || uid == -1) return;
        PlayerDataHandler.getInstance().getPlayerData(owner).getData(PlayerPokemobCache.class).addPokemob(stack, pc,
                deleted);
    }

    public Map<Integer, ItemStack> cache        = Maps.newHashMap();
    public Set<Integer>            inPC         = Sets.newHashSet();
    public Set<Integer>            genesDeleted = Sets.newHashSet();

    public PlayerPokemobCache()
    {
        super();
    }

    public void addPokemob(IPokemob mob)
    {
        if (!mob.isPlayerOwned() || mob.getOwnerId() == null) return;
        ItemStack stack = PokecubeManager.pokemobToItem(mob);
        addPokemob(stack, false, false);
    }

    public void addPokemob(String owner, ItemStack stack, boolean pc, boolean deleted)
    {
        Integer uid = PokecubeManager.getUID(stack);
        if (uid == null || uid == -1) return;
        cache.put(uid, stack);
        pc = pc ? inPC.add(uid) : inPC.remove(uid);
        if (deleted) genesDeleted.add(uid);
        PlayerDataHandler.getInstance().save(owner, getIdentifier());
    }

    public void addPokemob(ItemStack stack, boolean pc, boolean deleted)
    {
        String owner = PokecubeManager.getOwner(stack);
        addPokemob(owner, stack, pc, deleted);
    }

    @Override
    public void writeToNBT(CompoundNBT tag)
    {
        ListNBT list = new ListNBT();
        for (Integer id : cache.keySet())
        {
            CompoundNBT var = new CompoundNBT();
            var.putInt("uid", id);
            var.putBoolean("_in_pc_", inPC.contains(id));
            var.putBoolean("_dead_", genesDeleted.contains(id));
            ItemStack stack = cache.get(id);
            stack.writeToNBT(var);
            list.appendTag(var);
        }
        tag.put("data", list);
    }

    @Override
    public void readFromNBT(CompoundNBT tag)
    {
        cache.clear();
        inPC.clear();
        genesDeleted.clear();
        if (tag.hasKey("data"))
        {
            ListNBT list = (ListNBT) tag.getTag("data");
            for (int i = 0; i < list.size(); i++)
            {
                CompoundNBT var = list.getCompound(i);
                Integer id = -1;
                ItemStack stack = new ItemStack(var);
                if (var.hasKey("uid"))
                {
                    id = var.getInt("uid");
                }
                else
                {
                    id = PokecubeManager.getUID(stack);
                }
                if (id != -1)
                {
                    cache.put(id, stack);
                    if (var.getBoolean("_in_pc_"))
                    {
                        inPC.add(id);
                    }
                    if (var.getBoolean("_dead_"))
                    {
                        genesDeleted.add(id);
                    }
                }
            }
        }
    }

    @Override
    public String getIdentifier()
    {
        return "pokecube-pokemobs";
    }

    @Override
    public boolean shouldSync()
    {
        return false;
    }

    @Override
    public String dataFileName()
    {
        return "pokecube_pokemob_cache";
    }
}
