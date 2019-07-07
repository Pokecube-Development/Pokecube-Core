package pokecube.core.client.gui.blocks;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.inventory.pc.PCContainer;
import pokecube.core.inventory.pc.PCInventory;
import pokecube.core.inventory.pc.PCSlot;
import pokecube.core.network.packets.PacketPC;

public class PC<T extends PCContainer> extends ContainerScreen<T>
{

    String          page;
    TextFieldWidget textFieldSelectedBox;
    TextFieldWidget textFieldBoxName;
    TextFieldWidget textFieldSearch;

    String autoOn  = I18n.format("block.pc.autoon");
    String autoOff = I18n.format("block.pc.autooff");

    private String boxName = "1";
    boolean        bound   = false;
    boolean        release = false;

    public PC(final T container, final PlayerInventory ivplay, final ITextComponent name)
    {
        super(container, ivplay, name);
        this.xSize = 175;
        this.ySize = 229;
        this.page = container.getPageNb();
        this.boxName = container.getPage();
        // if (cont.pcTile != null) this.bound = cont.pcTile.isBound();
    }

    @Override
    public boolean charTyped(final char par1, final int par2)
    {
        // This replaces the super call, which also forces closed when "e" is
        // pressed:
        if (par2 == 1) this.minecraft.player.closeScreen();

        // this.checkHotbarKeys(par2);
        //
        // if (this.hoveredSlot != null && this.hoveredSlot.getHasStack())
        // if
        // (this.minecraft.gameSettings.keyBindPickBlock.isActiveAndMatches(par2))
        // this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber,
        // 0, ClickType.CLONE);
        // else if
        // (this.minecraft.gameSettings.keyBindDrop.isActiveAndMatches(par2))
        // this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber,
        // isCtrlKeyDown() ? 1 : 0, ClickType.THROW);

        // this.textFieldSearch.textboxKeyTyped(par1, par2);
        //
        // if (this.textFieldBoxName.visible)
        // this.textFieldBoxName.textboxKeyTyped(par1, par2);
        // if (par1 <= 57) this.textFieldSelectedBox.textboxKeyTyped(par1,
        // par2);
        if (par2 == 28)
        {
            final String entry = this.textFieldSelectedBox.getText();
            String box = this.textFieldBoxName.getText();
            int number = 1;

            try
            {
                number = Integer.parseInt(entry);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }

            number = Math.max(1, Math.min(number, PCInventory.PAGECOUNT));
            this.container.gotoInventoryPage(number);

            if (this.textFieldBoxName.visible && box != this.boxName)
            {

                if (this.textFieldBoxName.visible)
                {
                    box = this.textFieldBoxName.getText();
                    if (box != this.boxName) this.container.changeName(box);
                }
                this.textFieldBoxName.visible = !this.textFieldBoxName.visible;
            }
            return true;
        }
        return super.charTyped(par1, par2);

    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float f, final int i, final int j)
    {
        GL11.glColor4f(1f, 1f, 1f, 1f);

        this.minecraft.getTextureManager().bindTexture(new ResourceLocation(PokecubeMod.ID, "textures/gui/pcgui.png"));
        final int x = (this.width - this.xSize) / 2;
        final int y = (this.height - this.ySize) / 2;
        this.blit(x, y, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(final int par1, final int par2)
    {
        // GL11.glPushMatrix();
        // GL11.glScaled(0.8, 0.8, 0.8);
        // TODO title for PC
        // final String name = (this.container.pcTile != null) ?
        // this.container.pcTile.getName() : "";
        // final String pcTitle = this.bound ? name
        // : I18n.format("block.pc.title", this.container.inv.seenOwner ?
        // "Thutmose" :
        // "Someone");
        // this.font.drawString(this.container.getPage(),
        // this.xSize / 2 - this.font.getStringWidth(this.container.getPage()) /
        // 3 -
        // 60, 13, 4210752);
        // this.font.drawString(pcTitle, this.xSize / 2 -
        // this.font.getStringWidth(pcTitle) / 3 - 60, 4, 4210752);
        // GL11.glPopMatrix();

        for (int i = 0; i < 54; i++)
            if (this.container.toRelease[i])
            {
                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4f(0, 1, 0, 1);
                this.minecraft.getTextureManager().bindTexture(new ResourceLocation(PokecubeMod.ID,
                        "textures/hologram.png"));
                final int x = i % 9 * 18 + 8;
                final int y = 18 + i / 9 * 18;
                this.blit(x, y, 0, 0, 16, 16);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
        //// Auto on naming for auton button.
        // if (!this.bound) this.buttons.get(2).displayString =
        //// this.container.inv.autoToPC ? this.autoOn : this.autoOff;
    }

    @Override
    public void init()
    {
        super.init();
        final int xOffset = 0;
        final int yOffset = -11;
        final String next = I18n.format("block.pc.next");
        this.addButton(new Button(this.width / 2 - xOffset + 15, this.height / 2 - yOffset, 50, 20, next, b ->
        {
            this.container.updateInventoryPages((byte) 1, this.minecraft.player.inventory);
            this.textFieldSelectedBox.setText(this.container.getPageNb());
        }));
        final String prev = I18n.format("block.pc.previous");
        this.addButton(new Button(this.width / 2 - xOffset - 65, this.height / 2 - yOffset, 50, 20, prev, b ->
        {
            this.container.updateInventoryPages((byte) -1, this.minecraft.player.inventory);
            this.textFieldSelectedBox.setText(this.container.getPageNb());
        }));

        if (!this.bound)
        {
            final String auto = this.container.inv.autoToPC ? I18n.format("block.pc.autoon")
                    : I18n.format("block.pc.autooff");
            this.buttons.add(new Button(this.width / 2 - xOffset - 137, this.height / 2 - yOffset - 105, 50, 20, auto,
                    b -> this.container.toggleAuto()));
        }
        if (!this.bound)
        {
            final String rename = I18n.format("block.pc.rename");
            this.addButton(new Button(this.width / 2 - xOffset - 137, this.height / 2 - yOffset - 125, 50, 20, rename,
                    b ->
                    {
                        if (this.textFieldBoxName.visible)
                        {
                            final String box = this.textFieldBoxName.getText();
                            if (box != this.boxName) this.container.changeName(box);
                        }
                        this.textFieldBoxName.visible = !this.textFieldBoxName.visible;
                    }));
        }
        if (this.container.pcPos != null)
        {
            if (!this.bound) this.addButton(new Button(this.width / 2 - xOffset - 137, this.height / 2 - yOffset - 85,
                    50, 20, I18n.format("block.pc.option.private"), b ->
                    {
                        // TODO bind.
                        // this.container.pcTile.toggleBound();
                        this.minecraft.player.closeScreen();
                    }));
            else
            {
                this.addButton(new Button(this.width / 2 - xOffset - 137, this.height / 2 - yOffset - 125, 50, 20, I18n
                        .format("block.pc.option.public"), b ->
                        {
                            // TODO bind.
                            // this.container.pcTile.toggleBound();
                            this.minecraft.player.closeScreen();
                        }));
                this.addButton(new Button(this.width / 2 - xOffset - 137, this.height / 2 - yOffset - 105, 50, 20, I18n
                        .format("block.pc.option.bind"), b ->
                        {
                            // TODO bind.
                            // this.container.pcTile.setBoundOwner(this.minecraft.player);
                            this.minecraft.player.closeScreen();
                        }));
            }
        }
        else this.addButton(new Button(this.width / 2 - xOffset - 137, this.height / 2 - yOffset - 125, 0, 0, "", b ->
        {
            // TODO bind.
            // this.container.pcTile.toggleBound();
            this.minecraft.player.closeScreen();
        }));
        if (!this.bound)
        {
            this.addButton(new Button(this.width / 2 - xOffset - 137, this.height / 2 - yOffset - 65, 50, 20, I18n
                    .format("block.pc.option.release"), b ->
                    {
                        this.release = !this.release;
                        if (!this.release && this.container.release)
                        {
                            this.container.toRelease = new boolean[54];
                            for (int i = 0; i < 54; i++)
                            {
                                final int index = i;
                                final PCSlot slot = (PCSlot) this.container.inventorySlots.get(index);
                                slot.release = false;
                            }
                        }
                        else for (int i = 0; i < 54; i++)
                        {
                            final int index = i;
                            final PCSlot slot = (PCSlot) this.container.inventorySlots.get(index);
                            slot.release = true;
                        }
                        this.container.release = this.release;
                        final PacketPC packet = new PacketPC(PacketPC.RELEASE, this.container.inv.owner);
                        packet.data.putBoolean("T", true);
                        packet.data.putBoolean("R", this.release);
                        PokecubeCore.packets.sendToServer(packet);
                        if (this.release)
                        {
                            this.buttons.get(6).active = this.release;
                            this.buttons.get(6).visible = this.release;
                        }
                        else
                        {
                            this.buttons.get(6).active = this.release;
                            this.buttons.get(6).visible = this.release;
                        }
                    }));
            this.addButton(new Button(this.width / 2 - xOffset - 137, this.height / 2 - yOffset - 45, 50, 20, I18n
                    .format("block.pc.option.confirm"), b ->
                    {
                        this.release = !this.release;
                        this.container.setRelease(this.release);
                        if (this.release)
                        {
                            this.buttons.get(6).active = this.release;
                            this.buttons.get(6).visible = this.release;
                        }
                        else
                        {
                            this.buttons.get(6).active = this.release;
                            this.buttons.get(6).visible = this.release;
                        }
                        this.minecraft.player.closeScreen();
                    }));
            this.buttons.get(6).visible = false;
            this.buttons.get(6).active = false;
        }

        this.textFieldSelectedBox = new TextFieldWidget(this.font, this.width / 2 - xOffset - 13, this.height / 2
                - yOffset + 5, 25, 10, this.page);
        this.textFieldBoxName = new TextFieldWidget(this.font, this.width / 2 - xOffset - 190, this.height / 2 - yOffset
                - 40, 100, 10, this.boxName);
        this.textFieldBoxName.visible = false;
        this.textFieldSearch = new TextFieldWidget(this.font, this.width / 2 - xOffset - 10, this.height / 2 - yOffset
                - 121, 90, 10, "");

        this.addButton(this.textFieldSelectedBox);
        this.addButton(this.textFieldBoxName);
        this.addButton(this.textFieldSearch);
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat
     * events
     */
    @Override
    public void onClose()
    {
        if (this.minecraft.player != null) this.container.onContainerClosed(this.minecraft.player);
    }

    @Override
    public void render(final int mouseX, final int mouseY, final float f)
    {
        this.renderBackground();
        // this.textFieldSelectedBox.drawTextBox();
        //
        // if (!this.bound) this.textFieldSearch.drawTextBox();
        //
        // if (this.textFieldBoxName.visible)
        // this.textFieldBoxName.drawTextBox();
        final float zLevel = 1000;
        for (int i = 0; i < 54; i++)
            if (!this.textFieldSearch.getText().isEmpty())
            {
                final ItemStack stack = this.container.inv.getStackInSlot(i + 54 * this.container.inv.getPage());
                final int x = i % 9 * 18 + this.width / 2 - 80;
                final int y = i / 9 * 18 + this.height / 2 - 96;

                final String name = stack == null ? "" : stack.getDisplayName().getFormattedText();
                if (name.isEmpty() || !name.toLowerCase(java.util.Locale.ENGLISH).contains(this.textFieldSearch
                        .getText()))
                {
                    GL11.glPushMatrix();
                    GL11.glTranslated(0, 0, zLevel);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glColor4f(0, 0, 0, 1);
                    this.minecraft.getTextureManager().bindTexture(new ResourceLocation(PokecubeMod.ID,
                            "textures/hologram.png"));
                    this.blit(x, y, 0, 0, 16, 16);
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glTranslated(0, 0, -zLevel);
                    GL11.glPopMatrix();
                }
                else
                {
                    GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glColor4f(0, 1, 0, 1);
                    this.minecraft.getTextureManager().bindTexture(new ResourceLocation(PokecubeMod.ID,
                            "textures/hologram.png"));
                    this.blit(x, y, 0, 0, 16, 16);
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glPopMatrix();
                }
            }
        super.render(mouseX, mouseY, f);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

}