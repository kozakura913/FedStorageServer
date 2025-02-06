package com.github.kozakura913.fedstorage;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class ClientSession extends Thread {

	private static final byte DATA_FREQUENCY = 1;
	private static final byte DATA_ITEM_RECEIVE = 2;
	private static final byte DATA_ITEM_SEND = 3;
	private static final byte DATA_FLUID_RECEIVE = 4;
	private static final byte DATA_FLUID_SEND = 5;

	private String freq="";
	private DataInputStream soc_dis;
	private DataOutputStream soc_dos;

	private volatile boolean shutdown = false;

	public ClientSession(Socket soc) throws IOException {
		soc_dis = new DataInputStream(soc.getInputStream());
		soc_dos = new DataOutputStream(new BufferedOutputStream(soc.getOutputStream()));
		soc_dos.writeLong(FedStorageServer.VERSION);
		soc_dos.flush();
	}

	public void Shutdown() {
		shutdown = true;
	}

	public void run() {
		System.out.println("client connect");
		try {
			while(!shutdown) {
				int command = soc_dis.readByte();
				switch(command){
					case DATA_FREQUENCY:
						freq=soc_dis.readUTF();
						break;
					case DATA_ITEM_RECEIVE:
						itemRecv();
						break;
					case DATA_ITEM_SEND:
						itemSend();
						break;
					case DATA_FLUID_RECEIVE:
						fluidRecv();
						break;
					case DATA_FLUID_SEND:
						fluidSend();
						break;
					default: //NOP
						break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("client exit");
			return;
		}
		System.out.println("client exit");
		return;
	}

	private void fluidSend() throws IOException {
		String request_fluid_name=soc_dis.readUTF();
		long available=soc_dis.readLong();
		byte[] request_fluid_nbt=null;
		int nbt_length=soc_dis.readShort();
		if(nbt_length>0) {
			byte[] nbt=new byte[nbt_length];
			soc_dis.readFully(nbt);
			request_fluid_nbt=nbt;
		}
		HashMap<String, FluidStack> freq_buffer;
		synchronized(FedStorageServer.fluid_buffers) {
			freq_buffer = FedStorageServer.fluid_buffers.get(freq);
			if(freq_buffer==null) {
				freq_buffer=new HashMap<>();
				FedStorageServer.fluid_buffers.put(freq, freq_buffer);
			}
		}
		FluidStack ret_fs=null;
		synchronized(freq_buffer) {
			FluidStack buffer=null;
			if(request_fluid_name==null||request_fluid_name.isEmpty()) {
				Collection<FluidStack> values = freq_buffer.values();
				if(!values.isEmpty()) {
					buffer = values.iterator().next();
				}
			}else {
				String id=request_fluid_name+","+FedStorageServer.hash(request_fluid_nbt);
				buffer = freq_buffer.get(id);
			}
			if(buffer!=null) {
				if(buffer.count>0) {
					ret_fs=new FluidStack();
					ret_fs.name=buffer.name;
					ret_fs.count=Math.min(available,buffer.count);
					ret_fs.nbt=buffer.nbt;
					buffer.count-=ret_fs.count;
					if(buffer.count<=0) {
						freq_buffer.remove(buffer.id);
					}
				}
			}
		}
		if(ret_fs==null) {
			soc_dos.writeInt(0);
			soc_dos.flush();
			return;
		}
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		DataOutputStream resp_dos=new DataOutputStream(baos);
		ret_fs.write(resp_dos);
		byte[] bb=baos.toByteArray();
		int send_length=bb.length;
		soc_dos.writeInt(send_length);
		soc_dos.write(bb);
		soc_dos.flush();
	}

	private void fluidRecv() throws IOException {
		FluidStack fs=new FluidStack();
		fs.read(soc_dis);
		System.out.println("fluidRecv,"+fs.name+"@"+fs.count+"mb");
		HashMap<String, FluidStack> freq_buffer;
		synchronized(FedStorageServer.fluid_buffers) {
			freq_buffer = FedStorageServer.fluid_buffers.get(freq);
			if(freq_buffer==null) {
				freq_buffer=new HashMap<>();
				FedStorageServer.fluid_buffers.put(freq, freq_buffer);
			}
		}
		synchronized(freq_buffer) {
			FluidStack old_fs = freq_buffer.get(fs.id);
			if(old_fs!=null) {
				old_fs.count+=fs.count;
				fs.count=old_fs.count;
			}else {
				freq_buffer.put(fs.id,fs);
			}
		}
	}

	public void itemRecv() throws IOException {
		int item_count=soc_dis.readInt();
		ArrayList<ItemStack> queue=new ArrayList<>();
		for(int i=0;i<item_count;i++) {
			ItemStack is=new ItemStack();
			is.read(soc_dis);
			queue.add(is);
		}
		ArrayList<ItemStack> freq_buffer;
		synchronized(FedStorageServer.item_buffers) {
			freq_buffer = FedStorageServer.item_buffers.get(freq);
			if(freq_buffer==null) {
				freq_buffer=new ArrayList<>();
				FedStorageServer.item_buffers.put(freq, freq_buffer);
			}
		}
		int reject_start=0;
		synchronized(freq_buffer) {
			reject_start=Math.min(Math.max(0,10-freq_buffer.size()),queue.size());
			freq_buffer.addAll(queue.subList(0,reject_start));
		}
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		DataOutputStream resp_dos=new DataOutputStream(baos);
		resp_dos.writeInt(queue.size()-reject_start);
		for(int i=reject_start;i<queue.size();i++) {
			resp_dos.writeInt(i);
		}
		byte[] bb=baos.toByteArray();
		int send_length=bb.length;
		soc_dos.writeInt(send_length);
		soc_dos.write(bb);
		soc_dos.flush();
	}
	public void itemSend() throws IOException {
		int max_stacks=soc_dis.readInt();
		ArrayList<ItemStack> queue=null;
		ArrayList<ItemStack> freq_buffer;
		synchronized(FedStorageServer.item_buffers) {
			freq_buffer = FedStorageServer.item_buffers.get(freq);
			if(freq_buffer!=null) {
				queue=new ArrayList<>();
			}
		}
		if(queue==null) {
			soc_dos.writeInt(0);
			soc_dos.flush();
			return;
		}
		synchronized(freq_buffer) {
			for(int i=0;i<max_stacks;i++) {
				if(freq_buffer.isEmpty())break;
				queue.add(freq_buffer.remove(0));
			}
		}
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		DataOutputStream resp_dos=new DataOutputStream(baos);
		resp_dos.writeInt(queue.size());
		for(ItemStack is : queue) {
			is.write(resp_dos);
		}
		resp_dos.flush();
		byte[] bb=baos.toByteArray();
		soc_dos.writeInt(bb.length);
		soc_dos.write(bb);
		soc_dos.flush();
	}
}
