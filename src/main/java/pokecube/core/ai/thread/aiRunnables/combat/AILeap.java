package pokecube.core.ai.thread.aiRunnables.combat;

import java.util.Random;
import java.util.logging.Level;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import pokecube.core.ai.thread.aiRunnables.AIBase;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import thut.api.entity.ai.IAICombat;
import thut.api.maths.Vector3;

public class AILeap extends AIBase implements IAICombat
{
    final EntityLiving attacker;
    final IPokemob     pokemob;
    Entity             target;
    int                leapCooldown = 10;
    double             leapSpeed    = 1;
    double             movementSpeed;
    Vector3            leapTarget   = null;
    Vector3            leapOrigin   = null;

    public AILeap(IPokemob entity)
    {
        this.attacker = entity.getEntity();
        this.pokemob = entity;
        this.movementSpeed = attacker.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue()
                * 0.8;
        this.setMutex(0);
    }

    /** Gets a random sound to play on leaping, selects from the options in
     * config. */
    private SoundEvent getLeapSound()
    {
        if (PokecubeMod.core.getConfig().leaps.length == 1) return PokecubeMod.core.getConfig().leaps[0];
        return PokecubeMod.core.getConfig().leaps[new Random().nextInt(PokecubeMod.core.getConfig().leaps.length)];
    }

    @Override
    public void reset()
    {
        this.target = null;
        leapTarget = null;
        leapOrigin = null;
    }

    @Override
    public void run()
    {

        // Target loc could just be a position
        leapTarget = target != null ? Vector3.getNewVector().set(target) : pokemob.getTargetPos();
        Vector3 location = Vector3.getNewVector().set(attacker);
        Vector3 dir = leapTarget.subtract(location);

        /* Don't leap up if too far. */
        if (dir.y > 5) return;

        double dist = dir.x * dir.x + dir.z * dir.z;
        float diff = attacker.width + (target == null ? 0 : target.width);
        diff = diff * diff;

        // Wait till it is a bit closer than this...
        if (dist >= 16.0D) { return; }
        if (dist <= diff)
        {
            pokemob.setCombatState(CombatStates.LEAPING, false);
            leapCooldown = PokecubeMod.core.getConfig().attackCooldown / 2;
            return;
        }

        dir.norm();
        dir.scalarMultBy(leapSpeed * PokecubeMod.core.getConfig().leapSpeedFactor);
        if (dir.isNaN())
        {
            new Exception().printStackTrace();
            dir.clear();
        }

        if (PokecubeMod.debug)
        {
            PokecubeMod.log(Level.INFO, "Leap: " + attacker + " " + dir.mag());
        }

        //Compute differences in velocities, and then account for that during the leap.
        Vector3 v_a = Vector3.getNewVector().setToVelocity(attacker);
        Vector3 v_t = Vector3.getNewVector();
        if (target != null) v_t.setToVelocity(target);
        //Compute velocity differential.
        Vector3 dv = v_a.subtractFrom(v_t);
        //Adjust for existing velocity differential.
        dir.subtractFrom(dv);
        /*
         * Apply the leap
         */
        dir.addVelocities(attacker);
        // Only play sound once.
        if (leapCooldown == -1) toRun.add(new PlaySound(attacker.dimension, Vector3.getNewVector().set(attacker),
                getLeapSound(), SoundCategory.HOSTILE, 1, 1));
    }

    @Override
    public boolean shouldRun()
    {
        return leapCooldown-- < 0 && pokemob.getCombatState(CombatStates.LEAPING)
                && ((target = attacker.getAttackTarget()) != null || pokemob.getTargetPos() != null);
    }

}
