package pokecube.core.client.gui.blocks;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import pokecube.core.PokecubeCore;
import pokecube.core.client.Resources;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.inventory.tms.TMContainer;
import pokecube.core.moves.MovesUtils;
import pokecube.core.network.packets.PacketTMs;

public class TMs<T extends TMContainer> extends ContainerScreen<T>
{
    public static ResourceLocation TEXTURE = new ResourceLocation(PokecubeMod.ID, Resources.TEXTURE_GUI_FOLDER
            + "tm_machine.png");

    private TextFieldWidget search;
    int                     index = 0;

    public TMs(final T container, final PlayerInventory playerInventory, final ITextComponent name)
    {
        super(container, playerInventory, name);
    }

    @Override
    public boolean charTyped(final char p_charTyped_1_, final int p_charTyped_2_)
    {
        System.out.println(this.search.getText());
        return super.charTyped(p_charTyped_1_, p_charTyped_2_);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(TMs.TEXTURE);
        final int j2 = (this.width - this.xSize) / 2;
        final int k2 = (this.height - this.ySize) / 2;
        this.blit(j2, k2, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void init()
    {
        super.init();
        final String apply = I18n.format("block.tm_machine.apply");
        this.addButton(new Button(this.width / 2 - 8, this.height / 2 - 39, 60, 20, apply, b ->
        {
            final PacketTMs packet = new PacketTMs();
            packet.data.putInt("m", this.index);
            PokecubeCore.packets.sendToServer(packet);
        }));
        final String next = I18n.format(">");
        this.addButton(new Button(this.width / 2 + 68, this.height / 2 - 50, 10, 10, next, b ->
        {
            final String[] moves = this.container.moves;
            this.index++;
            if (this.index > moves.length - 1) this.index = 0;
        }));
        final String prev = I18n.format("<");
        this.addButton(new Button(this.width / 2 - 30, this.height / 2 - 50, 10, 10, prev, b ->
        {
            final String[] moves = this.container.moves;
            this.index--;
            if (this.index < 0 && moves.length > 0) this.index = moves.length - 1;
            else if (this.index < 0) this.index = 0;
        }));
        this.addButton(this.search = new TextFieldWidget(this.font, this.width / 2 - 19, this.height / 2 - 50, 87, 10,
                ""));
    }

    @Override
    /** Draws the screen and all the components in it. */
    public void render(final int mouseX, final int mouseY, final float partialTicks)
    {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        final String[] moves = this.container.moves;
        final String s = moves.length > 0 ? moves[this.index] : "";
        final Move_Base move = MovesUtils.getMoveFromName(s);
        if (move != null)
        {
            final int yOffset = this.height / 2 - 164;
            final int xOffset = this.width / 2 - 42;
            this.drawString(this.font, MovesUtils.getMoveName(s).getFormattedText(), xOffset + 14, yOffset + 99, move
                    .getType(null).colour);
            this.drawString(this.font, "" + move.getPWR(), xOffset + 102, yOffset + 99, 0xffffff);
        }
        this.renderHoveredToolTip(mouseX, mouseY);
    }

}
