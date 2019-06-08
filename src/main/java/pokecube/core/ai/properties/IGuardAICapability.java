package pokecube.core.ai.properties;

import java.util.List;

import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import pokecube.core.utils.TimePeriod;

public interface IGuardAICapability
{
    public static interface IGuardTask
    {
        TimePeriod getActiveTime();

        void setActiveTime(TimePeriod active);

        void startTask(MobEntity entity);

        void continueTask(MobEntity entity);

        void endTask(MobEntity entity);

        BlockPos getPos();

        float getRoamDistance();

        void setPos(BlockPos pos);

        void setRoamDistance(float roam);

        default INBT serialze()
        {
            CompoundNBT tag = new CompoundNBT();
            if (getPos() != null)
            {
                tag.put("pos", NBTUtil.createPosTag(getPos()));
            }
            tag.putFloat("d", getRoamDistance());
            TimePeriod time;
            if ((time = getActiveTime()) != null)
            {
                tag.putLong("start", time.startTick);
                tag.putLong("end", time.endTick);
            }
            return tag;
        }

        default void load(INBT tag)
        {
            CompoundNBT nbt = (CompoundNBT) tag;
            if (nbt.hasKey("pos")) setPos(NBTUtil.getPosFromTag(nbt.getCompound("pos")));
            setRoamDistance(nbt.getFloat("d"));
            setActiveTime(new TimePeriod((int) nbt.getLong("start"), (int) nbt.getLong("end")));
        }
    }

    public static enum GuardState
    {
        IDLE, RUNNING, COOLDOWN
    }

    public static class Storage implements Capability.IStorage<IGuardAICapability>
    {
        @Override
        public void readNBT(Capability<IGuardAICapability> capability, IGuardAICapability instance, Direction side,
                INBT nbt)
        {
            if (nbt instanceof CompoundNBT)
            {
                CompoundNBT data = (CompoundNBT) nbt;
                instance.setState(GuardState.values()[data.getInteger("state")]);
                if (data.hasKey("tasks"))
                {
                    ListNBT tasks = (ListNBT) data.getTag("tasks");
                    instance.loadTasks(tasks);
                }
            }
        }

        @Override
        public INBT writeNBT(Capability<IGuardAICapability> capability, IGuardAICapability instance, Direction side)
        {
            CompoundNBT ret = new CompoundNBT();
            ret.setInteger("state", instance.getState().ordinal());
            ret.put("tasks", instance.serializeTasks());
            return ret;
        }
    }

    List<IGuardTask> getTasks();

    GuardState getState();

    void setState(GuardState state);

    // do we have a task with a location, and a position
    boolean hasActiveTask(long time, long daylength);

    IGuardTask getActiveTask();

    // This should be primary task to try, usually will just be
    // getTasks().get(0)
    IGuardTask getPrimaryTask();

    void loadTasks(ListNBT list);

    ListNBT serializeTasks();
}
