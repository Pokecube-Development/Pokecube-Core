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
        MinecraftForge.EVENT_BUS.post(new CommandAttackEvent(pokemob.getEntity(), null));
        pokemob.executeMove(pokemob.getEntity(), null, 0);
    }
}
