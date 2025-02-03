package com.github.kozakura913.fedstorage;

public class FluidStack {
	String id;//name+nbt_hash
	String name;
	long count;
	byte[] nbt;
	@Override
	public String toString() {
		return id+"("+count+")";
	}
}
