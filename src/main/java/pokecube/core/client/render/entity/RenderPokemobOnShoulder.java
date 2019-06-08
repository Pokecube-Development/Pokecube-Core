package pokecube.core.client.render.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerEntityOnShoulder;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.utils.EntityTools;
import pokecube.modelloader.ModPokecubeML;
import pokecube.modelloader.client.ClientProxy;
import pokecube.modelloader.client.render.AnimationLoader;
import thut.core.client.render.model.IModelRenderer;

@OnlyIn(Dist.CLIENT)
public class RenderPokemobOnShoulder implements LayerRenderer<PlayerEntity>
{
    private final RenderManager                            renderManager;
    private final LayerEntityOnShoulder                    parent;
    protected RenderLivingBase<? extends LivingEntity> leftRenderer;
    protected IPokemob                                     leftMob;
    private ModelBase                                      leftModel;
    private PokedexEntry                                   leftEntry;
    private ResourceLocation                               leftResource;
    private UUID                                           leftUniqueId;
    private Class<?>                                       leftEntityClass;
    protected RenderLivingBase<? extends LivingEntity> rightRenderer;
    protected IPokemob                                     rightMob;
    private ModelBase                                      rightModel;
    private PokedexEntry                                   rightEntry;
    private ResourceLocation                               rightResource;
    private UUID                                           rightUniqueId;
    private Class<?>                                       rightEntityClass;

    public RenderPokemobOnShoulder(RenderManager renderManager, LayerEntityOnShoulder parent)
    {
        this.renderManager = renderManager;
        this.parent = parent;
    }

    @Override
    public void doRenderLayer(PlayerEntity player, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        if (player.getLeftShoulderEntity() != null || player.getRightShoulderEntity() != null)
        {
            GlStateManager.enableRescaleNormal();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            CompoundNBT CompoundNBT = player.getLeftShoulderEntity();

            boolean left = true;
            boolean right = true;

            if (!CompoundNBT.hasNoTags())
            {
                RenderPokemobOnShoulder.DataHolder holder = this.renderEntityOnShoulder(player, this.leftUniqueId,
                        CompoundNBT, this.leftRenderer, this.leftModel, this.leftResource, this.leftEntityClass,
                        leftEntry, leftMob, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch,
                        scale, true);
                if (holder == null)
                {
                    left = false;
                }
                else
                {
                    this.leftUniqueId = holder.entityId;
                    this.leftRenderer = holder.renderer;
                    this.leftResource = holder.textureLocation;
                    this.leftModel = holder.model;
                    this.leftEntityClass = holder.clazz;
                    this.leftEntry = holder.entry;
                    this.leftMob = holder.mob;
                }
            }
            else
            {
                this.leftUniqueId = null;
                this.leftRenderer = null;
                this.leftResource = null;
                this.leftModel = null;
                this.leftEntry = null;
                this.leftEntityClass = null;
                this.leftMob = null;
            }

            CompoundNBT CompoundNBT1 = player.getRightShoulderEntity();

            if (!CompoundNBT1.hasNoTags())
            {
                RenderPokemobOnShoulder.DataHolder holder = this.renderEntityOnShoulder(player, this.rightUniqueId,
                        CompoundNBT1, this.rightRenderer, this.rightModel, this.rightResource, this.rightEntityClass,
                        rightEntry, rightMob, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw,
                        headPitch, scale, false);
                if (holder == null)
                {
                    right = false;
                }
                else
                {
                    this.rightUniqueId = holder.entityId;
                    this.rightRenderer = holder.renderer;
                    this.rightResource = holder.textureLocation;
                    this.rightModel = holder.model;
                    this.rightEntityClass = holder.clazz;
                    this.rightEntry = holder.entry;
                    this.rightMob = holder.mob;
                }
            }
            else
            {
                this.rightUniqueId = null;
                this.rightRenderer = null;
                this.rightResource = null;
                this.rightModel = null;
                this.rightEntry = null;
                this.rightEntityClass = null;
                this.rightMob = null;
            }

            GlStateManager.disableRescaleNormal();
            if (!left || !right)
            {
                CompoundNBT bakLeft = CompoundNBT.copy();
                CompoundNBT bakRight = CompoundNBT1.copy();
                if (right)
                {
                    for (String s : bakLeft.getKeySet())
                        CompoundNBT.remove(s);
                }

                if (left)
                {
                    for (String s : bakRight.getKeySet())
                        CompoundNBT1.remove(s);
                }

                parent.doRenderLayer(player, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw,
                        headPitch, scale);

                if (right)
                {
                    for (String s : bakLeft.getKeySet())
                        CompoundNBT.put(s, bakLeft.getTag(s));
                }

                if (left)
                {
                    for (String s : bakRight.getKeySet())
                        CompoundNBT1.put(s, bakRight.getTag(s));
                }

            }
        }
    }

    @SuppressWarnings("unchecked")
    private RenderPokemobOnShoulder.DataHolder renderEntityOnShoulder(PlayerEntity player, @Nullable UUID mobUUID,
            CompoundNBT mobNBTTag, RenderLivingBase<? extends LivingEntity> mobRenderer, ModelBase mobModelBase,
            ResourceLocation texture, Class<?> mobClass, PokedexEntry entry, IPokemob mob, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
            float scaleFactor, boolean left)
    {
        if (mobUUID == null || !mobUUID.equals(mobNBTTag.getUniqueId("UUID")) || mob == null
                || entry != mob.getPokedexEntry())
        {
            mobUUID = mobNBTTag.getUniqueId("UUID");
            mobClass = EntityList.getClassFromName(mobNBTTag.getString("id"));
            mobRenderer = (RenderLivingBase<?>) this.renderManager
                    .getEntityClassRenderObject((Class<? extends Entity>) mobClass);
            mobModelBase = ((RenderLivingBase<?>) mobRenderer).getMainModel();
            Entity entity = EntityList.newEntity((Class<? extends Entity>) mobClass, player.world);
            entity.readFromNBT(mobNBTTag);
            mob = CapabilityPokemob.getPokemobFor(entity);
            if (mob == null) return null;
            entry = mob.getPokedexEntry();
            texture = RenderPokemobs.getInstance().getEntityTexturePublic(entity);
            if (mobModelBase == null)
            {
                IModelRenderer<?> model = RenderAdvancedPokemobModel
                        .getRenderer(mob.getPokedexEntry().getTrimmedName(), (MobEntity) entity);
                if (model == null && mob.getPokedexEntry().getBaseForme() != null)
                {
                    model = RenderAdvancedPokemobModel
                            .getRenderer(mob.getPokedexEntry().getBaseForme().getTrimmedName(), (MobEntity) entity);
                    AnimationLoader.modelMaps.put(mob.getPokedexEntry().getTrimmedName(), model);
                }
                if (model != null && model instanceof RenderLivingBase)
                {
                    mobModelBase = ((RenderLivingBase<?>) model).getMainModel();
                }
                if (mobModelBase == null)
                {
                    PokedexEntry mobEntry = mob.getPokedexEntry();
                    if (mobEntry.getBaseForme() != null)
                    {
                        mobEntry = mobEntry.getBaseForme();
                    }
                    for (PokedexEntry e : Database.getFormes(mobEntry))
                    {
                        ((ClientProxy) ModPokecubeML.proxy).reloadModel(e);
                    }
                }
            }
        }
        if (mobModelBase == null) return null;
        if (texture != null) mobRenderer.bindTexture(texture);
        GlStateManager.pushMatrix();
        float f = player.isSneaking() ? -1.3F : -1.5F;
        float f1 = left ? 0.4F : -0.4F;
        GlStateManager.translate(f1, f, 0.0F);

        if (mobClass == EntityParrot.class)
        {
            ageInTicks = 0.0F;
        }
        LivingEntity living = mob.getEntity();
        EntityTools.copyEntityTransforms(living, player);
        mobModelBase.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor,
                living);
        mobModelBase.setLivingAnimations(living, limbSwing, limbSwingAmount, partialTick);
        mobModelBase.render(living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        GlStateManager.popMatrix();
        return new RenderPokemobOnShoulder.DataHolder(mobUUID, mobRenderer, mobModelBase, texture, mobClass, entry,
                mob);
    }

    @Override
    public boolean shouldCombineTextures()
    {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    class DataHolder
    {
        public UUID                                         entityId;
        public RenderLivingBase<? extends LivingEntity> renderer;
        public ModelBase                                    model;
        public ResourceLocation                             textureLocation;
        public Class<?>                                     clazz;
        public IPokemob                                     mob;
        public PokedexEntry                                 entry;

        public DataHolder(UUID uuid, RenderLivingBase<? extends LivingEntity> renderer, ModelBase model,
                ResourceLocation texture, Class<?> clazz, PokedexEntry entry, IPokemob mob)
        {
            this.entityId = uuid;
            this.renderer = renderer;
            this.model = model;
            this.textureLocation = texture;
            this.clazz = clazz;
            this.entry = entry;
            this.mob = mob;
        }
    }
}