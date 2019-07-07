package pokecube.core.interfaces.pokemob;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.EvolutionData;
import pokecube.core.database.abilities.Ability;
import pokecube.core.database.abilities.AbilityManager;
import pokecube.core.entity.pokemobs.genetics.GeneticsManager;
import pokecube.core.events.pokemob.EvolveEvent;
import pokecube.core.handlers.playerdata.advancements.triggers.Triggers;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.HappinessType;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.moves.MovesUtils;
import pokecube.core.network.pokemobs.PacketSyncNewMoves;
import pokecube.core.network.pokemobs.PokemobPacketHandler.MessageServer;
import pokecube.core.utils.EntityTools;
import thut.core.common.network.EntityUpdate;

public interface ICanEvolve extends IHasEntry, IHasOwner
{
    /** Used to allow the pokemob to evolve over a few ticks */
    static class EvoTicker
    {
        /** take 10 ticks to evolve to give time to clean things up first. */
        int                tick = 10;
        /** Who we are evolving to. */
        final LivingEntity evo;
        /** UUID to evolve into. */
        final UUID         id;

        public EvoTicker(final LivingEntity evolution, final UUID id)
        {
            this.evo = evolution;
            this.id = id;
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void tick(final WorldTickEvent evt)
        {
            if (evt.world != this.evo.getEntityWorld() || evt.phase != Phase.END || evt.world.isRemote()) return;
            final ServerWorld world = (ServerWorld) evt.world;
            final boolean exists = world.getEntityByUuid(this.id) != null;
            if (!exists && this.tick-- <= 0)
            {
                this.evo.setUniqueId(this.id);
                EntityUpdate.sendEntityUpdate(this.evo);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }

    /** Simlar to EvoTicker, but for more general form changing. */
    static class MegaEvoTicker
    {
        final World          world;
        final Entity         mob;
        IPokemob             pokemob;
        final PokedexEntry   mega;
        final ITextComponent message;
        final long           evoTime;
        boolean              set = false;

        MegaEvoTicker(final PokedexEntry mega, final long evoTime, final IPokemob evolver, final ITextComponent message)
        {
            this.mob = evolver.getEntity();
            this.world = this.mob.getEntityWorld();
            this.evoTime = this.world.getGameTime() + evoTime;
            this.message = message;
            this.mega = mega;
            this.pokemob = evolver;

            // Flag as evolving
            this.pokemob.setGeneralState(GeneralStates.EVOLVING, true);
            this.pokemob.setGeneralState(GeneralStates.EXITINGCUBE, false);
            this.pokemob.setEvolutionTicks(PokecubeCore.getConfig().evolutionTicks + 50);

            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void tick(final WorldTickEvent evt)
        {
            if (evt.world != this.world || evt.phase != Phase.END) return;
            if (!this.mob.addedToChunk || !this.mob.isAlive())
            {
                MinecraftForge.EVENT_BUS.unregister(this);
                return;
            }
            if (evt.world.getGameTime() >= this.evoTime)
            {
                if (this.pokemob.getCombatState(CombatStates.MEGAFORME) && this.pokemob
                        .getOwner() instanceof ServerPlayerEntity) Triggers.MEGAEVOLVEPOKEMOB.trigger(
                                (ServerPlayerEntity) this.pokemob.getOwner(), this.pokemob);
                final int evoTicks = this.pokemob.getEvolutionTicks();
                final float hp = this.pokemob.getHealth();
                this.pokemob = this.pokemob.megaEvolve(this.mega);
                this.pokemob.setHealth(hp);
                /**
                 * Flag the new mob as evolving to continue the animation
                 * effects.
                 */
                this.pokemob.setGeneralState(GeneralStates.EVOLVING, true);
                this.pokemob.setGeneralState(GeneralStates.EXITINGCUBE, false);
                this.pokemob.setEvolutionTicks(evoTicks);

                if (this.message != null) this.pokemob.displayMessageToOwner(this.message);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }

    public static final ResourceLocation EVERSTONE = new ResourceLocation("pokecube:everstone");

    /**
     * Shedules mega evolution for a few ticks later
     *
     * @param evolver
     *            the mob to schedule to evolve
     * @param newForm
     *            the form to evolve to
     * @param message
     *            the message to send on completion
     */
    public static void setDelayedMegaEvolve(final IPokemob evolver, final PokedexEntry newForm,
            final ITextComponent message)
    {
        new MegaEvoTicker(newForm, PokecubeCore.getConfig().evolutionTicks / 2, evolver, message);
    }

    /**
     * Cancels the current evoluton for the pokemob, sends appropriate message
     * to owner.
     */
    default void cancelEvolve()
    {
        if (!this.isEvolving()) return;
        final LivingEntity entity = this.getEntity();
        if (this.getEntity().getEntityWorld().isRemote)
        {
            final MessageServer message = new MessageServer(MessageServer.CANCELEVOLVE, entity.getEntityId());
            PokecubeCore.packets.sendToServer(message);
            return;
        }
        this.setEvolutionTicks(-1);
        this.setGeneralState(GeneralStates.EVOLVING, false);
        this.displayMessageToOwner(new TranslationTextComponent("pokemob.evolution.cancel", CapabilityPokemob
                .getPokemobFor(entity).getDisplayName()));
    }

    /**
     * Called when give item. to override when the pokemob evolve with a stone.
     *
     * @param itemId
     *            the shifted index of the item
     * @return whether should evolve
     */
    default boolean canEvolve(final ItemStack stack)
    {
        if (PokecubeItems.is(ICanEvolve.EVERSTONE, stack)) return false;
        if (this.getPokedexEntry().canEvolve() && this.getEntity().isServerWorld()) for (final EvolutionData d : this
                .getPokedexEntry().getEvolutions())
            if (d.shouldEvolve((IPokemob) this, stack)) return true;
        return false;
    }

    /**
     * Evolve the pokemob.
     *
     * @param delayed
     *            true if we want to display the evolution animation
     * @return the evolution or this if the evolution failed
     */
    default IPokemob evolve(final boolean delayed, final boolean init)
    {
        final LivingEntity thisEntity = this.getEntity();
        final IPokemob thisMob = CapabilityPokemob.getPokemobFor(thisEntity);
        return this.evolve(delayed, init, thisMob.getHeldItem());
    }

    /**
     * Evolve the pokemob.
     *
     * @param delayed
     *            true if we want to display the evolution animation
     * @param init
     *            true if this is called during initialization of the mob
     * @param stack
     *            the itemstack to check for evolution.
     * @return the evolution or null if the evolution failed, or this if the
     *         evolution succeeded, but delayed.
     */
    default IPokemob evolve(final boolean delayed, final boolean init, final ItemStack stack)
    {
        final LivingEntity thisEntity = this.getEntity();
        final IPokemob thisMob = CapabilityPokemob.getPokemobFor(thisEntity);
        // If Init, then don't bother about getting ready for animations and
        // such, just evolve directly.
        if (init)
        {
            boolean neededItem = false;
            PokedexEntry evol = null;
            EvolutionData data = null;
            // Find which evolution to use.
            for (final EvolutionData d : this.getPokedexEntry().getEvolutions())
                if (d.shouldEvolve(thisMob, stack))
                {
                    evol = d.evolution;
                    if (!d.shouldEvolve(thisMob, ItemStack.EMPTY)) neededItem = true;
                    data = d;
                    break;
                }
            if (evol != null)
            {
                // Send evolve event.
                EvolveEvent evt = new EvolveEvent.Pre(thisMob, evol);
                PokecubeCore.POKEMOB_BUS.post(evt);
                if (evt.isCanceled()) return null;
                // change to new forme.
                final IPokemob evo = this.megaEvolve(((EvolveEvent.Pre) evt).forme);
                // Remove held item if it had one.
                if (neededItem && stack == thisMob.getHeldItem()) evo.setHeldItem(ItemStack.EMPTY);
                // Init things like moves.
                evo.getMoveStats().oldLevel = data.level - 1;
                evo.levelUp(evo.getLevel());

                // Learn evolution moves and update ability.
                for (final String s : evo.getPokedexEntry().getEvolutionMoves())
                    evo.learn(s);
                evo.setAbility(evo.getPokedexEntry().getAbility(thisMob.getAbilityIndex(), evo));

                // Send post evolve event.
                evt = new EvolveEvent.Post(evo);
                PokecubeCore.POKEMOB_BUS.post(evt);
                // Kill old entity.
                if (evo != this) this.getEntity().remove();
                return evo;
            }
            return null;
        }
        // Do not evolve if it is dead, or can't evolve.
        else if (this.getPokedexEntry().canEvolve() && thisEntity.isAlive())
        {
            boolean neededItem = false;
            PokedexEntry evol = null;
            EvolutionData data = null;
            // look for evolution data to use.
            for (final EvolutionData d : this.getPokedexEntry().getEvolutions())
                if (d.shouldEvolve(thisMob, stack))
                {
                    evol = d.evolution;
                    if (!d.shouldEvolve(thisMob, ItemStack.EMPTY) && stack == thisMob.getHeldItem()) neededItem = true;
                    data = d;
                    break;
                }

            if (evol != null)
            {
                EvolveEvent evt = new EvolveEvent.Pre(thisMob, evol);
                MinecraftForge.EVENT_BUS.post(evt);
                if (evt.isCanceled()) return null;
                if (delayed)
                {
                    // If delayed, set the pokemob as starting to evolve, and
                    // set the evolution for display effects.
                    if (stack != ItemStack.EMPTY) this.setEvolutionStack(stack.copy());
                    this.setEvolutionTicks(PokecubeCore.getConfig().evolutionTicks + 50);
                    this.setEvolvingEffects(evol);
                    this.setGeneralState(GeneralStates.EVOLVING, true);
                    // Send the message about evolving, to let user cancel.
                    this.displayMessageToOwner(new TranslationTextComponent("pokemob.evolution.start", thisMob
                            .getDisplayName()));
                    return thisMob;
                }
                // Evolve the mob.
                final IPokemob evo = this.megaEvolve(((EvolveEvent.Pre) evt).forme);
                if (evo != null)
                {
                    // Clear held item if used for evolving.
                    if (neededItem) evo.setHeldItem(ItemStack.EMPTY);
                    evt = new EvolveEvent.Post(evo);
                    MinecraftForge.EVENT_BUS.post(evt);
                    // Lean any moves that should are supposed to have just
                    // learnt.
                    if (delayed) evo.getMoveStats().oldLevel = evo.getLevel() - 1;
                    else if (data != null) evo.getMoveStats().oldLevel = data.level - 1;
                    evo.levelUp(evo.getLevel());

                    // Don't immediately try evolving again, only wild ones
                    // should do that.
                    evo.setEvolutionTicks(-1);
                    evo.setGeneralState(GeneralStates.EVOLVING, false);

                    // Learn evolution moves and update ability.
                    for (final String s : evo.getPokedexEntry().getEvolutionMoves())
                        evo.learn(s);
                    evo.setAbility(evo.getPokedexEntry().getAbility(thisMob.getAbilityIndex(), evo));

                    // Kill old entity.
                    if (evo != this) thisEntity.remove();
                }
                return evo;
            }
        }
        return null;
    }

    /** This entry is used for colouring evolution effects. */
    default PokedexEntry getEvolutionEntry()
    {
        return this.getPokedexEntry();
    }

    /**
     * This is the itemstack we are using for evolution, it is stored here for
     * use when evolution actually occurs.
     */
    ItemStack getEvolutionStack();

    /** @return if we are currently evolving */
    default boolean isEvolving()
    {
        return this.getGeneralState(GeneralStates.EVOLVING);
    }

    /**
     * Called when the level is up. Should be overridden to handle level up
     * events like evolution or move learning.
     *
     * @param level
     *            the new level
     */
    default IPokemob levelUp(final int level)
    {
        final LivingEntity theEntity = this.getEntity();
        final IPokemob theMob = CapabilityPokemob.getPokemobFor(theEntity);
        final List<String> moves = Database.getLevelUpMoves(theMob.getPokedexEntry(), level, theMob
                .getMoveStats().oldLevel);
        Collections.shuffle(moves);
        if (!theEntity.getEntityWorld().isRemote)
        {
            final ITextComponent mess = new TranslationTextComponent("pokemob.info.levelup", theMob.getDisplayName(),
                    level + "");
            theMob.displayMessageToOwner(mess);
        }
        HappinessType.applyHappiness(theMob, HappinessType.LEVEL);
        if (moves != null)
        {
            if (theMob.getGeneralState(GeneralStates.TAMED))
            {
                final String[] current = theMob.getMoves();
                if (current[3] != null)
                {
                    for (final String s : current)
                    {
                        if (s == null) continue;
                        for (final String s1 : moves)
                            if (s.equals(s1))
                            {
                                moves.remove(s1);
                                break;
                            }
                    }
                    for (final String s : moves)
                    {
                        final ITextComponent move = new TranslationTextComponent(MovesUtils.getUnlocalizedMove(s));
                        final ITextComponent mess = new TranslationTextComponent("pokemob.move.notify.learn", theMob
                                .getDisplayName(), move);
                        theMob.displayMessageToOwner(mess);
                        if (!theMob.getMoveStats().newMoves.contains(s))
                        {
                            theMob.getMoveStats().newMoves.add(s);
                            PacketSyncNewMoves.sendUpdatePacket((IPokemob) this);
                        }
                    }
                    EntityUpdate.sendEntityUpdate(this.getEntity());
                    return theMob;
                }
            }
            for (final String s : moves)
                theMob.learn(s);
        }
        return theMob;
    }

    /**
     * Converts us to the given entry
     *
     * @param newEntry
     *            new pokedex entry to have
     * @return the new pokemob, return this if it fails
     */
    default IPokemob megaEvolve(final PokedexEntry newEntry)
    {
        final LivingEntity thisEntity = this.getEntity();
        final IPokemob thisMob = CapabilityPokemob.getPokemobFor(thisEntity);
        LivingEntity evolution = thisEntity;
        IPokemob evoMob = thisMob;
        final PokedexEntry oldEntry = this.getPokedexEntry();
        if (newEntry != null && newEntry != oldEntry)
        {
            this.setGeneralState(GeneralStates.EVOLVING, true);

            evolution = PokecubeCore.createPokemob(newEntry, thisEntity.getEntityWorld());
            if (evolution == null)
            {
                System.err.println("No Entry for " + newEntry);
                return thisMob;
            }
            EntityTools.copyEntityTransforms(evolution, thisEntity);
            evoMob = CapabilityPokemob.getPokemobFor(evolution);
            // Reset nickname if needed.
            if (this.getPokemonNickname().equals(oldEntry.getName())) this.setPokemonNickname("");

            // Copy nbt tag over
            final CompoundNBT tag = new CompoundNBT();
            thisEntity.writeAdditional(tag);
            evoMob.getEntity().readAdditional(tag);

            // Flag the mob as evolving.
            evoMob.setGeneralState(GeneralStates.EVOLVING, true);

            GeneticsManager.handleEpigenetics(evoMob);

            evoMob.onGenesChanged();

            // Set entry, this should fix expressed species gene.
            evoMob.setPokedexEntry(newEntry);

            // Sync ability back, or store old ability.
            if (this.getCombatState(CombatStates.MEGAFORME))
            {
                if (thisMob.getAbility() != null) evolution.getEntityData().putString("Ability", thisMob.getAbility()
                        .toString());
                final Ability ability = newEntry.getAbility(0, evoMob);
                if (PokecubeMod.debug) PokecubeCore.LOGGER.info("Mega Evolving, changing ability to " + ability);
                if (ability != null) evoMob.setAbility(ability);
            }
            else if (thisEntity.getEntityData().contains("Ability"))
            {
                final String ability = thisEntity.getEntityData().getString("Ability");
                evolution.getEntityData().remove("Ability");
                if (!ability.isEmpty()) evoMob.setAbility(AbilityManager.getAbility(ability));
                if (PokecubeMod.debug) PokecubeCore.LOGGER.info("Un Mega Evolving, changing ability back to "
                        + ability);
            }

            // Set this mob wild, then kill it.
            this.setOwner((UUID) null);

            final EvolveEvent evt = new EvolveEvent.Post(evoMob);
            PokecubeCore.POKEMOB_BUS.post(evt);

            // Schedule adding to world.
            if (!evt.isCanceled() && thisEntity.addedToChunk)
            {
                thisEntity.remove();

                evolution.getEntityWorld().addEntity(evolution);

                // Remount riders on the new mob.
                final List<Entity> riders = thisEntity.getPassengers();
                for (final Entity e : riders)
                {
                    e.stopRiding();
                    e.startRiding(evolution);
                }

                new EvoTicker(evolution, thisEntity.getUniqueID());
                EntityUpdate.sendEntityUpdate(evolution);
            }
        }
        return evoMob;
    }

    /**
     * This itemstack will be used to evolve the pokemob after evolutionTicks
     * runs out.
     */
    void setEvolutionStack(ItemStack stack);

    /**
     * The evolution tick will be set when the mob evolves and then is
     * decreased each tick. It is used to render a special effect.
     *
     * @param evolutionTicks
     *            the evolutionTicks to set
     */
    void setEvolutionTicks(int evolutionTicks);

    /** Can set a custom entry for use with colouring the evolution effects. */
    default void setEvolvingEffects(final PokedexEntry entry)
    {

    }

    // // Start by syncing all of the capabilities, we will override some
    // // later. TODO delete this if needed
    // CapabilityDispatcher caps_old =
    // ReflectionHelper.getPrivateValue(Entity.class, thisEntity,
    // "capabilities");
    // CapabilityDispatcher caps_new =
    // ReflectionHelper.getPrivateValue(Entity.class, evolution,
    // "capabilities");
    // caps_new.deserializeNBT(caps_old.serializeNBT());
    //
    // // Sync tags besides the ones that define species and form.
    // CompoundNBT tag = thisMob.writePokemobData();
    // tag.getCompound(TagNames.OWNERSHIPTAG).remove(TagNames.POKEDEXNB);
    // tag.getCompound(TagNames.VISUALSTAG).remove(TagNames.FORME);
    // evoMob.readPokemobData(tag);
    //
    // // Sync held item
    // evoMob.setHeldItem(thisMob.getHeldItem());
    //
    // // Sync genes
    // IMobGenetics oldGenes =
    // thisEntity.getCapability(GeneRegistry.GENETICS_CAP, null).orElse(null);
    // IMobGenetics newGenes =
    // evolution.getCapability(GeneRegistry.GENETICS_CAP, null).orElse(null);
    // newGenes.getAlleles().putAll(oldGenes.getAlleles());
    //
}
