package pokecube.core.interfaces.capabilities.impl;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;

public abstract class PokemobSided extends PokemobBase
{
    private final Map<ResourceLocation, ResourceLocation> shinyTexs = Maps.newHashMap();

    @Override
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getTexture()
    {
        if (this.textures != null)
        {
            final PokedexEntry entry = this.getPokedexEntry();
            final int index = this.getSexe() == IPokemob.FEMALE && entry.textureDetails[1] != null ? 1 : 0;
            final boolean shiny = this.isShiny();
            final int effects = entry.textureDetails[index].length;
            final int texIndex = this.getEntity().ticksExisted % effects * 3 / effects + (shiny ? effects : 0);
            final ResourceLocation texture = this.textures[texIndex];
            return texture;
        }
        final String domain = this.getPokedexEntry().getModId();
        final int index = this.getSexe() == IPokemob.FEMALE && this.entry.textureDetails[1] != null ? 1 : 0;
        final int effects = this.entry.textureDetails[index].length;
        final int size = 2 * effects;
        this.textures = new ResourceLocation[size];
        for (int i = 0; i < effects; i++)
        {
            this.textures[i] = new ResourceLocation(domain, this.entry.texturePath + this.entry.getTrimmedName()
                    + this.entry.textureDetails[index][i] + ".png");
            this.textures[i + effects] = new ResourceLocation(domain, this.entry.texturePath + this.entry
                    .getTrimmedName() + this.entry.textureDetails[index][i] + "s.png");
        }
        return this.getTexture();

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation modifyTexture(ResourceLocation texture)
    {
        if (texture == null) return this.getTexture();
        if (!texture.getPath().contains("entity/"))
        {
            String path = this.getPokedexEntry().texturePath + texture.getPath();
            if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
            final int index = this.getSexe() == IPokemob.FEMALE && this.entry.textureDetails[1] != null ? 1 : 0;
            final int effects = this.entry.textureDetails[index].length;
            final int texIndex = this.getEntity().ticksExisted % effects * 3 / effects;
            path = path + this.entry.textureDetails[index][texIndex] + ".png";
            texture = new ResourceLocation(texture.getNamespace(), path);
        }
        if (this.isShiny()) if (!this.shinyTexs.containsKey(texture))
        {
            final String domain = texture.getNamespace();
            String texName = texture.getPath();
            texName = texName.replace(".png", "s.png");
            final ResourceLocation modified = new ResourceLocation(domain, texName);
            this.shinyTexs.put(texture, modified);
            return modified;
        }
        else texture = this.shinyTexs.get(texture);
        return texture;
    }
}