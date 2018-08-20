package com.mcf.davidee.nbteditpqb;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;

public class TileEntityHelper {
	
	public static <T extends TileEntity> void copyData(T from, T into) throws Exception{
		Class<?> clazz = from.getClass();
		Set<Field> fields = asSet(clazz.getFields(),clazz.getDeclaredFields());
		Field modifiers = Field.class.getDeclaredField("modifiers");
		modifiers.setAccessible(true);
		for (Field field : fields){
			field.setAccessible(true);
			modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			field.set(into, field.get(from));
		}
	}
	
	public static Set<Field> asSet(Field[] a, Field[] b){
		HashSet<Field> s = new HashSet<>();
		Collections.addAll(s, a);
		Collections.addAll(s, b);
		return s;
	}
	
}
