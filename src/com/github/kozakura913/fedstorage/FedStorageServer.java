package com.github.kozakura913.fedstorage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FedStorageServer {

	static HashMap<String,ArrayList<ItemStack>> item_buffers=new HashMap<>();
	static HashMap<String,HashMap<String,FluidStack>> fluid_buffers=new HashMap<>();
	private static final long VERSION=3;
	private static final String VERSION_STRING="3.6";

	public static void main(String[] args) throws IOException {
		new Thread(FedStorageServer::server,"Server").start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			String command=reader.readLine();
			if("save".equals(command)) {
				System.out.println("saveing...");
				long start=System.currentTimeMillis();
				try(GZIPOutputStream gzf = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream("save.dat.gz")))){
					save(gzf);
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("saved "+(System.currentTimeMillis()-start)+"ms");
			}else if("load".equals(command)) {
				System.out.println("loading...");
				long start=System.currentTimeMillis();
				try(GZIPInputStream gzf = new GZIPInputStream(new BufferedInputStream(new FileInputStream("save.dat.gz")))){
					load(gzf);
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("loaded "+(System.currentTimeMillis()-start)+"ms");
			}else if("stop".equals(command)) {
				System.out.println("Save And Exit...");
				try(GZIPInputStream gzf = new GZIPInputStream(new BufferedInputStream(new FileInputStream("save.dat.gz")))){
					load(gzf);
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Bye");
				System.exit(0);
			}else {
				System.out.println("Command Not Found");
			}
		}
	}
	private static void server() {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static void load(GZIPInputStream gzf) throws IOException {
		DataInputStream dis = new DataInputStream(gzf);
		synchronized(fluid_buffers) {
			fluid_buffers.clear();
			int fluid_freq_count=dis.readInt();
			for(int freq=0;freq<fluid_freq_count;freq++) {
				String id=dis.readUTF();
				HashMap<String, FluidStack> freq_buffer=new HashMap<>();
				int stacks=dis.readInt();
				for(int f=0;f<stacks;f++) {
					FluidStack fs = new FluidStack();
					fs.id=dis.readUTF();
					fs.name=dis.readUTF();
					fs.count=dis.readLong();
					fs.nbt=new byte[dis.readInt()];
					dis.readFully(fs.nbt);
					freq_buffer.put(fs.id,fs);
				}
				fluid_buffers.put(id,freq_buffer);
			}
		}
		synchronized(item_buffers) {
			item_buffers.clear();
			int fluid_freq_count=dis.readInt();
			for(int freq=0;freq<fluid_freq_count;freq++) {
				String id=dis.readUTF();
				ArrayList<ItemStack> freq_buffer=new ArrayList<>();
				int stacks=dis.readInt();
				for(int f=0;f<stacks;f++) {
					ItemStack is = new ItemStack();
					is.name=dis.readUTF();
					is.count=(int) Math.min(dis.readLong(),Integer.MAX_VALUE);
					is.damage=dis.readInt();
					is.nbt=new byte[dis.readInt()];
					dis.readFully(is.nbt);
					freq_buffer.add(is);
				}
				item_buffers.put(id,freq_buffer);
			}
		}
	}
	private static void save(GZIPOutputStream gzf) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		synchronized(fluid_buffers) {
			dos.writeInt(fluid_buffers.size());
			for(Entry<String, HashMap<String, FluidStack>> freq_buffer:fluid_buffers.entrySet()) {
				dos.writeUTF(freq_buffer.getKey());
				HashMap<String, FluidStack> map = freq_buffer.getValue();
				dos.writeInt(map.size());
				for(FluidStack fs:map.values()) {
					dos.writeUTF(fs.id);
					dos.writeUTF(fs.name);
					dos.writeLong(fs.count);
					dos.writeInt(fs.nbt.length);
					dos.write(fs.nbt);
				}
			}
		}
		dos.flush();
		baos.writeTo(gzf);
		baos.reset();
		synchronized(item_buffers) {
			dos.writeInt(item_buffers.size());
			for(Entry<String, ArrayList<ItemStack>> freq_buffer:item_buffers.entrySet()) {
				dos.writeUTF(freq_buffer.getKey());
				ArrayList<ItemStack> list = freq_buffer.getValue();
				dos.writeInt(list.size());
				for(ItemStack is:list) {
					dos.writeUTF(is.name);
					dos.writeLong(is.count);
					dos.writeInt(is.damage);
					dos.writeInt(is.nbt.length);
					dos.write(is.nbt);
				}
			}
		}
		dos.flush();
		baos.writeTo(gzf);
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
