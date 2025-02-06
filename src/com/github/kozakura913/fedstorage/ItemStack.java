package com.github.kozakura913.fedstorage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ItemStack {
	byte[] nbt;
	String name;
	int count;
	int damage;

	@Override
	public String toString() {
		return name+"("+count+")";
	}

	public void write(DataOutputStream resp_dos) throws IOException {
		resp_dos.writeUTF(name);	//アイテムID
		resp_dos.writeInt(damage);	//ダメージ値
		resp_dos.writeInt(count);	//スタックサイズ

		if (nbt == null) {
			resp_dos.writeShort(0);
		} else {
			resp_dos.writeShort(nbt.length);
			resp_dos.write(nbt);
		}
	}

	public void read(DataInputStream dis) throws IOException {
		name = dis.readUTF();
		damage = dis.readInt();
		count = dis.readInt();

		int nbt_length = dis.readShort();

		if (nbt_length > 0) {
			byte[] nbt = new byte[nbt_length];
			dis.readFully(nbt);
			this.nbt = nbt;
		}
	}
}
