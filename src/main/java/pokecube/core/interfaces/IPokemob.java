/**
 *
 */
package pokecube.core.interfaces;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.core.entity.pokemobs.AnimalChest;
import pokecube.core.entity.pokemobs.EntityPokemobPart;
import pokecube.core.interfaces.pokemob.ICanEvolve;
import pokecube.core.interfaces.pokemob.IHasCommands;
import pokecube.core.interfaces.pokemob.IHasMobAIStates;
import pokecube.core.interfaces.pokemob.IHasMoves;
import pokecube.core.interfaces.pokemob.IHasOwner;
import pokecube.core.interfaces.pokemob.IHasStats;
import pokecube.core.utils.PokeType;
import thut.api.entity.IBreedingMob;
import thut.api.entity.IHungrymob;
import thut.api.pathing.IPathingMob;
import thut.api.world.mobs.data.DataSync;

/** @author Manchou */
public interface IPokemob extends IHasMobAIStates, IHasMoves, ICanEvolve, IHasOwner, IHasStats, IHungrymob,
        IBreedingMob, IHasCommands, IPathingMob
{
    @CapabilityInject(IPokemob.class)
    public static final Capability<IPokemob> POKEMOB_CAP = null;

    public static enum HappinessType
    {
        TIME(2, 2, 1), LEVEL(5, 3, 2), BERRY(3, 2, 1), EVBERRY(10, 5, 2), FAINT(-1, -1, -1), TRADE(0, 0, 0);

        public static void applyHappiness(IPokemob mob, HappinessType type)
        {
            int current = mob.getHappiness();
            if (type == BERRY && mob.getStatus() != STATUS_NON) { return; }
            if (type != TRADE)
            {
                if (current < 100)
                {
                    mob.addHappiness(type.low);
                }
                else if (current < 200)
                {
                    mob.addHappiness(type.mid);
                }
                else
                {
                    mob.addHappiness(type.high);
                }
            }
            else
            {
                mob.addHappiness(-(current - mob.getPokedexEntry().getHappiness()));
            }
        }

        public final int low;
        public final int mid;
        public final int high;

        private HappinessType(int low, int mid, int high)
        {
            this.low = low;
            this.mid = mid;
            this.high = high;
        }
    }

    public static enum Stats
    {
        HP, ATTACK, DEFENSE, SPATTACK, SPDEFENSE, VIT, ACCURACY, EVASION,
    }

    /*
     * Genders of pokemobs
     */
    byte MALE         = 1;

    byte FEMALE       = 2;

    byte NOSEXE       = -1;

    byte SEXLEGENDARY = -2;

    int  TYPE_CRIT    = 2;

    /** Whether this mob can use the item HMDive to be ridden underwater.
     * 
     * @return whether this mob can be ridden with HMDive */
    default boolean canUseDive()
    {
        return (getPokedexEntry().shouldDive && PokecubeMod.core.getConfig().diveEnabled && canUseSurf());
    }

    /** Whether this mob can use the item HMFly to be ridden in the air.
     * 
     * @return whether this mob can be ridden with HMFly */
    default boolean canUseFly()
    {
        return (getPokedexEntry().shouldFly || getPokedexEntry().flys()) && !isGrounded();
    }

    /** Whether this mob can use the item HMSurf to be ridden on water.
     * 
     * @return whether this mob can be ridden with HMSurf */
    default boolean canUseSurf()
    {
        return getPokedexEntry().shouldSurf || getPokedexEntry().shouldDive || getPokedexEntry().swims()
                || isType(PokeType.getType("water"));
    }

    @Override
    void eat(Object eaten);

    /** See IMultiplePassengerEntity.getPitch() TODO remove this infavour of the
     * IMultiplePassengerentity implementation
     * 
     * @return */
    float getDirectionPitch();

    /** The evolution tick will be set when the mob evolves and then is
     * decreased each tick. It is used to render a special effect.
     * 
     * @return the evolutionTicks */
    int getEvolutionTicks();

    /** 1 for about to explode, -1 for not exploding, this should probably be
     * changed to a boolean. */
    int getExplosionState();

    /** @return how happy is the pokemob, see {@link HappinessType} */
    int getHappiness();

    BlockPos getHome();

    float getHomeDistance();

    default double getMovementSpeed()
    {
        return getEntity().getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
    }

    boolean getOnGround();

    AnimalChest getPokemobInventory();

    /** Returns the name to display in any GUI. Can be the nickname or the
     * Pokemob translated name.
     *
     * @return the name to display */
    default ITextComponent getPokemonDisplayName()
    {
        if (this.getPokemonNickname().isEmpty())
            return new TextComponentTranslation(getPokedexEntry().getUnlocalizedName());
        return new TextComponentString(this.getPokemonNickname());
    }

    /** Note: This only returns a unique number for player owned pokemobs. All
     * other pokemobs will return -1
     * 
     * @return */
    int getPokemonUID();

    /** {@link #MALE} or {@link #FEMALE} or {@link #NOSEXE}
     *
     * @return the byte sexe */
    @Override
    byte getSexe();

    default SoundEvent getSound()
    {
        return getPokedexEntry().getSoundEvent();
    }

    /** Currently used for mareep colour, can be used for other things if needed
     * 
     * @return */
    int getSpecialInfo();

    /** Statuses: {@link IMoveConstants#STATUS_PSN} for example.
     *
     * @return the status */
    byte getStatus();

    /** The timer for SLP. When reach 0, the mob wakes up.
     * 
     * @return the actual value of the timer. */
    short getStatusTimer();

    /** Returns the texture path.
     * 
     * @return */
    @SideOnly(Side.CLIENT)
    ResourceLocation getTexture();

    boolean hasHomeArea();

    /** Removes the current status. */
    void healStatus();

    /** Returns modified texture to account for shininess, animation, etc.
     * 
     * @return */
    @SideOnly(Side.CLIENT)
    ResourceLocation modifyTexture(ResourceLocation texture);

    /** This method should only be used to update any Alleles objects that are
     * stored for the mob's genes. */
    default void onGenesChanged()
    {

    }

    /** Called to init the mob after it went out of its pokecube. */
    void popFromPokecube();

    /** The mob returns to its pokecube. */
    void returnToPokecube();

    void setDirectionPitch(float pitch);

    /** Sets the experience.
     *
     * @param exp
     * @param notifyLevelUp
     *            should be false in an initialize step and true in a true exp
     *            earning */
    default IPokemob setForSpawn(int exp)
    {
        return setForSpawn(exp, true);
    }

    IPokemob setForSpawn(int exp, boolean evolve);

    /** 1 for about to explode, -1 for reset.
     * 
     * @param i */
    void setExplosionState(int i);

    default void setHeldItem(ItemStack stack)
    {
        getEntity().setHeldItem(EnumHand.MAIN_HAND, stack);
    }

    default ItemStack getHeldItem()
    {
        return getEntity().getHeldItemMainhand();
    }

    /** Sets the default home location and roam distance. This is probably
     * better managed via the IGuardAICapability.
     * 
     * @param x
     * @param y
     * @param z
     * @param distance */
    void setHome(int x, int y, int z, int distance);

    /** {@link #MALE} or {@link #FEMALE} or {@link #NOSEXE}
     *
     * @param sexe
     *            the byte sexe */
    @Override
    void setSexe(byte sexe);

    void setShiny(boolean shiny);

    /** first 4 bits are used for colour, can be used for other things if needed
     * 
     * @return */
    void setSpecialInfo(int info);

    /** Called when the mob spawns naturally. Used to set held item for
     * example. */
    IPokemob specificSpawnInit();

    /** Returns the held item this pokemob should have when found wild.
     * 
     * @param mob
     * @return */
    default ItemStack wildHeldItem(EntityLiving mob)
    {
        return this.getPokedexEntry().getRandomHeldItem(mob);
    }

    /** The personality value for the pokemob, used to determine nature,
     * ability, etc.<br>
     * http://bulbapedia.bulbagarden.net/wiki/Personality_value
     * 
     * @return */
    int getRNGValue();

    /** sets the personality value for the pokemob, see getRNGValue() */
    void setRNGValue(int value);

    default void setSubParts(EntityPokemobPart[] subParts)
    {

    }

    /** @param index
     * @return the value of the flavour amount for this mob, this will be used
     *         for particle effects, and possibly for boosts based on how much
     *         the mob likes the flavour */
    int getFlavourAmount(int index);

    /** Sets the flavour amount for that index.
     * 
     * @param index
     * @param amount */
    void setFlavourAmount(int index, int amount);

    void readPokemobData(NBTTagCompound tag);

    NBTTagCompound writePokemobData();

    /** If this is larger than 0, the pokemob shouldn't be allowed to attack. */
    @Override
    int getAttackCooldown();

    /** Sets the value obtained by getAttackCooldown() */
    @Override
    void setAttackCooldown(int timer);

    default boolean moveToShoulder(EntityPlayer player)
    {
        return false;
    }

    DataSync dataSync();

    void setDataSync(DataSync sync);

    boolean isGrounded();
}
