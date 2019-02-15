package pokecube.core.commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
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
    public String getUsage(ICommandSender sender)
    {
        return "/pokerestore <check|give> <player>";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
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
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 2) throw new CommandException(getUsage(sender));
        GameProfile profile = MakeCommand.getProfile(server, args[1]);
        if (profile == null)
            throw new PlayerNotFoundException("commands.generic.player.notFound", new Object[] { args[1] });
        Map<Integer, ItemStack> cache = PlayerDataHandler.getInstance().getPlayerData(profile.getId())
                .getData(PlayerPokemobCache.class).cache;

        if (args[0].equals("check") || args[0].equals("give"))
        {
            ITextComponent message = new TextComponentString("Pokemobs: ");
            sender.sendMessage(message);
            message = new TextComponentString("");
            for (Entry<Integer, ItemStack> entry : cache.entrySet())
            {
                Integer id = entry.getKey();
                ItemStack stack = entry.getValue();
                NBTTagList nbttaglist = stack.getTagCompound().getCompoundTag(TagNames.POKEMOB).getTagList("Pos", 6);
                double posX = nbttaglist.getDoubleAt(0);
                double posY = nbttaglist.getDoubleAt(1);
                double posZ = nbttaglist.getDoubleAt(2);
                String command;
                if (args[0].equals("check"))
                {
                    command = "/tp " + posX + " " + posY + " " + posZ;
                }
                else
                {
                    command = "/pokerestore restore " + args[1] + " " + id;
                }
                NBTTagCompound tag = stack.getTagCompound().copy();
                tag.removeTag(TagNames.POKEMOB);
                ItemStack copy = stack.copy();
                copy.setTagCompound(tag);
                tag = copy.writeToNBT(new NBTTagCompound());
                ClickEvent click = new ClickEvent(Action.RUN_COMMAND, command);
                ITextComponent sub = stack.getTextComponent();
                sub.getStyle().setClickEvent(click);
                sub.appendText(" ");
                message.appendSibling(sub);
                int size = message.toString().getBytes().length;
                if (size > 32000)
                {
                    sender.sendMessage(message);
                    message = new TextComponentString("");
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
