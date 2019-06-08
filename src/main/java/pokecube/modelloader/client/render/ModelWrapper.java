package pokecube.modelloader.client.render;

import java.util.Locale;
import java.util.Random;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.TextureOffset;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.LivingEntity;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.moves.MovesUtils;
import pokecube.modelloader.ModPokecubeML;
import pokecube.modelloader.common.Config;
import pokecube.modelloader.common.IEntityAnimator;
import thut.core.client.render.model.IModelRenderer;

public class ModelWrapper extends ModelBase
{
    final String             name;
    public ModelBase         wrapped;
    public IModelRenderer<?> model;

    // Used to check if it has a custom sleeping animation.
    private boolean          checkedForContactAttack   = false;
    private boolean          hasContactAttackAnimation = false;

    // Used to check if it has a custom sleeping animation.
    private boolean          checkedForRangedAttack    = false;
    private boolean          hasRangedAttackAnimation  = false;

    public boolean           overrideAnim              = false;
    public String            anim                      = "";

    public ModelWrapper(String name)
    {
        this.name = name;
        if (ModPokecubeML.preload || Config.instance.toPreload.contains(name)) checkWrapped();
    }

    public void setWrapped(ModelBase wrapped)
    {
        this.wrapped = wrapped;
    }

    private void checkWrapped()
    {
        if (wrapped == null || model == null)
        {
            model = AnimationLoader.getModel(name);
            if (model != null && model instanceof RenderLivingBase)
            {
                wrapped = ((RenderLivingBase<?>) model).getMainModel();
            }
            else wrapped = new ModelBiped();
        }
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scale)
    {
        checkWrapped();
        wrapped.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
    }

    @Override
    public void setLivingAnimations(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float partialTickTime)
    {
        checkWrapped();
        if (model != null)
        {
            String phase = "idle";
            IPokemob mob = CapabilityPokemob.getPokemobFor(entity);
            if (overrideAnim) phase = anim;
            else if (entity instanceof IEntityAnimator)
            {
                phase = ((IEntityAnimator) entity).getAnimation(partialTickTime);
            }
            else if (mob != null)
            {
                phase = getPhase(mob.getEntity(), mob, partialTickTime);
            }
            if (!model.hasAnimation(phase, entity)) phase = "idle";
            model.setAnimation(phase, entity);
        }
        wrapped.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTickTime);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scaleFactor, Entity entityIn)
    {
        checkWrapped();
        wrapped.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
    }

    @Override
    public ModelRenderer getRandomModelBox(Random rand)
    {
        checkWrapped();
        return wrapped.getRandomModelBox(rand);
    }

    @Override
    public TextureOffset getTextureOffset(String partName)
    {
        checkWrapped();
        return wrapped.getTextureOffset(partName);
    }

    @Override
    public void setModelAttributes(ModelBase model)
    {
        checkWrapped();
        wrapped.setModelAttributes(model);
    }

    private String getPhase(MobEntity entity, IPokemob pokemob, float partialTick)
    {
        String phase = "idle";
        if (model == null) return phase;
        float walkspeed = entity.prevLimbSwingAmount
                + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTick;

        boolean asleep = pokemob.getStatus() == IMoveConstants.STATUS_SLP
                || pokemob.getLogicState(LogicStates.SLEEPING);

        if (!checkedForContactAttack)
        {
            hasContactAttackAnimation = model.hasAnimation("attack_contact", entity);
            checkedForContactAttack = true;
        }
        if (!checkedForRangedAttack)
        {
            hasRangedAttackAnimation = model.hasAnimation("attack_ranged", entity);
            checkedForRangedAttack = true;
        }
        if (pokemob.getCombatState(CombatStates.EXECUTINGMOVE))
        {
            int index = pokemob.getMoveIndex();
            Move_Base move;
            if (index < 4 && (move = MovesUtils.getMoveFromName(pokemob.getMove(index))) != null)
            {
                if (hasContactAttackAnimation && (move.getAttackCategory() & IMoveConstants.CATEGORY_CONTACT) > 0)
                {
                    phase = "attack_contact";
                    return phase;
                }
                if (hasRangedAttackAnimation && (move.getAttackCategory() & IMoveConstants.CATEGORY_DISTANCE) > 0)
                {
                    phase = "attack_ranged";
                    return phase;
                }
            }
        }

        for (LogicStates state : LogicStates.values())
        {
            String anim = state.toString().toLowerCase(Locale.ENGLISH);
            if (pokemob.getLogicState(state) && model.hasAnimation(anim, entity)) { return anim; }
        }

        if (asleep && model.hasAnimation("sleeping", entity))
        {
            phase = "sleeping";
            return phase;
        }
        if (asleep && model.hasAnimation("asleep", entity))
        {
            phase = "asleep";
            return phase;
        }
        if (!entity.onGround && model.hasAnimation("flight", entity))
        {
            phase = "flight";
            return phase;
        }
        if (!entity.onGround && model.hasAnimation("flying", entity))
        {
            phase = "flying";
            return phase;
        }
        if (entity.isInWater() && model.hasAnimation("swimming", entity))
        {
            phase = "swimming";
            return phase;
        }
        if (entity.onGround && walkspeed > 0.1 && model.hasAnimation("walking", entity))
        {
            phase = "walking";
            return phase;
        }
        if (entity.onGround && walkspeed > 0.1 && model.hasAnimation("walk", entity))
        {
            phase = "walk";
            return phase;
        }

        for (CombatStates state : CombatStates.values())
        {
            String anim = state.toString().toLowerCase(Locale.ENGLISH);
            if (pokemob.getCombatState(state) && model.hasAnimation(anim, entity)) { return anim; }
        }
        return phase;
    }
}
