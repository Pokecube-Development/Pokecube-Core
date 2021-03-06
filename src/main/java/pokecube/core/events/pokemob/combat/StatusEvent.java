package pokecube.core.events.pokemob.combat;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.entity.impl.PersistantStatusEffect.Status;

/** This event is called to apply the effects of the status. It will by default
 * be handled by Pokecube, with priority listener of LOWEST. Cancel this event
 * to prevent pokecube dealing with it<br>
 * <br>
 * These events are fired on the
 * {@link pokecube.core.interfaces.PokecubeMod#MOVE_BUS} */
@Cancelable
public class StatusEvent extends EntityEvent
{
    final Status   status;
    final IPokemob pokemob;

    public StatusEvent(Entity entity, Status status)
    {
        super(entity);
        this.status = status;
        this.pokemob = CapabilityPokemob.getPokemobFor(entity);
    }

    public Status getStatus()
    {
        return status;
    }

    public IPokemob getPokemob()
    {
        return pokemob;
    }

}
