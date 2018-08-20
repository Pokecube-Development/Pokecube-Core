package pokecube.core.events;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class PokedexInspectEvent extends EntityEvent
{
    public final boolean shouldReward;

    public PokedexInspectEvent(Entity entity, boolean reward)
    {
        super(entity);
        shouldReward = reward;
    }
}
