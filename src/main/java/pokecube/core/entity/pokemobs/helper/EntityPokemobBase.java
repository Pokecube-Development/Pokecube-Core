/**
 * 
 */
package pokecube.core.entity.pokemobs.helper;

import java.util.List;
import java.util.logging.Level;

import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import pokecube.core.PokecubeCore;
import pokecube.core.database.Pokedex;
import pokecube.core.database.PokemobBodies;
import pokecube.core.entity.pokemobs.EntityPokemobPart;
import pokecube.core.entity.pokemobs.genetics.GeneticsManager;
import pokecube.core.events.SpawnEvent;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.TagNames;
import pokecube.core.utils.Tools;
import thut.api.maths.Matrix3;
import thut.api.maths.Vector3;

/** @author Manchou, Thutmose */
public abstract class EntityPokemobBase extends EntityAiPokemob implements IEntityMultiPart, TagNames
{
    public static boolean       multibox           = false;
    public static double        averagePokemobTick = 0;
    private int                 despawntimer       = 0;
    private EntityPokemobPart[] partsArray;

    public EntityPokemobBase(World world)
    {
        super(world);
        this.setSize(1, 1);
        this.width = 1;
        this.height = 1;
    }

    /** Returns true if other Entities should be prevented from moving through
     * this Entity. */
    @Override
    public boolean canBeCollidedWith()
    {
        return !this.isRiding();
    }

    /** Returns true if this entity should push and be pushed by other entities
     * when colliding. */
    @Override
    public boolean canBePushed()
    {
        return false;
    }

    private boolean cullCheck()
    {
        boolean player = true;
        despawntimer--;
        if (PokecubeMod.core.getConfig().despawn)
        {
            player = Tools.isAnyPlayerInRange(PokecubeMod.core.getConfig().cullDistance, this);
            if (despawntimer < 0 || player)
            {
                despawntimer = PokecubeMod.core.getConfig().despawnTimer;
            }
            else if (despawntimer == 0) { return true; }
        }
        player = Tools.isAnyPlayerInRange(PokecubeMod.core.getConfig().cullDistance, getEntityWorld().getHeight(),
                this);
        if (PokecubeMod.core.getConfig().cull && !player) return true;
        return false;
    }

    @Override
    protected boolean canDespawn()
    {
        // This is here for until kettle decides to not call this during
        // constructor.
        if (this.pokemobCap == null)
        {
            PokecubeMod.log(Level.FINE, "canDespawn() Called before construction is complete.");
            return false;
        }

        boolean canDespawn = pokemobCap.getHungerTime() > PokecubeMod.core.getConfig().pokemobLifeSpan;
        boolean checks = pokemobCap.getGeneralState(GeneralStates.TAMED) || pokemobCap.getOwnerId() != null
                || pokemobCap.getCombatState(CombatStates.ANGRY) || getAttackTarget() != null || this.hasCustomName()
                || isNoDespawnRequired();
        if (checks) return false;

        return canDespawn || cullCheck();
    }

    @Override
    protected boolean canTriggerWalking()
    {
        return true;
    }

    /** Makes the entity despawn if requirements are reached */
    @Override
    protected void despawnEntity()
    {
        if (!this.canDespawn() || this.getEntityWorld().isRemote) return;
        Result result = ForgeEventFactory.canEntityDespawn(this);
        if (result == Result.DENY) return;
        SpawnEvent.Despawn evt = new SpawnEvent.Despawn(here, getEntityWorld(), pokemobCap);
        MinecraftForge.EVENT_BUS.post(evt);
        if (evt.isCanceled()) return;
        this.setDead();
    }

    @Override
    public SoundEvent getAmbientSound()
    {
        return pokemobCap.getPokedexEntry().getSoundEvent();
    }

    /** Checks if the entity's current position is a valid location to spawn
     * this entity. */
    @Override
    public boolean getCanSpawnHere()
    {
        int i = MathHelper.floor(posX);
        int j = MathHelper.floor(getEntityBoundingBox().minY);
        int k = MathHelper.floor(posZ);
        here.set(i, j, k);
        float weight = pokemobCap.getBlockPathWeight(getEntityWorld(), here);
        return weight >= 0.0F && weight <= 100;
    }

    /** returns the bounding box for this entity */
    @Override
    public AxisAlignedBB getCollisionBoundingBox()
    {
        return null;// boundingBox;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return getAmbientSound();
    }

    @Override
    public ITextComponent getDisplayName()
    {
        ITextComponent textcomponentstring = pokemobCap.getPokemonDisplayName();
        textcomponentstring.getStyle().setHoverEvent(this.getHoverEvent());
        textcomponentstring.getStyle().setInsertion(this.getCachedUniqueIdString());
        return textcomponentstring;
    }

    @Override
    public float getEyeHeight()
    {
        return height * 0.8F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source)
    {
        return getAmbientSound();
    }

    @Override
    public int getMaxSpawnedInChunk()
    {
        return 8;
    }

    /** Hopefully this will fix the mod.pokemon kill messages */
    @Override
    public String getName()
    {
        return pokemobCap.getPokedexEntry().getName();
    }

    @Override
    protected float getSoundVolume()
    {
        // TODO possible config for this?
        return 0.15F;
    }

    @Override
    public int getVerticalFaceSpeed()
    {
        if (pokemobCap.getLogicState(LogicStates.SITTING)) { return 20; }
        return super.getVerticalFaceSpeed();
    }

    @Override
    public void init(int nb)
    {
        super.init(nb);
        this.getDisplayName();

        if (multibox)
        {
            this.noClip = true;
        }

        isImmuneToFire = pokemobCap.isType(PokeType.getType("fire"));
    }

    /** Checks if this entity is inside of an opaque block */
    @Override
    public boolean isEntityInsideOpaqueBlock()
    {
        return false;
    }

    /** Whether or not the current entity is in lava */
    @Override
    public boolean isInLava()
    {
        return pokemobCap.getLogicState(LogicStates.INLAVA);
    }

    List<AxisAlignedBB> aabbs = null;

    public List<AxisAlignedBB> getTileCollsionBoxes()
    {
        if (this.getEntityWorld().isRemote && this.isBeingRidden()
                && (this.getServer() == null || this.getServer().isDedicatedServer())
                && pokemobCap.getOwner() == PokecubeCore.proxy.getPlayer((String) null))
        {
            Vector3 vec = Vector3.getNewVector();
            Vector3 vec2 = Vector3.getNewVector();
            double x = pokemobCap.getPokedexEntry().width * pokemobCap.getSize();
            double z = pokemobCap.getPokedexEntry().length * pokemobCap.getSize();
            double y = pokemobCap.getPokedexEntry().height * pokemobCap.getSize();
            double v = vec.setToVelocity(this).mag();
            vec.set(this);
            vec2.set(x + v, y + v, z + v);
            Matrix3 mainBox = new Matrix3();
            Vector3 offset = Vector3.getNewVector();
            mainBox.boxMin().clear();
            mainBox.boxMax().x = x;
            mainBox.boxMax().z = y;
            mainBox.boxMax().y = z;
            offset.set(-mainBox.boxMax().x / 2, 0, -mainBox.boxMax().z / 2);
            double ar = mainBox.boxMax().x / mainBox.boxMax().z;
            if (ar > 2 || ar < 0.5) mainBox.set(2, mainBox.rows[2].set(0, 0, (-rotationYaw) * Math.PI / 180));
            mainBox.addOffsetTo(offset).addOffsetTo(vec);
            AxisAlignedBB box = mainBox.getBoundingBox();
            AxisAlignedBB box1 = box.grow(2 + x, 2 + y, 2 + z);
            box1 = box1.grow(motionX, motionY, motionZ);
            aabbs = mainBox.getCollidingBoxes(box1, getEntityWorld(), getEntityWorld());
            // Matrix3.mergeAABBs(aabbs, x/2, y/2, z/2);
            Matrix3.expandAABBs(aabbs, box);
            if (box.getAverageEdgeLength() < 3) Matrix3.mergeAABBs(aabbs, 0.01, 0.01, 0.01);
        }
        return aabbs;
    }

    public void setTileCollsionBoxes(List<AxisAlignedBB> list)
    {
        aabbs = list;
    }

    @Override
    public void onLivingUpdate()
    {
        super.onLivingUpdate();
        if (getParts() == null)
        {
            PokemobBodies.initBody(pokemobCap);
        }
        if (getParts() != null)
        {
            for (Entity e : getParts())
            {
                e.onUpdate();
            }
        }
    }

    @Override
    public void onStruckByLightning(EntityLightningBolt entitylightningbolt)
    {
        // do nothing
    }

    float yO = 0;
    float yC = 0;

    @Override
    public void onUpdate()
    {
        if (PokecubeMod.core.getConfig().pokemobsAreAllFrozen) return;
        if (!Pokedex.getInstance().isRegistered(pokemobCap.getPokedexEntry())) return;
        this.portalCounter = 0;
        long time = System.nanoTime();
        here.set(posX, posY, posZ);
        BlockPos pos = new BlockPos(posX, 1, posZ);
        boolean loaded = getEntityWorld().isAreaLoaded(pos, 8);
        if (loaded && PokecubeMod.core.getConfig().aiDisableDistance > 0
                && !(pokemobCap.getGeneralState(GeneralStates.STAYING)
                        || pokemobCap.getCombatState(CombatStates.GUARDING)
                        || pokemobCap.getCombatState(CombatStates.ANGRY) || getAttackTarget() != null))
        {
            loaded = Tools.isAnyPlayerInRange(PokecubeMod.core.getConfig().aiDisableDistance, 512, this);
        }
        // Only disable server side.
        if (!loaded && !getEntityWorld().isRemote)
        {
            despawnEntity();
            return;
        }
        super.onUpdate();
        double dt = (System.nanoTime() - time) / 10e3D;
        averagePokemobTick = ((averagePokemobTick * (ticksExisted - 1)) + dt) / ticksExisted;
        double toolong = 500;
        if (PokecubeMod.debug && dt > toolong && !getEntityWorld().isRemote)
        {
            here.set(here.getPos());
            String toLog = "%3$s took %2$s\u00B5s to tick, it is located at %1$s, the average has been %4$s\u00B5s";
            PokecubeMod.log(String.format(toLog, here, (int) dt,
                    pokemobCap.getPokemonDisplayName().getUnformattedComponentText(),
                    ((int) (averagePokemobTick * 100)) / 100d));
        }
    }

    @Override
    /** Sets the head's yaw rotation of the entity. */
    public void setRotationYawHead(float rotation)
    {
        this.rotationYawHead = rotation;
        /** This is to fix the jitteriness caused by this method not updating
         * prevRotationYawHead when it is called from the update packet. */
        float dYawHead = this.rotationYawHead - this.prevRotationYawHead;
        if (Math.abs(dYawHead) > 180)
        {
            this.prevRotationYawHead = this.rotationYawHead;
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        super.readEntityFromNBT(nbttagcompound);
        if (nbttagcompound.hasKey(POKEMOBTAG))
        {
            NBTTagCompound pokemobTag = nbttagcompound.getCompoundTag(POKEMOBTAG);
            pokemobCap.readPokemobData(pokemobTag);
            // readPokemobData(pokemobTag);
            return;
        }
        GeneticsManager.handleLoad(pokemobCap);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound)
    {
        GeneticsManager.handleEpigenetics(pokemobCap);
        return super.writeToNBT(nbttagcompound);
    }

    @Override
    public World getWorld()
    {
        return getEntityWorld();
    }

    @Override
    public boolean attackEntityFromPart(MultiPartEntityPart dragonPart, DamageSource source, float damage)
    {
        return false;
    }

    @Override
    public Entity[] getParts()
    {
        return partsArray;
    }

    @Override
    public void addEntityCrashInfo(CrashReportCategory category)
    {
        super.addEntityCrashInfo(category);
        category.addCrashSection("World:", getEntityWorld() == null ? "NULL" : getEntityWorld().toString());
        category.addCrashSection("Owner:",
                pokemobCap.getPokemonOwnerID() == null ? "NULL" : pokemobCap.getPokemonOwnerID().toString());
    }
}
