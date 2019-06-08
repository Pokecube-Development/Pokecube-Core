package pokecube.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.UUID;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.api.distmarker.Dist;
import pokecube.core.blocks.pc.InventoryPC;
import pokecube.core.interfaces.PokecubeMod;
import thut.core.common.handlers.PlayerDataHandler;

public class PCSaveHandler
{
    private static PCSaveHandler instance;

    private static PCSaveHandler clientInstance;

    public static PCSaveHandler getInstance()
    {
        if (FMLCommonHandler.instance().getEffectiveSide() == Dist.DEDICATED_SERVER)
        {
            if (instance == null) instance = new PCSaveHandler();
            return instance;
        }
        if (clientInstance == null) clientInstance = new PCSaveHandler();
        return clientInstance;
    }

    public boolean seenPCCreator = false;

    public PCSaveHandler()
    {
    }

    public void loadPC(UUID uuid)
    {
        if (FMLCommonHandler.instance().getEffectiveSide() == Dist.CLIENT) return;
        try
        {
            File file = PlayerDataHandler.getFileForUUID(uuid.toString(), "PCInventory");
            if (file != null && file.exists())
            {
                if (PokecubeMod.debug) PokecubeMod.log("Loading PC: " + uuid);
                FileInputStream fileinputstream = new FileInputStream(file);
                CompoundNBT CompoundNBT = CompressedStreamTools.readCompressed(fileinputstream);
                fileinputstream.close();
                readPcFromNBT(CompoundNBT.getCompound("Data"));
            }
        }
        catch (FileNotFoundException e)
        {
        }
        catch (Exception e)
        {
        }
    }

    public void readPcFromNBT(CompoundNBT nbt)
    {
        seenPCCreator = nbt.getBoolean("seenPCCreator");
        // Read PC Data from NBT
        INBT temp = nbt.getTag("PC");
        if (temp instanceof ListNBT)
        {
            ListNBT tagListPC = (ListNBT) temp;
            InventoryPC.loadFromNBT(tagListPC);
        }
    }

    public void savePC(UUID uuid)
    {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null
                || FMLCommonHandler.instance().getEffectiveSide() == Dist.CLIENT)
            return;
        try
        {
            File file = PlayerDataHandler.getFileForUUID(uuid.toString(), "PCInventory");
            if (file != null)
            {
                CompoundNBT CompoundNBT = new CompoundNBT();
                writePcToNBT(CompoundNBT, uuid);
                CompoundNBT CompoundNBT1 = new CompoundNBT();
                CompoundNBT1.put("Data", CompoundNBT);
                FileOutputStream fileoutputstream = new FileOutputStream(file);
                CompressedStreamTools.writeCompressed(CompoundNBT1, fileoutputstream);
                fileoutputstream.close();
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void writePcToNBT(CompoundNBT nbt, UUID uuid)
    {
        nbt.putBoolean("seenPCCreator", seenPCCreator);
        ListNBT tagsPC = InventoryPC.saveToNBT(uuid);
        nbt.put("PC", tagsPC);
    }

}
