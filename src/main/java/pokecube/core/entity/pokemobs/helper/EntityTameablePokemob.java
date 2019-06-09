/**
 *
 */
package pokecube.core.entity.pokemobs.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.IJumpingMount;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import pokecube.core.PokecubeItems;
import pokecube.core.database.PokedexEntry.InteractionLogic.Interaction;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.capabilities.DefaultPokemob;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import thut.api.entity.IMobColourable;
import thut.api.maths.Vector3;

/** @author Manchou */
public abstract class EntityTameablePokemob extends AnimalEntity implements IShearable, IEntityOwnable, IMobColourable,
        IRangedAttackMob, IEntityAdditionalSpawnData, IJumpingMount
{

    public float                   length = 1;
    protected Vector3              here   = Vector3.getNewVector();
    protected Vector3              vec    = Vector3.getNewVector();
    protected Vector3              v1     = Vector3.getNewVector();
    protected Vector3              v2     = Vector3.getNewVector();
    protected Vector3              vBak   = Vector3.getNewVector();

    protected final DefaultPokemob pokemobCap;

    /** @param par1World */
    public EntityTameablePokemob(World world)
    {
        super(world);
        // Here we do this, incase someone has constructed this mob before
        // capabilities are initialized.
        DefaultPokemob cap = (DefaultPokemob) getCapability(CapabilityPokemob.POKEMOB_CAP, null);
        pokemobCap = cap == null ? new DefaultPokemob(this) : cap;
    }

    public boolean canBeHeld(ItemStack itemStack)
    {
        return PokecubeItems.isValidHeldItem(itemStack);
    }

    /** returns true if a sheeps wool has been sheared */
    public boolean getSheared()
    {
        return pokemobCap.getGeneralState(GeneralStates.SHEARED);
    }

    @Override
    public Team getTeam()
    {
        if (pokemobCap.getOwner() == this) { return this.getEntityWorld().getScoreboard()
                .getPlayersTeam(this.getCachedUniqueIdString()); }
        return super.getTeam();
    }

    public void init(int nb)
    {
    }

    @Override
    protected boolean isMovementBlocked()
    {
        return this.getHealth() <= 0.0F;
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockReader world, BlockPos pos)
    {
        /** Checks if the pokedex entry has shears listed, if so, then apply to
         * any mod shears as well. */
        ItemStack key = new ItemStack(Items.SHEARS);
        if (pokemobCap.getPokedexEntry().interact(key))
        {
            long last = getEntityData().getLong("lastSheared");
            Interaction action = pokemobCap.getPokedexEntry().interactionLogic.actions
                    .get(pokemobCap.getPokedexEntry().interactionLogic.getKey(key));
            if (last < getEntityWorld().getGameTime() - (action.cooldown + rand.nextInt(1 + action.variance))
                    && !getEntityWorld().isRemote)
            {
                pokemobCap.setGeneralState(GeneralStates.SHEARED, false);
            }

            return !getSheared();
        }
        return false;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockReader world, BlockPos pos, int fortune)
    {
        ItemStack key = new ItemStack(Items.SHEARS);
        if (pokemobCap.getPokedexEntry().interact(key))
        {
            ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
            pokemobCap.setGeneralState(GeneralStates.SHEARED, true);
            getEntityData().putLong("lastSheared", getEntityWorld().getGameTime());
            List<ItemStack> list = pokemobCap.getPokedexEntry().getInteractResult(key);
            Interaction action = pokemobCap.getPokedexEntry().interactionLogic.actions
                    .get(pokemobCap.getPokedexEntry().interactionLogic.getKey(key));
            int time = pokemobCap.getHungerTime();
            pokemobCap.setHungerTime(time + action.hunger);
            for (ItemStack stack : list)
            {
                ItemStack toAdd = stack.copy();
                if (pokemobCap.getPokedexEntry().dyeable)
                    toAdd.setItemDamage(EnumDyeColor.byDyeDamage(pokemobCap.getSpecialInfo() & 15).getMetadata());
                ret.add(toAdd);
            }
            this.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0F, 1.0F);
            return ret;
        }
        return null;
    }

    @Override
    public UUID getOwnerId()
    {
        return pokemobCap.getPokemonOwnerID();
    }

    @Override
    public Entity getOwner()
    {
        return pokemobCap.getOwner();
    }

    private InvWrapper inventory;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, Direction facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return (T) (inventory == null ? inventory = new InvWrapper(pokemobCap.getPokemobInventory()) : inventory);
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, Direction facing)
    {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }
}
