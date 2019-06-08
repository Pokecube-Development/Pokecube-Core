package pokecube.core.ai.thread.aiRunnables.idle;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.Path;
import net.minecraft.world.IBlockAccess;
import pokecube.core.ai.thread.aiRunnables.AIBase;
import pokecube.core.ai.utils.pathing.PokemobNavigator;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IMoveConstants.AIRoutine;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import thut.api.TickHandler;
import thut.api.maths.Vector3;

/** This IAIRunnable makes the mobs randomly wander around if they have nothing
 * better to do. */
public class AIIdle extends AIBase
{
    public static int          IDLETIMER   = 1;
    private AttributeModifier  idlePathing = null;

    final private MobEntity entity;
    final IPokemob             mob;
    final PokedexEntry         entry;
    private double             x;
    private double             y;
    private double             z;
    private double             speed;
    private double             maxLength   = 16;

    Vector3                    v           = Vector3.getNewVector();
    Vector3                    v1          = Vector3.getNewVector();

    public AIIdle(IPokemob pokemob)
    {
        this.entity = pokemob.getEntity();
        this.setMutex(2);
        mob = pokemob;
        entry = mob.getPokedexEntry();
        this.speed = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        idlePathing = new AttributeModifier(UUID.fromString("4454b0d8-75ef-4689-8fce-daab61a7e1b1"),
                "pokecube:idle_path", 0.5, 2);
    }

    /** Floating things try to stay their preferedHeight from the ground. */
    private void doFloatingIdle()
    {
        v.set(x, y, z);
        Vector3 temp = Vector3.getNextSurfacePoint(world, v, Vector3.secondAxisNeg, v.y);
        if (temp == null || !mob.isRoutineEnabled(AIRoutine.AIRBORNE)) return;
        y = temp.y + entry.preferedHeight;
    }

    /** Flying things will path to air, so long as not airborne, somethimes they
     * will decide to path downwards, the height they path to will be centered
     * around players, to prevent them from all flying way up, or way down */
    private void doFlyingIdle()
    {
        boolean grounded = !mob.isRoutineEnabled(AIRoutine.AIRBORNE);
        boolean tamed = mob.getGeneralState(GeneralStates.TAMED) && !mob.getGeneralState(GeneralStates.STAYING);
        boolean up = Math.random() < 0.9;
        if (grounded && up && !tamed)
        {
            mob.setRoutineState(AIRoutine.AIRBORNE, true);
        }
        else if (!tamed)
        {
            mob.setRoutineState(AIRoutine.AIRBORNE, false);
            v.set(x, y, z);
            v.set(Vector3.getNextSurfacePoint(world, v, Vector3.secondAxisNeg, v.y));
            if (v != null) y = v.y;
        }
        List<PlayerEntity> players = getPlayersWithinDistance(entity, Integer.MAX_VALUE);
        if (!players.isEmpty())
        {
            PlayerEntity player = players.get(0);
            double diff = Math.abs(player.posY - y);
            if (diff > 5)
            {
                y = player.posY + 5 * (1 - Math.random());
            }
        }
    }

    /** Grounded things will path to surface points. */
    private void doGroundIdle()
    {
        v.set(x, y, z);
        v.set(Vector3.getNextSurfacePoint(world, v, Vector3.secondAxisNeg, v.y));
        if (v != null) y = v.y;
    }

    /** Stationary things will not idle path at all */
    public void doStationaryIdle()
    {
        x = entity.posX;
        y = entity.posY;
        z = entity.posZ;
    }

    /** Water things will not idle path out of water. */
    public void doWaterIdle()
    {
        v.set(this.x, this.y, this.z);
        if (world.getBlockState(v.getPos()).getMaterial() != Material.WATER)
        {
            x = entity.posX;
            y = entity.posY;
            z = entity.posZ;
        }
    }

    private boolean getLocation()
    {
        boolean tameFactor = mob.getGeneralState(GeneralStates.TAMED) && !mob.getGeneralState(GeneralStates.STAYING);
        int distance = (int) (maxLength = tameFactor ? PokecubeMod.core.getConfig().idleMaxPathTame
                : PokecubeMod.core.getConfig().idleMaxPathWild);
        boolean goHome = false;
        if (!tameFactor)
        {
            if (mob.getHome() == null
                    || (mob.getHome().getX() == 0 && mob.getHome().getY() == 0 & mob.getHome().getZ() == 0))
            {
                v1.set(entity);
                mob.setHome(v1.intX(), v1.intY(), v1.intZ(), 16);
            }
            distance = (int) Math.min(distance, mob.getHomeDistance());
            v.set(mob.getHome());
            if (entity.getDistanceSq(mob.getHome()) > mob.getHomeDistance() * mob.getHomeDistance())
            {
                goHome = true;
            }
        }
        else
        {
            LivingEntity setTo = entity;
            if (mob.getPokemonOwner() != null) setTo = mob.getPokemonOwner();
            v.set(setTo);
        }
        if (goHome)
        {
            this.x = v.x;
            this.y = Math.round(v.y);
            this.z = v.z;
        }
        else
        {
            Vector3 v = getRandomPointNear(world, mob, v1, distance);
            if (v == null) return false;
            double diff = Math.max(mob.getPokedexEntry().length * mob.getSize(),
                    mob.getPokedexEntry().width * mob.getSize());
            diff = Math.max(2, diff);
            if (this.v.distToSq(v) < diff) { return false; }
            this.x = v.x;
            this.y = Math.round(v.y);
            this.z = v.z;
        }
        mob.setGeneralState(GeneralStates.IDLE, true);
        return true;
    }

    @Override
    public void reset()
    {
        mob.setGeneralState(GeneralStates.IDLE, false);
    }

    @Override
    public void run()
    {
        if (!mob.getGeneralState(GeneralStates.IDLE))
        {
            if (!getLocation()) return;
        }
        if (mob.getPokedexEntry().flys())
        {
            doFlyingIdle();
        }
        else if (mob.getPokedexEntry().floats())
        {
            doFloatingIdle();
        }
        else if (entry.swims() && entity.isInWater())
        {
            doWaterIdle();
        }
        else if (entry.isStationary)
        {
            doStationaryIdle();
        }
        else
        {
            doGroundIdle();
        }
        v1.set(entity);
        v.set(this.x, this.y, this.z);

        mob.setGeneralState(GeneralStates.IDLE, false);
        if (v1.distToSq(v) <= 1) return;

        entity.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).removeModifier(idlePathing);
        entity.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).applyModifier(idlePathing);
        Path path = this.entity.getNavigator().getPathToXYZ(this.x, this.y, this.z);
        entity.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).removeModifier(idlePathing);
        if (path != null && path.getCurrentPathLength() > maxLength) path = null;
        addEntityPath(entity, path, speed);
    }

    @Override
    public boolean shouldRun()
    {
        // Configs can set this to -1 to disable idle movement entirely.
        if (IDLETIMER <= 0) return false;

        // Check random number
        if (new Random().nextInt(IDLETIMER) != 0) return false;

        // Wander disabled, so don't run.
        if (!mob.isRoutineEnabled(AIRoutine.WANDER)) return false;

        // Pokedex entry says it doesn't wander.
        if (mob.getPokedexEntry().isStationary) return false;

        // Angry at something
        if (mob.getCombatState(CombatStates.ANGRY)) return false;

        // Trying to use a move.
        if (mob.getCombatState(CombatStates.EXECUTINGMOVE)) return false;

        // Pathing somewhere.
        if (mob.getLogicState(LogicStates.PATHING)) return false;

        // Owner is controlling us.
        if (mob.getGeneralState(GeneralStates.CONTROLLED)) return false;

        // Sitting
        if (mob.getLogicState(LogicStates.SITTING)) return false;

        // Not currently able to move.
        if (entity.getNavigator() instanceof PokemobNavigator
                && !((PokemobNavigator) entity.getNavigator()).canNavigate())
            return false;

        Path current = null;
        world = TickHandler.getInstance().getWorldCache(entity.dimension);
        if (world == null) return false;
        if ((current = entity.getNavigator().getPath()) != null && entity.getNavigator().noPath())
        {
            addEntityPath(entity, null, speed);
            current = null;
        }

        // Have path, no need to idle
        if (current != null) return false;

        return true;
    }

    public static Vector3 getRandomPointNear(IBlockAccess world, IPokemob mob, Vector3 v, int distance)
    {
        Random rand = new Random();

        // SElect random gaussians from here.
        double x = rand.nextGaussian() * distance;
        double z = rand.nextGaussian() * distance;

        // Cap x and z to distance.
        if (Math.abs(x) > distance) x = Math.signum(x) * distance;
        if (Math.abs(z) > distance) z = Math.signum(z) * distance;

        // Don't select distances too far up/down from current.
        double y = Math.min(Math.max(1, rand.nextGaussian() * 4), 2);
        v.addTo(x, y, z);
        if (v.isClearOfBlocks(world) && mob.getBlockPathWeight(world, v) <= 40) { return v; }
        return null;
    }

}