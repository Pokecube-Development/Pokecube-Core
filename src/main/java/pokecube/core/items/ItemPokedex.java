/**
 *
 */
package pokecube.core.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;
import pokecube.core.PokecubeItems;
import pokecube.core.database.Database;
import pokecube.core.database.Pokedex;
import pokecube.core.handlers.events.SpawnHandler;
import pokecube.core.handlers.playerdata.PokecubePlayerStats;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.network.packets.PacketDataSync;
import pokecube.core.network.packets.PacketPokedex;
import pokecube.core.utils.Tools;
import thut.api.maths.Vector3;
import thut.core.common.commands.CommandTools;
import thut.core.common.handlers.PlayerDataHandler;
import thut.core.common.network.TerrainUpdate;

/** @author Manchou */
public class ItemPokedex extends Item
{
    public final boolean watch;

    public ItemPokedex(Properties props, boolean watch)
    {
        super(props);
        this.watch = watch;
        if (PokecubeItems.POKECUBE_ITEMS.isEmpty()) PokecubeItems.POKECUBE_ITEMS = new ItemStack(this);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand)
    {
        if (playerIn instanceof ServerPlayerEntity)
        {
            final IChunk chunk = playerIn.getEntityWorld().getChunk(playerIn.getPosition());
            TerrainUpdate.sendTerrainToClient(playerIn.getEntityWorld(), new ChunkPos(chunk.getPos().x, chunk
                    .getPos().z), (ServerPlayerEntity) playerIn);
            PacketDataSync.sendInitPacket(playerIn, "pokecube-stats");
            PacketPokedex.sendSecretBaseInfoPacket((ServerPlayerEntity) playerIn, this.watch);
            final Entity entityHit = target;
            final IPokemob pokemob = CapabilityPokemob.getPokemobFor(entityHit);
            if (pokemob != null) PlayerDataHandler.getInstance().getPlayerData(playerIn).getData(
                    PokecubePlayerStats.class).inspect(playerIn, pokemob);
            PacketPokedex.sendOpenPacket((ServerPlayerEntity) playerIn, pokemob, this.watch);
            return true;
        }
        return super.itemInteractionForEntity(stack, playerIn, target, hand);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        final ItemStack itemstack = player.getHeldItem(hand);
        if (!world.isRemote) SpawnHandler.refreshTerrain(Vector3.getNewVector().set(player), player.getEntityWorld());
        if (!player.isSneaking())
        {
            this.showGui(player);
            return new ActionResult<>(ActionResultType.SUCCESS, itemstack);
        }
        else
        {
            final Vector3 hit = Tools.getPointedLocation(player, 6);
            if (hit != null)
            {
                // Block block = hit.getBlockState(world).getBlock();
                // TODO healing table setting teleports
                // if (block instanceof BlockHealTable)
                // {
                // Vector4 loc = new Vector4(player);
                // TeleportHandler.setTeleport(loc,
                // player.getCachedUniqueIdString());
                // if (!world.isRemote)
                // {
                // CommandTools.sendMessage(player, "pokedex.setteleport");
                // PacketDataSync.sendInitPacket(player, "pokecube-data");
                // }
                // return new ActionResult<ItemStack>(ActionResultType.SUCCESS,
                // itemstack);
                // }
            }
        }
        return new ActionResult<>(ActionResultType.SUCCESS, itemstack);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context)
    {
        final World worldIn = context.getWorld();
        final PlayerEntity playerIn = context.getPlayer();
        // BlockPos pos = context.getPos();
        // Vector3 hit = Vector3.getNewVector().set(pos);
        // Block block = hit.getBlockState(worldIn).getBlock();
        // TODO healing table setting teleports
        // if (block instanceof BlockHealTable)
        // {
        // Vector4 loc = new Vector4(playerIn);
        // TeleportHandler.setTeleport(loc, playerIn.getCachedUniqueIdString());
        // if (!worldIn.isRemote)
        // {
        // CommandTools.sendMessage(playerIn, "pokedex.setteleport");
        // PacketDataSync.sendInitPacket(playerIn, "pokecube-data");
        // }
        // return ActionResultType.SUCCESS;
        // }

        if (playerIn.isSneaking() && !worldIn.isRemote)
        {
            ITextComponent message = CommandTools.makeTranslatedMessage("pokedex.locationinfo1", "green",
                    Database.spawnables.size());
            playerIn.sendMessage(message);
            message = CommandTools.makeTranslatedMessage("pokedex.locationinfo2", "green", Pokedex.getInstance()
                    .getEntries().size());
            playerIn.sendMessage(message);
            message = CommandTools.makeTranslatedMessage("pokedex.locationinfo3", "green", Pokedex.getInstance()
                    .getRegisteredEntries().size());
            playerIn.sendMessage(message);
        }

        if (!playerIn.isSneaking()) this.showGui(playerIn);
        return ActionResultType.FAIL;
    }

    private void showGui(PlayerEntity player)
    {
        if (player instanceof ServerPlayerEntity)
        {
            final IChunk chunk = player.getEntityWorld().getChunk(player.getPosition());
            TerrainUpdate.sendTerrainToClient(player.getEntityWorld(), new ChunkPos(chunk.getPos().x, chunk.getPos().z),
                    (ServerPlayerEntity) player);
            PacketDataSync.sendInitPacket(player, "pokecube-stats");
            PacketPokedex.sendSecretBaseInfoPacket((ServerPlayerEntity) player, this.watch);
            final Entity entityHit = Tools.getPointedEntity(player, 16);
            final IPokemob pokemob = CapabilityPokemob.getPokemobFor(entityHit);
            if (pokemob != null) PlayerDataHandler.getInstance().getPlayerData(player).getData(
                    PokecubePlayerStats.class).inspect(player, pokemob);
            PacketPokedex.sendOpenPacket((ServerPlayerEntity) player, pokemob, this.watch);
        }
    }

}
