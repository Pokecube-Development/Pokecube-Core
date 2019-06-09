package pokecube.core.network.packets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import pokecube.core.PokecubeCore;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.SpawnData;
import pokecube.core.database.PokedexEntry.SpawnData.SpawnEntry;
import pokecube.core.database.SpawnBiomeMatcher;
import pokecube.core.database.SpawnBiomeMatcher.SpawnCheck;
import pokecube.core.handlers.Config;
import pokecube.core.handlers.PokecubePlayerDataHandler;
import pokecube.core.handlers.PokedexInspector;
import pokecube.core.handlers.playerdata.PokecubePlayerStats;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.commandhandlers.TeleportHandler;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.utils.PokecubeSerializer;
import pokecube.core.utils.PokecubeSerializer.TeleDest;
import pokecube.core.world.dimensions.secretpower.SecretBaseManager;
import pokecube.core.world.dimensions.secretpower.SecretBaseManager.Coordinate;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;
import thut.api.terrain.BiomeDatabase;
import thut.api.terrain.BiomeType;
import thut.api.terrain.TerrainManager;
import thut.core.common.handlers.PlayerDataHandler;

public class PacketPokedex implements IMessage, IMessageHandler<PacketPokedex, IMessage>
{
    public static final byte                           REORDER      = -10;
    public static final byte                           INSPECTMOB   = -9;
    public static final byte                           SETWATCHPOKE = -8;
    public static final byte                           REQUESTLOC   = -7;
    public static final byte                           REQUESTMOB   = -6;
    public static final byte                           REQUEST      = -5;
    public static final byte                           INSPECT      = -4;
    public static final byte                           BASERADAR    = -3;
    public static final byte                           REMOVE       = -2;
    public static final byte                           RENAME       = -1;

    public static List<String>                         values       = Lists.newArrayList();
    public static List<SpawnBiomeMatcher>              selectedMob  = Lists.newArrayList();
    public static Map<PokedexEntry, SpawnBiomeMatcher> selectedLoc  = Maps.newHashMap();

    public static void sendInspectPacket(IPokemob pokemob)
    {
        PacketPokedex packet = new PacketPokedex(PacketPokedex.INSPECTMOB);
        PlayerDataHandler.getInstance().getPlayerData(PokecubeCore.getPlayer(null))
                .getData(PokecubePlayerStats.class).inspect(PokecubeCore.getPlayer(null), pokemob);
        packet.data.setInteger("V", pokemob.getEntity().getEntityId());
        PokecubePacketHandler.sendToServer(packet);
    }

    public static void updateWatchEntry(PokedexEntry entry)
    {
        String name = PokecubePlayerDataHandler.getCustomDataTag(PokecubeCore.getPlayer(null)).getString("WEntry");
        if (Database.getEntry(name) == entry) return;
        PacketPokedex packet = new PacketPokedex(PacketPokedex.SETWATCHPOKE);
        PokecubePlayerDataHandler.getCustomDataTag(PokecubeCore.getPlayer(null)).putString("WEntry", entry.getName());
        packet.data.putString("V", entry.getName());
        PokecubePacketHandler.sendToServer(packet);
    }

    public static void sendLocationSpawnsRequest()
    {
        selectedLoc.clear();
        PacketPokedex packet = new PacketPokedex(PacketPokedex.REQUESTLOC);
        PokecubePacketHandler.sendToServer(packet);
    }

    public static void sendSpecificSpawnsRequest(PokedexEntry entry)
    {
        selectedMob.clear();
        PacketPokedex packet = new PacketPokedex(PacketPokedex.REQUESTMOB);
        packet.data.putString("V", entry.getName());
        PokecubePacketHandler.sendToServer(packet);
    }

    public static void sendRenameTelePacket(String newName, int index)
    {
        PacketPokedex packet = new PacketPokedex();
        packet.message = RENAME;
        packet.data.putString("N", newName);
        packet.data.setInteger("I", index);
        PokecubePacketHandler.sendToServer(packet);
    }

    public static void sendReorderTelePacket(int index1, int index2)
    {
        PacketPokedex packet = new PacketPokedex();
        packet.message = REORDER;
        packet.data.setInteger("2", index2);
        packet.data.setInteger("1", index1);
        PokecubePacketHandler.sendToServer(packet);
        TeleportHandler.swapTeleports(PokecubeCore.getPlayer(null).getCachedUniqueIdString(), index1, index2);
    }

    public static void sendRemoveTelePacket(int index)
    {
        PacketPokedex packet = new PacketPokedex();
        packet.message = REMOVE;
        packet.data.setInteger("I", index);
        PokecubePacketHandler.sendToServer(packet);
    }

    public static void sendChangePagePacket(byte page, boolean mode, PokedexEntry selected)
    {
        PacketPokedex packet = new PacketPokedex();
        packet.message = page;
        packet.data.putBoolean("M", mode);
        if (selected != null) packet.data.putString("F", selected.getName());
        PokecubePacketHandler.sendToServer(packet);
    }

    public static void sendInspectPacket(boolean reward, String lang)
    {
        PacketPokedex packet = new PacketPokedex();
        packet.message = INSPECT;
        packet.data.putBoolean("R", reward);
        packet.data.putString("L", lang);
        PokecubePacketHandler.sendToServer(packet);
    }

    public static void sendSecretBaseInfoPacket(PlayerEntity player, boolean watch)
    {
        PacketPokedex packet = new PacketPokedex();
        BlockPos pos = player.getPosition();
        Coordinate here = new Coordinate(pos.getX(), pos.getY(), pos.getZ(), player.dimension);
        ListNBT list = new ListNBT();
        for (Coordinate c : SecretBaseManager.getNearestBases(here, PokecubeMod.core.getConfig().baseRadarRange))
        {
            list.appendTag(c.writeToNBT());
        }
        packet.data.put("B", list);
        packet.data.putBoolean("M", watch);
        packet.data.setInteger("R", PokecubeMod.core.getConfig().baseRadarRange);
        List<Vector4> meteors = PokecubeSerializer.getInstance().meteors;
        if (!meteors.isEmpty())
        {
            double dist = Double.MAX_VALUE;
            Vector4 closest = null;
            Vector4 posv = new Vector4(player);
            double check = 0;
            for (Vector4 loc : meteors)
            {
                if (loc.w != posv.w) continue;
                check = PokecubeSerializer.distSq(loc, posv);
                if (check < dist)
                {
                    closest = loc;
                    dist = check;
                }
            }
            if (closest != null)
            {
                CompoundNBT tag = new CompoundNBT();
                closest.writeToNBT(tag);
                packet.data.put("V", tag);
            }
        }
        packet.message = BASERADAR;
        PokecubePacketHandler.sendToClient(packet, player);
    }

    byte                  message;
    public CompoundNBT data = new CompoundNBT();

    public PacketPokedex()
    {
    }

    public PacketPokedex(byte message)
    {
        this.message = message;
    }

    @Override
    public IMessage onMessage(final PacketPokedex message, final MessageContext ctx)
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
        message = buf.readByte();
        if (buf.isReadable())
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
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeByte(message);
        if (!data.hasNoTags())
        {
            PacketBuffer buffer = new PacketBuffer(buf);
            buffer.writeCompoundTag(data);
        }
    }

    void processMessage(MessageContext ctx, PacketPokedex message)
    {
        final PlayerEntity player;
        if (ctx.side == Dist.CLIENT)
        {
            player = PokecubeCore.getPlayer(null);
        }
        else
        {
            player = ctx.getServerHandler().player;
        }
        if (message.message == INSPECTMOB)
        {
            if (ctx.side == Dist.DEDICATED_SERVER)
            {
                Entity mob = PokecubeMod.core.getEntityProvider().getEntity(player.getEntityWorld(),
                        message.data.getInt("V"), true);
                IPokemob pokemob = CapabilityPokemob.getPokemobFor(mob);
                if (pokemob != null) PlayerDataHandler.getInstance().getPlayerData(player)
                        .getData(PokecubePlayerStats.class).inspect(player, pokemob);
            }
            return;
        }

        if (message.message == SETWATCHPOKE)
        {
            if (ctx.side == Dist.DEDICATED_SERVER)
            {
                PokedexEntry entry = Database.getEntry(message.data.getString("V"));
                if (entry != null)
                    PokecubePlayerDataHandler.getCustomDataTag(player).putString("WEntry", entry.getName());
            }
            return;
        }

        TypeAdapter<QName> adapter = new TypeAdapter<QName>()
        {
            @Override
            public void write(JsonWriter out, QName value) throws IOException
            {
                out.value(value.toString());
            }

            @Override
            public QName read(JsonReader in) throws IOException
            {
                return new QName(in.nextString());
            }
        };
        ExclusionStrategy spawn_matcher = new ExclusionStrategy()
        {
            @Override
            public boolean shouldSkipField(FieldAttributes f)
            {
                switch (f.getName())
                {
                case "validBiomes":
                    return true;
                case "validSubBiomes":
                    return true;
                case "blackListBiomes":
                    return true;
                case "blackListSubBiomes":
                    return true;
                case "additionalConditions":
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz)
            {
                return false;
            }
        };

        Gson gson = new GsonBuilder().registerTypeAdapter(QName.class, adapter).setExclusionStrategies(spawn_matcher)
                .create();

        if (message.message == REQUESTLOC)
        {
            if (ctx.side == Dist.DEDICATED_SERVER)
            {
                final Map<PokedexEntry, Float> rates = Maps.newHashMap();
                final Vector3 pos = Vector3.getNewVector().set(player);
                final SpawnCheck checker = new SpawnCheck(pos, player.getEntityWorld());
                ArrayList<PokedexEntry> names = new ArrayList<PokedexEntry>();
                for (PokedexEntry e : Database.spawnables)
                {
                    if (e.getSpawnData().getMatcher(checker, false) != null)
                    {
                        names.add(e);
                    }
                }

                float total = 0;
                Map<PokedexEntry, SpawnBiomeMatcher> matchers = Maps.newHashMap();
                for (PokedexEntry e : names)
                {
                    SpawnBiomeMatcher matcher = e.getSpawnData().getMatcher(checker, false);
                    matchers.put(e, matcher);
                    float val = e.getSpawnData().getWeight(matcher);
                    float min = e.getSpawnData().getMin(matcher);
                    float num = min + (e.getSpawnData().getMax(matcher) - min) / 2;
                    val *= num;
                    total += val;
                    rates.put(e, val);
                }
                for (PokedexEntry e : names)
                {
                    float val = rates.get(e) / total;
                    rates.put(e, val);
                }
                PacketPokedex packet = new PacketPokedex(REQUESTLOC);
                int n = 0;
                CompoundNBT data = new CompoundNBT();
                for (PokedexEntry e : names)
                {
                    SpawnBiomeMatcher matcher = matchers.get(e);
                    matcher.spawnRule.values.put(new QName("Local_Rate"), rates.get(e) + "");
                    data.putString("e" + n, e.getName());
                    data.putString("" + n, gson.toJson(matcher));
                    n++;
                }
                packet.data.put("V", data);

                PokedexEntry entry = Database
                        .getEntry(PokecubePlayerDataHandler.getCustomDataTag(player).getString("WEntry"));
                if (entry != null) packet.data.putString("E", entry.getName());
                PokecubeMod.packetPipeline.sendTo(packet, (ServerPlayerEntity) player);
            }
            else
            {
                selectedLoc.clear();
                CompoundNBT data = message.data.getCompound("V");
                int n = data.getKeySet().size() / 2;
                for (int i = 0; i < n; i++)
                {
                    selectedLoc.put(Database.getEntry(data.getString("e" + i)),
                            gson.fromJson(data.getString("" + i), SpawnBiomeMatcher.class));
                }
                if (message.data.hasKey("E"))
                {
                    PokecubePlayerDataHandler.getCustomDataTag(player).putString("WEntry", message.data.getString("E"));
                }
            }
            return;
        }
        else if (message.message == REQUESTMOB)
        {
            if (ctx.side == Dist.DEDICATED_SERVER)
            {
                PacketPokedex packet = new PacketPokedex(REQUESTMOB);
                PokedexEntry entry = Database.getEntry(message.data.getString("V"));
                if (entry.getSpawnData() == null && entry.getChild() != entry)
                {
                    PokedexEntry child;
                    if ((child = entry.getChild()).getSpawnData() != null)
                    {
                        entry = child;
                    }
                }
                SpawnData data = entry.getSpawnData();
                boolean valid = data != null;
                if (valid)
                {
                    int n = 0;
                    for (SpawnBiomeMatcher matcher : data.matchers.keySet())
                    {
                        String value = gson.toJson(matcher);
                        packet.data.putString("" + n++, value);
                    }
                }
                PokecubeMod.packetPipeline.sendTo(packet, (ServerPlayerEntity) player);
            }
            else
            {
                selectedMob.clear();
                int n = message.data.getKeySet().size();
                for (int i = 0; i < n; i++)
                {
                    selectedMob.add(gson.fromJson(message.data.getString("" + i), SpawnBiomeMatcher.class));
                }
            }
            return;
        }

        if (message.message == REQUEST || (ctx.side == Dist.DEDICATED_SERVER && message.message >= 0))
        {
            if (ctx.side == Dist.CLIENT)
            {
                values.clear();
                int n = message.data.getKeySet().size();
                for (int i = 0; i < n; i++)
                {
                    values.add(message.data.getString("" + i));
                }
            }
            else
            {
                boolean mode = message.data.getBoolean("M");
                PacketPokedex packet = new PacketPokedex(REQUEST);
                if (!mode)
                {
                    final Map<PokedexEntry, Float> rates = Maps.newHashMap();
                    final Vector3 pos = Vector3.getNewVector().set(player);
                    final SpawnCheck checker = new SpawnCheck(pos, player.getEntityWorld());
                    ArrayList<PokedexEntry> names = new ArrayList<PokedexEntry>();
                    for (PokedexEntry e : Database.spawnables)
                    {
                        if (e.getSpawnData().getMatcher(checker, false) != null)
                        {
                            names.add(e);
                        }
                    }
                    Collections.sort(names, new Comparator<PokedexEntry>()
                    {
                        @Override
                        public int compare(PokedexEntry o1, PokedexEntry o2)
                        {
                            float rate1 = o1.getSpawnData().getWeight(o1.getSpawnData().getMatcher(checker, false))
                                    * 10e5f;
                            float rate2 = o2.getSpawnData().getWeight(o2.getSpawnData().getMatcher(checker, false))
                                    * 10e5f;
                            return (int) (-rate1 + rate2);
                        }
                    });
                    float total = 0;
                    for (PokedexEntry e : names)
                    {
                        SpawnBiomeMatcher matcher = e.getSpawnData().getMatcher(checker, false);
                        float val = e.getSpawnData().getWeight(matcher);
                        float min = e.getSpawnData().getMin(matcher);
                        float num = min + (e.getSpawnData().getMax(matcher) - min) / 2;
                        val *= num;
                        total += val;
                        rates.put(e, val);
                    }
                    for (PokedexEntry e : names)
                    {
                        float val = rates.get(e) * 100 / total;
                        rates.put(e, val);
                    }
                    int biome = TerrainManager.getInstance().getTerrainForEntity(player).getBiome(pos);
                    packet.data.putString("0", "" + biome);
                    packet.data.putString("1", BiomeDatabase.getReadableNameFromType(biome));
                    for (int i = 0; i < names.size(); i++)
                    {
                        PokedexEntry e = names.get(i);
                        packet.data.putString("" + (i + 2), e.getUnlocalizedName() + "`" + rates.get(e));
                    }
                }
                else
                {
                    List<String> biomes = Lists.newArrayList();
                    PokedexEntry entry = Database.getEntry(message.data.getString("F"));
                    if (entry.getSpawnData() == null && entry.getChild() != entry)
                    {
                        PokedexEntry child;
                        if ((child = entry.getChild()).getSpawnData() != null)
                        {
                            entry = child;
                        }
                    }
                    SpawnData data = entry.getSpawnData();
                    if (data != null)
                    {
                        boolean hasBiomes = false;
                        Map<SpawnBiomeMatcher, SpawnEntry> matchers = data.matchers;
                        for (SpawnBiomeMatcher matcher : matchers.keySet())
                        {
                            String biomeString = matcher.spawnRule.values.get(SpawnBiomeMatcher.BIOMES);
                            String typeString = matcher.spawnRule.values.get(SpawnBiomeMatcher.TYPES);
                            if (biomeString != null) hasBiomes = true;
                            else if (typeString != null)
                            {
                                String[] args = typeString.split(",");
                                BiomeType subBiome = null;
                                for (String s : args)
                                {
                                    for (BiomeType b : BiomeType.values())
                                    {
                                        if (b.name.replaceAll(" ", "").equalsIgnoreCase(s))
                                        {
                                            subBiome = b;
                                            break;
                                        }
                                    }
                                    if (subBiome == null) hasBiomes = true;
                                    subBiome = null;
                                    if (hasBiomes) break;
                                }
                            }
                            if (hasBiomes) break;
                        }
                        if (hasBiomes) for (ResourceLocation key : Biome.REGISTRY.getKeys())
                        {
                            Biome b = Biome.REGISTRY.getObject(key);
                            if (b != null)
                            {
                                if (data.isValid(b))
                                {
                                    biomes.add(ReflectionHelper.getPrivateValue(Biome.class, b, "biomeName",
                                            "field_76791_y", "y"));
                                }
                            }
                        }
                        for (BiomeType b : BiomeType.values())
                        {
                            if (data.isValid(b))
                            {
                                biomes.add(b.readableName);
                            }
                        }
                        for (int i = 0; i < biomes.size(); i++)
                        {
                            packet.data.putString("" + i, biomes.get(i));
                        }
                    }
                }
                PokecubeMod.packetPipeline.sendTo(packet, (ServerPlayerEntity) player);
            }
            if (message.message == REQUEST) return;
        }
        if (message.message == REORDER)
        {
            int index1 = message.data.getInt("1");
            int index2 = message.data.getInt("2");
            PlayerDataHandler.getInstance().save(player.getCachedUniqueIdString());
            TeleportHandler.swapTeleports(player.getCachedUniqueIdString(), index1, index2);
            PacketDataSync.sendInitPacket(player, "pokecube-data");
            return;
        }
        if (message.message == REMOVE)
        {
            int index = message.data.getInt("I");
            TeleDest loc = TeleportHandler.getTeleport(player.getCachedUniqueIdString(), index);
            TeleportHandler.unsetTeleport(index, player.getCachedUniqueIdString());
            player.sendMessage(new StringTextComponent("Deleted " + loc.getName()));
            PlayerDataHandler.getInstance().save(player.getCachedUniqueIdString());
            PacketDataSync.sendInitPacket(player, "pokecube-data");
            return;
        }
        if (message.message == RENAME)
        {
            String name = message.data.getString("N");
            int index = message.data.getInt("I");
            TeleportHandler.renameTeleport(player.getCachedUniqueIdString(), index, name);
            player.sendMessage(new StringTextComponent("Set teleport as " + name));
            PlayerDataHandler.getInstance().save(player.getCachedUniqueIdString());
            PacketDataSync.sendInitPacket(player, "pokecube-data");
            return;
        }
        if (message.message == BASERADAR)
        {
            boolean mode = message.data.getBoolean("M");
            player.openGui(PokecubeCore.instance, !mode ? Config.GUIPOKEDEX_ID : Config.GUIPOKEWATCH_ID,
                    player.getEntityWorld(), 0, 0, 0);
            if (message.data.hasKey("V"))
                pokecube.core.client.gui.watch.SecretBaseRadarPage.closestMeteor = new Vector4(
                        message.data.getCompound("V"));
            else pokecube.core.client.gui.watch.SecretBaseRadarPage.closestMeteor = null;
            if (!message.data.hasKey("B") || !(message.data.getTag("B") instanceof ListNBT)) return;
            ListNBT list = (ListNBT) message.data.getTag("B");
            pokecube.core.client.gui.watch.SecretBaseRadarPage.bases.clear();
            for (int i = 0; i < list.size(); i++)
            {
                CompoundNBT tag = list.getCompound(i);
                Coordinate c = Coordinate.readNBT(tag);
                pokecube.core.client.gui.watch.SecretBaseRadarPage.bases.add(c);
            }
            pokecube.core.client.gui.watch.SecretBaseRadarPage.baseRange = message.data.getInt("R");
            return;
        }
        if (message.message == INSPECT)
        {
            boolean reward = message.data.getBoolean("R");
            String lang = message.data.getString("L");
            PokecubePlayerDataHandler.getCustomDataTag(player).putString("lang", lang);
            boolean inspected = PokedexInspector.inspect(player, reward);

            if (!reward)
            {
                if (inspected)
                {
                    player.sendMessage(new TranslationTextComponent("pokedex.inspect.available"));
                }
            }
            else
            {
                if (!inspected)
                {
                    player.sendMessage(new TranslationTextComponent("pokedex.inspect.nothing"));
                }
                player.closeScreen();
            }
            return;
        }
        boolean mode = message.data.getBoolean("M");
        if (!player.getHeldItemMainhand().hasTag())
        {
            player.getHeldItemMainhand().put(new CompoundNBT());
        }
        player.getHeldItemMainhand().getTag().putBoolean("M", mode);
        player.getHeldItemMainhand().getTag().putString("F", message.data.getString("F"));
        player.getHeldItemMainhand().setItemDamage(message.message);
    }
}
