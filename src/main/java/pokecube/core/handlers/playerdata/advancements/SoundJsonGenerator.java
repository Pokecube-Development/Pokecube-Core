package pokecube.core.handlers.playerdata.advancements;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;

public class SoundJsonGenerator
{
    public static String generateSoundJson()
    {
        JsonObject soundJson = new JsonObject();
        List<PokedexEntry> baseFormes = Lists.newArrayList(Database.baseFormes.values());
        Collections.sort(baseFormes, new Comparator<PokedexEntry>()
        {
            @Override
            public int compare(PokedexEntry o1, PokedexEntry o2)
            {
                return o1.getPokedexNb() - o2.getPokedexNb();
            }
        });
        for (PokedexEntry entry : baseFormes)
        {
            JsonObject soundEntry = new JsonObject();
            soundEntry.addProperty("category", "hostile");
            soundEntry.addProperty("subtitle", entry.getUnlocalizedName());
            JsonArray sounds = new JsonArray();
            for (int i = 0; i < 3; i++)
            {
                JsonObject sound = new JsonObject();
                sound.addProperty("name", "pokecube_mobs:mobs/" + entry.getTrimmedName());
                sound.addProperty("volume", (i == 0 ? 0.8 : i == 1 ? 0.9 : 1));
                sound.addProperty("pitch", (i == 0 ? 0.9 : i == 1 ? 0.95 : 1));
                sounds.add(sound);
            }
            soundEntry.add("sounds", sounds);
            soundJson.add("mobs." + entry.getTrimmedName(), soundEntry);
        }
        return AdvancementGenerator.GSON.toJson(soundJson);
    }
}
