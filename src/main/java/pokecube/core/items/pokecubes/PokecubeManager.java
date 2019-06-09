package pokecube.core.items.pokecubes;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokecube.PokecubeBehavior;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.Stats;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.utils.TagNames;
import pokecube.core.utils.Tools;
import thut.lib.CompatWrapper;

public class PokecubeManager
{
    public static ItemStack getHeldItem(ItemStack stack)
    {
        if (!isFilled(stack)) return ItemStack.EMPTY;
        try
        {
            ListNBT equipmentTags = (ListNBT) TagNames.getPokecubePokemobTag(stack.getTag())
                    .getCompound(TagNames.INVENTORYTAG).getTag(TagNames.ITEMS);
            for (int i = 0; i < equipmentTags.size(); i++)
            {
                byte slot = equipmentTags.getCompound(i).getByte("Slot");
                if (slot != 1) continue;
                ItemStack held = new ItemStack(equipmentTags.getCompound(i));
                return held;
            }
        }
        catch (Exception e)
        {
        }
        return ItemStack.EMPTY;
    }

    public static String getOwner(ItemStack itemStack)
    {
        if (!CompatWrapper.isValid(itemStack) || !itemStack.hasTag()) return "";
        CompoundNBT poketag = TagNames.getPokecubePokemobTag(itemStack.getTag());
        // TODO remove this legacy support.
        if (poketag.hasNoTags())
        {
            if (itemStack.getTag().hasKey(TagNames.POKEMOB))
            {
                CompoundNBT nbt = itemStack.getTag().getCompound(TagNames.POKEMOB);
                if (nbt.hasKey("OwnerUUID")) { return nbt.getString("OwnerUUID"); }
            }
        }
        return poketag.getCompound(TagNames.OWNERSHIPTAG).getString(TagNames.OWNER);
    }

    public static int getPokedexNb(ItemStack itemStack)
    {
        if (!CompatWrapper.isValid(itemStack) || !itemStack.hasTag()) return 0;
        CompoundNBT poketag = TagNames.getPokecubePokemobTag(itemStack.getTag());
        if (poketag == null || poketag.hasNoTags()) return 0;
        int number = poketag.getCompound(TagNames.OWNERSHIPTAG).getInt(TagNames.POKEDEXNB);
        // TODO remove this legacy support as well
        if (poketag.hasNoTags() || number == 0) return itemStack.getTag().hasKey("PokedexNb")
                ? itemStack.getTag().getInt("PokedexNb") : 0;
        return number;
    }

    public static PokedexEntry getPokedexEntry(ItemStack itemStack)
    {
        if (!CompatWrapper.isValid(itemStack) || !itemStack.hasTag()) return null;
        CompoundNBT poketag = TagNames.getPokecubePokemobTag(itemStack.getTag());
        int number = poketag.getCompound(TagNames.OWNERSHIPTAG).getInt(TagNames.POKEDEXNB);
        if (!poketag.hasKey(TagNames.OWNERSHIPTAG)) return null;
        if (poketag.hasNoTags() || number == 0) return Database.getEntry(getPokedexNb(itemStack));
        String forme = poketag.getCompound(TagNames.VISUALSTAG).getString(TagNames.FORME);
        PokedexEntry entry = Database.getEntry(forme);
        return entry == null ? Database.getEntry(number) : entry;
    }

    public static CompoundNBT getSealTag(Entity pokemob)
    {
        IPokemob poke = CapabilityPokemob.getPokemobFor(pokemob);
        ItemStack cube;
        if (!CompatWrapper.isValid((cube = poke.getPokecube()))) return null;
        return CompatWrapper.getTag(cube, TagNames.POKESEAL, false);
    }

    public static CompoundNBT getSealTag(ItemStack stack)
    {
        if (isFilled(stack))
        {
            return stack.getTag().getCompound(TagNames.POKEMOB).getCompound(TagNames.VISUALSTAG)
                    .getCompound(TagNames.POKECUBE).getCompound("tag").getCompound(TagNames.POKESEAL);
        }
        else if (stack.hasTag()) { return stack.getTag().getCompound(TagNames.POKESEAL); }
        return null;
    }

    public static byte getStatus(ItemStack itemStack)
    {
        if (!itemStack.hasTag()) return 0;
        CompoundNBT poketag = TagNames.getPokecubePokemobTag(itemStack.getTag());
        return poketag.getCompound(TagNames.STATSTAG).getByte(TagNames.STATUS);
    }

    public static int getTilt(ItemStack itemStack)
    {
        return itemStack.hasTag() && itemStack.getTag().hasKey("tilt")
                ? itemStack.getTag().getInt("tilt") : 0;
    }

    public static boolean isFilled(ItemStack stack)
    {
        return (getPokedexNb(stack) != 0) || (stack.hasTag() && stack.getTag().hasKey("Othermob"));
    }

    public static boolean hasMob(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().hasKey(TagNames.OTHERMOB) || isFilled(stack);
    }

    public static IPokemob itemToPokemob(ItemStack itemStack, World world)
    {
        if (!itemStack.hasTag()) return null;
        PokedexEntry entry = getPokedexEntry(itemStack);
        if (entry != null)
        {
            IPokemob pokemob = CapabilityPokemob.getPokemobFor(PokecubeMod.core.createPokemob(entry, world));
            if (pokemob == null) { return null; }
            Entity poke = pokemob.getEntity();
            CompoundNBT pokeTag = itemStack.getTag().getCompound(TagNames.POKEMOB);
            poke.readFromNBT(pokeTag);
            ItemStack cubeStack = pokemob.getPokecube();
            if (!CompatWrapper.isValid(cubeStack))
            {
                cubeStack = itemStack.copy();
                cubeStack.getTag().remove(TagNames.POKEMOB);
                pokemob.setPokecube(cubeStack);
            }
            pokemob.getEntity().extinguish();
            return pokemob;
        }
        return null;
    }

    public static PokedexEntry getEntry(ItemStack cube)
    {
        PokedexEntry ret = null;
        if (isFilled(cube))
        {
            CompoundNBT poketag = cube.getTag().getCompound(TagNames.POKEMOB);
            if (poketag != null)
            {
                String forme = poketag.getString("forme");
                if (forme != null && !forme.isEmpty())
                {
                    ret = Database.getEntry(forme);
                }
            }
            if (ret == null)
            {
                int num = getPokedexNb(cube);
                ret = Database.getEntry(num);
            }
        }
        return ret;
    }

    public static ItemStack pokemobToItem(IPokemob pokemob)
    {
        ItemStack itemStack = pokemob.getPokecube();
        int damage = Tools.serialize(pokemob.getMaxHealth(), pokemob.getHealth());
        if (!CompatWrapper.isValid(itemStack))
        {
            itemStack = new ItemStack(PokecubeItems.getFilledCube(PokecubeBehavior.DEFAULTCUBE), 1, damage);
        }
        itemStack = itemStack.copy();
        itemStack.setItemDamage(damage);
        itemStack.setCount(1);
        if (!itemStack.hasTag()) itemStack.put(new CompoundNBT());
        String itemName = pokemob.getPokemonDisplayName().getFormattedText();
        Entity poke = pokemob.getEntity();
        CompoundNBT mobTag = new CompoundNBT();
        poke.writeToNBT(mobTag);
        itemStack.getTag().put(TagNames.POKEMOB, mobTag);
        itemStack.getTag().remove(TagNames.POKESEAL);
        setOwner(itemStack, pokemob.getPokemonOwnerID());
        setColor(itemStack);
        int status = pokemob.getStatus();
        setStatus(itemStack, pokemob.getStatus());
        if (status == IMoveConstants.STATUS_BRN) itemName += " (BRN)";
        else if (status == IMoveConstants.STATUS_FRZ) itemName += " (FRZ)";
        else if (status == IMoveConstants.STATUS_PAR) itemName += " (PAR)";
        else if (status == IMoveConstants.STATUS_PSN || status == IMoveConstants.STATUS_PSN2) itemName += " (PSN)";
        else if (status == IMoveConstants.STATUS_SLP) itemName += " (SLP)";
        itemStack.setStackDisplayName(itemName);
        return itemStack;
    }

    public static void setColor(ItemStack itemStack)
    {
        int color = 0xEEEEEE;

        ResourceLocation id = PokecubeItems.getCubeId(itemStack);

        if (itemStack.getItem() == PokecubeItems.pokemobEgg)
        {
            color = 0x78C848;
        }
        else if (id != null)
        {
            if (id.getResourcePath().equals("poke"))
            {
                color = 0xEE0000;
            }
            else if (id.getResourcePath().equals("great"))
            {
                color = 0x0B90CE;
            }
            else if (id.getResourcePath().equals("ultra"))
            {
                color = 0xDCA937;
            }
            else if (id.getResourcePath().equals("master"))
            {
                color = 0x332F6A;
            }
        }

        CompoundNBT var3 = itemStack.getTag();

        if (var3 == null)
        {
            var3 = new CompoundNBT();
            itemStack.put(var3);
        }

        CompoundNBT var4 = var3.getCompound("display");

        if (!var3.hasKey("display"))
        {
            var3.put("display", var4);
        }

        var4.setInteger("cubecolor", color);
    }

    public static void setOwner(ItemStack itemStack, UUID owner)
    {
        if (!itemStack.hasTag()) return;
        CompoundNBT poketag = TagNames.getPokecubePokemobTag(itemStack.getTag());
        if (owner == null) poketag.getCompound(TagNames.OWNERSHIPTAG).remove(TagNames.OWNER);
        else poketag.getCompound(TagNames.OWNERSHIPTAG).putString(TagNames.OWNER, owner.toString());
    }

    public static void setStatus(ItemStack itemStack, byte status)
    {
        if (!itemStack.hasTag()) return;
        CompoundNBT poketag = TagNames.getPokecubePokemobTag(itemStack.getTag());
        poketag.getCompound(TagNames.STATSTAG).setByte(TagNames.STATUS, status);
    }

    public static void setTilt(ItemStack itemStack, int number)
    {
        if (!itemStack.hasTag())
        {
            itemStack.put(new CompoundNBT());
        }
        itemStack.getTag().setInteger("tilt", number);
    }

    public static void heal(ItemStack stack)
    {
        if (isFilled(stack))
        {
            int serialization = Tools.getHealedPokemobSerialization();
            stack.setItemDamage(serialization);
            try
            {
                IPokemob pokemob = itemToPokemob(stack, PokecubeCore.getWorld());
                if (pokemob != null)
                {
                    pokemob.healStatus();
                    pokemob.healChanges();
                    pokemob.getEntity().hurtTime = 0;
                    pokemob.setHealth(pokemob.getStat(Stats.HP, false));
                    ItemStack healed = pokemobToItem(pokemob);
                    stack.put(healed.getTag());
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
            CompoundNBT poketag = TagNames.getPokecubePokemobTag(stack.getTag());
            poketag.getCompound(TagNames.AITAG).setInteger(TagNames.HUNGER,
                    -PokecubeMod.core.getConfig().pokemobLifeSpan / 4);
            PokecubeManager.setStatus(stack, IMoveConstants.STATUS_NON);
        }
    }

    public static UUID getUUID(ItemStack stack)
    {
        if (!isFilled(stack)) return null;
        CompoundNBT pokeTag = stack.getTag().getCompound(TagNames.POKEMOB);
        long min = pokeTag.getLong("UUIDLeast");
        long max = pokeTag.getLong("UUIDMost");
        return new UUID(max, min);
    }

    public static Integer getUID(ItemStack stack)
    {
        if (!isFilled(stack)) return null;
        CompoundNBT poketag = TagNames.getPokecubePokemobTag(stack.getTag());
        return poketag.getCompound(TagNames.MISCTAG).getInt(TagNames.UID);
    }
}