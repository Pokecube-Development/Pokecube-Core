package pokecube.modelloader.client.render.models.x3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import pokecube.modelloader.CommonProxy;
import pokecube.modelloader.client.render.AnimationLoader;
import pokecube.modelloader.client.render.TextureHelper;
import thut.core.client.render.animation.ModelHolder;

public class URLXML
{

    private static final Logger        logger                = LogManager.getLogger();
    private static final AtomicInteger threadDownloadCounter = new AtomicInteger(0);
    private final File                 cacheFile;
    private final String               modelUrl;
    private final ModelHolder          holder;
    public boolean                     done                  = false;
    private Thread                     downloadThread;

    public URLXML(String url, ModelHolder holder)
    {
        this.holder = holder;
        this.modelUrl = url;
        cacheFile = new File(CommonProxy.CACHEPATH + File.separator + holder.name + ".xml");
        if (!cacheFile.exists())
        {
            downloadModel(cacheFile);
        }
        else
        {
            URLXML.this.done = true;
            try
            {
                AnimationLoader.parse(new FileInputStream(cacheFile), URLXML.this.holder);
                if (AnimationLoader.getModel(holder.name).getTexturer() instanceof TextureHelper)
                {
                    TextureHelper tex = (TextureHelper) AnimationLoader.getModel(holder.name).getTexturer();
                    tex.default_tex = holder.texture;
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void downloadModel(final File path)
    {
        this.downloadThread = new Thread("Texture Downloader #" + threadDownloadCounter.incrementAndGet())
        {
            @Override
            public void run()
            {
                HttpURLConnection connection = null;
                URLXML.logger.debug("Downloading http texture from {} to {}",
                        new Object[] { URLXML.this.modelUrl, URLXML.this.cacheFile });
                try
                {
                    connection = (HttpURLConnection) (new URL(URLXML.this.modelUrl))
                            .openConnection(Minecraft.getMinecraft().getProxy());
                    connection.setDoInput(true);
                    connection.setDoOutput(false);
                    connection.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
                    connection.connect();
                    if (connection.getResponseCode() / 100 != 2) { return; }
                    if (URLXML.this.cacheFile != null)
                    {
                        FileUtils.copyInputStreamToFile(connection.getInputStream(), URLXML.this.cacheFile);
                        URLXML.this.done = true;
                        AnimationLoader.parse(new FileInputStream(cacheFile), URLXML.this.holder);
                    }
                }
                catch (Exception exception)
                {
                    URLXML.logger.error("Couldn\'t download http model", exception);
                }
                finally
                {
                    if (connection != null)
                    {
                        connection.disconnect();
                    }
                }
            }
        };
        this.downloadThread.setDaemon(true);
        this.downloadThread.start();
    }
}
