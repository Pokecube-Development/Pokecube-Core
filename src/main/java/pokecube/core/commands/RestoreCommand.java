package pokecube.core.commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import pokecube.core.handlers.playerdata.PlayerPokemobCache;
import pokecube.core.utils.TagNames;
import pokecube.core.utils.Tools;
import thut.core.common.handlers.PlayerDataHandler;

public class RestoreCommand extends CommandBase
{
    public RestoreCommand()
    {
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public String getName()
    {
        return "pokerestore";
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return "/pokerestore <check|checkpc|checkdeleted|give|givepc|givedeleted> <player>";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSource sender, String[] args,
            @Nullable BlockPos pos)
    {
        int last = args.length - 1;
        if (last >= 0 && isUsernameIndex(args,
                last)) { return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()); }
        return Collections.<String> emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int i)
    {
        return i == 1;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        if (args.length < 2) throw new CommandException(getUsage(sender));
        GameProfile profile = MakeCommand.getProfile(server, args[1]);
        if (profile == null)
            throw new PlayerNotFoundException("commands.generic.player.notFound", new Object[] { args[1] });
        PlayerPokemobCache pokemobCache = PlayerDataHandler.getInstance().getPlayerData(profile.getId())
                .getData(PlayerPokemobCache.class);
        Map<Integer, ItemStack> cache = pokemobCache.cache;

        if (args[0].startsWith("check") || args[0].startsWith("give"))
        {
            ITextComponent message = new StringTextComponent("Pokemobs: ");
            sender.sendMessage(message);
            message = new StringTextComponent("");
            boolean pc = args[0].endsWith("pc");
            boolean deleted = args[0].endsWith("deleted");
            for (Entry<Integer, ItemStack> entry : cache.entrySet())
            {
                Integer id = entry.getKey();
                boolean inPC = pokemobCache.inPC.contains(id);
                boolean wasDeleted = pokemobCache.genesDeleted.contains(id);

                // If it is in the PC, but we dont care, continue
                if (!pc && inPC) continue;
                else if (pc && !inPC) continue;
                // Same for deleted status.
                if (!deleted && wasDeleted) continue;
                else if (deleted && !wasDeleted) continue;

                ItemStack stack = entry.getValue();
                ListNBT ListNBT = stack.getTag().getCompound(TagNames.POKEMOB).getTagList("Pos", 6);
                double posX = ListNBT.getDoubleAt(0);
                double posY = ListNBT.getDoubleAt(1);
                double posZ = ListNBT.getDoubleAt(2);
                String command;
                if (args[0].startsWith("check"))
                {
                    command = "/tp " + posX + " " + posY + " " + posZ;
                }
                else
                {
                    command = "/pokerestore restore " + args[1] + " " + id;
                }
                CompoundNBT tag = stack.getTag().copy();
                tag.remove(TagNames.POKEMOB);
                ItemStack copy = stack.copy();
                copy.put(tag);
                tag = copy.writeToNBT(new CompoundNBT());
                ClickEvent click = new ClickEvent(Action.RUN_COMMAND, command);
                ITextComponent sub = stack.getTextComponent();
                sub.getStyle().setClickEvent(click);
                sub.appendText(" ");
                message.appendSibling(sub);
                int size = message.toString().getBytes().length;
                if (size > 32000)
                {
                    sender.sendMessage(message);
                    message = new StringTextComponent("");
                }
            }
            sender.sendMessage(message);
        }
        else if (args[0].equals("restore"))
        {
            Integer id = Integer.parseInt(args[2]);
            Tools.giveItem(MakeCommand.getPlayerBySender(sender), cache.get(id).copy());
        }
    }
}
