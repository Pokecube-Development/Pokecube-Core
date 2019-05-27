package pokecube.modelloader.client.render.models.x3d;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import pokecube.modelloader.CommonProxy;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.x3d.X3dModel;
import thut.core.client.render.x3d.X3dXML;

public class URLModel extends X3dModel
{
    private static final Logger        logger                = LogManager.getLogger();
    private static final AtomicInteger threadDownloadCounter = new AtomicInteger(0);
    private final File                 cacheFile;
    private final String               modelUrl;
    public boolean                     done                  = false;
    private Thread                     downloadThread;

    public URLModel()
    {
        cacheFile = null;
        modelUrl = "";
    }

    public URLModel(String url, String name)
    {
        this.modelUrl = url;
        cacheFile = new File(CommonProxy.CACHEPATH + File.separator + name + ".x3d");
        if (!cacheFile.exists())
        {
            downloadModel(cacheFile);
        }
        else
        {
            initModel();
        }
    }

    public void initModel()
    {
        try
        {
            synchronized (this)
            {
                FileInputStream stream = new FileInputStream(cacheFile);
                this.parts = new HashMap<String, IExtendedModelPart>();
                X3dXML xml = new X3dXML(stream);
                stream.close();
                makeObjects(xml);
                done = true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
                URLModel.logger.debug("Downloading http texture from {} to {}",
                        new Object[] { URLModel.this.modelUrl, URLModel.this.cacheFile });
                try
                {
                    connection = (HttpURLConnection) (new URL(URLModel.this.modelUrl))
                            .openConnection(Minecraft.getMinecraft().getProxy());
                    connection.setDoInput(true);
                    connection.setDoOutput(false);
                    connection.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
                    connection.connect();
                    if (connection.getResponseCode() / 100 != 2) { return; }
                    if (URLModel.this.cacheFile != null)
                    {
                        FileUtils.copyInputStreamToFile(connection.getInputStream(), URLModel.this.cacheFile);
                        URLModel.this.initModel();
                    }
                }
                catch (Exception exception)
                {
                    URLModel.logger.error("Couldn\'t download http model", exception);
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
