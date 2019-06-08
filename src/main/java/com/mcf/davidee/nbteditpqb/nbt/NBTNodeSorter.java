package com.mcf.davidee.nbteditpqb.nbt;

import java.util.Comparator;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;

public class NBTNodeSorter implements Comparator<Node<NamedNBT>>{

	@Override
	public int compare(Node<NamedNBT> a, Node<NamedNBT> b) {
		INBT n1 = a.getObject().getNBT(), n2 = b.getObject().getNBT();
		String s1 = a.getObject().getName(), s2 = b.getObject().getName();
		if (n1 instanceof CompoundNBT || n1 instanceof ListNBT){
			if (n2 instanceof CompoundNBT || n2 instanceof ListNBT){
				int dif = n1.getId() - n2.getId();
				return (dif == 0) ? s1.compareTo(s2) : dif;
			}
			return 1;
		}
		if (n2 instanceof CompoundNBT || n2 instanceof ListNBT)
			return -1;
		int dif =n1.getId() - n2.getId();
		return (dif == 0) ? s1.compareTo(s2) : dif;
	}

}
