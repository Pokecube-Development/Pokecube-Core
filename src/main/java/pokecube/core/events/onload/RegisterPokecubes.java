package pokecube.core.events.onload;

import java.util.List;

import com.google.common.collect.Lists;

import pokecube.core.interfaces.IPokecube.PokecubeBehavior;

/** This event is fired during the item registration phase. Add any pokecubes
 * you want to register here. The name of the cube will be <prefix_>cube */
public class RegisterPokecubes extends Event
{
    public List<PokecubeBehavior> behaviors = Lists.newArrayList();

    public RegisterPokecubes()
    {
    }

}
