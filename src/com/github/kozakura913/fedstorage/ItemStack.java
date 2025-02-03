package com.github.kozakura913.fedstorage;

public class ItemStack {
	byte[] nbt;
	String name;
	int count;
	int damage;
	@Override
	public String toString() {
		return name+"("+count+")";
	}
}
