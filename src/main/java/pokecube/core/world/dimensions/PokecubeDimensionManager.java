package pokecube.core.world.dimensions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.network.play.server.SPacketWorldBorder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import pokecube.core.database.worldgen.WorldgenHandler;
import pokecube.core.database.worldgen.WorldgenHandler.CustomDim;
import pokecube.core.handlers.PokecubePlayerDataHandler;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.network.packets.PacketSyncDimIds;
import pokecube.core.world.dimensions.custom.CustomDimensionManager;
import pokecube.core.world.dimensions.secretpower.DimensionSecretBase;
import thut.api.entity.Transporter;
import thut.api.maths.Vector3;

public class PokecubeDimensionManager
{
    private static PokecubeDimensionManager INSTANCE;
    public static DimensionType             SECRET_BASE_TYPE;

    public static boolean createNewSecretBaseDimension(int dim, boolean reset)
    {
        if (!DimensionManager.isDimensionRegistered(dim)) DimensionManager.registerDimension(dim, SECRET_BASE_TYPE);
        ServerWorld world1 = DimensionManager.getWorld(dim);
        boolean registered = true;
        if (world1 == null)
        {
            DimensionManager.initDimension(dim);
            world1 = DimensionManager.getWorld(dim);
            registered = getInstance().dims.contains(dim);
            if (!registered)
            {
                registerDim(dim);
                PokecubeMod.core.getConfig().save();
                getInstance().syncToAll();
            }
        }
        if (!registered || reset)
        {
            for (int i = -2; i <= 2; i++)
            {
                for (int j = -2; j <= 2; j++)
                {
                    for (int k = -2; k <= 4; k++)
                    {
                        world1.setBlockState(new BlockPos(i, k + 63, j),
                                k <= 0 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState());
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static int getDimensionForPlayer(PlayerEntity player)
    {
        return getDimensionForPlayer(player.getCachedUniqueIdString());
    }

    public static int getDimensionForPlayer(String player)
    {
        int dim = 0;
        CompoundNBT tag = PokecubePlayerDataHandler.getCustomDataTag(player);
        if (tag.hasKey("secretPowerDimID"))
        {
            dim = tag.getInt("secretPowerDimID");
            if (!getInstance().dimOwners.containsKey(dim)) getInstance().dimOwners.put(dim, player);
        }
        else
        {
            if (PokecubeMod.debug) PokecubeMod.log("Creating Base DimensionID for " + player);
            dim = DimensionManager.getNextFreeDimId();
            tag.putInt("secretPowerDimID", dim);
            PokecubePlayerDataHandler.saveCustomData(player);
            getInstance().dimOwners.put(dim, player);
        }
        return dim;
    }

    public static BlockPos getBaseEntrance(PlayerEntity player, int dim)
    {
        return getBaseEntrance(player.getCachedUniqueIdString(), dim);
    }

    public static BlockPos getBaseEntrance(String player, int dim)
    {
        BlockPos ret = null;
        CompoundNBT tag = PokecubePlayerDataHandler.getCustomDataTag(player);
        if (tag.hasKey("secretBase"))
        {
            CompoundNBT base = tag.getCompound("secretBase");
            if (base.hasKey(dim + "X"))
                ret = new BlockPos(base.getInt(dim + "X"), base.getInt(dim + "Y"), base.getInt(dim + "Z"));
        }
        return ret;
    }

    public static void setBaseEntrance(PlayerEntity player, int dim, BlockPos pos)
    {
        setBaseEntrance(player.getCachedUniqueIdString(), dim, pos);
    }

    public static void setBaseEntrance(String player, int dim, BlockPos pos)
    {
        CompoundNBT tag = PokecubePlayerDataHandler.getCustomDataTag(player);
        CompoundNBT base;
        if (tag.hasKey("secretBase"))
        {
            base = tag.getCompound("secretBase");
        }
        else
        {
            base = new CompoundNBT();
        }
        base.putInt(dim + "X", pos.getX());
        base.putInt(dim + "Y", pos.getY());
        base.putInt(dim + "Z", pos.getZ());
        tag.put("secretBase", base);
        PokecubePlayerDataHandler.saveCustomData(player);
    }

    public static boolean initPlayerBase(String player, BlockPos pos, int entranceDimension)
    {
        int dim = getDimensionForPlayer(player);
        if (!DimensionManager.isDimensionRegistered(dim))
        {
            if (createNewSecretBaseDimension(dim, false))
            {
                setBaseEntrance(player, entranceDimension, pos);
                return true;
            }
        }
        else if (DimensionManager.getWorld(dim) == null) { return createNewSecretBaseDimension(dim, false); }
        return false;
    }

    public static void sendToBase(String baseOwner, PlayerEntity toSend, int... optionalDefault)
    {
        int dim = getDimensionForPlayer(baseOwner);
        ServerWorld old = DimensionManager.getWorld(dim);
        Vector3 spawnPos = Vector3.getNewVector().set(0, 64, 0);
        BlockPos entrance = getBaseEntrance(baseOwner, dim);
        if (entrance != null) spawnPos.set(entrance);
        if (old == null)
        {
            BlockPos pos = toSend.getEntityWorld().getSpawnPoint();
            if (optionalDefault.length > 2)
                pos = new BlockPos(optionalDefault[0], optionalDefault[1], optionalDefault[2]);
            initPlayerBase(baseOwner, pos, optionalDefault.length > 3 ? optionalDefault[3] : toSend.dimension);
            old = DimensionManager.getWorld(dim);
        }
        if (old == null) { return; }
        if (dim == toSend.dimension)
        {
            dim = optionalDefault.length > 3 ? optionalDefault[3] : 0;
            BlockPos pos;
            if ((pos = getBaseEntrance(baseOwner, dim)) != null) spawnPos.set(pos);
            else
            {
                old = DimensionManager.getWorld(dim);
                spawnPos.set(old.getSpawnPoint());
            }
        }
        Transporter.teleportEntity(toSend, spawnPos.add(0.5, 0, 0.5), dim, false);
    }

    public static PokecubeDimensionManager getInstance()
    {
        return INSTANCE == null ? INSTANCE = new PokecubeDimensionManager() : INSTANCE;
    }

    public static String getOwner(int dim)
    {
        return getInstance().dimOwners.get(dim);
    }

    public static boolean registerDim(int dim)
    {
        return getInstance().dims.add(dim);
    }

    Set<Integer>         dims      = Sets.newHashSet();
    Map<Integer, String> dimOwners = Maps.newHashMap();

    public PokecubeDimensionManager()
    {
        MinecraftForge.EVENT_BUS.register(this);
        int id = -1;
        for (DimensionType type : DimensionType.values())
        {
            if (type.getId() > id)
            {
                id = type.getId();
            }
        }
        id++;
        if (PokecubeMod.debug) PokecubeMod.log("Registering Pokecube Secret Base Dimension type at id " + id);
        SECRET_BASE_TYPE = DimensionType.register("pokecube_secretbase", "_pokecube", id, DimensionSecretBase.class,
                PokecubeMod.core.getConfig().basesLoaded);
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save evt)
    {
        if (evt.getWorld().dimension.getDimension() == 0)
        {
            CompoundNBT CompoundNBT = getTag();
            ISaveHandler saveHandler = evt.getWorld().getSaveHandler();
            File file = saveHandler.getMapFileFromName("PokecubeDimensionIDs");
            try
            {
                FileOutputStream fileoutputstream = new FileOutputStream(file);
                CompressedStreamTools.writeCompressed(CompoundNBT, fileoutputstream);
                fileoutputstream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load evt)
    {
        if (evt.getWorld().provider instanceof DimensionSecretBase)
        {
            ((DimensionSecretBase) evt.getWorld().provider).onWorldLoad();
        }
    }

    @SubscribeEvent
    public void playerInitialSync(FMLNetworkEvent.ServerConnectionFromClientEvent event)
    {
        PacketSyncDimIds packet = new PacketSyncDimIds();
        packet.data = getTag();
        PokecubeMod.packetPipeline.sendTo(packet, event.getManager().channel());
    }

    @SubscribeEvent
    public void playerChangeDimension(PlayerChangedDimensionEvent event)
    {
        ((ServerPlayerEntity) event.player).connection.sendPacket(new SPacketWorldBorder(
                event.player.getEntityWorld().getWorldBorder(), SPacketWorldBorder.Action.INITIALIZE));
    }

    @SubscribeEvent
    public void playerLoggin(PlayerLoggedInEvent event)
    {
        World world = event.player.getEntityWorld();
        if (!world.isRemote)
        {
            ((ServerPlayerEntity) event.player).connection.sendPacket(new SPacketWorldBorder(
                    event.player.getEntityWorld().getWorldBorder(), SPacketWorldBorder.Action.INITIALIZE));
        }
    }

    public void onServerStop(FMLServerStoppingEvent event)
    {
        if (PokecubeMod.debug) PokecubeMod.log("Stopping server");
    }

    public void onServerStart(FMLServerStartingEvent event) throws IOException
    {
        if (PokecubeMod.debug) PokecubeMod.log("Starting server, Pokecube Registering Dimensions");
        ISaveHandler saveHandler = event.getServer().getEntityWorld().getSaveHandler();
        File file = saveHandler.getMapFileFromName("PokecubeDimensionIDs");
        dims.clear();
        dimOwners.clear();
        if (file != null && file.exists())
        {
            FileInputStream fileinputstream = new FileInputStream(file);
            CompoundNBT CompoundNBT = CompressedStreamTools.readCompressed(fileinputstream);
            fileinputstream.close();
            loadFromTag(CompoundNBT, true);
        }
        if (WorldgenHandler.dims != null) for (CustomDim dim : WorldgenHandler.dims.dims)
        {
            if (!dims.contains(dim.dimid))
            {
                DimensionType type = DimensionType.OVERWORLD;
                if (dim.dim_type != null)
                {
                    try
                    {
                        type = DimensionType.byName(dim.dim_type);
                    }
                    catch (Exception e)
                    {
                        PokecubeMod.log(Level.WARNING, "Error with dim_type: " + dim.dim_type, e);
                        type = DimensionType.OVERWORLD;
                    }
                }
                CustomDimensionManager.initDimension(dim.dimid, dim.world_name, dim.world_type, dim.generator_options,
                        type, dim.seed);
            }
        }
    }

    private CompoundNBT getTag()
    {
        CompoundNBT CompoundNBT = new CompoundNBT();
        CompoundNBT types = new CompoundNBT();
        int[] dim = new int[dims.size()];
        int n = 0;
        for (int i : dims)
        {
            dim[n++] = i;
            DimensionType type = SECRET_BASE_TYPE;
            if (DimensionManager.isDimensionRegistered(i))
            {
                type = DimensionManager.getProviderType(i);
            }
            types.putString("dim-" + i + "-type", type.toString());
            World world = DimensionManager.getWorld(i);
            if (world != null && type != SECRET_BASE_TYPE)
            {
                types.putString("dim-" + i + "-name", world.getWorldInfo().getWorldName());
                types.putString("dim-" + i + "-options", world.getWorldInfo().getGeneratorOptions());
                types.putString("dim-" + i + "-world", world.getWorldType().getName());
                types.putLong("dim-" + i + "-seed", world.getSeed());
            }
            if (dimOwners.containsKey(i)) CompoundNBT.putString("dim_" + i, dimOwners.get(i));
        }
        CompoundNBT.putIntArray("dims", dim);
        CompoundNBT.put("types", types);
        CompoundNBT ret = new CompoundNBT();
        ret.put("Data", CompoundNBT);
        return ret;
    }

    public void loadFromTag(CompoundNBT CompoundNBT, boolean init)
    {
        CompoundNBT = CompoundNBT.getCompound("Data");
        int[] nums = CompoundNBT.getIntArray("dims");
        CompoundNBT typesTag = CompoundNBT.getCompound("types");
        dims.clear();
        for (int i : nums)
        {
            dims.add(i);
            if (DimensionManager.isDimensionRegistered(i) && !init) continue;
            DimensionType type = SECRET_BASE_TYPE;
            if (typesTag.hasKey("dim-" + i + "-type"))
                type = DimensionType.valueOf(typesTag.getString("dim-" + i + "-type"));
            if (typesTag.hasKey("dim-" + i + "-name"))
            {
                String worldName = typesTag.getString("dim-" + i + "-name");
                String generatorOptions = typesTag.getString("dim-" + i + "-options");
                String worldType = typesTag.getString("dim-" + i + "-world");
                Long seed = typesTag.hasKey("dim-" + i + "-seed") ? typesTag.getLong("dim-" + i + "-seed") : null;
                CustomDimensionManager.initDimension(i, worldName, worldType, generatorOptions, seed);
            }
            else if (!DimensionManager.isDimensionRegistered(i)) DimensionManager.registerDimension(i, type);
            if (CompoundNBT.hasKey("dim_" + i))
            {
                dimOwners.put(i, CompoundNBT.getString("dim_" + i));
            }
        }
    }

    public void syncToAll()
    {
        PacketSyncDimIds packet = new PacketSyncDimIds();
        packet.data = getTag();
        PokecubeMod.packetPipeline.sendToAll(packet);
    }
}
