package pokecube.core.events.pokemob;

import net.minecraftforge.eventbus.api.Cancelable;
import pokecube.core.interfaces.IPokemob;

@Cancelable
public class LevelUpEvent extends Event 
{
	public final IPokemob mob;
	public final int newLevel;
	public final int oldLevel;
	
	public LevelUpEvent(IPokemob mob, int newLevel, int oldLevel)
	{
		this.mob = mob;
		this.newLevel = newLevel;
		this.oldLevel = oldLevel;
	}
	
}
