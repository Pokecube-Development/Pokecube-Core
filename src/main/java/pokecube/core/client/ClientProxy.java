package pokecube.core.client;

import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.FoliageColors;
import net.minecraft.world.biome.BiomeColors;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import pokecube.core.CommonProxy;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.client.gui.blocks.Healer;
import pokecube.core.client.gui.blocks.PC;
import pokecube.core.client.gui.blocks.TMs;
import pokecube.core.client.gui.blocks.Trade;
import pokecube.core.client.gui.pokemob.GuiPokemob;
import pokecube.core.client.gui.pokemob.GuiPokemobAI;
import pokecube.core.client.gui.pokemob.GuiPokemobRoutes;
import pokecube.core.client.gui.pokemob.GuiPokemobStorage;
import pokecube.core.client.render.RenderMoves;
import pokecube.core.client.render.RenderNPC;
import pokecube.core.client.render.RenderPokecube;
import pokecube.core.client.render.RenderPokemob;
import pokecube.core.client.render.util.URLSkinImageBuffer;
import pokecube.core.client.render.util.URLSkinTexture;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.ContainerPokemob;
import pokecube.core.entity.pokemobs.GenericPokemob;
import pokecube.core.entity.professor.EntityProfessor;
import pokecube.core.handlers.ItemGenerator;
import pokecube.core.inventory.healer.HealerContainer;
import pokecube.core.inventory.pc.PCContainer;
import pokecube.core.inventory.tms.TMContainer;
import pokecube.core.inventory.trade.TradeContainer;
import pokecube.core.items.pokecubes.EntityPokecube;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import pokecube.core.moves.animations.EntityMoveUse;
import pokecube.core.network.pokemobs.PacketPokemobGui;
import pokecube.core.utils.PokeType;
import pokecube.nbtedit.NBTEdit;

public class ClientProxy extends CommonProxy
{
    public static KeyBinding nextMob;
    public static KeyBinding nextMove;
    public static KeyBinding previousMob;
    public static KeyBinding previousMove;
    public static KeyBinding mobBack;
    public static KeyBinding mobAttack;
    public static KeyBinding mobStance;
    public static KeyBinding mobMegavolve;
    public static KeyBinding noEvolve;
    public static KeyBinding mobMove1;
    public static KeyBinding mobMove2;
    public static KeyBinding mobMove3;
    public static KeyBinding mobMove4;
    public static KeyBinding mobUp;
    public static KeyBinding mobDown;
    public static KeyBinding throttleUp;
    public static KeyBinding throttleDown;
    public static KeyBinding arrangeGui;

    private static Map<String, ResourceLocation> players  = Maps.newHashMap();
    private static Map<String, ResourceLocation> urlSkins = Maps.newHashMap();

    @SubscribeEvent
    public void colourBlocks(final ColorHandlerEvent.Block event)
    {
        final Block[] leaves = ItemGenerator.leaves.values().toArray(new Block[0]);
        event.getBlockColors().register((state, reader, pos, tintIndex) ->
        {
            return reader != null && pos != null ? BiomeColors.getFoliageColor(reader, pos)
                    : FoliageColors.getDefault();
        }, leaves);
    }

    @SubscribeEvent
    public void colourItems(final ColorHandlerEvent.Item event)
    {
        final Block[] leaves = ItemGenerator.leaves.values().toArray(new Block[0]);
        event.getItemColors().register((stack, tintIndex) ->
        {
            final BlockState blockstate = ((BlockItem) stack.getItem()).getBlock().getDefaultState();
            return event.getBlockColors().getColor(blockstate, null, null, tintIndex);
        }, leaves);

        event.getItemColors().register((stack, tintIndex) ->
        {
            final PokeType type = PokeType.unknown;
            final PokedexEntry entry = ItemPokemobEgg.getEntry(stack);
            if (entry != null) return tintIndex == 0 ? entry.getType1().colour : entry.getType2().colour;
            return tintIndex == 0 ? type.colour : 0xFFFFFFFF;
        }, PokecubeItems.EGG);

    }

    @Override
    public PlayerEntity getPlayer()
    {
        return Minecraft.getInstance().player;
    }

    @Override
    public ResourceLocation getPlayerSkin(final String name)
    {
        if (ClientProxy.players.containsKey(name)) return ClientProxy.players.get(name);
        final Minecraft minecraft = Minecraft.getInstance();
        GameProfile profile = new GameProfile((UUID) null, name);
        profile = SkullTileEntity.updateGameProfile(profile);
        final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager()
                .loadSkinFromCache(profile);
        ResourceLocation resourcelocation;
        if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) resourcelocation = minecraft.getSkinManager().loadSkin(
                map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
        else
        {
            final UUID uuid = PlayerEntity.getUUID(profile);
            resourcelocation = DefaultPlayerSkin.getDefaultSkin(uuid);
        }
        ClientProxy.players.put(name, resourcelocation);
        return resourcelocation;
    }

    @Override
    public ResourceLocation getUrlSkin(final String urlSkin)
    {
        if (ClientProxy.urlSkins.containsKey(urlSkin)) return ClientProxy.urlSkins.get(urlSkin);
        try
        {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            final byte[] hash = digest.digest(urlSkin.getBytes("UTF-8"));
            final StringBuilder sb = new StringBuilder(2 * hash.length);
            for (final byte b : hash)
                sb.append(String.format("%02x", b & 0xff));
            final ResourceLocation resourcelocation = new ResourceLocation("skins/" + sb.toString());
            final TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            final ITextureObject object = new URLSkinTexture(null, urlSkin, DefaultPlayerSkin.getDefaultSkinLegacy(),
                    new URLSkinImageBuffer());
            texturemanager.loadTexture(resourcelocation, object);
            ClientProxy.urlSkins.put(urlSkin, resourcelocation);
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
        return ClientProxy.urlSkins.get(urlSkin);
    }

    @Override
    public boolean hasSound(final BlockPos pos)
    {
        final ISound old = Minecraft.getInstance().worldRenderer.mapSoundPositions.get(pos);
        if (old == null) return false;
        final boolean var = Minecraft.getInstance().getSoundHandler().func_215294_c(old);
        if (!var) Minecraft.getInstance().worldRenderer.mapSoundPositions.remove(pos);
        return var;
    }

    @Override
    public void loaded(final FMLLoadCompleteEvent event)
    {
        super.loaded(event);
        RenderPokemob.register();
    }

    @Override
    public void setupClient(final FMLClientSetupEvent event)
    {
        PokecubeCore.LOGGER.debug("Pokecube Client Setup");
        if (!ModLoadingContext.get().getActiveContainer().getModId().equals(PokecubeCore.MODID)) return;

        // Register keybinds
        PokecubeCore.LOGGER.debug("Init Keybinds");
        ClientRegistry.registerKeyBinding(ClientProxy.nextMob = new KeyBinding("key.pokemob.next", GLFW.GLFW_KEY_RIGHT,
                "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.previousMob = new KeyBinding("key.pokemob.prev",
                GLFW.GLFW_KEY_LEFT, "Pokecube"));

        ClientRegistry.registerKeyBinding(ClientProxy.nextMove = new KeyBinding("key.pokemob.move.next",
                GLFW.GLFW_KEY_DOWN, "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.previousMove = new KeyBinding("key.pokemob.move.prev",
                GLFW.GLFW_KEY_UP, "Pokecube"));

        ClientRegistry.registerKeyBinding(ClientProxy.mobBack = new KeyBinding("key.pokemob.recall", GLFW.GLFW_KEY_R,
                "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.mobAttack = new KeyBinding("key.pokemob.attack", GLFW.GLFW_KEY_G,
                "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.mobStance = new KeyBinding("key.pokemob.stance",
                GLFW.GLFW_KEY_BACKSLASH, "Pokecube"));

        ClientRegistry.registerKeyBinding(ClientProxy.mobMegavolve = new KeyBinding("key.pokemob.megaevolve",
                GLFW.GLFW_KEY_M, "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.noEvolve = new KeyBinding("key.pokemob.b", GLFW.GLFW_KEY_B,
                "Pokecube"));

        ClientRegistry.registerKeyBinding(ClientProxy.mobMove1 = new KeyBinding("key.pokemob.move.1",
                InputMappings.INPUT_INVALID.getKeyCode(), "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.mobMove2 = new KeyBinding("key.pokemob.move.2",
                InputMappings.INPUT_INVALID.getKeyCode(), "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.mobMove3 = new KeyBinding("key.pokemob.move.3",
                InputMappings.INPUT_INVALID.getKeyCode(), "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.mobMove4 = new KeyBinding("key.pokemob.move.4",
                InputMappings.INPUT_INVALID.getKeyCode(), "Pokecube"));

        ClientRegistry.registerKeyBinding(ClientProxy.mobUp = new KeyBinding("key.pokemob.up", GLFW.GLFW_KEY_SPACE,
                "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.mobDown = new KeyBinding("key.pokemob.down",
                GLFW.GLFW_KEY_LEFT_CONTROL, "Pokecube"));

        ClientRegistry.registerKeyBinding(ClientProxy.throttleUp = new KeyBinding("key.pokemob.speed.up",
                GLFW.GLFW_KEY_LEFT_BRACKET, "Pokecube"));
        ClientRegistry.registerKeyBinding(ClientProxy.throttleDown = new KeyBinding("key.pokemob.speed.down",
                GLFW.GLFW_KEY_RIGHT_BRACKET, "Pokecube"));

        ClientRegistry.registerKeyBinding(ClientProxy.arrangeGui = new KeyBinding("key.pokemob.arrangegui",
                InputMappings.INPUT_INVALID.getKeyCode(), "Pokecube"));

        // Forward this to PCEdit mod:
        NBTEdit.setupClient(event);

        // Register to model loading registry
        OBJLoader.INSTANCE.addDomain(PokecubeCore.MODID);

        // Register the gui side of the screens.
        PokecubeCore.LOGGER.debug("Init Screen Factories");
        ScreenManager.registerFactory(ContainerPokemob.TYPE, (c, i, t) ->
        {
            switch (c.mode)
            {
            case PacketPokemobGui.AI:
                return new GuiPokemobAI(c, i);
            case PacketPokemobGui.STORAGE:
                return new GuiPokemobStorage(c, i);
            case PacketPokemobGui.ROUTES:
                return new GuiPokemobRoutes(c, i);
            }
            return new GuiPokemob<>(c, i);
        });
        ScreenManager.registerFactory(HealerContainer.TYPE, Healer<HealerContainer>::new);
        ScreenManager.registerFactory(PCContainer.TYPE, PC<PCContainer>::new);
        ScreenManager.registerFactory(TradeContainer.TYPE, Trade<TradeContainer>::new);
        ScreenManager.registerFactory(TMContainer.TYPE, TMs<TMContainer>::new);

        // Register mob rendering
        PokecubeCore.LOGGER.debug("Init Mob Renderers");
        RenderingRegistry.registerEntityRenderingHandler(GenericPokemob.class, (manager) -> new RenderPokemob(
                Database.missingno, manager));
        RenderingRegistry.registerEntityRenderingHandler(EntityPokecube.class, (manager) -> new RenderPokecube(
                manager));
        RenderingRegistry.registerEntityRenderingHandler(EntityMoveUse.class, (manager) -> new RenderMoves(manager));
        RenderingRegistry.registerEntityRenderingHandler(EntityProfessor.class, (manager) -> new RenderNPC<>(manager));

    }

    @Override
    public void toggleSound(final SoundEvent sound, final BlockPos pos, final boolean play, final boolean loops,
            final SoundCategory category)
    {
        if (sound != null)
        {
            final ISound old = Minecraft.getInstance().worldRenderer.mapSoundPositions.get(pos);
            if (play)
            {
                if (this.hasSound(pos))
                {
                    Minecraft.getInstance().worldRenderer.mapSoundPositions.remove(pos);
                    Minecraft.getInstance().getSoundHandler().stop(sound.getName(), category);
                }

                final ISound simplesound = new SimpleSound(sound.getName(), category, 4.0F, 1.0F, loops, 0,
                        ISound.AttenuationType.NONE, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, false);
                Minecraft.getInstance().worldRenderer.mapSoundPositions.put(pos, simplesound);
                Minecraft.getInstance().getSoundHandler().play(simplesound);
            }
            else if (old != null)
            {
                Minecraft.getInstance().worldRenderer.mapSoundPositions.remove(pos);
                Minecraft.getInstance().getSoundHandler().stop(sound.getName(), category);
            }
        }
    }
}
