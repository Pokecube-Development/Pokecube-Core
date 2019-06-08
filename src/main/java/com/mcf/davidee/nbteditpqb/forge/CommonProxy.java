package com.mcf.davidee.nbteditpqb.forge;

import java.io.File;

import com.mcf.davidee.nbteditpqb.NBTEdit;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class CommonProxy
{
    public void registerInformation()
    {

    }

    public File getMinecraftDirectory()
    {
        return new File(".");
    }

    public void openEditGUI(int entityID, CompoundNBT tag)
    {

    }

    public void openEditGUI(int entityID, String customName, CompoundNBT tag)
    {

    }

    public void openEditGUI(BlockPos pos, CompoundNBT tag)
    {

    }

    public void sendMessage(PlayerEntity player, String message, TextFormatting color)
    {
        if (player != null)
        {
            ITextComponent component = new StringTextComponent(message);
            component.getStyle().setColor(color);
            player.sendMessage(component);
        }
    }

    public boolean checkPermission(PlayerEntity player)
    {
        if (NBTEdit.opOnly ? player.canUseCommand(4, NBTEdit.MODID)
                : player.capabilities.isCreativeMode) { return true; }
        return false;
    }
}
