package com.github.kozakura913.fedstorage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FluidStack {
	String id; //name + nbt_hash
	String name;
	long count;
	byte[] nbt;

	@Override
	public String toString() {
		return id + "(" + count + ")";
	}

	public void resetId() {
		id = name + "," + FedStorageServer.hash(nbt);
	}

	public void read(DataInputStream dis) throws IOException {
		name = dis.readUTF();
		count = dis.readLong();
		int nbt_length = dis.readShort();
		if (nbt_length > 0) {
			byte[] nbt = new byte[nbt_length];
			dis.readFully(nbt);
			this.nbt = nbt;
		}
		resetId();
	}

	public void write(DataOutputStream resp_dos) throws IOException {
		resp_dos.writeUTF(name);
		resp_dos.writeLong(count);

		if (nbt == null) {
			resp_dos.writeShort(0);
		} else {
			resp_dos.writeShort(nbt.length);
			resp_dos.write(nbt);
		}
	}
}
