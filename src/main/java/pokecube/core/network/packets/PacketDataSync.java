package pokecube.core.network.packets;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

import javax.xml.ws.handler.MessageContext;

import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import pokecube.core.PokecubeCore;
import pokecube.core.client.gui.GuiInfoMessages;
import pokecube.core.handlers.Config;
import pokecube.core.handlers.SyncConfig;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.utils.PokecubeSerializer;
import thut.core.common.config.Configure;
import thut.core.common.handlers.PlayerDataHandler;
import thut.core.common.handlers.PlayerDataHandler.PlayerData;
import thut.core.common.handlers.PlayerDataHandler.PlayerDataManager;

public class PacketDataSync implements IMessage, IMessageHandler<PacketDataSync, IMessage>
{
    public CompoundNBT data = new CompoundNBT();

    public static void sendInitPacket(PlayerEntity player, String dataType)
    {
        PlayerDataManager manager = PlayerDataHandler.getInstance().getPlayerData(player);
        PlayerData data = manager.getData(dataType);
        PacketDataSync packet = new PacketDataSync();
        packet.data.putString("type", dataType);
        CompoundNBT tag1 = new CompoundNBT();
        data.writeToNBT(tag1);
        packet.data.put("data", tag1);
        PokecubeMod.packetPipeline.sendTo(packet, (ServerPlayerEntity) player);
        PlayerDataHandler.getInstance().save(player.getCachedUniqueIdString());
    }

    public static void sendInitHandshake(PlayerEntity player)
    {
        PacketDataSync packet = new PacketDataSync();
        packet.data.putBoolean("I", true);
        if (FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer())
            packet.data.put("C", writeConfigs());
        PokecubeMod.packetPipeline.sendTo(packet, (ServerPlayerEntity) player);
    }

    private static CompoundNBT writeConfigs()
    {
        CompoundNBT ret = new CompoundNBT();
        Config defaults = PokecubeCore.instance.getConfig();
        CompoundNBT longs = new CompoundNBT();
        CompoundNBT ints = new CompoundNBT();
        CompoundNBT bools = new CompoundNBT();
        CompoundNBT floats = new CompoundNBT();
        CompoundNBT doubles = new CompoundNBT();
        CompoundNBT strings = new CompoundNBT();
        CompoundNBT intarrs = new CompoundNBT();
        CompoundNBT stringarrs = new CompoundNBT();
        for (Field f : Config.class.getDeclaredFields())
        {
            SyncConfig c = f.getAnnotation(SyncConfig.class);
            Configure conf = f.getAnnotation(Configure.class);
            /** client stuff doesn't need to by synced, clients will use the
             * dummy config while on servers. */
            if (conf != null && conf.category().equals(Config.client)) continue;
            if (c != null)
            {
                try
                {
                    f.setAccessible(true);
                    if ((f.getType() == Long.TYPE) || (f.getType() == Long.class))
                    {
                        long defaultValue = f.getLong(defaults);
                        longs.put(f.getName(), new LongNBT(defaultValue));
                    }
                    else if (f.getType() == String.class)
                    {
                        String defaultValue = (String) f.get(defaults);
                        strings.put(f.getName(), new StringNBT(defaultValue));
                    }
                    else if ((f.getType() == Integer.TYPE) || (f.getType() == Integer.class))
                    {
                        int defaultValue = f.getInt(defaults);
                        ints.put(f.getName(), new IntNBT(defaultValue));
                    }
                    else if ((f.getType() == Float.TYPE) || (f.getType() == Float.class))
                    {
                        float defaultValue = f.getFloat(defaults);
                        floats.put(f.getName(), new FloatNBT(defaultValue));
                    }
                    else if ((f.getType() == Double.TYPE) || (f.getType() == Double.class))
                    {
                        double defaultValue = f.getDouble(defaults);
                        doubles.put(f.getName(), new DoubleNBT(defaultValue));
                    }
                    else if ((f.getType() == Boolean.TYPE) || (f.getType() == Boolean.class))
                    {
                        boolean defaultValue = f.getBoolean(defaults);
                        bools.put(f.getName(), new ByteNBT((byte) (defaultValue ? 1 : 0)));
                    }
                    else
                    {
                        Object o = f.get(defaults);
                        if (o instanceof String[])
                        {
                            String[] defaultValue = (String[]) o;
                            ListNBT arr = new ListNBT();
                            for (String s : defaultValue)
                                arr.appendTag(new StringNBT(s));
                            stringarrs.put(f.getName(), arr);
                        }
                        else if (o instanceof int[])
                        {
                            int[] defaultValue = (int[]) o;
                            intarrs.put(f.getName(), new IntArrayNBT(defaultValue));
                        }
                        else System.err.println("Unknown Type " + f.getType() + " " + f.getName() + " " + o.getClass());
                    }
                }
                catch (IllegalArgumentException e)
                {
                    e.printStackTrace();
                }
                catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }
            }
        }
        if (!longs.hasNoTags()) ret.put("L", longs);
        if (!ints.hasNoTags()) ret.put("I", ints);
        if (!bools.hasNoTags()) ret.put("B", bools);
        if (!floats.hasNoTags()) ret.put("F", floats);
        if (!doubles.hasNoTags()) ret.put("D", doubles);
        if (!strings.hasNoTags()) ret.put("S", strings);
        if (!intarrs.hasNoTags()) ret.put("A", intarrs);
        if (!stringarrs.hasNoTags()) ret.put("R", stringarrs);
        return ret;
    }

    public PacketDataSync()
    {
    }

    @Override
    public IMessage onMessage(final PacketDataSync message, final MessageContext ctx)
    {
        PokecubeCore.proxy.getMainThreadListener().addScheduledTask(new Runnable()
        {
            @Override
            public void run()
            {
                processMessage(ctx, message);
            }
        });
        return null;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        try
        {
            data = buffer.readCompoundTag();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeCompoundTag(data);
    }

    void processMessage(MessageContext ctx, PacketDataSync message)
    {
        PlayerEntity player;
        if (ctx.side == Dist.CLIENT)
        {
            player = PokecubeCore.getPlayer(null);
            if (message.data.getBoolean("I"))
            {
                PokecubeSerializer.getInstance().clearInstance();
                GuiInfoMessages.clear();
                try
                {
                    if (FMLCommonHandler.instance().getMinecraftServerInstance() == null)
                        syncConfigs(message.data.getCompound("C"));
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, "Error with config Sync: " + message.data.getCompound("C"), e);
                }
            }
            else
            {
                PlayerDataManager manager = PlayerDataHandler.getInstance().getPlayerData(player);
                manager.getData(message.data.getString("type")).readFromNBT(message.data.getCompound("data"));
            }
        }
    }

    private void syncConfigs(CompoundNBT tag) throws Exception
    {
        Config defaults = PokecubeCore.instance.getConfig();
        Field f;
        if (tag.hasKey("L"))
        {
            CompoundNBT longs = tag.getCompound("L");
            for (String s : longs.getKeySet())
            {
                try
                {
                    long l = longs.getLong(s);
                    f = defaults.getClass().getDeclaredField(s);
                    f.setAccessible(true);
                    defaults.updateField(f, l + "");
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, s, e);
                }
            }
        }
        if (tag.hasKey("I"))
        {
            CompoundNBT ints = tag.getCompound("I");
            for (String s : ints.getKeySet())
            {
                try
                {
                    int l = ints.getInteger(s);
                    f = defaults.getClass().getDeclaredField(s);
                    f.setAccessible(true);
                    defaults.updateField(f, l + "");
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, s, e);
                }
            }
        }
        if (tag.hasKey("B"))
        {
            CompoundNBT bools = tag.getCompound("B");
            for (String s : bools.getKeySet())
            {
                try
                {
                    boolean l = bools.getByte(s) != 0;
                    f = defaults.getClass().getDeclaredField(s);
                    f.setAccessible(true);
                    defaults.updateField(f, l + "");
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, s, e);
                }
            }
        }
        if (tag.hasKey("F"))
        {
            CompoundNBT floats = tag.getCompound("F");
            for (String s : floats.getKeySet())
            {
                try
                {
                    float l = floats.getFloat(s);
                    f = defaults.getClass().getDeclaredField(s);
                    f.setAccessible(true);
                    defaults.updateField(f, l + "");
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, s, e);
                }
            }
        }
        if (tag.hasKey("D"))
        {
            CompoundNBT doubles = tag.getCompound("D");
            for (String s : doubles.getKeySet())
            {
                try
                {
                    double l = doubles.getDouble(s);
                    f = defaults.getClass().getDeclaredField(s);
                    f.setAccessible(true);
                    defaults.updateField(f, l + "");
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, s, e);
                }
            }
        }
        if (tag.hasKey("S"))
        {
            CompoundNBT strings = tag.getCompound("S");
            for (String s : strings.getKeySet())
            {
                try
                {
                    String l = strings.getString(s);
                    f = defaults.getClass().getDeclaredField(s);
                    f.setAccessible(true);
                    defaults.updateField(f, l + "");
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, s, e);
                }
            }
        }
        if (tag.hasKey("A"))
        {
            CompoundNBT intarrs = tag.getCompound("A");
            for (String s : intarrs.getKeySet())
            {
                try
                {
                    int[] l = intarrs.getIntArray(s);
                    f = defaults.getClass().getDeclaredField(s);
                    f.setAccessible(true);
                    defaults.updateField(f, l);
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, s, e);
                }
            }
        }
        if (tag.hasKey("R"))
        {
            CompoundNBT stringarrs = tag.getCompound("R");
            for (String s : stringarrs.getKeySet())
            {
                try
                {
                    ListNBT list = (ListNBT) stringarrs.getTag(s);
                    List<String> vars = Lists.newArrayList();
                    for (int i = 0; i < list.size(); i++)
                        vars.add(list.getStringTagAt(i));
                    String[] arr = vars.toArray(new String[0]);
                    f = defaults.getClass().getDeclaredField(s);
                    f.setAccessible(true);
                    defaults.updateField(f, arr);
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, s, e);
                }
            }
        }
    }
}
