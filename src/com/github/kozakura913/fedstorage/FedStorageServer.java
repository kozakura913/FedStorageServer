package com.github.kozakura913.fedstorage;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FedStorageServer {

	static HashMap<String,ArrayList<ItemStack>> item_buffers=new HashMap<>();
	static HashMap<String,HashMap<String,FluidStack>> fluid_buffers=new HashMap<>();
	private static final long VERSION=3;
	private static final String VERSION_STRING="3.3";

	public static void main(String[] args) throws IOException {
		try (ServerSocket server = new java.net.ServerSocket(3030)) {
			System.out.println("ServerStart! v"+VERSION_STRING);
			new Thread(HttpServer::start).start();
			while(true) {
				Socket soc = server.accept();
				soc.setSoTimeout(10000);//10s
				new Thread(()->{
					try {
						System.out.println("client connect");
						client(soc);
						System.out.println("client exit");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}).start();
			}
		}
	}

	private static void client(Socket soc) throws IOException {
		DataInputStream dis = new DataInputStream(soc.getInputStream());
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(soc.getOutputStream()));
		dos.writeLong(VERSION);
		dos.flush();
		String freq="";
		while(true) {
			int command=dis.readByte();
			if(command==-1);//NOP
			if(command==1)freq=dis.readUTF();
			if(command==2)itemRecv(dis,dos,freq);
			if(command==3)itemSend(dis,dos,freq);
			if(command==4)fluidRecv(dis,dos,freq);
			if(command==5)fluidSend(dis,dos,freq);
		}
	}
	private static void fluidSend(DataInputStream dis, DataOutputStream dos, String freq) throws IOException {
		String request_fluid_name=dis.readUTF();
		int available=dis.readInt();
		byte[] request_fluid_nbt=null;
		int nbt_length=dis.readShort();
		if(nbt_length>0) {
			byte[] nbt=new byte[nbt_length];
			dis.readFully(nbt);
			request_fluid_nbt=nbt;
		}
		HashMap<String, FluidStack> freq_buffer;
		synchronized(fluid_buffers) {
			freq_buffer = fluid_buffers.get(freq);
			if(freq_buffer==null) {
				freq_buffer=new HashMap<>();
				fluid_buffers.put(freq, freq_buffer);
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
				String id=request_fluid_name+","+hash(request_fluid_nbt);
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
			dos.writeInt(0);
			dos.flush();
			return;
		}
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		DataOutputStream resp_dos=new DataOutputStream(baos);
		resp_dos.writeUTF(ret_fs.name);
		resp_dos.writeInt((int)ret_fs.count);
		if(ret_fs.nbt==null) {
			resp_dos.writeShort(0);
		}else {
			resp_dos.writeShort(ret_fs.nbt.length);
			resp_dos.write(ret_fs.nbt);
		}
		byte[] bb=baos.toByteArray();
		int send_length=bb.length;
		dos.writeInt(send_length);
		dos.write(bb);
		dos.flush();
	}

	private static void fluidRecv(DataInputStream dis, DataOutputStream dos, String freq) throws IOException {
		FluidStack fs=new FluidStack();
		fs.name=dis.readUTF();
		fs.count=dis.readInt();
		System.out.println("fluidRecv,"+fs.name+"@"+fs.count+"mb");
		int nbt_length=dis.readShort();
		if(nbt_length>0) {
			byte[] nbt=new byte[nbt_length];
			dis.readFully(nbt);
			fs.nbt=nbt;
		}
		HashMap<String, FluidStack> freq_buffer;
		synchronized(fluid_buffers) {
			freq_buffer = fluid_buffers.get(freq);
			if(freq_buffer==null) {
				freq_buffer=new HashMap<>();
				fluid_buffers.put(freq, freq_buffer);
			}
		}
		fs.id=fs.name+","+hash(fs.nbt);
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
	private static String hash(byte[] bb) {
		if(bb==null)return "";
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] result = md5.digest(bb);
			StringBuffer sb = new StringBuffer();
			for (int j=0; j < result.length; j++){
				int i = (int)result[j] & 0xff;
				if (i<=15){
					sb.append("0");
				}
				sb.append(Integer.toHexString(i));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static void itemRecv(DataInputStream dis,DataOutputStream dos,String freq) throws IOException {
		int item_count=dis.readInt();
		ArrayList<ItemStack> queue=new ArrayList<>();
		for(int i=0;i<item_count;i++) {
			ItemStack is=new ItemStack();
			is.name=dis.readUTF();
			is.damage=dis.readInt();
			is.count=dis.readInt();
			int nbt_length=dis.readShort();
			if(nbt_length>0) {
				byte[] nbt=new byte[nbt_length];
				dis.readFully(nbt);
				is.nbt=nbt;
			}
			queue.add(is);
		}
		ArrayList<ItemStack> freq_buffer;
		synchronized(item_buffers) {
			freq_buffer = item_buffers.get(freq);
			if(freq_buffer==null) {
				freq_buffer=new ArrayList<>();
				item_buffers.put(freq, freq_buffer);
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
		dos.writeInt(send_length);
		dos.write(bb);
		dos.flush();
	}
	public static void itemSend(DataInputStream dis,DataOutputStream dos,String freq) throws IOException {
		int max_stacks=dis.readInt();
		ArrayList<ItemStack> queue=null;
		ArrayList<ItemStack> freq_buffer;
		synchronized(item_buffers) {
			freq_buffer = item_buffers.get(freq);
			if(freq_buffer!=null) {
				queue=new ArrayList<>();
			}
		}
		if(queue==null) {
			dos.writeInt(0);
			dos.flush();
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
			resp_dos.writeUTF(is.name);//アイテムID
			resp_dos.writeInt(is.damage);//ダメージ値
			resp_dos.writeInt(is.count);//スタックサイズ
			if(is.nbt==null) {
				resp_dos.writeShort(0);
			}else {
				resp_dos.writeShort(is.nbt.length);
				resp_dos.write(is.nbt);
			}
		}
		resp_dos.flush();
		byte[] bb=baos.toByteArray();
		dos.writeInt(bb.length);
		dos.write(bb);
		dos.flush();
	}
}
