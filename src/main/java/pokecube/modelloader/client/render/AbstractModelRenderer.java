package pokecube.modelloader.client.render;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.FMLClientHandler;
import pokecube.core.ai.thread.logicRunnables.LogicMiscUpdate;
import pokecube.core.client.render.entity.RenderPokemob;
import pokecube.core.client.render.entity.RenderPokemobs;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.modelloader.client.render.models.URLModelHolder;
import thut.api.maths.Vector3;
import thut.core.client.render.animation.ModelHolder;
import thut.core.client.render.model.IAnimationChanger;
import thut.core.client.render.model.IModel;
import thut.core.client.render.model.IModelRenderer;
import thut.core.client.render.model.IPartTexturer;

public abstract class AbstractModelRenderer<T extends MobEntity> extends RenderLivingBase<T>
        implements IModelRenderer<T>
{
    final Vector3             rotPoint          = Vector3.getNewVector();

    private IPartTexturer     texturer;
    private IPartTexturer     texturer2;
    private IAnimationChanger animator;

    // Used to check if it has a custom sleeping animation.
    private boolean           checkedForSleep   = false;
    private boolean           hasSleepAnimation = false;

    // Values used to properly reset the GL state after rendering.
    public ModelHolder        model_holder;
    boolean                   blend;
    boolean                   light;
    int                       src;
    int                       dst;

    public AbstractModelRenderer(RenderManager renderManagerIn, ModelBase modelBaseIn, float shadowSizeIn)
    {
        super(renderManagerIn, modelBaseIn, shadowSizeIn);
    }

    @Override
    protected void applyRotations(T par1MobEntity, float par2, float par3, float par4)
    {
        super.applyRotations(par1MobEntity, par2, par3, par4);
        if (!checkedForSleep)
        {
            checkedForSleep = true;
            hasSleepAnimation = hasAnimation("sleeping", par1MobEntity) || hasAnimation("sleep", par1MobEntity)
                    || hasAnimation("asleep", par1MobEntity);
        }
        if (par1MobEntity.getHealth() <= 0)
        {
            float f = (par1MobEntity.deathTime + par4 - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);

            if (f > 1.0F)
            {
                f = 1.0F;
            }

            GlStateManager.rotate(f * this.getDeathMaxRotation(par1MobEntity), 0.0F, 0.0F, 1.0F);
            return;
        }
        if (!hasSleepAnimation)
        {
            IPokemob pokemob = CapabilityPokemob.getPokemobFor(par1MobEntity);
            boolean status = pokemob.getStatus() == IMoveConstants.STATUS_SLP;
            if (status || pokemob.getLogicState(LogicStates.SLEEPING))
            {
                float ratio = 1F;
                GL11.glRotatef(80 * ratio, 0.0F, 0.0F, 1F);
                return;
            }
        }
    }

    private void postRenderStatus()
    {
        if (light) GL11.glEnable(GL11.GL_LIGHTING);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(src, dst);
    }

    private void preRenderStatus()
    {
        blend = GL11.glGetBoolean(GL11.GL_BLEND);
        light = GL11.glGetBoolean(GL11.GL_LIGHTING);
        src = GL11.glgetInt(GL11.GL_BLEND_SRC);
        dst = GL11.glgetInt(GL11.GL_BLEND_DST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void renderStatus(T entity, double d0, double d1, double d2, float f, float partialTick)
    {
        preRenderStatus();
        RenderPokemob.renderStatus(this, entity, d0, d1, d2, f, partialTick);
        postRenderStatus();
    }

    @Override
    protected boolean canRenderName(T entity)
    {
        return entity.getEntityData().getBoolean("isPlayer");
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity)
    {
        ResourceLocation ret = null;
        IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
        if (model_holder != null)
        {
            ret = model_holder.texture;
        }
        if (ret == null) ret = RenderPokemobs.getInstance().getEntityTexturePublic(entity);
        else if (pokemob != null) ret = pokemob.modifyTexture(ret);
        return ret;
    }

    @Override
    public void scaleEntity(Entity entity, IModel model, float partialTick)
    {
        IPokemob mob = CapabilityPokemob.getPokemobFor(entity);
        if (mob != null)
        {
            float s = 1;
            float sx = (float) getScale().x;
            float sy = (float) getScale().y;
            float sz = (float) getScale().z;
            s = (mob.getSize());
            if (partialTick <= 1 && mob.getGeneralState(GeneralStates.EXITINGCUBE))
            {
                int ticks = -mob.getEvolutionTicks() + 50 + LogicMiscUpdate.EXITCUBEDURATION;
                if (ticks <= LogicMiscUpdate.EXITCUBEDURATION / 2)
                {
                    float max = LogicMiscUpdate.EXITCUBEDURATION / 2;
                    s *= (ticks + partialTick) / max;
                }
            }
            sx *= s;
            sy *= s;
            sz *= s;
            rotPoint.set(getRotationOffset()).scalarMultBy(s);
            model.setOffset(rotPoint);
            if (!getScale().isEmpty()) GlStateManager.scale(sx, sy, sz);
            else
            {
                GlStateManager.scale(s, s, s);
            }
        }
    }

    @Override
    public IAnimationChanger getAnimationChanger()
    {
        return animator;
    }

    @Override
    public IPartTexturer getTexturer()
    {
        if (model_holder instanceof URLModelHolder) { return texturer2 == null ? texturer2 = new IPartTexturer()
        {

            @Override
            public boolean shiftUVs(String part, double[] toFill)
            {
                return false;
            }

            @Override
            public boolean isFlat(String part)
            {
                return texturer.isFlat(part);
            }

            @Override
            public boolean hasMapping(String part)
            {
                return false;
            }

            @Override
            public void bindObject(Object thing)
            {

            }

            @Override
            public void applyTexture(String part)
            {
                FMLClientHandler.instance().getClient().renderEngine.bindTexture(model_holder.texture);
            }

            @Override
            public void addMapping(String part, String tex)
            {

            }

            @Override
            public void addCustomMapping(String part, String state, String tex)
            {

            }
        } : texturer2; }
        return texturer;
    }

    @Override
    public void setTexturer(IPartTexturer texturer)
    {
        this.texturer = texturer;
    }

    @Override
    public void setAnimationChanger(IAnimationChanger changer)
    {
        this.animator = changer;
    }
}
