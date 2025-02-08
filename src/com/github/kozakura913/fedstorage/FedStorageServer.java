package com.github.kozakura913.fedstorage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FedStorageServer {

	static HashMap<String,ArrayList<ItemStack>> item_buffers = new HashMap<>();
	static HashMap<String,HashMap<String,FluidStack>> fluid_buffers = new HashMap<>();

	public static final long VERSION = 4;
	public static final String VERSION_STRING = "4.0";

	public static void main(String[] args) throws IOException {
		new Thread(FedStorageServer::server,"Server").start();
		CLI.main(args);
	}

	private static void server() {
		try (ServerSocket server = new java.net.ServerSocket(3030)) {
			System.out.println("ServerStart! v" + VERSION_STRING);
			new Thread(HttpServer::start).start();

			while(true) {
				Socket soc = server.accept();
				soc.setSoTimeout(10000);//10s
				new Thread(()->{
					try {
						System.out.println("client connect");
						new ClientSession(soc);
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

	public static final String hash(byte[] bb) {
		if (bb == null)
			return "";

		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] result = md5.digest(bb);
			StringBuffer sb = new StringBuffer();

			for (int j = 0; j < result.length; j++){
				int i = (int)result[j] & 0xff;

				if (i <= 15){
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
}
