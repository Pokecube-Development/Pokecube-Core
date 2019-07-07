package pokecube.core.items.pokecubes;

import java.util.ArrayList;
import java.util.Random;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.util.FakePlayer;
import pokecube.core.PokecubeCore;
import pokecube.core.events.pokemob.CaptureEvent;
import pokecube.core.network.packets.PacketPokecube;
import pokecube.core.utils.Tools;
import thut.api.maths.Vector3;

public class EntityPokecube extends EntityPokecubeBase
{

    public static class CollectEntry
    {
        static CollectEntry createFromNBT(CompoundNBT nbt)
        {
            final String player = nbt.getString("player");
            final long time = nbt.getLong("time");
            return new CollectEntry(player, time);
        }

        final String player;
        final long   time;

        public CollectEntry(String player, long time)
        {
            this.player = player;
            this.time = time;
        }

        void writeToNBT(CompoundNBT nbt)
        {
            nbt.putString("player", this.player);
            nbt.putLong("time", this.time);
        }
    }

    public static class LootEntry
    {
        static LootEntry createFromNBT(CompoundNBT nbt)
        {
            final ItemStack loot = ItemStack.read(nbt.getCompound("loot"));
            return new LootEntry(loot, nbt.getInt("rolls"));
        }

        final ItemStack loot;

        final int rolls;

        public LootEntry(ItemStack loot, int rolls)
        {
            this.loot = loot;
            this.rolls = rolls;
        }

        void writeToNBT(CompoundNBT nbt)
        {
            final CompoundNBT loot = new CompoundNBT();
            this.loot.write(loot);
            nbt.put("loot", loot);
            nbt.putInt("rolls", this.rolls);
        }

    }

    public static final EntityType<EntityPokecube> TYPE;

    static
    {
        TYPE = EntityType.Builder.create(EntityPokecube::new, EntityClassification.MISC)
                .setShouldReceiveVelocityUpdates(true).setTrackingRange(32).setUpdateInterval(1).disableSummoning()
                .immuneToFire().size(0.25f, 0.25f).build("pokecube");
    }

    public long                    reset      = 0;
    public long                    resetTime  = 0;
    public ArrayList<CollectEntry> players    = Lists.newArrayList();
    public ArrayList<LootEntry>    loot       = Lists.newArrayList();
    public ArrayList<ItemStack>    lootStacks = Lists.newArrayList();

    public EntityPokecube(EntityType<? extends EntityPokecubeBase> type, World worldIn)
    {
        super(type, worldIn);
    }

    public void addLoot(LootEntry entry)
    {
        this.loot.add(entry);
        for (int i = 0; i < entry.rolls; i++)
            this.lootStacks.add(entry.loot);
    }

    public boolean cannotCollect(Entity e)
    {
        if (e == null) return false;
        final String name = e.getCachedUniqueIdString();
        for (final CollectEntry s : this.players)
            if (s.player.equals(name))
            {
                if (this.resetTime > 0)
                {
                    final long diff = this.getEntityWorld().getGameTime() - s.time;
                    if (diff > this.resetTime)
                    {
                        this.players.remove(s);
                        return false;
                    }
                }
                return true;
            }
        return false;
    }

    public EntityPokecube copy()
    {
        final EntityPokecube copy = new EntityPokecube(EntityPokecube.TYPE, this.getEntityWorld());
        copy.copyLocationAndAnglesFrom(this);
        copy.copyDataFromOld(this);
        return copy;
    }

    @Override
    public boolean processInitialInteract(PlayerEntity player, Hand hand)
    {
        final ItemStack stack = player.getHeldItem(hand);
        if (!player.getEntityWorld().isRemote)
        {
            if (player.isSneaking() && PokecubeManager.isFilled(this.getItem()) && player.abilities.isCreativeMode)
                if (!stack.isEmpty())
                {
                this.isLoot = true;
                this.addLoot(new LootEntry(stack, 1));
                return true;
                }
            if (!this.isReleasing()) if (PokecubeManager.isFilled(this.getItem()))
            {
                if (player.isSneaking())
                {
                    Tools.giveItem(player, this.getItem());
                    this.remove();
                }
                else this.sendOut();
            }
            else
            {
                if (this.isLoot)
                {
                    if (this.cannotCollect(player)) return false;
                    this.players.add(new CollectEntry(player.getCachedUniqueIdString(), this.getEntityWorld()
                            .getGameTime()));
                    ItemStack loot = ItemStack.EMPTY;
                    if (!this.lootStacks.isEmpty())
                    {
                        loot = this.lootStacks.get(new Random().nextInt(this.lootStacks.size()));
                        if (!loot.isEmpty())
                        {
                            PacketPokecube.sendMessage(player, this.getEntityId(), this.getEntityWorld().getGameTime()
                                    + this.resetTime);
                            Tools.giveItem(player, loot.copy());
                        }
                    }
                    else if (this.lootTable != null)
                    {
                        final LootTable loottable = this.getEntityWorld().getServer().getLootTableManager()
                                .getLootTableFromLocation(this.lootTable);
                        final LootContext.Builder lootcontext$builder = new LootContext.Builder((ServerWorld) this
                                .getEntityWorld()).withParameter(LootParameters.THIS_ENTITY, this);
                        for (final ItemStack itemstack : loottable.generate(lootcontext$builder.build(loottable
                                .func_216122_a())))
                            if (!itemstack.isEmpty()) Tools.giveItem(player, itemstack.copy());
                        PacketPokecube.sendMessage(player, this.getEntityId(), this.getEntityWorld().getGameTime()
                                + this.resetTime);
                    }
                    return true;
                }
                Tools.giveItem(player, this.getItem());
                this.remove();
            }
        }
        return true;
    }

    @Override
    public void readAdditional(CompoundNBT nbt)
    {
        super.readAdditional(nbt);
        this.isLoot = nbt.getBoolean("isLoot");
        this.setReleasing(nbt.getBoolean("releasing"));
        if (nbt.contains("resetTime")) this.resetTime = nbt.getLong("resetTime");
        this.players.clear();
        this.loot.clear();
        this.lootStacks.clear();
        if (nbt.contains("players", 9))
        {
            final ListNBT ListNBT = nbt.getList("players", 10);
            for (int i = 0; i < ListNBT.size(); i++)
                this.players.add(CollectEntry.createFromNBT(ListNBT.getCompound(i)));
        }
        if (nbt.contains("loot", 9))
        {
            final ListNBT ListNBT = nbt.getList("loot", 10);
            for (int i = 0; i < ListNBT.size(); i++)
                this.addLoot(LootEntry.createFromNBT(ListNBT.getCompound(i)));
        }
        final String lootTable = nbt.getString("lootTable");
        if (!lootTable.isEmpty()) this.lootTable = new ResourceLocation(lootTable);
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void tick()
    {
        final boolean filled = PokecubeManager.isFilled(this.getItem());
        if (filled || this.isReleasing()) this.setTime(this.getTime() - 1);
        if (this.isReleasing())
        {
            if (this.getTime() < 0 || this.getReleased() == null || !this.getReleased().isAlive()) this.remove();
            return;
        }
        if (this.getTime() <= 0 && this.tilt >= 4) // Captured the pokemon
        {
            if (this.captureSucceed())
            {
                boolean gave = false;
                if (filled)
                {
                    final CaptureEvent.Post event = new CaptureEvent.Post(this);
                    gave = PokecubeCore.POKEMOB_BUS.post(event);
                }
                else if (this.shootingEntity instanceof ServerPlayerEntity
                        && !(this.shootingEntity instanceof FakePlayer))
                {
                    Tools.giveItem((PlayerEntity) this.shootingEntity, this.getItem());
                    gave = true;
                }
                if (!gave) this.entityDropItem(this.getItem(), 0.5f);
            }
            this.remove();
            return;
        }
        else if (this.getTime() < 0 && this.tilt >= 4)
        {
            if (this.shootingEntity != null)
            {
                final Vector3 here = Vector3.getNewVector().set(this);
                final Vector3 dir = Vector3.getNewVector().set(this.shootingEntity);
                final double dist = dir.distanceTo(here);
                dir.subtractFrom(here);
                dir.scalarMultBy(1 / dist);
                dir.setVelocities(this);
            }
        }
        else if (this.getTime() <= 0 && this.tilt >= 0) // Missed the pokemon
        {
            this.captureFailed();
            this.remove();
            return;
        }
        super.tick();
    }

    @Override
    public void writeAdditional(CompoundNBT nbt)
    {
        super.writeAdditional(nbt);
        nbt.putLong("resetTime", this.resetTime);
        nbt.putBoolean("isLoot", this.isLoot);
        if (this.isReleasing()) nbt.putBoolean("releasing", true);
        ListNBT ListNBT = new ListNBT();
        for (final CollectEntry entry : this.players)
        {
            final CompoundNBT CompoundNBT = new CompoundNBT();
            entry.writeToNBT(CompoundNBT);
            ListNBT.add(CompoundNBT);
        }
        if (!this.players.isEmpty()) nbt.put("players", ListNBT);
        ListNBT = new ListNBT();
        for (final LootEntry entry : this.loot)
        {
            final CompoundNBT CompoundNBT = new CompoundNBT();
            entry.writeToNBT(CompoundNBT);
            ListNBT.add(CompoundNBT);
        }
        if (!this.loot.isEmpty()) nbt.put("loot", ListNBT);
        if (this.lootTable != null) nbt.putString("lootTable", this.lootTable.toString());
        else nbt.putString("lootTable", "");
    }

}