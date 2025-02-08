package com.github.kozakura913.fedstorage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CLI {
	private static long SAVE_DATA_FORMAT = 1;

	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while(true) {
			String command = reader.readLine();

			if ("save".equals(command)) {
				System.out.println("saveing...");
				long start = System.currentTimeMillis();
				save();
				System.out.println("saved " + (System.currentTimeMillis() - start) + "ms");

			} else if ("load".equals(command)) {
				System.out.println("loading...");
				long start = System.currentTimeMillis();
				load();
				System.out.println("loaded " + (System.currentTimeMillis() - start) + "ms");

			} else if ("stop".equals(command)) {
				System.out.println("Save And Exit...");
				save();
				System.out.println("Bye");
				System.exit(0);

			} else {
				System.out.println("Command Not Found");
			}
		}
	}
	private static void save() {
		try(GZIPOutputStream gzf = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream("save.dat.gz")))){
			save(gzf);

		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	private static void load() {
		try(GZIPInputStream gzf = new GZIPInputStream(new BufferedInputStream(new FileInputStream("save.dat.gz")))){
			load(gzf);

		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	private static void load(GZIPInputStream gzf) throws IOException {
		DataInputStream dis = new DataInputStream(gzf);
		if (dis.readLong() != SAVE_DATA_FORMAT) {
			System.err.println("Bad Data Format Version");
			return;

		}

		synchronized(FedStorageServer.fluid_buffers) {
			FedStorageServer.fluid_buffers.clear();
			int fluid_freq_count = dis.readInt();

			for(int freq = 0; freq < fluid_freq_count; freq++) {
				String id = dis.readUTF();
				HashMap<String, FluidStack> freq_buffer = new HashMap<>();
				int stacks = dis.readInt();

				for(int f = 0; f < stacks; f++) {
					FluidStack fs = new FluidStack();
					fs.read(dis);
					freq_buffer.put(fs.id,fs);

				}

				FedStorageServer.fluid_buffers.put(id,freq_buffer);

			}
		}

		synchronized(FedStorageServer.item_buffers) {
			FedStorageServer.item_buffers.clear();
			int fluid_freq_count = dis.readInt();

			for(int freq = 0; freq < fluid_freq_count; freq++) {
				String id = dis.readUTF();
				ArrayList<ItemStack> freq_buffer = new ArrayList<>();
				int stacks = dis.readInt();

				for(int f = 0; f < stacks; f++) {
					ItemStack is = new ItemStack();
					is.read(dis);
					freq_buffer.add(is);

				}

				FedStorageServer.item_buffers.put(id,freq_buffer);
			}
		}
	}
	private static void save(GZIPOutputStream gzf) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		dos.writeLong(SAVE_DATA_FORMAT);

		synchronized(FedStorageServer.fluid_buffers) {
			dos.writeInt(FedStorageServer.fluid_buffers.size());

			for(Entry<String, HashMap<String, FluidStack>> freq_buffer : FedStorageServer.fluid_buffers.entrySet()) {
				dos.writeUTF(freq_buffer.getKey());
				HashMap<String, FluidStack> map = freq_buffer.getValue();
				dos.writeInt(map.size());

				for(FluidStack fs : map.values()) {
					fs.write(dos);

				}

			}
		}

		dos.flush();
		baos.writeTo(gzf);
		baos.reset();

		synchronized(FedStorageServer.item_buffers) {
			dos.writeInt(FedStorageServer.item_buffers.size());

			for(Entry<String, ArrayList<ItemStack>> freq_buffer : FedStorageServer.item_buffers.entrySet()) {
				dos.writeUTF(freq_buffer.getKey());
				ArrayList<ItemStack> list = freq_buffer.getValue();
				dos.writeInt(list.size());

				for(ItemStack is : list) {
					is.write(dos);

				}

			}
		}

		dos.flush();
		baos.writeTo(gzf);
	}
}
