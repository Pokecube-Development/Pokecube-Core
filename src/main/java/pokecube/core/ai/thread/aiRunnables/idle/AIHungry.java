package pokecube.core.ai.thread.aiRunnables.idle;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import pokecube.core.ai.thread.aiRunnables.AIBase;
import pokecube.core.blocks.berries.BerryGenManager;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.interfaces.IBerryFruitBlock;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.items.berries.ItemBerry;
import pokecube.core.utils.ChunkCoordinate;
import pokecube.core.utils.TimePeriod;
import pokecube.core.world.terrain.PokecubeTerrainChecker;
import thut.api.TickHandler;
import thut.api.maths.Vector3;
import thut.lib.CompatWrapper;
import thut.lib.ItemStackTools;

/** This IAIRunnable is responsible for finding food for the mobs. It also is
 * what adds berries to their inventories based on which biome they are
 * currently in. */
public class AIHungry extends AIBase
{
    private static class GenBerries implements IRunnable
    {
        final IPokemob pokemob;

        public GenBerries(IPokemob mob)
        {
            pokemob = mob;
        }

        @Override
        public boolean run(World world)
        {
            ItemStack stack = BerryGenManager.getRandomBerryForBiome(world, pokemob.getEntity().getPosition());
            if (!stack.isEmpty())
            {
                ItemStackTools.addItemStackToInventory(stack, pokemob.getPokemobInventory(), 2);
                pokemob.eat(new EntityItem(world, 0, 0, 0, stack));
            }
            return true;
        }

    }

    public static int  TICKRATE = 20;

    final EntityLiving entity;
    // final World world;
    final EntityItem   berry;
    final double       distance;
    IPokemob           pokemob;
    Vector3            foodLoc  = null;
    boolean            block    = false;
    boolean            sleepy   = false;
    int                hungerTime;
    double             moveSpeed;
    Vector3            v        = Vector3.getNewVector();
    Vector3            v1       = Vector3.getNewVector();
    Random             rand;

    public AIHungry(final IPokemob pokemob, final EntityItem berry_, double distance)
    {
        this.entity = pokemob.getEntity();
        berry = berry_;
        this.distance = distance;
        this.pokemob = pokemob;
        this.moveSpeed = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() * 0.75;
    }

    /** Checks its own inventory for berries to eat, returns true if it finds
     * some.
     * 
     * @return found any berries to eat in inventory. */
    protected boolean checkInventory()
    {
        for (int i = 2; i < 7; i++)
        {
            ItemStack stack = pokemob.getPokemobInventory().getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBerry)
            {
                setCombatState(pokemob, CombatStates.HUNTING, false);
                pokemob.eat(berry);
                stack.shrink(1);
                if (stack.isEmpty()) pokemob.getPokemobInventory().setInventorySlotContents(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    /** Swimming things look for fish hooks to try to go eat.
     * 
     * @return found a hook. */
    protected boolean checkBait()
    {
        if (pokemob.getPokedexEntry().swims())
        {
            AxisAlignedBB bb = v.set(entity).addTo(0, entity.getEyeHeight(), 0).getAABB()
                    .grow(PokecubeMod.core.getConfig().fishHookBaitRange);
            List<EntityFishHook> hooks = entity.getEntityWorld().getEntitiesWithinAABB(EntityFishHook.class, bb);
            pokemob.setCombatState(CombatStates.HUNTING, true);
            if (!hooks.isEmpty())
            {
                Collections.shuffle(hooks);
                EntityFishHook hook = hooks.get(0);
                if (v.isVisible(world, v1.set(hook)))
                {
                    Path path = entity.getNavigator().getPathToEntityLiving(hook);
                    addEntityPath(entity, path, moveSpeed);
                    addTargetInfo(entity, hook);
                    if (entity.getDistanceSq(hook) < 2)
                    {
                        hook.caughtEntity = entity;
                        pokemob.eat(hook);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /** Check for places and times to sleep, this sets path to sleeping place
     * and returns false if it finds somewhere, but doesn't set sleep.
     * 
     * @return went to sleep. */
    protected boolean checkSleep()
    {
        sleepy = true;
        for (TimePeriod p : pokemob.getPokedexEntry().activeTimes())
        {// TODO find some way to determine actual length of day for things like
         // AR support.
            if (p != null && p.contains(entity.getEntityWorld().getWorldTime(), 24000)) ;
            {
                sleepy = false;
                pokemob.setLogicState(LogicStates.SLEEPING, false);
                break;
            }
        }
        ChunkCoordinate c = new ChunkCoordinate(v, entity.dimension);
        boolean ownedSleepCheck = pokemob.getGeneralState(GeneralStates.TAMED)
                && !(pokemob.getGeneralState(GeneralStates.STAYING));
        if (sleepy && hungerTime < 0 && !ownedSleepCheck)
        {
            if (!isGoodSleepingSpot(c))
            {
                Path path = this.entity.getNavigator().getPathToPos(pokemob.getHome());
                if (path != null && path.getCurrentPathLength() > 32) path = null;
                addEntityPath(entity, path, moveSpeed);
            }
            else if (entity.getNavigator().noPath())
            {
                pokemob.setLogicState(LogicStates.SLEEPING, true);
                pokemob.setCombatState(CombatStates.HUNTING, false);
                return true;
            }
            else if (!entity.getNavigator().noPath())
            {
                pokemob.setLogicState(LogicStates.SLEEPING, false);
            }
        }
        else if (!pokemob.getLogicState(LogicStates.TIRED))
        {
            pokemob.setLogicState(LogicStates.SLEEPING, false);
        }
        if (ownedSleepCheck)
        {
            pokemob.setLogicState(LogicStates.SLEEPING, false);
        }
        return false;
    }

    /** Checks for light to eat.
     * 
     * @return found light */
    protected boolean checkPhotoeat()
    {
        if (entity.getEntityWorld().provider.isDaytime() && v.canSeeSky(world))
        {
            pokemob.setHungerTime(pokemob.getHungerTime() - PokecubeMod.core.getConfig().pokemobLifeSpan / 4);
            setCombatState(pokemob, CombatStates.HUNTING, false);
            return true;
        }
        return false;
    }

    /** Checks for redstone blocks nearby.
     * 
     * @return found redstone block. */
    protected boolean checkElectricEat()
    {
        int num = v.blockCount(world, Blocks.REDSTONE_BLOCK, 8);
        if (num >= 1)
        {
            pokemob.setHungerTime(pokemob.getHungerTime() - PokecubeMod.core.getConfig().pokemobLifeSpan / 4);
            setCombatState(pokemob, CombatStates.HUNTING, false);
            return true;
        }
        return false;
    }

    /** Checks for rocks nearby to eat
     * 
     * @return found and ate rocks. */
    protected boolean checkRockEat()
    {
        IBlockState state = v.offset(EnumFacing.DOWN).getBlockState(world);
        Block b = state.getBlock();
        // Look for nearby rocks.
        if (!PokecubeTerrainChecker.isRock(state))
        {
            Vector3 temp = v.findClosestVisibleObject(world, true, (int) distance,
                    PokecubeMod.core.getConfig().getRocks());
            if (temp != null)
            {
                block = true;
                foodLoc = temp.copy();
            }
            if (foodLoc != null) return true;
        }
        else
        {
            // Check configs, and if so, actually eat the rocks
            if (PokecubeMod.core.getConfig().pokemobsEatRocks)
            {
                v.set(entity).offsetBy(EnumFacing.DOWN);
                if (SpawnHandler.checkNoSpawnerInArea(entity.getEntityWorld(), v.intX(), v.intY(), v.intZ()))
                {
                    if (b == Blocks.COBBLESTONE)
                    {
                        TickHandler.addBlockChange(v, entity.dimension, Blocks.GRAVEL);
                    }
                    else if (b == Blocks.GRAVEL && PokecubeMod.core.getConfig().pokemobsEatGravel)
                    {
                        TickHandler.addBlockChange(v, entity.dimension, Blocks.AIR);
                    }
                    else if (state.getMaterial() == Material.ROCK)
                    {
                        TickHandler.addBlockChange(v, entity.dimension, Blocks.COBBLESTONE);
                    }
                }
            }
            // Apply the eating of the item.
            berry.setItem(new ItemStack(b));
            setCombatState(pokemob, CombatStates.HUNTING, false);
            pokemob.eat(berry);
            foodLoc = null;
            return true;
        }
        return false;
    }

    /** Checks for a variety of nearby food supplies, returns true if it finds
     * food.
     * 
     * @return found food */
    protected boolean checkHunt()
    {
        if (pokemob.isPhototroph())
        {
            if (checkPhotoeat()) return true;
        }
        if (pokemob.isLithotroph())
        {
            if (checkRockEat()) return true;
        }
        if (pokemob.isElectrotroph())
        {
            if (checkElectricEat()) return true;
        }
        return false;
    }

    @Override
    public void doMainThreadTick(World world)
    {
        super.doMainThreadTick(world);

        // Configs can set this to -1 to disable ticking.
        if (TICKRATE < 0) return;

        int deathTime = PokecubeMod.core.getConfig().pokemobLifeSpan;
        double hurtTime = deathTime / 2d;
        hungerTime = pokemob.getHungerTime();
        int hungerTicks = TICKRATE;
        float ratio = (float) ((hungerTime - hurtTime) / deathTime);

        // Check own inventory for berries to eat, and then if the mob is
        // allowed to, collect berries if none to eat.
        if (hungerTime > 0 && !checkInventory())
        {
            // Pokemobs set to stay can collect berries, or wild ones,
            boolean tameCheck = !pokemob.isPlayerOwned() || pokemob.getGeneralState(GeneralStates.STAYING);
            if (entity.getEntityData().hasKey("lastInteract"))
            {
                long time = entity.getEntityData().getLong("lastInteract");
                long diff = entity.getEntityWorld().getTotalWorldTime() - time;
                if (diff < PokecubeMod.core.getConfig().pokemobLifeSpan) tameCheck = false;
            }
            // If they are allowed to, find the berries.
            if (tameCheck)
            {
                // Only run this if we are getting close to hurt damage, mostly
                // to allow trying other food sources first.
                if (hungerTime > deathTime / 4) toRun.add(new GenBerries(pokemob));
            }
            // Otherwise take damage.
            else if (entity.ticksExisted % hungerTicks == 0 && ratio > 0)
            {
                boolean dead = entity.getMaxHealth() * ratio > entity.getHealth();
                float damage = entity.getMaxHealth() * ratio;
                if (damage >= 1 && ratio >= 0.0625)
                {
                    entity.attackEntityFrom(DamageSource.STARVE, damage);
                    if (!dead) pokemob.displayMessageToOwner(
                            new TextComponentTranslation("pokemob.hungry.hurt", pokemob.getPokemonDisplayName()));
                    else pokemob.displayMessageToOwner(
                            new TextComponentTranslation("pokemob.hungry.dead", pokemob.getPokemonDisplayName()));
                }
            }
        }

        // Everything after here only applies about once per second.
        if (entity.ticksExisted % hungerTicks != 0) return;

        v.set(entity);

        // Reset hunting status if we are not actually hungry
        if (!pokemob.neverHungry() && pokemob.getHungerCooldown() < 0)
        {
            if (hungerTime > 0 && !pokemob.getCombatState(CombatStates.HUNTING))
            {
                pokemob.setCombatState(CombatStates.HUNTING, true);
            }
        }

        // Check if we should go after bait. The Math.random() > 0.99 is to
        // allow non-hungry fish to also try to get bait.
        if (shouldRun() || Math.random() > 0.99) checkBait();

        // Check if we should go to sleep instead.
        checkSleep();

        Random rand = new Random(pokemob.getRNGValue());
        int cur = (entity.ticksExisted / hungerTicks);
        int tick = rand.nextInt(10);

        /*
         * Check the various hunger types if it is hunting.
         */
        if (pokemob.getCombatState(CombatStates.HUNTING)) checkHunt();

        // cap hunger.
        hungerTime = pokemob.getHungerTime();
        int hunger = Math.max(hungerTime, -deathTime / 4);
        if (hunger != hungerTime) pokemob.setHungerTime(hunger);

        // Regenerate health if out of battle.
        if (entity.getAttackTarget() == null && entity.getHealth() > 0 && !entity.isDead
                && !entity.getEntityWorld().isRemote && pokemob.getHungerCooldown() < 0 && pokemob.getHungerTime() < 0
                && cur % 10 == tick)
        {
            float dh = Math.max(1, entity.getMaxHealth() * 0.05f);
            float toHeal = entity.getHealth() + dh;
            entity.setHealth(Math.min(toHeal, entity.getMaxHealth()));
        }
    }

    /** Eats a berry
     * 
     * @param b
     *            the berry
     * @param distance
     *            to the berry */
    protected void eatBerry(IBlockState b, double distance)
    {
        ItemStack fruit = ((IBerryFruitBlock) b.getBlock()).getBerryStack(world, foodLoc.getPos());

        if (!CompatWrapper.isValid(fruit))
        {
            foodLoc = null;
            pokemob.noEat(null);
            return;
        }

        if (distance < 3)
        {
            setCombatState(pokemob, CombatStates.HUNTING, false);
            berry.setItem(fruit);
            pokemob.eat(berry);
            toRun.addElement(new InventoryChange(entity, 2, fruit, true));
            TickHandler.addBlockChange(foodLoc, entity.dimension, Blocks.AIR);
            foodLoc = null;
        }
        else if (entity.ticksExisted % 20 == rand.nextInt(20))
        {
            boolean shouldChangePath = true;
            if (!this.entity.getNavigator().noPath())
            {
                Vector3 p = v.set(this.entity.getNavigator().getPath().getFinalPathPoint());
                Vector3 v = v1.set(foodLoc);
                if (p.distToSq(v) <= 16) shouldChangePath = false;
            }
            Path path = null;
            if (shouldChangePath
                    && (path = entity.getNavigator().getPathToXYZ(foodLoc.x, foodLoc.y, foodLoc.z)) == null)
            {
                addEntityPath(entity, path, moveSpeed);
                setCombatState(pokemob, CombatStates.HUNTING, false);
                berry.setItem(fruit);
                pokemob.noEat(berry);
                foodLoc.clear();
            }
            else addEntityPath(entity, path, moveSpeed);
        }
    }

    /** Eats a plant.
     * 
     * @param b
     *            the plant
     * @param location
     *            where the plant is
     * @param dist
     *            distance to the plant */
    protected void eatPlant(IBlockState b, Vector3 location, double dist)
    {
        double diff = 1;
        diff = Math.max(diff, entity.width);
        if (dist < diff)
        {
            setCombatState(pokemob, CombatStates.HUNTING, false);
            berry.setItem(new ItemStack(b.getBlock()));
            pokemob.eat(berry);
            if (PokecubeMod.core.getConfig().pokemobsEatPlants)
            {
                TickHandler.addBlockChange(foodLoc, entity.dimension,
                        location.getBlockState(world).getMaterial() == Material.GRASS ? Blocks.DIRT : Blocks.AIR);
                if (location.getBlockState(world).getMaterial() != Material.GRASS)
                {
                    NonNullList<ItemStack> list = NonNullList.create();
                    b.getBlock().getDrops(list, world, foodLoc.getPos(), foodLoc.getBlockState(world), 0);
                    for (ItemStack stack : list)
                        toRun.addElement(new InventoryChange(entity, 2, stack, true));
                }
            }
            foodLoc = null;
            addEntityPath(entity, null, moveSpeed);
        }
        else
        {
            boolean shouldChangePath = true;
            block = false;
            v.set(entity).add(0, entity.height, 0);
            if (!this.entity.getNavigator().noPath())
            {
                Vector3 pathEnd, destination;
                pathEnd = v.set(this.entity.getNavigator().getPath().getFinalPathPoint());
                destination = v1.set(foodLoc);
                if (pathEnd.distToSq(destination) < 1) shouldChangePath = false;
            }
            Path path = null;
            if (shouldChangePath)
            {
                path = entity.getNavigator().getPathToXYZ(foodLoc.x, foodLoc.y, foodLoc.z);
                if (path == null)
                {
                    setCombatState(pokemob, CombatStates.HUNTING, false);
                    berry.setItem(new ItemStack(b.getBlock()));
                    pokemob.noEat(berry);
                    foodLoc = null;
                    addEntityPath(entity, null, moveSpeed);
                }
                else addEntityPath(entity, path, moveSpeed);
            }
        }
    }

    /** Eats a rock.
     * 
     * @param b
     *            the rock
     * @param location
     *            where the rock is
     * @param dist
     *            distance to the rock */
    protected void eatRocks(IBlockState b, Vector3 location, double dist)
    {
        double diff = 2;
        diff = Math.max(diff, entity.width);
        if (dist < diff)
        {
            if (PokecubeMod.core.getConfig().pokemobsEatRocks)
            {
                if (b.getBlock() == Blocks.COBBLESTONE)
                {
                    TickHandler.addBlockChange(foodLoc, entity.dimension, Blocks.GRAVEL);
                }
                else if (b.getBlock() == Blocks.GRAVEL && PokecubeMod.core.getConfig().pokemobsEatGravel)
                {
                    TickHandler.addBlockChange(foodLoc, entity.dimension, Blocks.AIR);
                }
                else if (location.getBlockState(world).getMaterial() == Material.ROCK)
                {
                    TickHandler.addBlockChange(foodLoc, entity.dimension, Blocks.COBBLESTONE);
                }
            }
            setCombatState(pokemob, CombatStates.HUNTING, false);
            berry.setItem(new ItemStack(b.getBlock()));
            pokemob.eat(berry);
            foodLoc = null;
            addEntityPath(entity, null, moveSpeed);
        }
        else if (entity.ticksExisted % 20 == rand.nextInt(20))
        {
            boolean shouldChangePath = true;
            block = false;
            v.set(entity).add(0, entity.height, 0);

            Vector3 temp = v.findClosestVisibleObject(world, true, (int) distance,
                    PokecubeMod.core.getConfig().getRocks());
            if (temp != null)
            {
                block = true;
                foodLoc = temp.copy();
            }

            Vector3 p, m;
            if (!this.entity.getNavigator().noPath())
            {
                p = v.set(this.entity.getNavigator().getPath().getFinalPathPoint());
                m = v1.set(foodLoc);
                if (p.distToSq(m) >= 16) shouldChangePath = false;
            }
            boolean pathed = false;
            Path path = null;
            if (shouldChangePath)
            {
                path = entity.getNavigator().getPathToXYZ(foodLoc.x, foodLoc.y, foodLoc.z);
                pathed = path != null;
                addEntityPath(entity, path, moveSpeed);
            }
            if (shouldChangePath && !pathed)
            {
                setCombatState(pokemob, CombatStates.HUNTING, false);
                berry.setItem(new ItemStack(b.getBlock()));
                pokemob.noEat(berry);
                foodLoc = null;
                if (pokemob.hasHomeArea())
                {
                    path = entity.getNavigator().getPathToXYZ(pokemob.getHome().getX(), pokemob.getHome().getY(),
                            pokemob.getHome().getZ());
                    addEntityPath(entity, path, moveSpeed);
                }
                else
                {
                    addEntityPath(entity, null, moveSpeed);
                }
            }
        }
    }

    protected void findFood()
    {
        v.set(entity).addTo(0, entity.getEyeHeight(), 0);

        /*
         * Tame pokemon can eat berries out of trapped chests, so check for one
         * of those here.
         */
        if (pokemob.getGeneralState(GeneralStates.TAMED))
        {
            IInventory container = null;
            v.set(entity).add(0, entity.height, 0);

            Vector3 temp = v.findClosestVisibleObject(world, true, 10, Blocks.TRAPPED_CHEST);

            if (temp != null && temp.getBlock(world) == Blocks.TRAPPED_CHEST)
            {
                container = (IInventory) temp.getTileEntity(world);

                for (int i1 = 0; i1 < container.getSizeInventory(); i1++)
                {
                    ItemStack stack = container.getStackInSlot(i1);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemBerry)
                    {
                        stack.shrink(1);
                        if (stack.isEmpty())
                        {
                            container.setInventorySlotContents(i1, ItemStack.EMPTY);
                        }
                        setCombatState(pokemob, CombatStates.HUNTING, false);
                        Path path = entity.getNavigator().getPathToXYZ(temp.x, temp.y, temp.z);
                        addEntityPath(entity, path, moveSpeed);
                        pokemob.eat(berry);
                        return;
                    }
                }
            }
        }

        // No food already obtained, reset mating rules, hungry things don't
        // mate
        pokemob.resetLoveStatus();

        if (pokemob.getGeneralState(GeneralStates.TAMED) && pokemob.getLogicState(LogicStates.SITTING))
        {
            pokemob.setHungerCooldown(100);
            setCombatState(pokemob, CombatStates.HUNTING, false);
            return;
        }
        block = false;
        v.set(entity).add(0, entity.height, 0);

        if (foodLoc == null)
        {
            if (!block && pokemob.isHerbivore())
            {
                Vector3 temp = v.findClosestVisibleObject(world, true, (int) distance,
                        PokecubeMod.core.getConfig().getPlantTypes());
                if (temp != null)
                {
                    block = true;
                    foodLoc = temp.copy();
                }
            }
            if (!block && pokemob.filterFeeder())
            {
                Vector3 temp = v.findClosestVisibleObject(world, true, (int) distance, Blocks.WATER);
                if (entity.isInWater())
                {
                    pokemob.eat(berry);
                    setCombatState(pokemob, CombatStates.HUNTING, false);
                    return;
                }
                if (temp != null)
                {
                    block = true;
                    foodLoc = temp.copy();
                }
            }
            if (!block && pokemob.eatsBerries())
            {
                if (pokemob.getGeneralState(GeneralStates.TAMED))
                {
                    Vector3 temp = v.findClosestVisibleObject(world, true, (int) distance, IBerryFruitBlock.class);
                    if (temp != null)
                    {
                        block = true;
                        foodLoc = temp.copy();
                    }
                }
            }
        }

        if (foodLoc == null)
        {
            pokemob.setHungerCooldown(10);
        }
    }

    // 0 is sunrise, 6000 noon, 12000 dusk, 18000 midnight, 23999
    public boolean isGoodSleepingSpot(ChunkCoordinate c)
    {
        if (pokemob.getHome() == null || pokemob.getHome().equals(BlockPos.ORIGIN))
        {
            v1.set(entity);
            pokemob.setHome(v1.intX(), v1.intY(), v1.intZ(), 16);
        }
        if (pokemob.hasHomeArea() && entity.getPosition().distanceSq(pokemob.getHome()) > 9) return false;
        // TODO search for possible better place to sleep
        return true;
    }

    @Override
    public void reset()
    {
        foodLoc = null;
    }

    @Override
    public void run()
    {
        if (foodLoc == null)
        {
            findFood();
        }
        else
        {
            rand = new Random(pokemob.getRNGValue());
            // Go find and eat the block
            double d = foodLoc.addTo(0.5, 0.5, 0.5).distToEntity(entity);
            foodLoc.addTo(-0.5, -0.5, -0.5);
            IBlockState b = foodLoc.getBlockState(world);
            if (b == null)
            {
                foodLoc = null;
                return;
            }
            if (b.getBlock() instanceof IBerryFruitBlock)
            {
                eatBerry(b, d);
            }
            else if (PokecubeTerrainChecker.isPlant(b))
            {
                eatPlant(b, foodLoc, d);
            }
            else if ((PokecubeTerrainChecker.isRock(b)) && pokemob.isLithotroph())
            {
                eatRocks(b, foodLoc, d);
            }
        }
    }

    @Override
    public boolean shouldRun()
    {
        world = entity.getEntityWorld();
        if (world == null) return false;

        int hungerTicks = TICKRATE;
        // This can be set in configs to disable.
        if (hungerTicks < 0) return false;

        // Only run this every few ticks.
        if (entity.ticksExisted % hungerTicks != 0) return false;

        // Do not run if the mob is in battle.
        if (pokemob.getCombatState(CombatStates.ANGRY)) return false;

        // Apply cooldowns and increment hunger.
        pokemob.setHungerCooldown(pokemob.getHungerCooldown() - hungerTicks);
        pokemob.setHungerTime(pokemob.getHungerTime() + hungerTicks);

        // Do not run this if on cooldown
        if (pokemob.getHungerCooldown() > 0) return false;

        // Do not run this if not really hungry
        if (pokemob.getHungerTime() < 0) return false;

        boolean hunting = pokemob.getCombatState(CombatStates.HUNTING);
        if (pokemob.getLogicState(LogicStates.SLEEPING) || !hunting || pokemob.neverHungry())
        {
            if (pokemob.neverHungry()) pokemob.setHungerTime(0);
            if (hunting) setCombatState(pokemob, CombatStates.HUNTING, false);
            return false;
        }
        // Ensure food location is not too far away.
        if (foodLoc != null && foodLoc.distToEntity(entity) > 32) foodLoc = null;
        // We have a location, so can run.
        if (foodLoc != null) return true;
        // We are hunting for food, so can run.
        return hunting;
    }
}
