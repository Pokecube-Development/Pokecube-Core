package pokecube.core.events;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class PCEvent extends Event
{
    public final ItemStack        toPC;
    public final EntityLivingBase owner;

    public PCEvent(ItemStack stack, EntityLivingBase owner)
    {
        this.toPC = stack;
        this.owner = owner;
    }
}
