package pokecube.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import pokecube.core.items.ItemTM;
import pokecube.core.moves.MovesUtils;
import thut.core.common.commands.CommandTools;

public class TMCommand
{
    private static SuggestionProvider<CommandSource> SUGGEST_TMS = (ctx,
            sb) -> net.minecraft.command.ISuggestionProvider.suggest(MovesUtils.moves.keySet(), sb);

    public static int execute(final CommandSource source, final ServerPlayerEntity serverplayerentity, final String tm)
    {
        final ItemStack itemstack = ItemTM.getTM(tm);
        final boolean flag = serverplayerentity.inventory.addItemStackToInventory(itemstack);
        if (flag && itemstack.isEmpty())
        {
            itemstack.setCount(1);
            final ItemEntity itementity1 = serverplayerentity.dropItem(itemstack, false);
            if (itementity1 != null) itementity1.makeFakeItem();
            serverplayerentity.world.playSound((PlayerEntity) null, serverplayerentity.posX, serverplayerentity.posY,
                    serverplayerentity.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
                    ((serverplayerentity.getRNG().nextFloat() - serverplayerentity.getRNG().nextFloat()) * 0.7F + 1.0F)
                            * 2.0F);
            serverplayerentity.container.detectAndSendChanges();
        }
        else
        {
            final ItemEntity itementity = serverplayerentity.dropItem(itemstack, false);
            if (itementity != null)
            {
                itementity.setNoPickupDelay();
                itementity.setOwnerId(serverplayerentity.getUniqueID());
            }
        }
        return 0;
    }

    public static int execute(final CommandSource source, final String tm) throws CommandSyntaxException
    {
        // final boolean var = tm.isEmpty();
        // if (!var)
        // {
        // for (final ItemBerry berry : BerryManager.berryItems.values())
        // {
        // // final Gson gson = new
        // // GsonBuilder().setPrettyPrinting().create();
        // final String filename = "fruit_" + berry.type.name;
        // final File dir = new File("./generated/");
        // if (!dir.exists()) dir.mkdirs();
        // final File file = new File(dir, filename + ".json");
        //
//            //@formatter:off
//            final String json =
//                "{\n"+
//                "  \"type\": \"minecraft:block\",\n"+
//                "  \"pools\": [\n"+
//                "    {\n"+
//                "      \"name\": \""+berry.getRegistryName()+"\",\n"+
//                "      \"rolls\": 1,\n"+
//                "      \"entries\": [\n"+
//                "        {\n"+
//                "          \"type\": \"minecraft:item\",\n"+
//                "          \"name\": \""+berry.getRegistryName()+"\"\n"+
//                "        }\n"+
//                "      ],\n"+
//                "      \"conditions\": [\n"+
//                "        {\n"+
//                "          \"condition\": \"minecraft:survives_explosion\"\n"+
//                "        }\n"+
//                "      ]\n"+
//                "    }\n"+
//                "  ]\n"+
//                "}";//@formatter:on
        // try
        // {
        // final FileWriter write = new FileWriter(file);
        // write.write(json);
        // write.close();
        // }
        // catch (final IOException e)
        // {
        // e.printStackTrace();
        // }
        // }
        // return 0;
        // }
        final ServerPlayerEntity player = source.asPlayer();
        return TMCommand.execute(source, player, tm);
    }

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        PermissionAPI.registerNode("command.poketm", DefaultPermissionLevel.OP, "Is the player allowed to use /poketm");

        final LiteralArgumentBuilder<CommandSource> command = Commands.literal("poketm").requires(cs -> CommandTools
                .hasPerm(cs, "command.poketm")).then(Commands.argument("tm", StringArgumentType.string()).suggests(
                        TMCommand.SUGGEST_TMS).executes(ctx -> TMCommand.execute(ctx.getSource(), StringArgumentType
                                .getString(ctx, "tm")))).then(Commands.argument("tm", StringArgumentType.string())
                                        .suggests(TMCommand.SUGGEST_TMS).then(Commands.argument("player", EntityArgument
                                                .player()).executes(ctx -> TMCommand.execute(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"), StringArgumentType
                                                                .getString(ctx, "tm")))));
        commandDispatcher.register(command);
    }
}
