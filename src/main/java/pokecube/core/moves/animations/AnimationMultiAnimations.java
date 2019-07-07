package pokecube.core.moves.animations;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import pokecube.core.PokecubeCore;
import pokecube.core.database.moves.MoveEntry;
import pokecube.core.database.moves.json.JsonMoves.AnimationJson;
import pokecube.core.interfaces.IMoveAnimation;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.moves.animations.presets.Thunder;

public class AnimationMultiAnimations extends MoveAnimationBase
{
    public static class WrappedAnimation
    {
        IMoveAnimation   wrapped;
        ResourceLocation sound;
        SoundEvent       soundEvent;
        boolean          soundSource = false;
        boolean          soundTarget = false;
        float            volume      = 1;
        float            pitch       = 1;
        int              start;
    }

    public static boolean isThunderAnimation(IMoveAnimation input)
    {
        if (input == null) return false;
        if (!(input instanceof AnimationMultiAnimations)) return input instanceof Thunder;
        final AnimationMultiAnimations anim = (AnimationMultiAnimations) input;
        for (final WrappedAnimation a : anim.components)
            if (a.wrapped instanceof Thunder) return true;
        return false;
    }

    List<WrappedAnimation> components = Lists.newArrayList();

    private int applicationTick = 0;

    public AnimationMultiAnimations(MoveEntry move)
    {
        final List<AnimationJson> animations = move.baseEntry.animations;
        this.duration = 0;
        if (animations == null || animations.isEmpty()) return;
        for (final AnimationJson anim : animations)
        {
            if (!anim.preset.endsWith(":~" + move.name)) anim.preset = anim.preset + ":~" + move.name;
            final IMoveAnimation animation = MoveAnimationHelper.getAnimationPreset(anim.preset);
            if (animation == null) continue;
            final int start = Integer.parseInt(anim.starttick);
            final int dur = Integer.parseInt(anim.duration);
            if (anim.applyAfter) this.applicationTick = Math.max(start + dur, this.applicationTick);
            this.duration = Math.max(this.duration, start + dur);
            final WrappedAnimation wrapped = new WrappedAnimation();
            if (anim.sound != null)
            {
                wrapped.sound = new ResourceLocation(anim.sound);
                wrapped.soundSource = anim.soundSource != null ? anim.soundSource : false;
                wrapped.soundTarget = anim.soundTarget != null ? anim.soundTarget : true;
                wrapped.pitch = anim.pitch != null ? anim.pitch : 1;
                wrapped.volume = anim.volume != null ? anim.volume : 1;
            }
            wrapped.wrapped = animation;
            wrapped.start = start;
            this.components.add(wrapped);
        }
        if (this.applicationTick == 0) this.applicationTick = this.duration;
        this.components.sort((arg0, arg1) -> arg0.start - arg1.start);
    }

    @Override
    public void clientAnimation(MovePacketInfo info, float partialTick)
    {
        final int tick = info.currentTick;
        for (int i = 0; i < this.components.size(); i++)
        {
            info.currentTick = tick;
            final WrappedAnimation toRun = this.components.get(i);
            if (tick > toRun.start + toRun.wrapped.getDuration()) continue;
            if (toRun.start > tick) continue;
            info.currentTick = tick - toRun.start;
            toRun.wrapped.clientAnimation(info, partialTick);
        }
    }

    @Override
    public int getApplicationTick()
    {
        return this.applicationTick;
    }

    @Override
    public void initColour(long time, float partialTicks, Move_Base move)
    {
        // We don't do this.
    }

    @Override
    public void spawnClientEntities(MovePacketInfo info)
    {
        final int tick = info.currentTick;
        for (int i = 0; i < this.components.size(); i++)
        {
            info.currentTick = tick;
            final WrappedAnimation toRun = this.components.get(i);
            if (tick > toRun.start + toRun.wrapped.getDuration()) continue;
            if (toRun.start > tick) continue;
            info.currentTick = tick - toRun.start;
            toRun.wrapped.spawnClientEntities(info);
            sound:
            if (info.currentTick == 0 && toRun.sound != null)
            {
                final World world = PokecubeCore.proxy.getWorld();
                if (toRun.soundEvent == null)
                {
                    toRun.soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(toRun.sound);
                    if (toRun.soundEvent == null)
                    {
                        PokecubeCore.LOGGER.error("No Registered Sound for " + toRun.sound);
                        toRun.sound = null;
                        break sound;
                    }
                }
                if (toRun.soundSource) if (info.source != null) world.playSound(info.source.x, info.source.y,
                        info.source.z, toRun.soundEvent, SoundCategory.HOSTILE, toRun.volume, toRun.pitch, false);
                else if (info.attacker != null) world.playSound(info.attacker.posX, info.attacker.posY,
                        info.attacker.posZ, toRun.soundEvent, SoundCategory.HOSTILE, toRun.volume, toRun.pitch, false);
                if (toRun.soundTarget) if (info.target != null) world.playSound(info.target.x, info.target.y,
                        info.target.z, toRun.soundEvent, SoundCategory.HOSTILE, toRun.volume, toRun.pitch, false);
                else if (info.attacked != null) world.playSound(info.attacked.posX, info.attacked.posY,
                        info.attacked.posZ, toRun.soundEvent, SoundCategory.HOSTILE, toRun.volume, toRun.pitch, false);
            }
        }
    }

}
