package pokecube.modelloader;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.MobEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import pokecube.core.PokecubeCore;
import pokecube.core.client.render.entity.RenderAdvancedPokemobModel;
import pokecube.core.database.PokedexEntry;
import pokecube.modelloader.common.Config;

public interface IMobProvider
{
    /** Locations of model inside the resources.<br>
     * Example for a default location would be<br>
     * "models/pokemobs/" */
    String getModelDirectory(PokedexEntry entry);

    /** Locations of texture inside the resources.<br>
     * Example for a default location would be<br>
     * "models/pokemobs/" */
    String getTextureDirectory(PokedexEntry entry);

    /** Should return the @Mod object associated with this provider, used for
     * locating the modid */
    Object getMod();

    @OnlyIn(Dist.CLIENT)
    default void registerModel(PokedexEntry entry)
    {
        PokecubeCore.proxy.registerPokemobRenderer(entry.getTrimmedName(), new IRenderFactory<MobEntity>()
        {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public Render<? super MobEntity> createRenderFor(RenderManager manager)
            {
                RenderAdvancedPokemobModel<?> renderer = new RenderAdvancedPokemobModel(entry.getTrimmedName(), manager,
                        1);
                if (entry != null && (ModPokecubeML.preload || Config.instance.toPreload.contains(entry.getName())
                        || Config.instance.toPreload.contains(entry.getTrimmedName())))
                {
                    renderer.preload();
                }
                return (Render<? super MobEntity>) renderer;
            }
        }, getMod());
    }
}
