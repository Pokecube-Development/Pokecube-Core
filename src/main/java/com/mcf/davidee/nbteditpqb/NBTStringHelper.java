package com.mcf.davidee.nbteditpqb;

import com.google.common.base.Strings;
import com.mcf.davidee.nbteditpqb.nbt.NamedNBT;

import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.ByteNBTArray;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.nbt.ShortNBT;
import net.minecraft.nbt.StringNBT;

public class NBTStringHelper {

	public static final char SECTION_SIGN = '\u00A7';

	public static String getNBTName(NamedNBT namedNBT){
		String name = namedNBT.getName();
		INBT obj = namedNBT.getNBT();

		String s = toString(obj);
		return Strings.isNullOrEmpty(name) ? "" + s : name + ": " + s;
	}

	public static String getNBTNameSpecial(NamedNBT namedNBT){
		String name = namedNBT.getName();
		INBT obj = namedNBT.getNBT();

		String s = toString(obj);
		return Strings.isNullOrEmpty(name) ? "" + s : name + ": " + s + SECTION_SIGN + 'r';
	}

	public static INBT newTag(byte type){
		switch (type)
		{
		case 0:
			return new NBTTagEnd2();
		case 1:
			return new ByteNBT((byte) 0);
		case 2:
			return new ShortNBT();
		case 3:
			return new IntNBT(0);
		case 4:
			return new LongNBT(0);
		case 5:
			return new FloatNBT(0);
		case 6:
			return new DoubleNBT(0);
		case 7:
			return new ByteNBTArray(new byte[0]);
		case 8:
			return new StringNBT("");
		case 9:
			return new ListNBT();
		case 10:
			return new CompoundNBT();
		case 11:
			return new IntArrayNBT(new int[0]);
		default:
			return null;
		}
	}

	public static String toString(INBT base) {
		switch(base.getId()) {
		case 1:
			return "" + ((ByteNBT)base).getByte();
		case 2:
			return "" + ((ShortNBT)base).getShort();
		case 3:
			return "" + ((IntNBT)base).getInt();
		case 4:
			return "" + ((LongNBT)base).getLong();
		case 5:
			return "" + ((FloatNBT)base).getFloat();
		case 6:
			return "" + ((DoubleNBT)base).getDouble();
		case 7:
			return base.toString();
		case 8:
			return ((StringNBT)base).getString();
		case 9:
			return "(TagList)";
		case 10:
			return "(TagCompound)";
		case 11:
			return base.toString();
		default:
			return "?";
		}
	}

	public static String getButtonName(byte id){
		switch(id){
		case 1 :
			return "Byte";
		case 2: 
			return "Short";
		case 3:
			return "Int";
		case 4:
			return "Long";
		case 5:
			return "Float";
		case 6:
			return "Double";
		case 7: 
			return "Byte[]";
		case 8:
			return "String";
		case 9:
			return "List";
		case 10:
			return "Compound";
		case 11:
			return "Int[]";
		case 12:
			return "Edit";
		case 13:
			return "Delete";
		case 14:
			return "Copy";
		case 15:
			return "Cut";
		case 16:
			return "Paste";
		default:
			return "Unknown";
		}
	}
	
	private static class NBTTagEnd2 extends NBTTagEnd
	{
	    public NBTTagEnd2()
        {
        }
	}
}
