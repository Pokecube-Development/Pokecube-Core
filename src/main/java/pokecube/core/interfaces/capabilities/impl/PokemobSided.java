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
    private Map<ResourceLocation, ResourceLocation> shinyTexs = Maps.newHashMap();

    @Override
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getTexture()
    {
        if (this.textures != null)
        {
            PokedexEntry entry = getPokedexEntry();
            int index = getSexe() == IPokemob.FEMALE && entry.textureDetails[1] != null ? 1 : 0;
            boolean shiny = isShiny();
            int effects = entry.textureDetails[index].length;
            int texIndex = ((getEntity().ticksExisted % effects * 3) / effects) + (shiny ? effects : 0);
            ResourceLocation texture = textures[texIndex];
            return texture;
        }
        else
        {
            String domain = getPokedexEntry().getModId();
            int index = getSexe() == IPokemob.FEMALE && entry.textureDetails[1] != null ? 1 : 0;
            int effects = entry.textureDetails[index].length;
            int size = 2 * (effects);
            textures = new ResourceLocation[size];
            for (int i = 0; i < effects; i++)
            {
                textures[i] = new ResourceLocation(domain,
                        entry.texturePath + entry.getTrimmedName() + entry.textureDetails[index][i] + ".png");
                textures[i + effects] = new ResourceLocation(domain,
                        entry.texturePath + entry.getTrimmedName() + entry.textureDetails[index][i] + "s.png");
            }
            return getTexture();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation modifyTexture(ResourceLocation texture)
    {
        if (texture == null) { return getTexture(); }
        if (!texture.getResourcePath().contains("entity/"))
        {
            String path = getPokedexEntry().texturePath + texture.getResourcePath();
            if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
            int index = getSexe() == IPokemob.FEMALE && entry.textureDetails[1] != null ? 1 : 0;
            int effects = entry.textureDetails[index].length;
            int texIndex = ((getEntity().ticksExisted % effects * 3) / effects);
            path = path + entry.textureDetails[index][texIndex] + ".png";
            texture = new ResourceLocation(texture.getResourceDomain(), path);
        }
        if (isShiny())
        {
            if (!shinyTexs.containsKey(texture))
            {
                String domain = texture.getResourceDomain();
                String texName = texture.getResourcePath();
                texName = texName.replace(".png", "s.png");
                ResourceLocation modified = new ResourceLocation(domain, texName);
                shinyTexs.put(texture, modified);
                return modified;
            }
            else
            {
                texture = shinyTexs.get(texture);
            }
        }
        return texture;
    }
}