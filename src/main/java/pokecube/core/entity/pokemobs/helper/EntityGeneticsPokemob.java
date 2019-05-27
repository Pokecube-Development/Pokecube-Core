package pokecube.core.entity.pokemobs.helper;

import static pokecube.core.entity.pokemobs.genetics.GeneticsManager.COLOURGENE;

import java.util.List;

import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import pokecube.core.entity.pokemobs.genetics.genes.ColourGene;
import pokecube.core.interfaces.capabilities.AICapWrapper;
import thut.api.entity.ai.IAIMob;
import thut.api.entity.genetics.Alleles;
import thut.api.entity.genetics.IMobGenetics;
import thut.api.world.mobs.data.Data;
import thut.core.common.world.mobs.data.DataSync_Impl;

/** This class will store the various stats of the pokemob as Alleles, and will
 * provider quick getters and setters for the genes. */
public abstract class EntityGeneticsPokemob extends EntityTameablePokemob
{
    Alleles genesColour;

    public EntityGeneticsPokemob(World world)
    {
        super(world);
    }

    @Override
    public int[] getRGBA()
    {
        if (genesColour == null)
        {
            IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
            if (genes == null) throw new RuntimeException("This should not be called here");
            genesColour = genes.getAlleles().get(COLOURGENE);
            if (genesColour == null)
            {
                genesColour = new Alleles();
                genes.getAlleles().put(COLOURGENE, genesColour);
            }
            if (genesColour.getAlleles()[0] == null)
            {
                ColourGene gene = new ColourGene();
                genesColour.getAlleles()[0] = gene.getMutationRate() > rand.nextFloat() ? gene.mutate() : gene;
                genesColour.getAlleles()[1] = gene.getMutationRate() > rand.nextFloat() ? gene.mutate() : gene;
                genesColour.refreshExpressed();
            }
        }
        return genesColour.getExpressed().getValue();
    }

    @Override
    public void setRGBA(int... colours)
    {
        int[] rgba = getRGBA();
        for (int i = 0; i < colours.length && i < rgba.length; i++)
        {
            rgba[i] = colours[i];
        }
    }

    public void onGenesChanged()
    {
        genesColour = null;
        getRGBA();
    }

    /** Use this for anything that does not change or need to be updated. */
    @Override
    public void writeSpawnData(ByteBuf data)
    {
        // Write the dataSync stuff
        List<Data<?>> data_list = pokemobCap.dataSync().getAll();
        byte num = (byte) (data_list.size());
        data.writeByte(num);
        for (int i = 0; i < num; i++)
        {
            Data<?> val = data_list.get(i);
            data.writeInt(val.getUID());
            val.write(data);
        }

        this.pokemobCap.updateHealth();
        this.onGenesChanged();
        pokemobCap.onGenesChanged();
        IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
        PacketBuffer buffer = new PacketBuffer(data);
        NBTTagList list = (NBTTagList) IMobGenetics.GENETICS_CAP.writeNBT(genes, null);
        NBTTagCompound nbt = new NBTTagCompound();
        IAIMob ai = getCapability(IAIMob.THUTMOBAI, null);
        if (ai instanceof AICapWrapper)
        {
            AICapWrapper wrapper = (AICapWrapper) ai;
            nbt.setTag("a", wrapper.serializeNBT());
        }
        nbt.setTag("p", pokemobCap.writePokemobData());
        nbt.setTag("g", list);
        buffer.writeCompoundTag(nbt);
        nbt = getEntityData().getCompoundTag("url_model");
        buffer.writeCompoundTag(nbt);
    }

    @Override
    public void readSpawnData(ByteBuf data)
    {
        // Read the datasync stuff
        List<Data<?>> data_list = Lists.newArrayList();
        byte num = data.readByte();
        if (num > 0)
        {
            for (int i = 0; i < num; i++)
            {
                int uid = data.readInt();
                try
                {
                    Data<?> val = DataSync_Impl.makeData(uid);
                    val.read(data);
                    data_list.add(val);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            pokemobCap.dataSync().update(data_list);
        }

        PacketBuffer buffer = new PacketBuffer(data);
        try
        {
            NBTTagCompound tag = buffer.readCompoundTag();
            NBTTagList list = (NBTTagList) tag.getTag("g");
            IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
            IMobGenetics.GENETICS_CAP.readNBT(genes, null, list);
            pokemobCap.readPokemobData(tag.getCompoundTag("p"));
            pokemobCap.onGenesChanged();
            this.onGenesChanged();
            IAIMob ai = getCapability(IAIMob.THUTMOBAI, null);
            if (ai instanceof AICapWrapper)
            {
                AICapWrapper wrapper = (AICapWrapper) ai;
                wrapper.deserializeNBT(tag.getCompoundTag("a"));
            }
            tag = buffer.readCompoundTag();
            if (!tag.hasNoTags())
            {
                getEntityData().setTag("url_model", tag);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        super.readEntityFromNBT(nbttagcompound);
        onGenesChanged();
    }
}
