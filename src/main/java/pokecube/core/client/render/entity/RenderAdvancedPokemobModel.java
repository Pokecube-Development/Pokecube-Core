package pokecube.core.client.render.entity;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.modelloader.client.render.AbstractModelRenderer;
import pokecube.modelloader.client.render.AnimationLoader;
import pokecube.modelloader.client.render.ModelWrapper;
import pokecube.modelloader.client.render.models.URLModelHolder;
import thut.core.client.render.model.IModelRenderer;

public class RenderAdvancedPokemobModel<T extends EntityLiving> extends RenderPokemobInfos<T>
{
    public static IModelRenderer<?> getRenderer(String name, EntityLiving entity)
    {
        models:
        if (entity.getEntityData().hasKey("url_model"))
        {
            NBTTagCompound modeltag = entity.getEntityData().getCompoundTag("url_model");
            String tag_name = modeltag.getString("name");
            if (tag_name.isEmpty()) break models;
            name = tag_name + "___" + name;
            if (!AnimationLoader.modelMaps.containsKey(name))
            {
                String url_model = modeltag.getString("model");
                String url_texture = modeltag.getString("texture");
                String url_xml = modeltag.getString("xml");
                new URLModelHolder(name, url_model, url_xml, url_texture);
            }
        }
        return AnimationLoader.getModel(name);
    }

    public IModelRenderer<T> model;
    final String             modelName;
    public ModelWrapper      wrapper;

    boolean                  blend;

    boolean                  normalize;

    int                      src;
    int                      dst;

    public RenderAdvancedPokemobModel(String name, RenderManager manager, float par2)
    {
        super(manager, new ModelWrapper(name), par2);
        wrapper = (ModelWrapper) mainModel;
        modelName = name;
    }

    @SuppressWarnings("unchecked")
    public void preload()
    {
        PokedexEntry entry = Database.getEntry(modelName);
        model = (IModelRenderer<T>) getRenderer(entry.getTrimmedName(), null);
        if (model == null && entry.getBaseForme() != null)
        {
            model = (IModelRenderer<T>) getRenderer(entry.getBaseForme().getTrimmedName(), null);
            AnimationLoader.modelMaps.put(entry.getTrimmedName(), model);
        }
        if (model != null && model instanceof RenderLivingBase)
        {
            wrapper.setWrapped(((RenderLivingBase<?>) model).getMainModel());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void doRender(T entity, double x, double y, double z, float yaw, float partialTick)
    {
        if (!RenderPokemobs.shouldRender(entity, x, y, z, yaw, partialTick)) return;
        IPokemob mob = CapabilityPokemob.getPokemobFor(entity);
        T toRender = entity;
        IPokemob temp;
        if ((temp = CapabilityPokemob.getPokemobFor(mob.getTransformedTo())) != null)
        {
            toRender = (T) mob.getTransformedTo();
            mob = temp;
        }
        model = (IModelRenderer<T>) getRenderer(mob.getPokedexEntry().getTrimmedName(), entity);
        if (model == null && mob.getPokedexEntry().getBaseForme() != null)
        {
            model = (IModelRenderer<T>) getRenderer(mob.getPokedexEntry().getBaseForme().getTrimmedName(), entity);
            AnimationLoader.modelMaps.put(mob.getPokedexEntry().getTrimmedName(), model);
        }
        if (model != null && model instanceof RenderLivingBase)
        {
            wrapper.setWrapped(((RenderLivingBase<?>) model).getMainModel());
        }
        if (model == null) return;
        if (MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre(entity, this, partialTick, x, y, z))) return;
        GL11.glPushMatrix();
        this.preRenderCallback(entity, partialTick);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        if ((partialTick <= 1))
        {
            boolean exitCube = mob.getGeneralState(GeneralStates.EXITINGCUBE);
            if (mob.isEvolving()) RenderPokemob.renderEvolution(mob, yaw);
            if (exitCube) RenderPokemob.renderExitCube(mob, yaw);
        }
        float s = (mob.getSize());
        this.shadowSize = (float) (entity.addedToChunk ? Math.sqrt(s * mob.getPokedexEntry().width) : 0);
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        if (model.getTexturer() == null)
        {
            ResourceLocation texture = getEntityTexture(entity);
            FMLClientHandler.instance().getClient().renderEngine.bindTexture(texture);
        }
        doAnimations(entity, partialTick);
        model.doRender(toRender, x, y, z, yaw, partialTick);
        model.renderStatus(toRender, x, y, z, yaw, partialTick);
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(entity, this, partialTick, x, y, z));
        GL11.glPopMatrix();
        this.postRenderCallback();
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity)
    {
        ResourceLocation ret = null;
        IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
        if (model instanceof AbstractModelRenderer)
        {
            AbstractModelRenderer<?> render = (AbstractModelRenderer<?>) model;
            if (render.model_holder != null)
            {
                ret = render.model_holder.texture;

                if (ret != null && pokemob != null)
                {
                    PokedexEntry entry = pokemob.getPokedexEntry();
                    if (ret.equals(new ResourceLocation(entry.getModId(),
                            entry.texturePath + entry.getTrimmedName() + ".png")))
                    {
                        ret = null;
                        render.model_holder.texture = null;
                    }
                }
            }
        }
        if (ret == null) ret = RenderPokemobs.getInstance().getEntityTexturePublic(entity);
        else if (pokemob != null) ret = pokemob.modifyTexture(ret);
        return ret;
    }

    protected void doAnimations(EntityLiving entity, float partialTick)
    {
        float f5 = 0.0F;
        float f6 = 0.0F;
        if (!entity.isRiding())
        {
            f5 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTick;
            f6 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTick);

            if (entity.isChild())
            {
                f6 *= 3.0F;
            }

            if (f5 > 1.0F)
            {
                f5 = 1.0F;
            }
        }
        this.mainModel.setLivingAnimations(entity, f6, f5, partialTick);
    }

    protected void postRenderCallback()
    {
        // Reset to original state. This fixes changes to guis when rendered in
        // them.
        if (!normalize) GL11.glDisable(GL11.GL_NORMALIZE);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(src, dst);
    }

    @Override
    protected void preRenderCallback(T entity, float f)
    {
        blend = GL11.glGetBoolean(GL11.GL_BLEND);
        normalize = GL11.glGetBoolean(GL11.GL_NORMALIZE);
        src = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        dst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        if (!normalize) GL11.glEnable(GL11.GL_NORMALIZE);
        if (!blend) GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }
}
