package pokecube.core.interfaces.capabilities.impl;

import java.util.UUID;
import java.util.logging.Level;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.utils.TagNames;
import thut.lib.CompatWrapper;

public abstract class PokemobSaves extends PokemobOwned implements TagNames
{
    private void cleanLoadedAIStates()
    {
        // First clear out any non-persistant ai states from logic states
        for (LogicStates state : LogicStates.values())
        {
            if (!state.persists()) this.setLogicState(state, false);
        }
        // Then clean up general states
        for (GeneralStates state : GeneralStates.values())
        {
            if (!state.persists()) this.setGeneralState(state, false);
        }
        // Finally cleanup combat states
        for (CombatStates state : CombatStates.values())
        {
            if (!state.persists()) this.setCombatState(state, false);
        }
    }

    @Override
    public void readPokemobData(CompoundNBT tag)
    {
        CompoundNBT ownerShipTag = tag.getCompound(OWNERSHIPTAG);
        CompoundNBT statsTag = tag.getCompound(STATSTAG);
        CompoundNBT movesTag = tag.getCompound(MOVESTAG);
        CompoundNBT inventoryTag = tag.getCompound(INVENTORYTAG);
        CompoundNBT breedingTag = tag.getCompound(BREEDINGTAG);
        CompoundNBT visualsTag = tag.getCompound(VISUALSTAG);
        CompoundNBT aiTag = tag.getCompound(AITAG);
        CompoundNBT miscTag = tag.getCompound(MISCTAG);
        // Read Ownership Tag
        if (!ownerShipTag.hasNoTags())
        {
            this.setPokemobTeam(ownerShipTag.getString(TEAM));
            this.setPokemonNickname(ownerShipTag.getString(NICKNAME));
            this.players = ownerShipTag.getBoolean(PLAYERS);
            try
            {
                if (ownerShipTag.hasKey(OT)) this.setOriginalOwnerUUID(UUID.fromString(ownerShipTag.getString(OT)));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try
            {
                if (ownerShipTag.hasKey(OWNER)) this.setPokemonOwner(UUID.fromString(ownerShipTag.getString(OWNER)));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        // Read stats tag
        if (!statsTag.hasNoTags())
        {
            this.setExp(statsTag.getInt(EXP), false);
            this.setStatus(statsTag.getByte(STATUS));
            addHappiness(statsTag.getInt(HAPPY));
        }
        // Read moves tag
        if (!movesTag.hasNoTags())
        {
            getMoveStats().newMoves.clear();
            if (movesTag.hasKey(NEWMOVES))
            {
                try
                {
                    ListNBT newMoves = (ListNBT) movesTag.getTag(NEWMOVES);
                    for (int i = 0; i < newMoves.size(); i++)
                        if (!getMoveStats().newMoves.contains(newMoves.getStringTagAt(i)))
                            getMoveStats().newMoves.add(newMoves.getStringTagAt(i));
                }
                catch (Exception e)
                {
                    PokecubeMod.log(Level.WARNING, "Error loading new moves for " + getEntity().getName(), e);
                }
            }
            this.setMoveIndex(movesTag.getInt(MOVEINDEX));
            this.setAttackCooldown(movesTag.getInt(COOLDOWN));
            int[] disables = movesTag.getIntArray(DISABLED);
            if (disables.length == 4) for (int i = 0; i < 4; i++)
            {
                setDisableTimer(i, disables[i]);
            }
        }
        // Read Inventory tag
        if (!inventoryTag.hasNoTags())
        {
            ListNBT ListNBT = inventoryTag.getTagList(ITEMS, 10);
            for (int i = 0; i < ListNBT.size(); ++i)
            {
                CompoundNBT CompoundNBT1 = ListNBT.getCompound(i);
                int j = CompoundNBT1.getByte("Slot") & 255;
                if (j < this.getPokemobInventory().getSizeInventory())
                {
                    this.getPokemobInventory().setInventorySlotContents(j, new ItemStack(CompoundNBT1));
                }
                this.setHeldItem(this.getPokemobInventory().getStackInSlot(1));
            }
        }
        // Read Breeding tag
        if (!breedingTag.hasNoTags())
        {
            this.loveTimer = breedingTag.getInt(SEXETIME);
        }
        // Read visuals tag
        if (!visualsTag.hasNoTags())
        {
            dataSync().set(params.SPECIALINFO, visualsTag.getInt(SPECIALTAG));
            setSize((float) (getSize() / PokecubeMod.core.getConfig().scalefactor));
            int[] flavourAmounts = visualsTag.getIntArray(FLAVOURSTAG);
            if (flavourAmounts.length == 5) for (int i = 0; i < flavourAmounts.length; i++)
            {
                setFlavourAmount(i, flavourAmounts[i]);
            }
            if (visualsTag.hasKey(POKECUBE))
            {
                CompoundNBT pokecubeTag = visualsTag.getCompound(POKECUBE);
                this.setPokecube(new ItemStack(pokecubeTag));
            }
        }

        // Read AI
        if (!aiTag.hasNoTags())
        {
            setTotalCombatState(aiTag.getInt(COMBATSTATE));
            setTotalGeneralState(aiTag.getInt(GENERALSTATE));
            setTotalLogicState(aiTag.getInt(LOGICSTATE));
            cleanLoadedAIStates();

            setHungerTime(aiTag.getInt(HUNGER));
            CompoundNBT routines = aiTag.getCompound(AIROUTINES);
            for (String s : routines.getKeySet())
            {
                // try/catch block incase addons add more routines to the enum.
                try
                {
                    AIRoutine routine = AIRoutine.valueOf(s);
                    setRoutineState(routine, routines.getBoolean(s));
                }
                catch (Exception e)
                {

                }
            }
        }
        // Read Misc other
        if (!miscTag.hasNoTags())
        {
            this.setRNGValue(miscTag.getInt(RNGVAL));
            this.uid = miscTag.getInt(UID);
            this.wasShadow = miscTag.getBoolean(WASSHADOW);
        }
    }

    @Override
    public CompoundNBT writePokemobData()
    {
        CompoundNBT pokemobTag = new CompoundNBT();
        pokemobTag.putInt(VERSION, 1);
        // Write Ownership tag
        CompoundNBT ownerShipTag = new CompoundNBT();
        // This is still written for pokecubes to read from. Actual number is
        // stored in genes.
        ownerShipTag.putInt(POKEDEXNB, this.getPokedexNb());
        ownerShipTag.putString(NICKNAME, getPokemonNickname());
        ownerShipTag.putBoolean(PLAYERS, isPlayerOwned());
        ownerShipTag.putString(TEAM, getPokemobTeam());
        if (getOriginalOwnerUUID() != null) ownerShipTag.putString(OT, getOriginalOwnerUUID().toString());
        if (getPokemonOwnerID() != null) ownerShipTag.putString(OWNER, getPokemonOwnerID().toString());

        // Write stats tag
        CompoundNBT statsTag = new CompoundNBT();
        statsTag.putInt(EXP, getExp());
        statsTag.setByte(STATUS, getStatus());
        statsTag.putInt(HAPPY, bonusHappiness);

        // Write moves tag
        CompoundNBT movesTag = new CompoundNBT();
        movesTag.putInt(MOVEINDEX, getMoveIndex());
        if (!getMoveStats().newMoves.isEmpty())
        {
            ListNBT newMoves = new ListNBT();
            for (String s : getMoveStats().newMoves)
            {
                newMoves.appendTag(new StringNBT(s));
            }
            movesTag.put(NEWMOVES, newMoves);
        }
        movesTag.putInt(COOLDOWN, getAttackCooldown());
        int[] disables = new int[4];
        boolean tag = false;
        for (int i = 0; i < 4; i++)
        {
            disables[i] = getDisableTimer(i);
            tag = tag || disables[i] > 0;
        }
        if (tag)
        {
            movesTag.putIntArray(DISABLED, disables);
        }

        // Write Inventory tag
        CompoundNBT inventoryTag = new CompoundNBT();
        ListNBT ListNBT = new ListNBT();
        this.getPokemobInventory().setInventorySlotContents(1, this.getHeldItem());
        for (int i = 0; i < this.getPokemobInventory().getSizeInventory(); ++i)
        {
            ItemStack itemstack = this.getPokemobInventory().getStackInSlot(i);
            if (CompatWrapper.isValid(itemstack))
            {
                CompoundNBT CompoundNBT1 = new CompoundNBT();
                CompoundNBT1.setByte("Slot", (byte) i);
                itemstack.writeToNBT(CompoundNBT1);
                ListNBT.appendTag(CompoundNBT1);
            }
        }
        inventoryTag.put(ITEMS, ListNBT);

        // Write Breeding tag
        CompoundNBT breedingTag = new CompoundNBT();
        breedingTag.putInt(SEXETIME, loveTimer);

        // Write visuals tag
        CompoundNBT visualsTag = new CompoundNBT();

        // This is still written for pokecubes to read from. Actual form is
        // stored in genes.
        visualsTag.putString(FORME, getPokedexEntry().getTrimmedName());
        visualsTag.putInt(SPECIALTAG, dataSync().get(params.SPECIALINFO));
        int[] flavourAmounts = new int[5];
        for (int i = 0; i < flavourAmounts.length; i++)
        {
            flavourAmounts[i] = getFlavourAmount(i);
        }
        visualsTag.putIntArray(FLAVOURSTAG, flavourAmounts);
        if (CompatWrapper.isValid(getPokecube()))
        {
            CompoundNBT pokecubeTag = getPokecube().writeToNBT(new CompoundNBT());
            visualsTag.put(POKECUBE, pokecubeTag);
        }
        // Misc AI
        CompoundNBT aiTag = new CompoundNBT();

        aiTag.putInt(GENERALSTATE, getTotalGeneralState());
        aiTag.putInt(LOGICSTATE, getTotalLogicState());
        aiTag.putInt(COMBATSTATE, getTotalCombatState());

        aiTag.putInt(HUNGER, getHungerTime());
        CompoundNBT aiRoutineTag = new CompoundNBT();
        for (AIRoutine routine : AIRoutine.values())
        {
            aiRoutineTag.putBoolean(routine.toString(), isRoutineEnabled(routine));
        }
        aiTag.put(AIROUTINES, aiRoutineTag);

        // Misc other
        CompoundNBT miscTag = new CompoundNBT();
        miscTag.putInt(RNGVAL, getRNGValue());
        miscTag.putInt(UID, getPokemonUID());
        miscTag.putBoolean(WASSHADOW, wasShadow);

        // Set tags to the pokemob tag.
        pokemobTag.put(OWNERSHIPTAG, ownerShipTag);
        pokemobTag.put(STATSTAG, statsTag);
        pokemobTag.put(MOVESTAG, movesTag);
        pokemobTag.put(INVENTORYTAG, inventoryTag);
        pokemobTag.put(BREEDINGTAG, breedingTag);
        pokemobTag.put(VISUALSTAG, visualsTag);
        pokemobTag.put(AITAG, aiTag);
        pokemobTag.put(MISCTAG, miscTag);
        return pokemobTag;
    }

}
