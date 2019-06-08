package pokecube.core.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.genetics.GeneticsManager;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import pokecube.core.utils.Tools;
import thut.api.maths.Vector3;
import thut.core.common.commands.CommandTools;

public class EggCommand extends CommandBase
{
    public static ResourceLocation CHERISHCUBE;

    private List<String>           aliases;

    public EggCommand()
    {
        this.aliases = new ArrayList<String>();
        this.aliases.add("pokeegg");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        String text = "";
        ITextComponent message;
        String name;
        if (args.length > 0)
        {
            PlayerEntity getsEgg = null;
            int index = 1;
            PokedexEntry entry = null;
            ItemStack egg = null;

            try
            {
                int id = Integer.parseInt(args[0]);
                entry = Database.getEntry(id);
                name = entry.getName();
            }
            catch (NumberFormatException e)
            {
                name = args[0];
                if (name.startsWith("\'"))
                {
                    for (int j = 1; j < args.length; j++)
                    {
                        name += " " + args[j];
                        if (args[j].contains("\'"))
                        {
                            index = j + 1;
                            break;
                        }
                    }
                }
                ArrayList<PokedexEntry> entries = Lists.newArrayList(Database.getSortedFormes());
                Collections.shuffle(entries);
                Iterator<PokedexEntry> iterator = entries.iterator();
                if (name.equalsIgnoreCase("random"))
                {
                    entry = iterator.next();
                    while (entry.legendary || !entry.base)
                    {
                        entry = iterator.next();
                    }
                }
                else if (name.equalsIgnoreCase("randomall"))
                {
                    entry = iterator.next();
                    while (!entry.base)
                    {
                        entry = iterator.next();
                    }
                }
                else if (name.equalsIgnoreCase("randomlegend"))
                {
                    entry = iterator.next();
                    while (!entry.legendary || !entry.base)
                    {
                        entry = iterator.next();
                    }
                }
                else entry = Database.getEntry(name);
            }

            if (args.length > 1 && args[1].equalsIgnoreCase("literal"))
            {
                egg = ItemPokemobEgg.getEggStack(entry);
                if (args.length > 2) getsEgg = getPlayer(server, sender, args[2]);
            }
            else
            {

                Entity mob = PokecubeMod.core.createPokemob(entry, sender.getEntityWorld());
                if (mob == null)
                {
                    CommandTools.sendError(sender, "pokecube.command.makeinvalid");
                    return;
                }
                IPokemob pokemob = CapabilityPokemob.getPokemobFor(mob);
                pokemob.specificSpawnInit();
                Vector3 offset = Vector3.getNewVector().set(0, 1, 0);
                String ownerName = MakeCommand.setToArgs(args, pokemob, index, offset);
                GameProfile profile = null;
                if (ownerName != null && !ownerName.isEmpty())
                {
                    profile = MakeCommand.getProfile(server, ownerName);
                }
                if (profile == null && ownerName != null && !ownerName.isEmpty())
                {
                    getsEgg = getPlayer(server, sender, ownerName);
                    profile = getsEgg.getGameProfile();
                }
                Vector3 temp = Vector3.getNewVector();
                if (profile != null)
                {
                    PokecubeMod.log(Level.INFO, "Creating " + pokemob.getPokedexEntry() + " for " + profile.getName());
                    pokemob.setPokemonOwner(profile.getId());
                    pokemob.setGeneralState(GeneralStates.TAMED, true);
                }

                temp.set(sender.getPosition()).addTo(offset);
                temp.moveEntity(mob);
                GeneticsManager.initMob(mob);
                egg = ItemPokemobEgg.getEggStack(pokemob);
            }

            if (getsEgg == null) getsEgg = getCommandSenderAsPlayer(sender);

            text = TextFormatting.GREEN + "Made Egg Of " + entry;
            message = ITextComponent.Serializer.jsonToComponent("[\"" + text + "\"]");
            sender.sendMessage(message);
            Tools.giveItem(getsEgg, egg);
            return;
        }
        CommandTools.sendError(sender, "pokecube.command.makeneedname");
    }

    @Override
    public List<String> getAliases()
    {
        return this.aliases;
    }

    @Override
    public String getName()
    {
        return aliases.get(0);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return "/" + aliases.get(0) + "<pokemob name/number> <arguments>";
    }

    @Override
    /** Return the required permission level for this command. */
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSource sender, String[] args, BlockPos pos)
    {
        List<String> ret = new ArrayList<String>();
        if (args.length == 1)
        {
            String text = args[0];
            for (PokedexEntry entry : Database.getSortedFormes())
            {
                String check = entry.getName().toLowerCase(java.util.Locale.ENGLISH);
                if (check.startsWith(text.toLowerCase(java.util.Locale.ENGLISH)))
                {
                    String name = entry.getName();
                    if (name.contains(" "))
                    {
                        name = "\'" + name + "\'";
                    }
                    ret.add(name);
                }
            }
            Collections.sort(ret, new Comparator<String>()
            {
                @Override
                public int compare(String o1, String o2)
                {
                    if (o1.contains("'") && !o2.contains("'")) return 1;
                    else if (o2.contains("'") && !o1.contains("'")) return -1;
                    return o1.compareToIgnoreCase(o2);
                }
            });
            return getListOfStringsMatchingLastWord(args, ret);
        }
        return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
    }

}
