package pokecube.core.items.megastuff;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.client.models.ModelRing;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.megastuff.MegaCapability.RingChecker;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.x3d.X3dModel;
import thut.lib.CompatWrapper;
import thut.wearables.EnumWearable;
import thut.wearables.IActiveWearable;

public class WearablesCompat
{
    public abstract static class WearablesRenderer
    {
        @OnlyIn(Dist.CLIENT)
        public abstract void renderWearable(thut.wearables.EnumWearable slot, LivingEntity wearer, ItemStack stack,
                float partialTicks);
    }

    private static final ResourceLocation        WEARABLESKEY = new ResourceLocation("pokecube:wearable");

    public static Map<String, WearablesRenderer> renderers    = Maps.newHashMap();

    static
    {
        renderers.put("pokewatch", new WearablesRenderer()
        {
            // 2 layers of belt rendering for the different colours.
            @OnlyIn(Dist.CLIENT)
            private X3dModel         model;

            // Textures for each belt layer.
            private ResourceLocation strap;
            private ResourceLocation watch;

            @OnlyIn(Dist.CLIENT)
            @Override
            public void renderWearable(EnumWearable slot, LivingEntity wearer, ItemStack stack, float partialTicks)
            {
                if (slot != EnumWearable.WRIST) return;
                if (model == null)
                {
                    model = new X3dModel(new ResourceLocation(PokecubeMod.ID, "models/worn/pokewatch.x3d"));
                    strap = new ResourceLocation(PokecubeMod.ID, "textures/worn/megabelt_2.png");
                    watch = new ResourceLocation(PokecubeMod.ID, "textures/worn/watch.png");
                }
                int brightness = wearer.getBrightnessForRender();
                int[] col = new int[] { 255, 255, 255, 255, brightness };
                float s, sy, sx, sz, dx, dy, dz;
                dx = 0.f;
                dy = .06f;
                dz = -0.075f;
                s = .65f;
                sx = s;
                sy = s;
                sz = s * 1.5f;
                GL11.glPushMatrix();
                GL11.glTranslatef(dx, dy, dz);
                GL11.glScalef(sx * 0.75f, sy, sz);
                GL11.glRotatef(-90, 1, 0, 0);
                Minecraft.getInstance().renderEngine.bindTexture(strap);
                EnumDyeColor ret = EnumDyeColor.GRAY;
                if (stack.hasTag() && stack.getTag().hasKey("dyeColour"))
                {
                    int damage = stack.getTag().getInteger("dyeColour");
                    ret = EnumDyeColor.byDyeDamage(damage);
                }
                Color colour = new Color(ret.getColorValue() + 0xFF000000);
                col[0] = colour.getRed();
                col[1] = colour.getGreen();
                col[2] = colour.getBlue();
                model.getParts().get("strap").setRGBAB(col);
                model.renderOnly("strap");
                GL11.glPopMatrix();
                GL11.glPushMatrix();
                GL11.glTranslatef(dx, dy, dz);
                GL11.glScalef(sx, sy, sz);
                GL11.glRotatef(-90, 1, 0, 0);
                Minecraft.getInstance().renderEngine.bindTexture(watch);
                model.renderOnly("watch");
                GL11.glPopMatrix();

            }
        });
        renderers.put("ring", new WearablesRenderer()
        {
            // rings use own model, so only 1 layer here, ring model handles own
            // textures.
            @OnlyIn(Dist.CLIENT)
            private ModelRing ring;

            @OnlyIn(Dist.CLIENT)
            @Override
            public void renderWearable(EnumWearable slot, LivingEntity wearer, ItemStack stack, float partialTicks)
            {
                if (slot != EnumWearable.FINGER) return;
                if (ring == null) ring = new ModelRing();
                ring.stack = stack;
                ring.render(wearer, 0, 0, partialTicks, 0, 0, 0.0625f);
            }
        });
        renderers.put("belt", new WearablesRenderer()
        {
            // 2 layers of belt rendering for the different colours.
            @OnlyIn(Dist.CLIENT)
            private X3dModel         belt;

            // Textures for each belt layer.
            private ResourceLocation keystone;
            private ResourceLocation belt_2;

            @OnlyIn(Dist.CLIENT)
            @Override
            public void renderWearable(EnumWearable slot, LivingEntity wearer, ItemStack stack, float partialTicks)
            {
                if (slot != EnumWearable.WAIST) return;
                if (belt == null)
                {
                    belt = new X3dModel(new ResourceLocation(PokecubeMod.ID, "models/worn/megabelt.x3d"));
                    keystone = new ResourceLocation(PokecubeMod.ID, "textures/worn/keystone.png");
                    belt_2 = new ResourceLocation(PokecubeMod.ID, "textures/worn/megabelt_2.png");
                }
                int brightness = wearer.getBrightnessForRender();
                int[] col = new int[] { 255, 255, 255, 255, brightness };
                GL11.glPushMatrix();
                float dx = 0, dy = .6f, dz = -0.f;
                GL11.glTranslatef(dx, dy, dz);
                float s = 1.1f;
                if (!CompatWrapper.isValid(wearer.getItemStackFromSlot(EntityEquipmentSlot.LEGS)))
                {
                    s = 0.95f;
                }
                GL11.glScalef(s, s, s);
                GL11.glRotatef(90, 1, 0, 0);
                GL11.glRotatef(180, 0, 1, 0);
                Minecraft.getInstance().renderEngine.bindTexture(keystone);
                GL11.glRotatef(90, 1, 0, 0);
                belt.renderOnly("stone");
                GL11.glPopMatrix();

                GL11.glPushMatrix();
                GL11.glTranslatef(dx, dy, dz);
                GL11.glScalef(s, s, s);
                GL11.glRotatef(90, 1, 0, 0);
                GL11.glRotatef(180, 0, 1, 0);
                Minecraft.getInstance().renderEngine.bindTexture(belt_2);
                EnumDyeColor ret = EnumDyeColor.GRAY;
                if (stack.hasTag() && stack.getTag().hasKey("dyeColour"))
                {
                    int damage = stack.getTag().getInteger("dyeColour");
                    ret = EnumDyeColor.byDyeDamage(damage);
                }
                Color colour = new Color(ret.getColorValue() + 0xFF000000);
                col[0] = colour.getRed();
                col[1] = colour.getGreen();
                col[2] = colour.getBlue();
                belt.getParts().get("belt").setRGBAB(col);
                belt.renderOnly("belt");
                GL11.glPopMatrix();
            }
        });
        renderers.put("hat", new WearablesRenderer()
        {
            // 2 layers of hat rendering for the different colours.
            @OnlyIn(Dist.CLIENT)
            X3dModel                 hat;

            // Textures for each hat layer.
            private ResourceLocation hat_1 = new ResourceLocation(PokecubeCore.ID, "textures/worn/hat.png");
            private ResourceLocation hat_2 = new ResourceLocation(PokecubeCore.ID, "textures/worn/hat2.png");

            @OnlyIn(Dist.CLIENT)
            @Override
            public void renderWearable(EnumWearable slot, LivingEntity wearer, ItemStack stack, float partialTicks)
            {
                if (slot != EnumWearable.HAT) return;

                if (hat == null)
                {
                    hat = new X3dModel(new ResourceLocation(PokecubeMod.ID, "models/worn/hat.x3d"));
                }

                Minecraft minecraft = Minecraft.getInstance();
                GlStateManager.pushMatrix();
                float s = 0.285f;
                GL11.glScaled(s, -s, -s);
                int brightness = wearer.getBrightnessForRender();
                int[] col = new int[] { 255, 255, 255, 255, brightness };
                minecraft.renderEngine.bindTexture(hat_1);
                for (IExtendedModelPart part1 : hat.getParts().values())
                {
                    part1.setRGBAB(col);
                }
                hat.renderAll();
                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                GL11.glScaled(s * 0.995f, -s * 0.995f, -s * 0.995f);
                minecraft.renderEngine.bindTexture(hat_2);
                EnumDyeColor ret = EnumDyeColor.RED;
                if (stack.hasTag() && stack.getTag().hasKey("dyeColour"))
                {
                    int damage = stack.getTag().getInteger("dyeColour");
                    ret = EnumDyeColor.byDyeDamage(damage);
                }
                Color colour = new Color(ret.getColorValue() + 0xFF000000);
                col[0] = colour.getRed();
                col[1] = colour.getGreen();
                col[2] = colour.getBlue();
                for (IExtendedModelPart part : hat.getParts().values())
                {
                    part.setRGBAB(col);
                }
                hat.renderAll();
                GL11.glColor3f(1, 1, 1);
                GlStateManager.popMatrix();
            }
        });
    }

    public WearablesCompat()
    {
        MegaCapability.checker = new RingChecker()
        {
            @Override
            public boolean canMegaEvolve(PlayerEntity player, PokedexEntry toEvolve)
            {
                Set<ItemStack> worn = thut.wearables.ThutWearables.getWearables(player).getWearables();
                for (ItemStack stack : worn)
                {
                    if (stack != null)
                    {
                        if (MegaCapability.matches(stack, toEvolve)) return true;
                    }
                }
                for (int i = 0; i < player.inventory.armorInventory.size(); i++)
                {
                    ItemStack stack = player.inventory.armorInventory.get(i);
                    if (stack != null)
                    {
                        if (MegaCapability.matches(stack, toEvolve)) return true;
                    }
                }
                return false;
            }
        };
    }

    @SubscribeEvent
    public void onItemCapabilityAttach(AttachCapabilitiesEvent<ItemStack> event)
    {
        if (event.getObject().getItem() instanceof ItemMegawearable)
        {
            event.addCapability(WEARABLESKEY, new WearableMega());
        }
        else if (event.getObject().getItem() == PokecubeItems.pokewatch)
        {
            event.addCapability(WEARABLESKEY, new WearableWatch());
        }
    }

    public static class WearableMega implements thut.wearables.IActiveWearable, ICapabilityProvider
    {
        String getVariant(ItemStack stack)
        {
            if (stack.getItem() instanceof ItemMegawearable) { return ((ItemMegawearable) stack.getItem()).name; }
            return "";
        }

        String getSlotSt(ItemStack stack)
        {
            if (stack.getItem() instanceof ItemMegawearable) { return ((ItemMegawearable) stack.getItem()).slot; }
            return "";
        }

        @Override
        public thut.wearables.EnumWearable getSlot(ItemStack stack)
        {
            return thut.wearables.EnumWearable.valueOf(getSlotSt(stack));
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void renderWearable(thut.wearables.EnumWearable slot, LivingEntity wearer, ItemStack stack,
                float partialTicks)
        {
            WearablesRenderer renderer = renderers.get(getVariant(stack));
            if (renderer != null) renderer.renderWearable(slot, wearer, stack, partialTicks);
        }

        @Override
        public boolean hasCapability(Capability<?> capability, Direction facing)
        {
            return capability == WEARABLE_CAP;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, Direction facing)
        {
            return hasCapability(capability, facing) ? IActiveWearable.WEARABLE_CAP.cast(this) : null;
        }

        @Override
        public boolean dyeable(ItemStack stack)
        {
            return true;
        }
    }

    public static class WearableWatch implements thut.wearables.IActiveWearable, ICapabilityProvider
    {

        @Override
        public thut.wearables.EnumWearable getSlot(ItemStack stack)
        {
            return thut.wearables.EnumWearable.WRIST;
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void renderWearable(thut.wearables.EnumWearable slot, LivingEntity wearer, ItemStack stack,
                float partialTicks)
        {
            WearablesRenderer renderer = renderers.get("pokewatch");
            if (renderer != null) renderer.renderWearable(slot, wearer, stack, partialTicks);
        }

        @Override
        public boolean hasCapability(Capability<?> capability, Direction facing)
        {
            return capability == WEARABLE_CAP;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, Direction facing)
        {
            return hasCapability(capability, facing) ? IActiveWearable.WEARABLE_CAP.cast(this) : null;
        }

        @Override
        public boolean dyeable(ItemStack stack)
        {
            return true;
        }
    }
}
