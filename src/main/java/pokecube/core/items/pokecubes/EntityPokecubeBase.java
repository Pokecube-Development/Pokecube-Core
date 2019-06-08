package pokecube.core.items.pokecubes;

import java.util.UUID;
import java.util.logging.Level;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import pokecube.core.ai.thread.logicRunnables.LogicMiscUpdate;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.abilities.AbilityManager;
import pokecube.core.events.SpawnEvent.SendOut;
import pokecube.core.handlers.Config;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.HappinessType;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.utils.Permissions;
import pokecube.core.utils.TagNames;
import pokecube.core.utils.Tools;
import thut.api.maths.Vector3;
import thut.core.common.commands.CommandTools;
import thut.lib.CompatWrapper;

public class EntityPokecubeBase extends MobEntity implements IEntityAdditionalSpawnData, IProjectile
{
    public static final String CUBETIMETAG = "lastCubeTime";

    public static boolean canCaptureBasedOnConfigs(IPokemob pokemob)
    {
        if (PokecubeMod.core
                .getConfig().captureDelayTillAttack) { return !pokemob.getCombatState(CombatStates.NOITEMUSE); }
        long lastAttempt = pokemob.getEntity().getEntityData().getLong(CUBETIMETAG);
        boolean capture = lastAttempt <= pokemob.getEntity().getEntityWorld().getGameTime();
        if (capture) pokemob.getEntity().getEntityData().remove(CUBETIMETAG);
        return capture;
    }

    public static void setNoCaptureBasedOnConfigs(IPokemob pokemob)
    {

        if (PokecubeMod.core.getConfig().captureDelayTillAttack) pokemob.setCombatState(CombatStates.NOITEMUSE, true);
        else pokemob.getEntity().getEntityData().putLong(CUBETIMETAG,
                pokemob.getEntity().getEntityWorld().getGameTime()
                        + PokecubeMod.core.getConfig().captureDelayTicks);
    }

    public static SoundEvent                      POKECUBESOUND;
    static final DataParameter<Integer>           ENTITYID       = EntityDataManager
            .<Integer> createKey(EntityPokecubeBase.class, DataSerializers.VARINT);
    private static final DataParameter<ItemStack> ITEM           = EntityDataManager
            .<ItemStack> createKey(EntityPokecubeBase.class, DataSerializers.ITEM_STACK);
    static final DataParameter<Boolean>           RELEASING      = EntityDataManager
            .<Boolean> createKey(EntityPokecubeBase.class, DataSerializers.BOOLEAN);

    public static boolean                         SEEKING        = true;

    /** Seems to be some sort of timer for animating an arrow. */
    public int                                    arrowShake;
    /** 1 if the player can pick up the arrow */
    public int                                    canBePickedUp;
    public boolean                                isLoot         = false;
    public ResourceLocation                       lootTable      = null;
    protected int                                 inData;
    protected boolean                             inGround;
    public UUID                                   shooter;
    public LivingEntity                       shootingEntity;

    public double                                 speed          = 2;
    public LivingEntity                       targetEntity;
    public Vector3                                targetLocation = Vector3.getNewVector();

    /** The owner of this arrow. */
    protected int                                 ticksInGround;
    protected Block                               tile;
    protected BlockPos                            tilePos;
    public int                                    tilt           = -1;
    public int                                    time           = 0;
    protected Vector3                             v0             = Vector3.getNewVector();
    protected Vector3                             v1             = Vector3.getNewVector();

    public EntityPokecubeBase(World worldIn)
    {
        super(worldIn);
        this.setSize(0.25F, 0.25F);
        this.isImmuneToFire = true;
        this.enablePersistence();
    }

    /** Called when the entity is attacked. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float damage)
    {
        if (source == DamageSource.OUT_OF_WORLD)
        {
            if (PokecubeManager.isFilled(getItem()))
            {
                IPokemob mob = CapabilityPokemob.getPokemobFor(this.sendOut());
                if (mob != null) mob.returnToPokecube();
            }
            this.setDead();
        }
        return false;
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        this.getDataManager().register(ITEM, ItemStack.EMPTY);
        getDataManager().register(RELEASING, false);
        getDataManager().register(ENTITYID, -1);
    }

    /** Returns the ItemStack corresponding to the Entity (Note: if no item
     * exists, will log an error but still return an ItemStack containing
     * Block.stone) */
    public ItemStack getItem()
    {
        ItemStack itemstack = this.getDataManager().get(ITEM);
        return itemstack == null ? new ItemStack(Blocks.STONE) : itemstack;
    }

    // For compatiblity.
    public ItemStack getItemEntity()
    {
        return getItem();
    }

    public Entity getReleased()
    {
        int id = getDataManager().get(ENTITYID);
        Entity ret = getEntityWorld().getEntityByID(id);
        return ret;
    }

    public boolean isReleasing()
    {
        return getDataManager().get(RELEASING);
    }

    @Override
    public void readSpawnData(ByteBuf buffer)
    {
        motionX = buffer.readDouble();
        motionY = buffer.readDouble();
        motionZ = buffer.readDouble();
    }

    /** Sets the ItemStack for this entity */
    public void setItem(ItemStack stack)
    {
        this.getDataManager().set(ITEM, stack);
        this.getDataManager().setDirty(ITEM);
    }

    // For compatiblity
    public void setItemEntityStack(ItemStack stack)
    {
        setItem(stack);
    }

    public void setReleased(Entity entity)
    {
        getDataManager().set(ENTITYID, entity.getEntityId());
    }

    public void setReleasing(boolean tag)
    {
        getDataManager().set(RELEASING, tag);
    }

    @Override
    protected boolean canDespawn()
    {
        return false;
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inacurracy)
    {
        float f2 = MathHelper.sqrt(x * x + y * y + z * z);
        x /= f2;
        y /= f2;
        z /= f2;
        x += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inacurracy;
        y += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inacurracy;
        z += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inacurracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
        float f3 = MathHelper.sqrt(x * x + z * z);
        this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
        this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(y, f3) * 180.0D / Math.PI);
        this.ticksInGround = 0;
    }

    /** Sets the velocity to the args. Args: x, y, z */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void setVelocity(double x, double y, double z)
    {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;

        if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F)
        {
            float f = MathHelper.sqrt(x * x + z * z);
            this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
            this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(y, f) * 180.0D / Math.PI);
            this.prevRotationPitch = this.rotationPitch;
            this.prevRotationYaw = this.rotationYaw;
            this.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
            this.ticksInGround = 0;
        }
    }

    @Override
    public void writeSpawnData(ByteBuf buffer)
    {
        buffer.writeDouble(motionX);
        buffer.writeDouble(motionY);
        buffer.writeDouble(motionZ);
    }

    protected void captureFailed()
    {
        IPokemob entity1 = PokecubeManager.itemToPokemob(getItem(), getEntityWorld());
        if (entity1 != null)
        {
            entity1.getEntity().setLocationAndAngles(posX, posY + 1.0D, posZ, rotationYaw, 0.0F);
            boolean ret = getEntityWorld().spawnEntity(entity1.getEntity());
            if (ret == false)
            {
                PokecubeMod.log(Level.SEVERE, String.format("The pokemob %1$s spawn from pokecube has failed. ",
                        entity1.getPokemonDisplayName().getFormattedText()));
            }
            setNoCaptureBasedOnConfigs(entity1);
            entity1.setCombatState(CombatStates.ANGRY, true);
            entity1.setLogicState(LogicStates.SITTING, false);
            entity1.setGeneralState(GeneralStates.TAMED, false);
            entity1.setPokemonOwner((UUID) null);
            if (shootingEntity instanceof PlayerEntity && !(shootingEntity instanceof FakePlayer))
            {
                ITextComponent mess = new TranslationTextComponent("pokecube.missed", entity1.getPokemonDisplayName());
                ((PlayerEntity) shootingEntity).sendMessage(mess);
                entity1.getEntity().setAttackTarget(shootingEntity);
            }
        }
        else
        {
            sendOut();
        }
    }

    protected boolean captureSucceed()
    {
        PokecubeManager.setTilt(getItem(), -1);
        IPokemob mob = PokecubeManager.itemToPokemob(getItem(), getEntityWorld());
        if (mob == null)
        {
            if ((getItem().hasTag() && getItem().getTag().hasKey(TagNames.MOBID)))
            {
                Entity caught = EntityList.createEntityByIDFromName(
                        new ResourceLocation(getItem().getTag().getString(TagNames.MOBID)), getEntityWorld());
                if (caught == null) return false;
                caught.readFromNBT(getItem().getTag().getCompound(TagNames.OTHERMOB));

                if (shootingEntity instanceof PlayerEntity && !(shootingEntity instanceof FakePlayer))
                {
                    if (caught instanceof EntityTameable)
                    {
                        ((EntityTameable) caught).setOwnerId(shootingEntity.getUniqueID());
                    }
                    else if (caught instanceof EntityHorse)
                    {// .1.12 use AbstractHorse instead
                        ((EntityHorse) caught).setOwnerUniqueId(shootingEntity.getUniqueID());
                    }
                    CompoundNBT tag = new CompoundNBT();
                    caught.writeToNBT(tag);
                    getItem().getTag().put(TagNames.OTHERMOB, tag);
                    getItem().setStackDisplayName(caught.getDisplayName().getFormattedText());
                    ITextComponent mess = new TranslationTextComponent("pokecube.caught", caught.getDisplayName());
                    ((PlayerEntity) shootingEntity).sendMessage(mess);
                    this.playSound(POKECUBESOUND, 1, 1);
                }
                return true;
            }
            new NullPointerException("Mob is null").printStackTrace();
            return false;
        }
        HappinessType.applyHappiness(mob, HappinessType.TRADE);
        if (shootingEntity != null && !mob.getGeneralState(GeneralStates.TAMED)) mob.setPokemonOwner((shootingEntity));
        if (mob.getCombatState(CombatStates.MEGAFORME) || mob.getPokedexEntry().isMega)
        {
            mob.setCombatState(CombatStates.MEGAFORME, false);
            IPokemob revert = mob.megaEvolve(mob.getPokedexEntry().getBaseForme());
            if (revert != null) mob = revert;
            if (mob.getEntity().getEntityData().hasKey(TagNames.ABILITY))
                mob.setAbility(AbilityManager.getAbility(mob.getEntity().getEntityData().getString(TagNames.ABILITY)));
        }
        ItemStack mobStack = PokecubeManager.pokemobToItem(mob);
        this.setItem(mobStack);
        if (shootingEntity instanceof PlayerEntity && !(shootingEntity instanceof FakePlayer))
        {
            ITextComponent mess = new TranslationTextComponent("pokecube.caught", mob.getPokemonDisplayName());
            ((PlayerEntity) shootingEntity).sendMessage(mess);
            this.setPosition(shootingEntity.posX, shootingEntity.posY, shootingEntity.posZ);
            this.playSound(POKECUBESOUND, 1, 1);
        }
        return true;
    }

    @Override
    public void writeEntityToNBT(CompoundNBT CompoundNBT)
    {
        super.writeEntityToNBT(CompoundNBT);
        CompoundNBT.setInteger("tilt", tilt);
        CompoundNBT.setInteger("time", time);
        if (shooter != null) CompoundNBT.putString("shooter", shooter.toString());
        if (this.getItem() != null)
        {
            CompoundNBT.put("Item", this.getItem().writeToNBT(new CompoundNBT()));
        }
        if (tilePos != null)
        {
            CompoundNBT.setInteger("xTile", this.tilePos.getX());
            CompoundNBT.setInteger("yTile", this.tilePos.getY());
            CompoundNBT.setInteger("zTile", this.tilePos.getZ());
        }
        CompoundNBT.setShort("life", (short) this.ticksInGround);
        CompoundNBT.setByte("inTile", (byte) Block.getIdFromBlock(this.tile));
        CompoundNBT.setByte("inData", (byte) this.inData);
        CompoundNBT.setByte("shake", (byte) this.arrowShake);
        CompoundNBT.setByte("inGround", (byte) (this.inGround ? 1 : 0));
    }

    @Override
    public void readEntityFromNBT(CompoundNBT CompoundNBT)
    {
        super.readEntityFromNBT(CompoundNBT);
        tilt = CompoundNBT.getInteger("tilt");
        time = CompoundNBT.getInteger("time");
        CompoundNBT CompoundNBT1 = CompoundNBT.getCompound("Item");
        this.setItem(new ItemStack(CompoundNBT1));

        ItemStack item = getItem();

        if (CompoundNBT.hasKey("shooter"))
        {
            shooter = UUID.fromString(CompoundNBT.getString("shooter"));
        }

        if (!CompatWrapper.isValid(item))
        {
            this.setDead();
        }
        this.tilePos = new BlockPos(CompoundNBT.getInteger("xTile"), CompoundNBT.getInteger("yTile"),
                CompoundNBT.getInteger("zTile"));
        this.ticksInGround = CompoundNBT.getShort("life");
        this.tile = Block.getBlockById(CompoundNBT.getByte("inTile") & 255);
        this.inData = CompoundNBT.getByte("inData") & 255;
        this.arrowShake = CompoundNBT.getByte("shake") & 255;
        this.inGround = CompoundNBT.getByte("inGround") == 1;
    }

    public LivingEntity sendOut()
    {
        if (getEntityWorld().isRemote || isReleasing()) { return null; }
        IPokemob pokemob = PokecubeManager.itemToPokemob(getItem(), getEntityWorld());
        Config config = PokecubeMod.core.getConfig();
        // Check permissions
        if (config.permsSendOut && shootingEntity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) shootingEntity;
            IPermissionHandler handler = PermissionAPI.getPermissionHandler();
            PlayerContext context = new PlayerContext(player);
            boolean denied = false;
            if (!handler.hasPermission(player.getGameProfile(), Permissions.SENDOUTPOKEMOB, context)) denied = true;
            if (denied)
            {
                Tools.giveItem((PlayerEntity) shootingEntity, getItem());
                this.setDead();
                return null;
            }
        }
        if (pokemob != null)
        {
            // Check permissions
            if (config.permsSendOutSpecific && shootingEntity instanceof PlayerEntity)
            {
                PokedexEntry entry = pokemob.getPokedexEntry();
                PlayerEntity player = (PlayerEntity) shootingEntity;
                IPermissionHandler handler = PermissionAPI.getPermissionHandler();
                PlayerContext context = new PlayerContext(player);
                boolean denied = false;
                if (!handler.hasPermission(player.getGameProfile(), Permissions.SENDOUTSPECIFIC.get(entry), context))
                    denied = true;
                if (denied)
                {
                    Tools.giveItem((PlayerEntity) shootingEntity, getItem());
                    this.setDead();
                    return null;
                }
            }

            Vector3 v = v0.set(this).addTo(-motionX, -motionY, -motionZ);
            Vector3 dv = v1.set(motionX, motionY, motionZ);
            v = Vector3.getNextSurfacePoint(getEntityWorld(), v, dv, Math.max(2, dv.mag()));
            if (v == null) v = v0.set(this);
            v.set(v.intX() + 0.5, v.y, v.intZ() + 0.5);
            IBlockState state = v.getBlockState(getEntityWorld());
            if (state.getMaterial().isSolid()) v.y = Math.ceil(v.y);
            MobEntity entity = pokemob.getEntity();
            entity.fallDistance = 0;
            v.moveEntity(entity);

            SendOut evt = new SendOut.Pre(pokemob.getPokedexEntry(), v, getEntityWorld(), pokemob);
            if (MinecraftForge.EVENT_BUS.post(evt))
            {
                if (shootingEntity != null && shootingEntity instanceof PlayerEntity)
                {
                    Tools.giveItem((PlayerEntity) shootingEntity, getItem());
                    this.setDead();
                }
                return null;
            }

            getEntityWorld().spawnEntity(entity);
            pokemob.popFromPokecube();
            pokemob.setGeneralState(GeneralStates.TAMED, true);
            pokemob.setGeneralState(GeneralStates.EXITINGCUBE, true);
            pokemob.setEvolutionTicks(50 + LogicMiscUpdate.EXITCUBEDURATION);
            Entity owner = pokemob.getPokemonOwner();
            if (owner instanceof PlayerEntity)
            {
                ITextComponent mess = CommandTools.makeTranslatedMessage("pokemob.action.sendout", "green",
                        pokemob.getPokemonDisplayName());
                pokemob.displayMessageToOwner(mess);
            }

            if (pokemob.getHealth() <= 0)
            {
                // notify the mob is dead
                this.getEntityWorld().setEntityState(entity, (byte) 3);
            }
            setReleased(entity);
            motionX = motionY = motionZ = 0;
            time = 10;
            setReleasing(true);
            this.setItem(pokemob.getPokecube());
            evt = new SendOut.Post(pokemob.getPokedexEntry(), v, getEntityWorld(), pokemob);
            MinecraftForge.EVENT_BUS.post(evt);
        }
        else
        {
            CompoundNBT tag;
            if (getItem().hasTag() && (tag = getItem().getTag()).hasKey(TagNames.MOBID))
            {
                CompoundNBT mobTag = tag.getCompound(TagNames.OTHERMOB);
                ResourceLocation id = new ResourceLocation(tag.getString(TagNames.MOBID));
                Entity newMob = EntityList.createEntityByIDFromName(id, getEntityWorld());
                if (newMob != null && newMob instanceof LivingEntity)
                {
                    newMob.readFromNBT(mobTag);
                    Vector3 v = v0.set(this).addTo(-motionX, -motionY, -motionZ);
                    Vector3 dv = v1.set(motionX, motionY, motionZ);
                    v = Vector3.getNextSurfacePoint(getEntityWorld(), v, dv, Math.max(2, dv.mag()));
                    if (v == null) v = v0.set(this);
                    v.set(v.intX() + 0.5, v.y, v.intZ() + 0.5);
                    IBlockState state = v.getBlockState(getEntityWorld());
                    if (state.getMaterial().isSolid()) v.y = Math.ceil(v.y);
                    v.moveEntity(newMob);
                    getEntityWorld().spawnEntity(newMob);
                    tag.remove(TagNames.MOBID);
                    tag.remove(TagNames.OTHERMOB);
                    tag.remove("display");
                    tag.remove("tilt");
                    if (tag.hasNoTags()) getItem().put(null);
                    entityDropItem(getItem(), 0.5f);
                    setReleased(newMob);
                    motionX = motionY = motionZ = 0;
                    time = 10;
                    setReleasing(true);
                    return (LivingEntity) newMob;
                }
            }
            System.err.println("Send out no pokemob?");
            Thread.dumpStack();
            this.entityDropItem(getItem(), 0.5f);
            this.setDead();
        }
        if (pokemob == null) return null;
        return pokemob.getEntity();
    }
}