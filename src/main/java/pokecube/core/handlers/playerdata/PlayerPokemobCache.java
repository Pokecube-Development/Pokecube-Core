package pokecube.core.handlers.playerdata;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
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
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        // Schedule this to run at some point, as it takes a while.
        server.execute(() -> PlayerPokemobCache.UpdateCache(stack, false, false));
    }

    public static void UpdateCache(ItemStack stack, boolean pc, boolean deleted)
    {
        final String owner = PokecubeManager.getOwner(stack);
        if (owner.isEmpty()) return;
        final Integer uid = PokecubeManager.getUID(stack);
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
        final ItemStack stack = PokecubeManager.pokemobToItem(mob);
        this.addPokemob(stack, false, false);
    }

    public void addPokemob(ItemStack stack, boolean pc, boolean deleted)
    {
        final String owner = PokecubeManager.getOwner(stack);
        this.addPokemob(owner, stack, pc, deleted);
    }

    public void addPokemob(String owner, ItemStack stack, boolean pc, boolean deleted)
    {
        final Integer uid = PokecubeManager.getUID(stack);
        if (uid == null || uid == -1) return;
        this.cache.put(uid, stack);
        pc = pc ? this.inPC.add(uid) : this.inPC.remove(uid);
        if (deleted) this.genesDeleted.add(uid);
        PlayerDataHandler.getInstance().save(owner, this.getIdentifier());
    }

    @Override
    public String dataFileName()
    {
        return "pokecube_pokemob_cache";
    }

    @Override
    public String getIdentifier()
    {
        return "pokecube-pokemobs";
    }

    @Override
    public void readFromNBT(CompoundNBT tag)
    {
        this.cache.clear();
        this.inPC.clear();
        this.genesDeleted.clear();
        if (tag.contains("data"))
        {
            final ListNBT list = (ListNBT) tag.get("data");
            for (int i = 0; i < list.size(); i++)
            {
                final CompoundNBT var = list.getCompound(i);
                Integer id = -1;
                final ItemStack stack = ItemStack.read(var);
                if (var.contains("uid")) id = var.getInt("uid");
                else id = PokecubeManager.getUID(stack);
                if (id != -1)
                {
                    this.cache.put(id, stack);
                    if (var.getBoolean("_in_pc_")) this.inPC.add(id);
                    if (var.getBoolean("_dead_")) this.genesDeleted.add(id);
                }
            }
        }
    }

    @Override
    public boolean shouldSync()
    {
        return false;
    }

    @Override
    public void writeToNBT(CompoundNBT tag)
    {
        final ListNBT list = new ListNBT();
        for (final Integer id : this.cache.keySet())
        {
            final CompoundNBT var = new CompoundNBT();
            var.putInt("uid", id);
            var.putBoolean("_in_pc_", this.inPC.contains(id));
            var.putBoolean("_dead_", this.genesDeleted.contains(id));
            final ItemStack stack = this.cache.get(id);
            stack.write(var);
            list.add(var);
        }
        tag.put("data", list);
    }
}
