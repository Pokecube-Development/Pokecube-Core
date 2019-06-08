package com.mcf.davidee.nbteditpqb.nbt;

import net.minecraft.nbt.INBT;

public class NamedNBT {
	
	protected String name;
	protected INBT nbt;
	
	public NamedNBT(INBT nbt) {
		this("", nbt);
	}
	
	public NamedNBT(String name, INBT nbt) {
		this.name = name;
		this.nbt = nbt;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public INBT getNBT() {
		return nbt;
	}
	
	public void setNBT(INBT nbt) {
		this.nbt = nbt;
	}
	
	public NamedNBT copy() {
		return new NamedNBT(name, nbt.copy());
	}
	
}
