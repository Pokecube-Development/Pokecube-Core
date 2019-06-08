package pokecube.core.utils;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import pokecube.core.interfaces.IPokemob;

public class EntityTools
{
    public static void copyEntityTransforms(LivingEntity to, LivingEntity from)
    {
        to.setEntityId(from.getEntityId());
        to.posX = from.posX;
        to.posY = from.posY;
        to.posZ = from.posZ;
        to.lastTickPosX = from.lastTickPosX;
        to.lastTickPosY = from.lastTickPosY;
        to.lastTickPosZ = from.lastTickPosZ;

        to.motionX = from.motionX;
        to.motionY = from.motionY;
        to.motionZ = from.motionZ;

        to.rotationPitch = from.rotationPitch;
        to.ticksExisted = from.ticksExisted;
        to.rotationYaw = from.rotationYaw;
        to.setRotationYawHead(from.getRotationYawHead());
        to.prevRotationPitch = from.prevRotationPitch;
        to.prevRotationYaw = from.prevRotationYaw;
        to.prevRotationYawHead = from.prevRotationYawHead;
        to.prevRenderYawOffset = from.prevRenderYawOffset;
        to.renderYawOffset = from.renderYawOffset;

        to.dimension = from.dimension;

        to.onGround = from.onGround;

        to.prevLimbSwingAmount = from.prevLimbSwingAmount;
        to.limbSwing = from.limbSwing;
        to.limbSwingAmount = from.limbSwingAmount;
    }

    public static void copyEntityData(LivingEntity to, LivingEntity from)
    {
        CompoundNBT tag = new CompoundNBT();
        from.writeEntityToNBT(tag);
        to.readEntityFromNBT(tag);
    }

    public static void copy(IPokemob from, IPokemob to)
    {
        to.readPokemobData(from.writePokemobData());
    }
}
