package pokecube.core.handlers.playerdata;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.items.pokecubes.PokecubeManager;
import thut.core.common.handlers.PlayerDataHandler;
import thut.core.common.handlers.PlayerDataHandler.PlayerData;

/** This is a backup cache of the pokemobs owned by the player. */
public class PlayerPokemobCache extends PlayerData
{
    public Map<UUID, ItemStack> cache = Maps.newHashMap();

    public PlayerPokemobCache()
    {
        super();
    }

    public void addPokemob(IPokemob mob)
    {
        if (!mob.isPlayerOwned() || mob.getOwnerId() == null) return;
        ItemStack stack = PokecubeManager.pokemobToItem(mob);
        cache.put(mob.getEntity().getUniqueID(), stack);
        PlayerDataHandler.getInstance().save(mob.getOwnerId().toString(), getIdentifier());
    }

    @Override
    public void writeToNBT(NBTTagCompound tag)
    {
        NBTTagList list = new NBTTagList();
        for (UUID id : cache.keySet())
        {
            NBTTagCompound var = new NBTTagCompound();
            var.setUniqueId("I", id);
            ItemStack stack = cache.get(id);
            stack.writeToNBT(var);
            list.appendTag(var);
        }
        tag.setTag("data", list);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        cache.clear();
        if (tag.hasKey("data"))
        {
            NBTTagList list = (NBTTagList) tag.getTag("data");
            for (int i = 0; i < list.tagCount(); i++)
            {
                NBTTagCompound var = list.getCompoundTagAt(i);
                UUID id = var.getUniqueId("I");
                cache.put(id, new ItemStack(var));
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
