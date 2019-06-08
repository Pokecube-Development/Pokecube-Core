package pokecube.core.moves.animations.presets;

import java.util.Random;

import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.moves.animations.AnimPreset;
import thut.api.maths.Vector3;

@AnimPreset(getPreset = "pons")
public class ParticlesOnSource extends ParticlesOnTarget
{
    public ParticlesOnSource()
    {
        super();
    }

    @Override
    public void spawnClientEntities(MovePacketInfo info)
    {
        if (Math.random() > density) return;
        initColour((info.attacker.getEntityWorld().getDayTime()), 0, info.move);
        Vector3 temp = Vector3.getNewVector().set(info.source);
        Random rand = new Random();
        float dw = 0.25f;
        if (info.attacker != null) dw = info.attacker.width;
        float width = this.width * dw;
        temp.addTo(rand.nextGaussian() * width, rand.nextGaussian() * width, rand.nextGaussian() * width);
        PokecubeMod.core.spawnParticle(info.attacker.getEntityWorld(), particle, temp, null, rgba);
    }
}
