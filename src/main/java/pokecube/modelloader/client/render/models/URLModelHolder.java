package pokecube.modelloader.client.render.models;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import pokecube.modelloader.CommonProxy;
import pokecube.modelloader.client.render.AnimationLoader;
import pokecube.modelloader.client.render.DefaultIModelRenderer;
import pokecube.modelloader.client.render.models.x3d.URLModel;
import pokecube.modelloader.client.render.models.x3d.URLXML;
import thut.core.client.render.animation.ModelHolder;
import thut.core.client.render.model.IModelRenderer.Vector5;

public class URLModelHolder extends ModelHolder
{
    final String                url_model;
    final String                url_model_xml;
    final String                url_texture;
    final String                name;

    public final URLModel       model;
    public final URLXML         xml;
    public final URLSkinTexture texture;

    public URLModelHolder(String name, String model, String xml, String texture)
    {
        super(new ResourceLocation(CommonProxy.CACHEPATH + File.separator + name),
                new ResourceLocation(CommonProxy.CACHEPATH + "/entity/" + name + ".png"),
                new ResourceLocation(CommonProxy.CACHEPATH + File.separator + name), name);
        this.name = name;
        this.url_model = model;
        this.url_model_xml = xml;
        this.url_texture = texture;

        this.model = new URLModel(model, name);

        ResourceLocation resource = ((ModelHolder) this).texture;
        ModelHolder holder = new ModelHolder(resource, resource, resource, name);

        DefaultIModelRenderer<?> loaded = new DefaultIModelRenderer<>(new HashMap<String, ArrayList<Vector5>>(),
                holder);
        loaded.model.imodel = this.model;
        AnimationLoader.models.put(name, this);
        AnimationLoader.modelMaps.put(name, loaded);

        this.texture = new URLSkinTexture(new File(CommonProxy.CACHEPATH + File.separator + name + ".png"), texture,
                null, new URLSkinImageBuffer());
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        texturemanager.loadTexture(resource, this.texture);

        // This handles telling AnimationLoader to parse.
        this.xml = new URLXML(xml, this);

    }
}
