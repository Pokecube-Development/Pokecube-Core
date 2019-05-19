package pokecube.core.interfaces.capabilities.impl;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import pokecube.core.ai.properties.IGuardAICapability;
import pokecube.core.ai.thread.logicRunnables.LogicCollision;
import pokecube.core.ai.thread.logicRunnables.LogicFloatFlySwim;
import pokecube.core.ai.thread.logicRunnables.LogicInLiquid;
import pokecube.core.ai.thread.logicRunnables.LogicInMaterials;
import pokecube.core.ai.thread.logicRunnables.LogicMiscUpdate;
import pokecube.core.ai.thread.logicRunnables.LogicMountedControl;
import pokecube.core.ai.thread.logicRunnables.LogicMovesUpdates;
import pokecube.core.ai.utils.PokemobMoveHelper;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.AnimalChest;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.pokemob.moves.PokemobMoveStats;
import pokecube.core.interfaces.pokemob.stats.StatModifiers;
import pokecube.core.moves.animations.EntityMoveUse;
import thut.api.entity.IBreedingMob;
import thut.api.entity.ai.AIThreadManager.AIStuff;
import thut.api.entity.genetics.Alleles;
import thut.api.entity.genetics.IMobGenetics;
import thut.api.maths.Matrix3;
import thut.api.maths.Vector3;
import thut.api.world.mobs.data.DataSync;
import thut.core.common.world.mobs.data.SyncHandler;
import thut.core.common.world.mobs.data.types.Data_Byte;
import thut.core.common.world.mobs.data.types.Data_Float;
import thut.core.common.world.mobs.data.types.Data_Int;
import thut.core.common.world.mobs.data.types.Data_ItemStack;
import thut.core.common.world.mobs.data.types.Data_String;

public abstract class PokemobBase implements IPokemob
{
    public static class DataParameters
    {

        public final int[] FLAVOURS = new int[5];

        public int         HELDITEMDW;
        public int         EVOLTICKDW;
        public int         HAPPYDW;
        public int         ATTACKCOOLDOWN;
        public int         NICKNAMEDW;
        public int         ZMOVECD;
        public int         DIRECTIONPITCHDW;
        public int         HEADINGDW;
        public int         TRANSFORMEDTODW;
        public int         GENERALSTATESDW;
        public int         LOGICSTATESDW;
        public int         COMBATSTATESDW;
        public int         ATTACKTARGETIDDW;
        public int         HUNGERDW;
        public int         STATUSDW;
        public int         STATUSTIMERDW;
        public int         MOVEINDEXDW;
        public int         SPECIALINFO;
        public int         TYPE1DW;
        public int         TYPE2DW;
        public final int[] DISABLE  = new int[4];
        public int         ACTIVEMOVEID;

        public void register(IPokemob pokemob)
        {
            DataSync sync = pokemob.dataSync();
            // Held Item timer
            HELDITEMDW = sync.register(new Data_ItemStack(), ItemStack.EMPTY);

            // Humger timer
            HUNGERDW = sync.register(new Data_Int(), new Integer(0));
            // // for sheared status
            NICKNAMEDW = sync.register(new Data_String(), "");// nickname
            HAPPYDW = sync.register(new Data_Int(), new Integer(0));// Happiness
            TYPE1DW = sync.register(new Data_String(), "");// overriden type1
            TYPE2DW = sync.register(new Data_String(), "");// overriden type2

            // From EntityAiPokemob
            DIRECTIONPITCHDW = sync.register(new Data_Float(), Float.valueOf(0));
            HEADINGDW = sync.register(new Data_Float(), Float.valueOf(0));
            ATTACKTARGETIDDW = sync.register(new Data_Int(), Integer.valueOf(-1));
            GENERALSTATESDW = sync.register(new Data_Int(), Integer.valueOf(0));
            LOGICSTATESDW = sync.register(new Data_Int(), Integer.valueOf(0));
            COMBATSTATESDW = sync.register(new Data_Int(), Integer.valueOf(0));

            // from EntityEvolvablePokemob
            EVOLTICKDW = sync.register(new Data_Int(), new Integer(0));// evolution
                                                                       // tick

            // From EntityMovesPokemb
            STATUSDW = sync.register(new Data_Byte(), Byte.valueOf((byte) -1));
            MOVEINDEXDW = sync.register(new Data_Byte(), Byte.valueOf((byte) -1));
            STATUSTIMERDW = sync.register(new Data_Int(), Integer.valueOf(0));
            ATTACKCOOLDOWN = sync.register(new Data_Int(), Integer.valueOf(0));

            SPECIALINFO = sync.register(new Data_Int(), Integer.valueOf(-1));
            TRANSFORMEDTODW = sync.register(new Data_Int(), Integer.valueOf(-1));

            ZMOVECD = sync.register(new Data_Int(), Integer.valueOf(-1));

            // Flavours for various berries eaten.
            for (int i = 0; i < 5; i++)
            {
                FLAVOURS[i] = sync.register(new Data_Int(), Integer.valueOf(0));
            }

            // Flavours for various berries eaten.
            for (int i = 0; i < 4; i++)
            {
                DISABLE[i] = sync.register(new Data_Int(), Integer.valueOf(0));
            }

            // EntityID of the active move use entity.
            ACTIVEMOVEID = sync.register(new Data_Int(), Integer.valueOf(-1));
        }
    }

    /** Inventory of the pokemob. */
    protected AnimalChest          pokeChest;
    /** Prevents duplication on returning to pokecubes */
    protected boolean              returning        = false;
    /** Is this owned by a player? */
    protected boolean              players          = false;
    /** Cached Team for this Pokemob */
    protected String               team             = "";
    protected double               moveSpeed;
    /** Cached Pokedex Entry for this pokemob. */
    protected PokedexEntry         entry;

    /** The happiness value of the pokemob */
    protected int                  bonusHappiness   = 0;
    /** Tracks whether this was a shadow mob at some point. */
    protected boolean              wasShadow        = false;
    /** Number used as seed for various RNG things. */
    protected int                  personalityValue = 0;
    /** Modifiers on stats. */
    protected StatModifiers        modifiers        = new StatModifiers();
    /** Egg we are trying to protect. */
    protected Entity               egg              = null;
    /** Mob to breed with */
    protected Entity               lover;
    /** Timer for determining whether wants to breed, will only do so if this is
     * greater than 0 */
    protected int                  loveTimer;
    /** List of nearby male mobs to breed with */
    protected Vector<IBreedingMob> males            = new Vector<>();
    /** Simpler UID for some client sync things. */
    protected int                  uid              = -1;
    /** The pokecube this mob is "in" */
    protected ItemStack            pokecube         = ItemStack.EMPTY;
    /** Tracker for things related to moves. */
    protected PokemobMoveStats     moveInfo         = new PokemobMoveStats();
    /** The current move being used, this is used to track whether the mob can
     * launch a new move, only allows sending a new move if this returns true
     * for isDone() */
    protected EntityMoveUse        activeMove;
    /** Used for size when pathing */
    protected Vector3              sizes            = Vector3.getNewVector();
    /** Cooldown for hunger AI */
    protected int                  hungerCooldown   = 0;

    // Here we have all of the genes currently used.
    Alleles                        genesSize;
    Alleles                        genesIVs;
    Alleles                        genesEVs;
    Alleles                        genesMoves;
    Alleles                        genesNature;
    Alleles                        genesAbility;
    Alleles                        genesColour;
    Alleles                        genesShiny;
    Alleles                        genesSpecies;

    /** Stack which will be used for evolution */
    protected ItemStack            stack            = ItemStack.EMPTY;
    /** Location to try to attack. */
    protected Vector3              target;
    /** Manages mounted control */
    public LogicMountedControl     controller;
    /** Holder for all the custom AI stuff */
    protected AIStuff              aiStuff;

    /** Custom navigator */
    public PathNavigate            navi;
    /** Custom move helper. */
    public PokemobMoveHelper       mover;

    /** Used for various cases where things at mobs location need checking */
    protected Vector3              here             = Vector3.getNewVector();

    /** The Entity this IPokemob is attached to. */
    protected EntityLiving         entity;
    /** RNG used, should be entity.getRNG() */
    protected Random               rand             = new Random();
    /** Data manager used for syncing data */
    public DataSync                dataSync;
    /** Holds the data parameters used for syncing our stuff. */
    protected final DataParameters params           = new DataParameters();

    /** Our owner. */
    protected UUID                 ownerID;
    /** Our original owner. */
    protected UUID                 OTID;

    /** Used for maintaining/storing homes and routes. */
    protected IGuardAICapability   guardCap;

    // Things here are used for collision stuff.
    List<AxisAlignedBB>            aabbs            = null;
    public Matrix3                 mainBox          = new Matrix3();
    public Vector3                 offset           = Vector3.getNewVector();

    /** How long the mob is */
    protected float                length;

    /** The IMobGenetics used to store our genes. */
    public IMobGenetics            genes;

    /** Used to cache current texture for quicker lookups, array to include any
     * animated textures */
    protected ResourceLocation[]   textures;

    @Override
    public void setEntity(EntityLiving entityIn)
    {
        rand = entityIn.getRNG();
        entity = entityIn;
        this.aiStuff = new AIStuff(entity);
        this.getAI().aiLogic.clear();
        // Controller is done separately for ease of locating it for
        // controls.
        this.getAI().addAILogic(controller = new LogicMountedControl(this));

        // Add in the various logic AIs that are needed on both client and
        // server, so it is done here instead of in initAI.
        this.getAI().addAILogic(new LogicInLiquid(this));
        this.getAI().addAILogic(new LogicCollision(this));
        this.getAI().addAILogic(new LogicMovesUpdates(this));
        this.getAI().addAILogic(new LogicInMaterials(this));
        this.getAI().addAILogic(new LogicFloatFlySwim(this));
        this.getAI().addAILogic(new LogicMiscUpdate(this));
    }

    @Override
    public EntityLiving getEntity()
    {
        return entity;
    }

    @Override
    public DataSync dataSync()
    {
        if (this.dataSync == null)
        {
            this.dataSync = SyncHandler.getData(getEntity());
        }
        return dataSync;
    }

    @Override
    public void setDataSync(DataSync sync)
    {
        this.dataSync = sync;
        params.register(this);
    }

    protected void setMaxHealth(float maxHealth)
    {
        IAttributeInstance health = getEntity().getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        List<AttributeModifier> mods = Lists.newArrayList(health.getModifiers());
        for (AttributeModifier modifier : mods)
        {
            health.removeModifier(modifier);
        }
        health.setBaseValue(maxHealth);
    }

    /** Handles health update.
     * 
     * @param level */
    public void updateHealth()
    {
        float old = this.getMaxHealth();
        float maxHealth = getStat(Stats.HP, false);
        float health = this.getHealth();

        if (maxHealth > old)
        {
            float damage = old - health;
            health = maxHealth - damage;

            if (health > maxHealth)
            {
                health = maxHealth;
            }
        }
        setMaxHealth(maxHealth);
        this.setHealth(health);
    }
}
