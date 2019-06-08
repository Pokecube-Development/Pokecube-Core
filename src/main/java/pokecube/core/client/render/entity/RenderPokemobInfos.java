package pokecube.core.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;

@OnlyIn(Dist.CLIENT)
public abstract class RenderPokemobInfos<T extends MobEntity> extends RenderLiving<T>
{
    public static boolean shouldShow(LivingEntity entity)
    {
        if (((Minecraft) PokecubeCore.getMinecraftInstance()).gameSettings.hideGUI
                || entity.isBeingRidden()) { return false; }

        LivingEntity player = Minecraft.getInstance().player;
        if (!entity.addedToChunk || entity.getRidingEntity() == player) return false;
        float d = entity.getDistance(player);
        IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
        boolean tameFactor = pokemob.getGeneralState(GeneralStates.TAMED)
                && !pokemob.getGeneralState(GeneralStates.STAYING);
        if ((tameFactor && d < 35) || d < 8) { return true; }
        Entity target = ((EntityCreature) entity).getAttackTarget();
        return (player.equals(target) || pokemob.getGeneralState(GeneralStates.TAMED));
    }

    public RenderPokemobInfos(RenderManager m, ModelBase modelbase, float shadowSize)
    {
        super(m, modelbase, shadowSize);
    }

    @Override
    public void doRender(T MobEntity, double d, double d1, double d2, float f, float partialTick)
    {
        super.doRender(MobEntity, d, d1, d2, f, partialTick);
    }

    @Override
    public ModelBase getMainModel()
    {
        return mainModel;
    }

    public void setShadowSize(float size)
    {
        this.shadowSize = size;
    }
}
