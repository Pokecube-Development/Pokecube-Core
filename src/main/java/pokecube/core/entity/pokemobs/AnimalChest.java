package pokecube.core.entity.pokemobs;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AnimalChest extends InventoryBasic
{
    public AnimalChest(String inventoryName, int slotCount)
    {
        super(inventoryName, false, slotCount);
    }

    @OnlyIn(Dist.CLIENT)
    public AnimalChest(ITextComponent invTitle, int slotCount)
    {
        super(invTitle, slotCount);
    }
}
