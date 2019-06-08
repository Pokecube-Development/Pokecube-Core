package pokecube.core.interfaces.capabilities;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import pokecube.core.PokecubeCore;
import thut.api.entity.ai.AIThreadManager.AIStuff;
import thut.api.entity.ai.IAIMob;
import thut.api.entity.ai.IAIRunnable;
import thut.api.entity.ai.ILogicRunnable;

public class AICapWrapper implements IAIMob, ICapabilitySerializable<CompoundNBT>
{
    public static final ResourceLocation AICAP = new ResourceLocation(PokecubeCore.ID, "ai");

    final DefaultPokemob                 wrapped;
    private CompoundNBT               read  = null;

    public AICapWrapper(DefaultPokemob wrapped)
    {
        this.wrapped = wrapped;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void init()
    {
        if (read == null) return;
        CompoundNBT nbt = read;
        read = null;
        CompoundNBT aiTag = nbt.getCompound("ai");
        CompoundNBT logicTag = nbt.getCompound("logic");
        for (IAIRunnable runnable : getAI().aiTasks)
        {
            if (runnable instanceof INBTSerializable)
            {
                if (aiTag.hasKey(runnable.getIdentifier()))
                {
                    ((INBTSerializable) runnable).deserializeNBT(aiTag.getTag(runnable.getIdentifier()));
                }
            }
        }
        for (ILogicRunnable runnable : getAI().aiLogic)
        {
            if (runnable instanceof INBTSerializable<?>)
            {
                if (logicTag.hasKey(runnable.getIdentifier()))
                {
                    ((INBTSerializable) runnable).deserializeNBT(aiTag.getTag(runnable.getIdentifier()));
                }
            }
        }
    }

    @Override
    public AIStuff getAI()
    {
        return wrapped.getAI();
    }

    @Override
    public boolean selfManaged()
    {
        return wrapped.selfManaged();
    }

    @Override
    public boolean vanillaWrapped()
    {
        return this.wrapped.vanillaWrapped();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, Direction facing)
    {
        return capability == THUTMOBAI;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, Direction facing)
    {
        if (hasCapability(capability, facing)) return THUTMOBAI.cast(this);
        return null;
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT savedAI = new CompoundNBT();
        CompoundNBT savedLogic = new CompoundNBT();
        for (IAIRunnable runnable : getAI().aiTasks)
        {
            if (runnable instanceof INBTSerializable<?>)
            {
                INBT base = INBTSerializable.class.cast(runnable).serializeNBT();
                savedAI.put(runnable.getIdentifier(), base);
            }
        }
        for (ILogicRunnable runnable : getAI().aiLogic)
        {
            if (runnable instanceof INBTSerializable<?>)
            {
                INBT base = INBTSerializable.class.cast(runnable).serializeNBT();
                savedLogic.put(runnable.getIdentifier(), base);
            }
        }
        tag.put("ai", savedAI);
        tag.put("logic", savedLogic);
        if (read != null && savedAI.hasNoTags() && savedLogic.hasNoTags())
        {
            tag = read;
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {
        read = nbt;
    }

    @Override
    public void setWrapped(boolean wrapped)
    {
        this.wrapped.setWrapped(wrapped);
    }

}
