package com.github.kozakura913.fedstorage.json;

public class Items {
    private String name;
    private int count;
    private String nbt;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getNbt() {
        return this.nbt;
    }

    public void setNbt(String nbt) {
        this.nbt = nbt;
    }    
}
