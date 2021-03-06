package pokecube.core.ai.thread.aiRunnables.utility;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import pokecube.core.ai.thread.aiRunnables.AIBase;
import pokecube.core.interfaces.IMoveConstants.AIRoutine;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.world.terrain.PokecubeTerrainChecker;
import thut.api.TickHandler;
import thut.api.maths.Vector3;
import thut.lib.CompatWrapper;
import thut.lib.ItemStackTools;

/** This IAIRunnable gets the mob to look for and collect dropped items and
 * berries. It requires an AIStoreStuff to have located a suitable storage
 * before it will run. */
public class AIGatherStuff extends AIBase
{
    public static int                           COOLDOWN_SEARCH  = 200;
    public static int                           COOLDOWN_COLLECT = 5;
    public static int                           COOLDOWN_PATH    = 5;

    // Matcher used to determine if a block is a fruit or crop to be picked.
    private static final Predicate<IBlockState> berryMatcher     = new Predicate<IBlockState>()
                                                                 {
                                                                     @Override
                                                                     public boolean apply(IBlockState input)
                                                                     {
                                                                         return PokecubeTerrainChecker.isFruit(input);
                                                                     }
                                                                 };
    private static final Predicate<EntityItem>  deaditemmatcher  = new Predicate<EntityItem>()
                                                                 {
                                                                     @Override
                                                                     public boolean apply(EntityItem input)
                                                                     {
                                                                         return input.isDead || !input.addedToChunk
                                                                                 || !input.isAddedToWorld();
                                                                     }
                                                                 };

    /** This manages the pokemobs replanting anything that they gather.
     * 
     * @author Patrick */
    private static class ReplantTask implements IRunnable
    {
        final int       entityID;
        final ItemStack seeds;
        final BlockPos  pos;

        public ReplantTask(Entity entity, ItemStack seeds, BlockPos pos)
        {
            this.seeds = seeds.copy();
            this.pos = new BlockPos(pos);
            this.entityID = entity.getEntityId();
        }

        @Override
        public boolean run(World world)
        {
            if (seeds.isEmpty()) return true;
            // Check if is is plantable.
            if (seeds.getItem() instanceof IPlantable)
            {
                BlockPos down = pos.down();
                if (!world.isAirBlock(down))
                {
                    // Use the fakeplayer to plant it
                    EntityPlayer player = PokecubeMod.getFakePlayer(world);
                    player.setPosition(pos.getX(), pos.getY(), pos.getZ());
                    player.setHeldItem(EnumHand.MAIN_HAND, seeds);
                    seeds.getItem().onItemUse(player, world, down, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 1, 0.5f);
                }
                Entity mob = world.getEntityByID(entityID);
                IPokemob pokemob;
                // Attempt to plant it.
                if (!seeds.isEmpty() && ((pokemob = CapabilityPokemob.getPokemobFor(mob)) != null))
                {
                    // Add the "returned" stack to the inventory (ie remaining
                    // seeds)
                    if (!ItemStackTools.addItemStackToInventory(seeds, pokemob.getPokemobInventory(), 2))
                    {
                        mob.entityDropItem(seeds, 0);
                    }
                }
            }
            return true;
        }
    }

    final EntityLiving entity;
    final double       distance;
    IPokemob           pokemob;
    boolean            block           = false;
    List<EntityItem>   stuff           = Lists.newArrayList();
    Vector3            stuffLoc        = Vector3.getNewVector();
    boolean            hasRoom         = true;
    int                collectCooldown = 0;
    int                pathCooldown    = 0;
    final AIStoreStuff storage;
    Vector3            seeking         = Vector3.getNewVector();
    Vector3            v               = Vector3.getNewVector();
    Vector3            v1              = Vector3.getNewVector();

    public AIGatherStuff(IPokemob entity, double distance, AIStoreStuff storage)
    {
        this.entity = entity.getEntity();
        this.pokemob = entity;
        this.distance = distance;
        this.storage = storage;
        this.setMutex(1);
    }

    @Override
    public void doMainThreadTick(World world)
    {
        super.doMainThreadTick(world);
        // if (collectCooldown-- > 0) return;
        synchronized (stuffLoc)
        {
            // check stuff for being still around.
            if (!stuff.isEmpty())
            {
                int num = stuff.size();
                stuff.removeIf(deaditemmatcher);
                Collections.sort(stuff, new Comparator<EntityItem>()
                {
                    @Override
                    public int compare(EntityItem o1, EntityItem o2)
                    {
                        int dist1 = (int) o1.getDistanceSq(entity);
                        int dist2 = (int) o2.getDistanceSq(entity);
                        return dist1 - dist2;
                    }
                });

                if (stuff.isEmpty())
                {
                    reset();
                    return;
                }

                if (stuff.size() != num)
                {
                    stuffLoc.set(stuff.get(0));
                    return;
                }
            }
            else if (!stuffLoc.isEmpty())
            {
                if (!stuff.isEmpty())
                {
                    EntityItem itemStuff = stuff.get(0);
                    if (itemStuff.isDead || !itemStuff.addedToChunk || !itemStuff.isAddedToWorld())
                    {
                        stuff.remove(0);
                        return;
                    }
                    double close = entity.width * entity.width;
                    close = Math.max(close, 2);
                    if (itemStuff.getDistance(entity) < close)
                    {
                        ItemStackTools.addItemStackToInventory(itemStuff.getItem(), pokemob.getPokemobInventory(), 2);
                        itemStuff.setDead();
                        stuff.remove(0);
                        if (stuff.isEmpty()) reset();
                        else
                        {
                            stuffLoc.set(stuff.get(0));
                        }
                    }
                }
                else
                {
                    gatherStuff(true);
                }
            }
        }
    }

    private void findStuff()
    {
        // Only mobs that are standing with homes should look for stuff.
        if (pokemob.getHome() == null || pokemob.getGeneralState(GeneralStates.TAMED)
                && pokemob.getLogicState(LogicStates.SITTING)) { return; }
        block = false;
        v.set(pokemob.getHome()).add(0, entity.height, 0);

        int distance = pokemob.getGeneralState(GeneralStates.TAMED) ? PokecubeMod.core.getConfig().tameGatherDistance
                : PokecubeMod.core.getConfig().wildGatherDistance;

        List<Entity> list = getEntitiesWithinDistance(entity, distance, EntityItem.class);
        stuff.clear();
        double closest = 1000;

        // Check for items to possibly gather.
        for (Entity o : list)
        {
            EntityItem e = (EntityItem) o;
            double dist = e.getDistanceSqToCenter(pokemob.getHome());
            v.set(e);
            if (dist < closest && Vector3.isVisibleEntityFromEntity(entity, e))
            {
                stuff.add(e);
                closest = dist;
            }
        }
        // Found an item, return.
        if (!stuff.isEmpty())
        {
            Collections.sort(stuff, new Comparator<EntityItem>()
            {
                @Override
                public int compare(EntityItem o1, EntityItem o2)
                {
                    int dist1 = (int) o1.getDistanceSq(entity);
                    int dist2 = (int) o2.getDistanceSq(entity);
                    return dist1 - dist2;
                }
            });
            stuffLoc.set(stuff.get(0));
            return;
        }
        v.set(entity).addTo(0, entity.getEyeHeight(), 0);
        // check for berries to collect.
        if (!block && pokemob.eatsBerries())
        {
            Vector3 temp = v.findClosestVisibleObject(world, true, distance, berryMatcher);
            if (temp != null)
            {
                block = true;
                stuffLoc.set(temp);
            }
        }
        if (pokemob.isElectrotroph())
        {

        }
        // Nothing found, enter cooldown.
        if (stuffLoc.isEmpty())
        {
            collectCooldown = COOLDOWN_SEARCH;
        }
    }

    private void gatherStuff(boolean mainThread)
    {
        if (!mainThread)
        {
            if (pathCooldown-- > 0) return;
            // Set path to the stuff found.
            double speed = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
            if (stuff != null)
            {
                stuffLoc.set(stuff);
                Path path = entity.getNavigator().getPathToXYZ(stuffLoc.x, stuffLoc.y, stuffLoc.z);
                addEntityPath(entity, path, speed);
            }
            else
            {
                Path path = entity.getNavigator().getPathToXYZ(stuffLoc.x, stuffLoc.y, stuffLoc.z);
                addEntityPath(entity, path, speed);
            }
            pathCooldown = COOLDOWN_PATH;
        }
        else if (!stuffLoc.isEmpty())
        {
            double diff = 3;
            diff = Math.max(diff, entity.width);
            double dist = stuffLoc.distToEntity(entity);
            v.set(entity).subtractFrom(stuffLoc);
            double dot = v.normalize().dot(Vector3.secondAxis);
            // This means that the item is directly above the pokemob, assume it
            // can pick up to 3 blocks upwards.
            if (dot < -0.9 && entity.onGround)
            {
                diff = Math.max(3, diff);
            }
            if (dist < diff)
            {
                setCombatState(pokemob, CombatStates.HUNTING, false);
                IBlockState state = stuffLoc.getBlockState(entity.getEntityWorld());
                Block plant = stuffLoc.getBlock(entity.getEntityWorld());
                TickHandler.addBlockChange(stuffLoc, entity.dimension, Blocks.AIR);
                if (state.getMaterial() != Material.GRASS)
                {
                    NonNullList<ItemStack> list = NonNullList.create();
                    plant.getDrops(list, entity.getEntityWorld(), stuffLoc.getPos(), state, 0);
                    boolean replanted = false;
                    // See if anything dropped was a seed for the thing we
                    // picked.
                    for (ItemStack stack : list)
                    {
                        // If so, Replant it.
                        if (stack.getItem() instanceof IPlantable && !replanted)
                        {
                            toRun.addElement(new ReplantTask(entity, stack.copy(), stuffLoc.getPos()));
                            replanted = true;
                        }
                        else toRun.addElement(new InventoryChange(entity, 2, stack.copy(), true));
                    }
                    if (!replanted)
                    {
                        // Try to find a seed in our inventory for this plant.
                        for (int i = 2; i < pokemob.getPokemobInventory().getSizeInventory(); i++)
                        {
                            ItemStack stack = pokemob.getPokemobInventory().getStackInSlot(i);
                            if (!stack.isEmpty() && stack.getItem() instanceof IPlantable)
                            {
                                IPlantable plantable = (IPlantable) stack.getItem();
                                IBlockState plantState = plantable.getPlant(world, stuffLoc.getPos().up());
                                if (plantState.getBlock() == state.getBlock())
                                {
                                    toRun.addElement(new ReplantTask(entity, stack.copy(), stuffLoc.getPos()));
                                    replanted = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                stuffLoc.clear();
                addEntityPath(entity, null, 0);
            }
        }
    }

    @Override
    public void reset()
    {
        stuffLoc.clear();
        stuff.clear();
    }

    @Override
    public void run()
    {
        if (stuffLoc.isEmpty() && collectCooldown-- < 0)
        {
            findStuff();
        }
        if (!stuffLoc.isEmpty())
        {
            gatherStuff(false);
        }
    }

    @Override
    public boolean shouldRun()
    {
        // Check if gather is enabled first.
        if (!pokemob.isRoutineEnabled(AIRoutine.GATHER)) return false;
        world = TickHandler.getInstance().getWorldCache(entity.dimension);
        boolean wildCheck = !PokecubeMod.core.getConfig().wildGather && !pokemob.getGeneralState(GeneralStates.TAMED);
        // Check if this should be doing something else instead, if so return
        // false.
        if (world == null || tameCheck() || entity.getAttackTarget() != null || wildCheck) return false;
        int rate = pokemob.getGeneralState(GeneralStates.TAMED) ? PokecubeMod.core.getConfig().tameGatherDelay
                : PokecubeMod.core.getConfig().wildGatherDelay;
        Random rand = new Random(pokemob.getRNGValue());
        // Check if it has a location, if so, apply a delay and return false if
        // not correct tick for this pokemob.
        if (pokemob.getHome() == null || entity.ticksExisted % rate != rand.nextInt(rate)) return false;

        // Apply cooldown.
        if (collectCooldown < -2000)
        {
            collectCooldown = COOLDOWN_SEARCH;
        }
        // If too far, clear location.
        if (stuffLoc.distToEntity(entity) > 32) stuffLoc.clear();

        // check if pokemob has room in inventory for stuff, if so, return true.
        IInventory inventory = pokemob.getPokemobInventory();
        for (int i = 3; i < inventory.getSizeInventory(); i++)
        {
            hasRoom = !CompatWrapper.isValid(inventory.getStackInSlot(i));
            if (hasRoom) return true;
        }
        // Otherwise return false.
        return false;
    }

    /** Only tame pokemobs set to "stay" should run this AI.
     * 
     * @return */
    private boolean tameCheck()
    {
        return pokemob.getGeneralState(GeneralStates.TAMED)
                && (!pokemob.getGeneralState(GeneralStates.STAYING) || !PokecubeMod.core.getConfig().tameGather);
    }
}
