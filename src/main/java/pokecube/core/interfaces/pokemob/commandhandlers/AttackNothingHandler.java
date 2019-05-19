package pokecube.core.interfaces.pokemob.commandhandlers;

import net.minecraftforge.common.MinecraftForge;
import pokecube.core.events.pokemob.combat.CommandAttackEvent;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.network.pokemobs.PacketCommand.DefaultHandler;

public class AttackNothingHandler extends DefaultHandler
{

    public AttackNothingHandler()
    {
    }

    @Override
    public void handleCommand(IPokemob pokemob)
    {
        CommandAttackEvent event = new CommandAttackEvent(pokemob.getEntity(), null);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) pokemob.executeMove(pokemob.getEntity(), null, 0);
    }
}
