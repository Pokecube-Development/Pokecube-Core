package pokecube.core.client.gui.pokemob;

import java.util.List;

import javax.vecmath.Vector3f;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import pokecube.core.PokecubeCore;
import pokecube.core.client.Resources;
import pokecube.core.client.render.RenderHealth;
import pokecube.core.entity.pokemobs.ContainerPokemob;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.IHasCommands.Command;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.interfaces.pokemob.commandhandlers.StanceHandler;
import pokecube.core.network.pokemobs.PacketCommand;
import pokecube.core.network.pokemobs.PacketPokemobGui;
import thut.api.entity.IHungrymob;

public class GuiPokemob<T extends ContainerPokemob> extends ContainerScreen<T>
{
    public static class HungerBar extends Widget
    {
        public IHungrymob mob;
        public float      value = 0;

        public HungerBar(final int xIn, final int yIn, final int widthIn, final int heightIn, final IHungrymob mob)
        {
            super(xIn, yIn, widthIn, heightIn, "pokemob.gui.hungerbar");
            this.mob = mob;
        }

        @Override
        public void playDownSound(final SoundHandler p_playDownSound_1_)
        {
        }

        @Override
        public void render(final int mx, final int my, final float tick)
        {
            super.render(mx, my, tick);
        }

        @Override
        public void renderButton(final int mx, final int my, final float tick)
        {
            // Render the hunger bar for the pokemob.
            // Get the hunger values.
            final float full = PokecubeCore.getConfig().pokemobLifeSpan / 4 + PokecubeCore.getConfig().pokemobLifeSpan;
            float current = -(this.mob.getHungerTime() - PokecubeCore.getConfig().pokemobLifeSpan);
            // Convert to a scale
            final float scale = 100f / full;
            current *= scale / 100f;
            current = Math.min(1, current);
            this.value = (int) (1000 * (1 - current)) / 10f;

            int col = 0xFF555555;
            // Fill the background
            AbstractGui.fill(this.x, this.y, this.x + this.width, this.y + this.height, col);
            col = 0xFFFFFFFF;
            final int col1 = 0xFF00FF77;
            // Fill the bar
            this.fillGradient(this.x, this.y, this.x + (int) (this.width * current), this.y + this.height, col, col1);
        }

    }

    public static void renderMob(final LivingEntity entity, final int width, final int height, final int unusedA,
            final int unusedB, final float xRenderAngle, float yRenderAngle, final float zRenderAngle,
            final float scale)
    {

        final IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
        float size = 1;
        final int j = width;
        final int k = height;
        if (pokemob != null)
        {
            final float mobScale = pokemob.getSize();
            final Vector3f dims = pokemob.getPokedexEntry().getModelSize();
            size = Math.max(dims.z * mobScale, Math.max(dims.y * mobScale, dims.x * mobScale));
        }
        if (Float.isNaN(yRenderAngle)) yRenderAngle = entity.renderYawOffset - entity.ticksExisted;
        final float zoom = 25f / size * scale;
        GlStateManager.pushMatrix();
        GL11.glTranslatef(j + 55, k + 60, 50F);
        GL11.glScalef(-zoom, zoom, zoom);
        GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(135F, 0.0F, 1.0F, 0.0F);

        GL11.glRotatef(yRenderAngle, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(xRenderAngle, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(zRenderAngle, 0.0F, 0.0F, 1.0F);

        GlStateManager.enableColorMaterial();
        RenderHelper.enableStandardItemLighting();
        RenderHealth.enabled = false;
        final EntityRendererManager entityrenderermanager = Minecraft.getInstance().getRenderManager();
        entityrenderermanager.setPlayerViewY(180.0F);
        entityrenderermanager.setRenderShadow(false);
        entityrenderermanager.renderEntity(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        entityrenderermanager.setRenderShadow(true);
        RenderHealth.enabled = true;
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.activeTexture(GLX.GL_TEXTURE1);
        GlStateManager.disableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        GL11.glPopMatrix();
    }

    public static void setPokemob(final IPokemob pokemobIn)
    {
        if (pokemobIn == null)
        {
            PokecubeCore.LOGGER.error("Error syncing pokemob", new IllegalArgumentException());
            return;
        }
    }

    private float           yRenderAngle = 10;
    private TextFieldWidget name         = new TextFieldWidget(null, 1 / 2, 1 / 2, 120, 10, "");

    private float xRenderAngle = 0;

    Button sit;
    Button stay;
    Button guard;

    HungerBar bar;

    public GuiPokemob(final T container, final PlayerInventory inv)
    {
        super(container, inv, container.pokemob.getDisplayName());
        this.name.setText(container.pokemob.getDisplayName().getUnformattedComponentText().trim());
        this.name.setEnableBackgroundDrawing(false);
    }

    @Override
    public boolean charTyped(final char typedChar, final int keyCode)
    {
        if (this.name.isFocused()) if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            // name.setFocused(false);
        }
        else
        {
            // name.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == GLFW.GLFW_KEY_ENTER) this.container.pokemob.setPokemonNickname(this.name.getText());
            return true;
        }
        return super.charTyped(typedChar, keyCode);

    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.getMinecraft().getTextureManager().bindTexture(Resources.GUI_POKEMOB);
        final int k = (this.width - this.xSize) / 2;
        final int l = (this.height - this.ySize) / 2;
        this.blit(k, l, 0, 0, this.xSize, this.ySize);
        this.blit(k + 79, l + 17, 0, this.ySize, 90, 18);
        this.blit(k + 7, l + 35, 0, this.ySize + 54, 18, 18);
        this.yRenderAngle = -45;
        this.xRenderAngle = 0;
        if (this.container.pokemob != null) GuiPokemob.renderMob(this.container.pokemob.getEntity(), k, l, this.xSize,
                this.ySize, this.xRenderAngle, this.yRenderAngle, 0, 1);
    }

    /**
     * Draw the foreground layer for the ContainerScreen (everything in front
     * of the items)
     */
    @Override
    protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY)
    {
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8.0F, this.ySize - 96 + 2,
                4210752);
    }

    @Override
    public void init()
    {
        super.init();
        int xOffset = 8;
        int yOffset = 43;
        // Button width
        int w = 89;
        // Button height
        int h = 10;

        this.addButton(this.sit = new Button(this.width / 2 - xOffset, this.height / 2 - yOffset + 00, w, h, I18n
                .format("pokemob.gui.sit"), c -> PacketCommand.sendCommand(this.container.pokemob, Command.STANCE,
                        new StanceHandler(!this.container.pokemob.getLogicState(LogicStates.SITTING),
                                StanceHandler.BUTTONTOGGLESIT))));
        this.addButton(this.stay = new Button(this.width / 2 - xOffset, this.height / 2 - yOffset + 10, w, h, I18n
                .format("pokemob.gui.stay"), c -> PacketCommand.sendCommand(this.container.pokemob, Command.STANCE,
                        new StanceHandler(!this.container.pokemob.getGeneralState(GeneralStates.STAYING),
                                StanceHandler.BUTTONTOGGLESTAY))));
        this.addButton(this.guard = new Button(this.width / 2 - xOffset, this.height / 2 - yOffset + 20, w, h, I18n
                .format("pokemob.gui.guard"), c -> PacketCommand.sendCommand(this.container.pokemob, Command.STANCE,
                        new StanceHandler(!this.container.pokemob.getCombatState(CombatStates.GUARDING),
                                StanceHandler.BUTTONTOGGLEGUARD))));
        // Bar width
        w = 89;
        // Bar height
        h = 5;
        // Bar positioning
        final int i = 9, j = 48;
        this.addButton(this.bar = new HungerBar(this.width / 2 - i, this.height / 2 - j, w, h, this.container.pokemob));

        xOffset = 10;
        yOffset = 77;
        w = 30;
        h = 10;
        this.addButton(new Button(this.width / 2 - xOffset + 60, this.height / 2 - yOffset, w, h, I18n.format(
                "pokemob.gui.ai"), c -> PacketPokemobGui.sendPagePacket(PacketPokemobGui.AI, this.container.pokemob
                        .getEntity().getEntityId())));
        this.addButton(new Button(this.width / 2 - xOffset + 30, this.height / 2 - yOffset, w, h, I18n.format(
                "pokemob.gui.storage"), c -> PacketPokemobGui.sendPagePacket(PacketPokemobGui.STORAGE,
                        this.container.pokemob.getEntity().getEntityId())));
        this.addButton(new Button(this.width / 2 - xOffset + 00, this.height / 2 - yOffset, w, h, I18n.format(
                "pokemob.gui.routes"), c -> PacketPokemobGui.sendPagePacket(PacketPokemobGui.ROUTES,
                        this.container.pokemob.getEntity().getEntityId())));
        xOffset = 80;
        this.name = new TextFieldWidget(this.font, this.width / 2 - xOffset, this.height / 2 - yOffset, 120, 10, "");
        this.name.setEnableBackgroundDrawing(false);
        if (this.container.pokemob != null) this.name.setText(this.container.pokemob.getDisplayName()
                .getUnformattedComponentText().trim());
        this.name.setEnableBackgroundDrawing(false);
        this.name.setTextColor(0xFFFFFFFF);
        this.addButton(this.name);
    }

    /** Draws the screen and all the components in it. */
    @Override
    public void render(final int x, final int y, final float z)
    {
        super.renderBackground();
        super.render(x, y, z);
        final List<String> text = Lists.newArrayList();
        if (this.container.pokemob == null) return;

        final boolean guarding = this.container.pokemob.getCombatState(CombatStates.GUARDING);
        final boolean sitting = this.container.pokemob.getLogicState(LogicStates.SITTING);
        final boolean staying = this.container.pokemob.getGeneralState(GeneralStates.STAYING);

        this.guard.setFGColor(guarding ? 0xFF00FF00 : 0xFFFF0000);
        this.sit.setFGColor(sitting ? 0xFF00FF00 : 0xFFFF0000);
        this.stay.setFGColor(staying ? 0xFF00FF00 : 0xFFFF0000);

        if (this.guard.isMouseOver(x, y)) if (guarding) text.add(I18n.format("pokemob.stance.guard"));
        else text.add(I18n.format("pokemob.stance.no_guard"));
        if (this.stay.isMouseOver(x, y)) if (staying) text.add(I18n.format("pokemob.stance.stay"));
        else text.add(I18n.format("pokemob.stance.follow"));
        if (this.sit.isMouseOver(x, y)) if (sitting) text.add(I18n.format("pokemob.stance.sit"));
        else text.add(I18n.format("pokemob.stance.no_sit"));
        if (this.bar.isMouseOver(x, y)) text.add(I18n.format("pokemob.bar.value", this.bar.value));
        if (!text.isEmpty()) this.renderTooltip(text, x, y);
        this.renderHoveredToolTip(x, y);
    }
}
