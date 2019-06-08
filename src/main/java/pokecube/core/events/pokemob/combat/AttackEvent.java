package pokecube.core.events.pokemob.combat;

import pokecube.core.interfaces.pokemob.moves.MovePacket;

/** This event is fired when MovePacket is being constructed, you may use this
 * to interfere with the packet. */
public class AttackEvent extends Event
{
    public final MovePacket moveInfo;

    public AttackEvent(MovePacket moveInfo)
    {
        this.moveInfo = moveInfo;
    }
}
