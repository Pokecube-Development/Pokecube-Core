package pokecube.core.commands;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.packets.PacketChoose;
import pokecube.core.utils.PokecubeSerializer;
import thut.core.common.commands.CommandTools;

public class ResetCommand extends CommandBase
{
    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public String getName()
    {
        return "pokereset";
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return "/pokereset <player>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource cSender, String[] args) throws CommandException
    {
        PlayerEntity player = args.length == 0 ? getCommandSenderAsPlayer(cSender)
                : getPlayer(server, cSender, args[0]);
        PokecubeSerializer.getInstance().setHasStarter(player, false);
        PacketChoose packet = new PacketChoose(PacketChoose.OPENGUI);
        packet.data.putBoolean("C", false);
        packet.data.putBoolean("H", false);
        PokecubePacketHandler.sendToClient(packet, player);
        cSender.sendMessage(CommandTools.makeTranslatedMessage("pokecube.command.reset", "", player.getName()));
        CommandTools.sendMessage(player, "pokecube.command.canchoose");
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
    public boolean isUsernameIndex(String[] astring, int i)
    {
        return i == 0;
    }
}
